package com.hik_proxy.customized.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.hik_proxy.customized.demo.Tools;
import com.hik_proxy.customized.dto.param.PlaybackParam;
import com.hik_proxy.customized.vo.BaseVo;
import com.hikvision.artemis.sdk.ArtemisHttpUtil;
import com.hikvision.artemis.sdk.Response;
import com.hikvision.artemis.sdk.config.ArtemisConfig;
import com.hikvision.artemis.sdk.constant.Constants;
import com.hikvision.artemis.sdk.constant.SystemHeader;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import static com.hikvision.artemis.sdk.util.HttpUtil.wrapClient;

@Slf4j
@Component
public class HikHttpUtil {

    @Value("${artemis.config}")
    private String configJsonStr;
    private static ArtemisConfig config;
    private static final String ARTEMIS_PATH = "/artemis";

    @PostConstruct
    public void init() {
        try {
            if (StringUtils.isNotBlank(configJsonStr) && configJsonStr.strip().startsWith("{")) {
                config = JSONObject.parseObject(configJsonStr, ArtemisConfig.class);
            } else {
                log.error("configJsonStr为空或格式错误,{}", configJsonStr);
            }
        } catch (Exception e) {
            log.error("configJsonStr:{},解析异常:{}", configJsonStr, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据需求调整超时时间
     */
    static {
        //连接超时时间
        Constants.DEFAULT_TIMEOUT = 10000;
        //读取超时时间
        Constants.SOCKET_TIMEOUT = 60000;
    }

    /*根据编号获取监控点详细信息*/
    public static String queryCameraInfo(String cameraIndexCode) throws Exception {
        final String getCamsApi = ARTEMIS_PATH + "/api/resource/v1/cameras/indexCode";
        config = getConfig();
        Map<String, String> path = new HashMap<>() {{
            put("http://", getCamsApi);
        }};
        Map<String, String> paramMap = new HashMap<>() {{
            put("cameraIndexCode", cameraIndexCode);
        }};
        String body = JSON.toJSON(paramMap).toString();
        return ArtemisHttpUtil.doPostStringArtemis(config, path, body, null, null, "application/json");
    }

    public static String cameraSearch() throws Exception {
        final String getCamsApi = ARTEMIS_PATH + "/api/resource/v2/camera/search";

        config = getConfig();
        Map<String, String> path = new HashMap<String, String>() {
            {
                put("http://", getCamsApi);
            }
        };
        Map<String, String> paramMap = new HashMap<String, String>();// post请求Form表单参数
        paramMap.put("pageNo", "1");
        paramMap.put("pageSize", "500");
        String body = JSON.toJSON(paramMap).toString();
        return ArtemisHttpUtil.doPostStringArtemis(config, path, body, null, null, "application/json");
    }

    public static String playbackURLs(PlaybackParam param) throws Exception {
        final String getCamsApi = ARTEMIS_PATH + "/api/video/v2/cameras/playbackURLs";
        config = getConfig();
        Map<String, String> path = new HashMap<>() {{
            put("http://", getCamsApi);
        }};
        String body = JSON.toJSONString(param);
        return ArtemisHttpUtil.doPostStringArtemis(config, path, body, null, null, "application/json");
    }

    public static long downloadVideo(String url, String savePath ,String fileName) throws Exception {
        final OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (okhttp3.Response response = client.newCall(request).execute();) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Response body is null");
            }

            // 确保目标目录存在
            Path dest = Paths.get(savePath,fileName);
            Files.createDirectories(dest.getParent());

            try (InputStream inputStream = body.byteStream();
                 FileOutputStream outputStream = new FileOutputStream(dest.toFile())) {

                byte[] buffer = new byte[8192]; // 8KB buffer
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
            return Files.size(dest);
        }
    }

    /**
     * 调用POST请求类型接口，这里以获取组织列表为例
     * 接口实际url:http://ip:port/artemis/api/resource/v1/org/orgList
     *
     * @return
     */
    public static String callPostApiGetOrgList() throws Exception {
        /**
         * http://ip:port/artemis/api/resource/v1/org/orgList
         * 通过查阅AI Cloud开放平台文档或网关门户的文档可以看到获取组织列表的接口定义,该接口为POST请求的Rest接口, 入参为JSON字符串，接口协议为http。
         * ArtemisHttpUtil工具类提供了doPostStringArtemis调用POST请求的方法，入参可传JSON字符串, 请阅读开发指南了解方法入参，没有的参数可传null
         */
        config = getConfig();
        final String getCamsApi = ARTEMIS_PATH + "/api/resource/v1/org/orgList";
        Map<String, String> paramMap = new HashMap<String, String>();// post请求Form表单参数
        paramMap.put("pageNo", "1");
        paramMap.put("pageSize", "2");
        String body = JSON.toJSON(paramMap).toString();
        Map<String, String> path = new HashMap<String, String>(2) {
            {
                put("http://", getCamsApi);
            }
        };
        return ArtemisHttpUtil.doPostStringArtemis(config, path, body, null, null, "application/json");
    }


    /**
     * 调用POST请求类型接口，这里以分页获取区域列表为例
     * 接口实际url：http://ip:port/artemis/api/api/resource/v1/regions
     *
     * @return
     */
    public static String callPostApiGetRegions() throws Exception {
        /**
         * http://ip:port/artemis/api/resource/v1/regions
         * 过查阅AI Cloud开放平台文档或网关门户的文档可以看到分页获取区域列表的定义,这是一个POST请求的Rest接口, 入参为JSON字符串，接口协议为http。
         * ArtemisHttpUtil工具类提供了doPostStringArtemis调用POST请求的方法，入参可传JSON字符串, 请阅读开发指南了解方法入参，没有的参数可传null
         */
        ArtemisConfig config = new ArtemisConfig();
        config.setHost("127.0.0.1"); // 代理API网关nginx服务器ip端口
        config.setAppKey("20469790");  // 秘钥appkey
        config.setAppSecret("lofnD6DbnBllHmk5YOyx");// 秘钥appSecret
        final String getCamsApi = ARTEMIS_PATH + "/api/resource/v1/regions";
        Map<String, String> paramMap = new HashMap<String, String>();// post请求Form表单参数
        paramMap.put("pageNo", "1");
        paramMap.put("pageSize", "2");
        paramMap.put("treeCode", "0");
        String body = JSON.toJSON(paramMap).toString();
        Map<String, String> path = new HashMap<String, String>(2) {
            {
                put("http://", getCamsApi);
            }
        };
        return ArtemisHttpUtil.doPostStringArtemis(config, path, body, null, null, "application/json");
    }

    /**
     * 调用POST接口，返回图片
     * 接口实际url：http://ip:port/artemis/api/visitor/v1/record/pictures
     *
     * @return
     */
    public static String callPostImgs() throws Exception {
        ArtemisConfig config = new ArtemisConfig();
        config.setHost("127.0.0.1"); // 代理API网关nginx服务器ip端口
        config.setAppKey("20469790");  // 秘钥appkey
        config.setAppSecret("lofnD6DbnBllHmk5YOyx");// 秘钥appSecret
        final String getSecurityApi = "/artemis" + "/api/visitor/v1/record/pictures"; // 接口路径
        Map<String, String> path = new HashMap<String, String>(2) {
            {
                put("http://", getSecurityApi);
            }
        };
        Map<String, String> head = new HashMap<String, String>(2) {  //get请求的head参数
            {
                put("headpost", "sky-test");
            }
        };
        Map<String, String> query = new HashMap<String, String>(2) {  //get请求的head参数
            {
                put("domainId", "0");
            }
        };
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("svrIndexCode", "9ff58bc2-65a5-464b-b28c-daea67ba9569");
        jsonBody.put("picUri", "/pic?9dda12i40-e*5b84626c4105m5ep=t=i3p*i=d1s*i=d3b*i1d3b*855925cea-96008b--2718943z855s=5i76=");
        String body = jsonBody.toJSONString();
        //参数根据接口实际情况设置
        //参数根据接口实际情况设置
        //【注】：该方法从artemis-http-client v1.1.12版本后，返回值修改为Response，此前为HttpResponse;升级时注意兼容性问题
        Response result = ArtemisHttpUtil.doPostStringImgArtemis(config, path, body, null, null, "application/json", null);

        try {

            if (302 == result.getStatusCode()) {
                /*
                获取图片数据保存到本地
                注：1.对于有时效的图片，必须尽快保存到本地
                   2.若无时效，则可以直接保存location，后续自行访问获取
                 */
                String location = result.getHeader("Location");
                HttpGet httpget = new HttpGet(location);
                HttpClient httpClient = wrapClient(httpget.getURI().getScheme() + "://" + httpget.getURI().getHost());
                HttpResponse execute = httpClient.execute(httpget);
                HttpEntity entity = execute.getEntity();
                InputStream in = entity.getContent();
                Tools.savePicToDisk(in, "d:/", "test311.jpg");
                //TODO 可以返回保存后的路径
            } else {
                System.out.println("下载出错:" + result.getBody());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return getSecurityApi;
    }

    /**
     * 调用POST接口，下载文件
     * 接口实际url：http://ip:port/artemis/api/fedof/v1/org/downloadCameraCSV
     *
     * @return
     */
    public static void callPostDownloadFile() throws Exception {
        ArtemisConfig config = new ArtemisConfig();
        config.setHost("127.0.0.1"); // 代理API网关nginx服务器ip端口
        config.setAppKey("20469790");  // 秘钥appkey
        config.setAppSecret("lofnD6DbnBllHmk5YOyx");// 秘钥appSecret
        final String getSecurityApi = "/artemis" + "/api/fedof/v1/org/downloadCameraCSV"; // 接口路径
        Map<String, String> path = new HashMap<String, String>(2) {
            {
                put("http://", getSecurityApi);
            }
        };
        Map<String, String> head = new HashMap<String, String>(2) {  //get请求的head参数
            {
                put("headpost", "sky-test");
            }
        };
        Map<String, String> query = new HashMap<String, String>(2) {  //get请求的head参数
            {
                put("domainId", "0");
            }
        };
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("svrIndexCode", "9ff58bc2-65a5-464b-b28c-daea67ba9569");
        jsonBody.put("picUri", "/pic?9dda12i40-e*5b84626c4105m5ep=t=i3p*i=d1s*i=d3b*i1d3b*855925cea-96008b--2718943z855s=5i76=");
        String body = jsonBody.toJSONString();
        //参数根据接口实际情况设置
        //【注】：该方法从artemis-http-client v1.1.12版本后，返回值修改为Response，此前为HttpResponse;升级时注意兼容性问题
        try (Response response = ArtemisHttpUtil.doPostDownloadFileArtemis(config, path, body, null, null, null, null);) {
            // 1. 检查响应状态
            if (response.getStatusCode() != 200) {
                throw new IOException("下载失败: " + response.getErrorMessage());
            }
            String filePath = "D:\\download\\";
            Tools.savePicToDisk(response.getResponse().getEntity().getContent(), filePath, "test.zip");
        }
    }

    /**
     * 调用POST请求类型接口，这里演示了外层代理服务对artemis对外接口封装的场景。例如：
     * artemis上开放的接口实际url：http://ip:port/artemis/api/api/resource/v1/regions
     * 外部代理服务上上开放的接口url：http://ip:port/proxy/api/api/resource/v1/regions
     * 需保证安全认证库签名使用的path参数与artemis一致，可在请求头中设置x-ca-path参数，内容为:/artemis/api/api/resource/v1/regions
     *
     * @return
     */
    public static String callPostApiGetRegionsByProxy() throws Exception {
        /**
         * http://ip:port/proxy/api/api/resource/v1/regions
         */
        ArtemisConfig config = new ArtemisConfig();
        config.setHost("127.0.0.1"); // 代理API网关nginx服务器ip端口
        config.setAppKey("20469790");  // 秘钥appkey
        config.setAppSecret("lofnD6DbnBllHmk5YOyx");// 秘钥appSecret
        final String getCamsApi = "/proxy/api/resource/v1/regions";
        Map<String, String> paramMap = new HashMap<String, String>();// post请求Form表单参数
        paramMap.put("pageNo", "1");
        paramMap.put("pageSize", "2");
        paramMap.put("treeCode", "0");
        String body = JSON.toJSON(paramMap).toString();
        Map<String, String> path = new HashMap<String, String>(2) {
            {
                put("http://", getCamsApi);
            }
        };
        Map<String, String> head = new HashMap<String, String>(2) {
            {
                put(SystemHeader.X_CA_PATH, "/artemis/api/api/resource/v1/regions");
            }
        };
        return ArtemisHttpUtil.doPostStringArtemis(config, path, body, null, null, "application/json", head);
    }

    public static ArtemisConfig getConfig() {
        ArtemisConfig config = new ArtemisConfig();
        config.setHost("123.56.136.71:40443"); // 平台nginx所在ip及http对应端口号
        config.setAppKey("27084252");  // 秘钥appkey
        config.setAppSecret("TwWHFXio2Rgm11M2u3N9");// 秘钥appSecret
        return config;
    }

    public static void main(String[] args) throws Exception {
//        String result = callPostApiGetOrgList();
//        System.out.println(result);
//        String VechicleDataResult = callPostApiGetRegions();
//        System.out.println(VechicleDataResult);

//        String s = cameraSearch();
//        JSONObject jsonObject = JSONObject.parseObject(s);
//        JSONObject data = (JSONObject) jsonObject.get("data");
//        JSONArray list = (JSONArray) data.get("list");
//        System.out.println(list.get(0));

        String s1 = queryCameraInfo("ec60d60ebfd44e8da7b68071e1660c50");
        BaseVo baseVo1 = JSON.parseObject(s1, BaseVo.class);
        System.out.println("s1:" + JSON.toJSONString(baseVo1, JSONWriter.Feature.PrettyFormat));


        PlaybackParam param = new PlaybackParam();
        param.setCameraIndexCode("ec60d60ebfd44e8da7b68071e1660c50");
        OffsetDateTime startTime = OffsetDateTime.of(LocalDateTime.of(2025, 9, 24, 10, 22, 33), ZoneOffset.ofHours(8));
        param.setBeginTime(startTime);
        OffsetDateTime endTime = OffsetDateTime.of(LocalDateTime.of(2025, 9, 24, 22, 44, 55), ZoneOffset.ofHours(8));
        param.setEndTime(endTime);
        String s2 = playbackURLs(param);
        JSONObject jsonObject2 = JSONObject.parseObject(s2);
        System.out.println("jsonObject2:" + JSON.toJSONString(jsonObject2, JSONWriter.Feature.PrettyFormat));
        JSONObject data2 = (JSONObject) jsonObject2.get("data");
        String url = data2.get("url").toString();
        System.out.println(url);


        String[] split = url.split("8950");
//        downloadVideo("http://123.56.136.71:40559"+split[1]);


    }

}
