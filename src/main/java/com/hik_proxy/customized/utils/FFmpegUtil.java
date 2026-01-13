package com.hik_proxy.customized.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class FFmpegUtil {

    private static final Logger log = LoggerFactory.getLogger(FFmpegUtil.class);

    // 可配置：FFmpeg 超时时间（秒），防止卡死
    private static final int DEFAULT_TIMEOUT_SECONDS = 60 * 60; // 1小时

    /**
     * @param inputPath  输入文件路径
     * @param outputPath 输出文件路径
     * @return true 成功，false 失败
     * @throws IllegalArgumentException 输入参数非法
     */
    public static long convertToCompatibleMp4(File inputPath, File outputPath) {
        return convertToCompatibleMp4(inputPath, outputPath, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * 带超时控制的转换方法
     */
    public static long convertToCompatibleMp4(File inputFile, File outputFile, int timeoutSeconds) {
        // 参数校验
        if (inputFile == null) {
            throw new IllegalArgumentException("Input File cannot be null ");
        }
        if (outputFile == null) {
            throw new IllegalArgumentException("Output File cannot be null ");
        }

        Assert.isTrue(inputFile.exists() && inputFile.isFile(), String.format("Input file does not exist: %s", inputFile));

        // 构建 FFmpeg 命令（使用 List 避免空格/特殊字符问题）
        List<String> command = buildFFmpegCommand(inputFile, outputFile);

        log.info("Starting FFmpeg conversion: {} -> {}", inputFile, outputFile);
        log.debug("FFmpeg command: {}", String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true); // 合并 stderr 到 stdout

        Process process = null;
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            process = pb.start();
            Process finalProcess = process;

            // 异步读取输出（避免缓冲区满导致阻塞）
            StringBuilder outputLog = new StringBuilder();
            Future<?> readerFuture = executor.submit(() -> {
                try (var reader = new java.util.Scanner(finalProcess.getInputStream()).useDelimiter("\\A")) {
                    if (reader.hasNext()) {
                        outputLog.append(reader.next());
                    }
                } catch (Exception e) {
                    log.warn("Error reading FFmpeg output", e);
                }
            });
            log.warn("outputLog:{}", outputLog);
            // 等待进程结束（带超时）
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            readerFuture.get(10, TimeUnit.SECONDS); // 确保日志读完

            Assert.isTrue(finished, String.format("FFmpeg conversion timed out after %d seconds", timeoutSeconds));
            int exitCode = process.exitValue();
            Assert.isTrue(exitCode == 0, String.format("FFmpeg failed with exit code: %d, output:%s", exitCode, outputLog));
            return outputFile.length();
        } catch (IOException e) {
            log.error("Failed to start FFmpeg process: {}", e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("Error during FFmpeg execution: {}", e.getMessage(), e);
            if (process != null) {
                process.destroyForcibly();
            }
            Thread.currentThread().interrupt(); // 恢复中断状态
            throw new RuntimeException(e.getMessage());
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 构建 FFmpeg 命令参数列表（安全处理路径）
     */
    private static List<String> buildFFmpegCommand(File inputPath, File outputPath) {
        List<String> cmd = new ArrayList<>();
        cmd.add("ffmpeg");
        cmd.add("-y"); // 自动覆盖
        cmd.add("-i");
        cmd.add(inputPath.getAbsolutePath());

        // 视频滤镜：缩放 + 填充 + SAR
        cmd.add("-vf");
        cmd.add("scale=1920:1080:force_original_aspect_ratio=decrease,pad=1920:1080:(ow-iw)/2:(oh-ih)/2,setsar=1");

        // 视频编码
        cmd.add("-c:v");
        cmd.add("libx264");
        cmd.add("-profile:v");
        cmd.add("main");
        cmd.add("-level");
        cmd.add("40");
        cmd.add("-preset");
        cmd.add("fast");
        cmd.add("-b:v");
        cmd.add("1900k");
        cmd.add("-maxrate");
        cmd.add("2500k");
        cmd.add("-bufsize");
        cmd.add("3800k");
        cmd.add("-g");
        cmd.add("25");

        // 音频编码
        cmd.add("-c:a");
        cmd.add("aac");
        cmd.add("-b:a");
        cmd.add("64k");
        cmd.add("-ar");
        cmd.add("8000");
        cmd.add("-ac");
        cmd.add("1");
        cmd.add("-threads");
        cmd.add("4");

        // 容器优化
        cmd.add("-movflags");
        cmd.add("+faststart");

        cmd.add(outputPath.getAbsolutePath());
        return cmd;
    }

    /**
     * 检查系统是否安装了 FFmpeg（可选调用）
     */
    public static boolean isFFmpegAvailable() {
        try {
            Process p = new ProcessBuilder("ffmpeg", "-version").start();
            boolean finished = p.waitFor(5, TimeUnit.SECONDS);
            return finished && p.exitValue() == 0;
        } catch (Exception e) {
            log.warn("FFmpeg not available", e);
            return false;
        }
    }
}