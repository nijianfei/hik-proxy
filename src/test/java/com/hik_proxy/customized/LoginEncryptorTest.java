package com.hik_proxy.customized;

import com.hik_proxy.customized.utils.OkHttpUtil;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class LoginEncryptorTest {
    private static final String LOGIN_URL = "http://10.130.160.9/ac_portal/login.php"; // 替换为实际地址
    private static final MediaType JSON = MediaType.get("application/x-www-form-urlencoded; charset=utf-8");

    public static void main(String[] args) throws Exception {
        while (true) {
            boolean networkConnected = true;
            try {
                OkHttpUtil.get("https://www.baidu.com");
            } catch (IOException e) {
                System.out.println(e.getMessage());
                networkConnected = false;
            }
            if (!networkConnected) {
                System.out.println("网络连接异常,开始网络认证...");
                boolean flag = authentication();
                System.out.println("网络认证结果:" + flag);
            } else {
                System.out.println("网络连接正常,跳过本次网络认证...");
            }
            Thread.sleep(60 * 1000);
        }
    }

    private static boolean authentication() throws IOException {
        String username = "倪建飞";
        String password = "2222222";

        // 1. 生成 auth_tag（当前时间戳）
        String authTag = String.valueOf(System.currentTimeMillis());
        // 2. RC4 加密密码（输出 hex）
        String encryptedPwd = rc4EncryptToHex(password, authTag);
        // 3. 构造表单参数
        String formBody = "opr=pwdLogin" +
                "&userName=" + encodeURIComponent(username) +
                "&pwd=" + encryptedPwd +
                "&auth_tag=" + authTag +
                "&rememberPwd=0";

        // 4. 发起 POST 请求
        OkHttpClient client = new OkHttpClient.Builder()
                .followRedirects(false)
                .followSslRedirects(false)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(LOGIN_URL)
                .post(RequestBody.create(formBody, JSON))
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36")
                .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .addHeader("Referer", "http://10.130.160.9/ac_portal/default/pc.html?template=default&tabs=pwd&vlanid=1208&url=http://www.baidu.com")
                .addHeader("Origin", "http://10.130.160.9")
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("Accept", "*/*")
                .addHeader("Accept-Encoding", "gzip, deflate")
                .addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
                .addHeader("Cache-Control", "no-cache")
                .addHeader("Connection", "keep-alive")
                .addHeader("Pragma", "no-cache")
                .addHeader("Cookie", "Sessionid=3959514616-2")
                .build();

        try (Response response = client.newCall(request).execute()) {
            System.out.println("Status: " + response.code());
            String responseBody = response.body() != null ? response.body().string() : "";
            System.out.println("Response: " + responseBody);

            // 检查是否包含 success:true
            if (responseBody.contains("logon success")) {
                System.out.println("✅ 登录成功！");
                // 可提取 Cookie 用于后续请求
                String cookies = response.headers("Set-Cookie").toString();
                System.out.println("Cookies: " + cookies);
                return true;
            } else {
                System.out.println("❌ 登录失败");
                return false;
            }
        }

    }

    // === RC4 加密（输出 hex 字符串）===
    public static String rc4EncryptToHex(String input, String key) {
        input = input == null ? "" : input.trim();
        key = key == null ? "" : key;

        int[] sbox = new int[256];  // ← 使用 int[] 避免负数问题
        int[] keyArray = new int[256];

        // 初始化 keyArray
        for (int i = 0; i < 256; i++) {
            keyArray[i] = key.charAt(i % key.length());
        }

        // 初始化 sbox
        for (int i = 0; i < 256; i++) {
            sbox[i] = i;
        }

        // KSA
        int j = 0;
        for (int i = 0; i < 256; i++) {
            j = (j + sbox[i] + keyArray[i]) % 256;
            int temp = sbox[i];
            sbox[i] = sbox[j];
            sbox[j] = temp;
        }

        // PRGA
        StringBuilder hexOutput = new StringBuilder();
        int a = 0, b = 0;
        for (int i = 0; i < input.length(); i++) {
            a = (a + 1) % 256;
            b = (b + sbox[a]) % 256;
            int temp = sbox[a];
            sbox[a] = sbox[b];
            sbox[b] = temp;
            int c = (sbox[a] + sbox[b]) % 256;
            int plainChar = input.charAt(i);
            int cipherByte = plainChar ^ sbox[c];
            hexOutput.append(String.format("%02x", cipherByte));
        }
        return hexOutput.toString();
    }

    // 简易 URL 编码（仅处理特殊字符）
    public static String encodeURIComponent(String value) {
        return value.replace(" ", "%20")
                .replace("!", "%21")
                .replace("'", "%27")
                .replace("(", "%28")
                .replace(")", "%29")
                .replace("~", "%7E")
                .replace("*", "%2A");
        // 更严谨可用 URLEncoder，但注意它会把空格转成+，需替换
    }
}