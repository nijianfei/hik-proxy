package com.hik_proxy.customized.task;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.google.common.collect.Lists;
import com.hik_proxy.customized.dto.param.PlaybackParam;
import com.hik_proxy.customized.entity.DownloadVideoInfoEntity;
import com.hik_proxy.customized.enums.TaskCodeEnum;
import com.hik_proxy.customized.mapper.DownloadVideoInfoMapper;
import com.hik_proxy.customized.utils.FFmpegUtil;
import com.hik_proxy.customized.utils.HikHttpUtil;
import com.hik_proxy.customized.vo.BaseVo;
import com.hik_proxy.customized.vo.playback.PlaybackInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Slf4j
@Component
public class VideoDownloadTask {

    @Value("${download.host}")
    private String downloadHost;
    @Value("${download.task.maxCount:1}")
    private Integer downloadTaskMaxCount;
    @Autowired
    private DownloadVideoInfoMapper mapper;

    @Autowired
    @Qualifier("videoTaskExecutor")
    private ThreadPoolTaskExecutor videoTaskExecutor;

    @Autowired
    @Qualifier("taskSliceExecutor")
    private ThreadPoolTaskExecutor taskSliceExecutor;

    @Scheduled(initialDelay = 30 * 1000, fixedDelay = 60 * 1000) // 每60秒执行一次
    public void videoDownTask() {
        QueryWrapper<DownloadVideoInfoEntity> queryWrapper = new QueryWrapper();
        LocalDateTime lastDate = LocalDateTime.now().minusDays(1);
        //查询一天内,待下载的录像
        queryWrapper.ge("create_time", lastDate);
        queryWrapper.eq("status", TaskCodeEnum.T10.getCode());
        List<DownloadVideoInfoEntity> videoInfos = mapper.selectList(queryWrapper);
        Map<String, List<DownloadVideoInfoEntity>> reqGroup = videoInfos.stream().collect(Collectors.groupingBy(DownloadVideoInfoEntity::getReqNo));
        List<List<String>> partitionReqNos = Lists.partition(new ArrayList<>(reqGroup.keySet()), downloadTaskMaxCount);
        log.info("查得待下载录像任务:{}条,分组数:{}", reqGroup.size(), partitionReqNos);
        for (int i = 0; i < partitionReqNos.size(); i++) {
            List<String> reqNos = partitionReqNos.get(i);
            List<Future<?>> submitTasks = new ArrayList<>(reqNos.size());
            log.info("开始第{}组的下载任务{}条", i + 1, reqNos.size());
            StopWatch watch = StopWatch.createStarted();
            for (String reqNo : reqNos) {
                List<DownloadVideoInfoEntity> taskInfos = reqGroup.get(reqNo);
                if (updateTaskStatus(reqNo, taskInfos.size())) {
                    Future<?> submitTask = videoTaskExecutor.submit(() -> downVideo(taskInfos));
                    submitTasks.add(submitTask);
                } else {
                    log.error("reqNo:{},taskInfos.size:{} != updateCount,跳过", reqNo, taskInfos.size());
                }
            }
            for (Future<?> submitTask : submitTasks) {
                try {
                    submitTask.get();
                } catch (Exception e) {
                    log.error("submitTask.get():{}", e.getMessage(), e);
                }
            }
            watch.stop();
            log.info("第{}组的下载任务已结束,耗时:{}", i + 1, watch.getTime());
        }

    }

    private boolean updateTaskStatus(String reqNo, int batchCount) {
        UpdateWrapper updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("req_no", reqNo);
        updateWrapper.eq("status", TaskCodeEnum.T10.getCode());
        updateWrapper.set("status", TaskCodeEnum.T20.getCode());
        updateWrapper.set("update_time", new Date());
        int updateCount = mapper.update(updateWrapper);
        return batchCount == updateCount;
    }

