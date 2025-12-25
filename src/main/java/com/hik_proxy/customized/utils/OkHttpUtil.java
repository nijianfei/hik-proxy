package com.hik_proxy.customized.utils;

import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OkHttpUtil {

    private static final int CONNECT_TIMEOUT = 3; // 连接超时（秒）
    private static final int READ_TIMEOUT = 30;    // 读取超时（秒）
    private static final int WRITE_TIMEOUT = 30;   // 写入超时（秒）

    private static OkHttpClient client;
    private static Gson gson = new Gson();

    static {
        // 添加日志拦截器（可选）
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC); // 可设为 NONE/BASIC/HEADERS/BODY

        client = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor) // 启用日志（开发阶段建议开启）
                .build();
    }

    // ==================== GET 请求 ====================
    public static String get(String url) throws IOException {
        return get(url, null);
    }

    public static String get(String url, Map<String, String> headers) throws IOException {
        Request.Builder builder = new Request.Builder().url(url);
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        Request request = builder.build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body().string();
        }
    }

    // ==================== POST 表单 ====================
    public static String postForm(String url, Map<String, String> formParams) throws IOException {
        return postForm(url, formParams, null);
    }

    public static String postForm(String url, Map<String, String> formParams, Map<String, String> headers) throws IOException {
        FormBody.Builder formBuilder = new FormBody.Builder();
        if (formParams != null) {
            for (Map.Entry<String, String> entry : formParams.entrySet()) {
                formBuilder.add(entry.getKey(), entry.getValue());
            }
        }

        Request.Builder builder = new Request.Builder().url(url).post(formBuilder.build());
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        Request request = builder.build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body().string();
        }
    }

    // ==================== POST JSON ====================
    public static String postJson(String url, Object jsonBody) throws IOException {
        return postJson(url, jsonBody, null);
    }

    public static String postJson(String url, Object jsonBody, Map<String, String> headers) throws IOException {
        String json = gson.toJson(jsonBody);
        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));

        Request.Builder builder = new Request.Builder().url(url).post(body);
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        Request request = builder.build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body().string();
        }
    }

    // ==================== PUT JSON ====================
    public static String putJson(String url, Object jsonBody) throws IOException {
        return putJson(url, jsonBody, null);
    }

    public static String putJson(String url, Object jsonBody, Map<String, String> headers) throws IOException {
        String json = gson.toJson(jsonBody);
        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));

        Request.Builder builder = new Request.Builder().url(url).put(body);
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        Request request = builder.build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body().string();
        }
    }

    // ==================== DELETE ====================
    public static String delete(String url) throws IOException {
        return delete(url, null);
    }

    public static String delete(String url, Map<String, String> headers) throws IOException {
        Request.Builder builder = new Request.Builder().url(url).delete();
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        Request request = builder.build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body().string();
        }
    }

    /**
     * GET 方式下载文件
     *
     * @param url         下载地址
     * @param destFile    目标文件（需包含完整路径和文件名）
     * @param headers     请求头（可为 null）
     * @throws IOException
     */
    public static void downloadFile(String url, File destFile, Map<String, String> headers) throws IOException {
        Request.Builder builder = new Request.Builder().url(url);
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        Request request = builder.build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Download failed: " + response.code() + " " + response.message());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Empty response body");
            }

            // 确保父目录存在
            Files.createDirectories(destFile.toPath().getParent());

            try (InputStream is = body.byteStream();
                 FileOutputStream fos = new FileOutputStream(destFile)) {
                byte[] buffer = new byte[8192]; // 8KB buffer
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
                fos.flush();
            }
        }
    }

    /**
     * GET 方式下载文件（简化版，无 headers）
     */
    public static void downloadFile(String url, File destFile) throws IOException {
        downloadFile(url, destFile, null);
    }

    /**
     * POST 方式下载文件（例如带参数/Token 的下载接口）
     *
     * @param url         下载地址
     * @param formParams  表单参数（可为 null）
     * @param destFile    目标文件
     * @param headers     请求头（可为 null）
     * @throws IOException
     */
    public static void downloadFileByPost(String url, Map<String, String> formParams, File destFile, Map<String, String> headers) throws IOException {
        FormBody.Builder formBuilder = new FormBody.Builder();
        if (formParams != null) {
            for (Map.Entry<String, String> entry : formParams.entrySet()) {
                formBuilder.add(entry.getKey(), entry.getValue());
            }
        }

        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(formBuilder.build());

        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        Request request = builder.build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Download failed: " + response.code() + " " + response.message());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Empty response body");
            }

            Files.createDirectories(destFile.toPath().getParent());

            try (InputStream is = body.byteStream();
                 FileOutputStream fos = new FileOutputStream(destFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
                fos.flush();
            }
        }
    }

    /**
     * POST 下载（无 headers，无 formParams 的简化调用）
     */
    public static void downloadFileByPost(String url, File destFile) throws IOException {
        downloadFileByPost(url, null, destFile, null);
    }

    /**
     * POST 下载（仅带 formParams）
     */
    public static void downloadFileByPost(String url, Map<String, String> formParams, File destFile) throws IOException {
        downloadFileByPost(url, formParams, destFile, null);
    }

    // ==================== 获取 OkHttpClient 实例（高级用法）====================
    public static OkHttpClient getClient() {
        return client;
    }
}