    private void downVideo(List<DownloadVideoInfoEntity> taskInfos) {
        DownloadVideoInfoEntity v1 = taskInfos.get(0);
        String reqNo = v1.getReqNo();
        final String baseUrl;
        try {
            log.info("开始获取[{}]的录像下载链接", reqNo);
            PlaybackParam param = buildDownParam(v1, taskInfos.get(taskInfos.size() - 1).getVideoDateTo());
            String response = HikHttpUtil.playbackURLs(param);
            BaseVo<PlaybackInfoVo> baseVo = JSON.parseObject(response, new TypeReference<>() {
            });
            String url = baseVo.getData().getUrl();
            log.info("开始获取[{}]的录像下载链接,url:{}", reqNo, url);
            String splitUrl = url.split("\\?")[0].split("8950")[1];
            baseUrl = downloadHost + splitUrl;
            log.info("开始获取[{}]的录像下载链接,baseUrl:{}", reqNo, baseUrl);
        } catch (Exception e) {
            log.error("录像任务[{}]-获取下载url失败", reqNo, e.getMessage(), e);
            throw new RuntimeException(e);
        }

        log.info("开始异步下载录像任务{},共计{}条", reqNo, taskInfos.size());
        List<Future> futures = new ArrayList<>();
        for (final DownloadVideoInfoEntity taskInfo : taskInfos) {
            Future<?> submit = taskSliceExecutor.submit(() -> {
                File tempFile = null;
                try {
                    String fullUrl = String.format("%s?beginTime=%s&endTime=%s", baseUrl, taskInfo.getVideoDateFrom(), taskInfo.getVideoDateTo());
                    tempFile = new File(new File(taskInfo.getVideoPath(),"TEMP"), taskInfo.getVideoFilename());
                    log.info("开始发送请求:{},\r\n保存到临时目录:{}", fullUrl, tempFile);
                    long fileSize = HikHttpUtil.downloadVideo(fullUrl, tempFile.getParent(), taskInfo.getVideoFilename());
                    log.info("发送请求结束:{},\r\n保存到临时目录:{},下载完成,文件大小:{}", fullUrl, new File(tempFile.getParent(), taskInfo.getVideoFilename()), fileSize);
                    log.info("开始转码[{}]...",taskInfo.getVideoFilename());
                    fileSize = FFmpegUtil.convertToCompatibleMp4(tempFile, new File(taskInfo.getVideoPath(), taskInfo.getVideoFilename()));
                    log.info("转码结束[{}],文件大小:{}",taskInfo.getVideoFilename(),fileSize);
                    taskInfo.setStatus(TaskCodeEnum.T70.getCode());
                    taskInfo.setMsg(TaskCodeEnum.T70.getName());
                    taskInfo.setByteLength(fileSize);
                    taskInfo.setUpdateTime(new Date());
                    mapper.updateById(taskInfo);
                } catch (Exception e) {
                    log.error("录像任务[{}]-{},下载失败:{}", reqNo, taskInfo.getSeq(), e.getMessage(), e);
                    taskInfo.setStatus(TaskCodeEnum.T90.getCode());
                    taskInfo.setMsg(e.getMessage());
                    taskInfo.setUpdateTime(new Date());
                    mapper.updateById(taskInfo);
                }finally {
                    FileUtils.deleteQuietly(tempFile);
                }
            });
            futures.add(submit);
        }
        log.info("录像任务[{}],共计{}条,阻塞等待下载结果", reqNo, taskInfos.size());
        for (Future future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                log.error("future.get():{}", e.getMessage(), e);
            }
        }
        log.info("录像任务[{}],共计{}条,阻塞等待下载结果结束!!!", reqNo, taskInfos.size());
    }

    private PlaybackParam buildDownParam(DownloadVideoInfoEntity v1, String videoDateTo) {
        PlaybackParam param = new PlaybackParam();
        param.setCameraIndexCode(v1.getCameraIndexCode());
        OffsetDateTime startTime = OffsetDateTime.of(LocalDateTime.parse(v1.getVideoDateFrom(), DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")), ZoneOffset.ofHours(8));
        param.setBeginTime(startTime);
        OffsetDateTime endTime = OffsetDateTime.of(LocalDateTime.parse(videoDateTo, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")), ZoneOffset.ofHours(8));
        param.setEndTime(endTime);
        return param;
    }

}
