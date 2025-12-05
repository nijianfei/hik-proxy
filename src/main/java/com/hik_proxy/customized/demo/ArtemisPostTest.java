package com.hik_proxy.customized.demo;   //修改包路径

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hikvision.artemis.sdk.ArtemisHttpUtil;
import com.hikvision.artemis.sdk.Response;
import com.hikvision.artemis.sdk.config.ArtemisConfig;
import com.hikvision.artemis.sdk.constant.Constants;
import com.hikvision.artemis.sdk.constant.SystemHeader;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import static com.hikvision.artemis.sdk.util.HttpUtil.wrapClient;

public class ArtemisPostTest {
	/**
	 * 请根据技术支持提供的实际的平台IP/端口和API网关中的合作方信息更换static静态块中的三个参数.
	 * [1 host]
	 * 		host格式为IP：Port，如10.0.0.1:443
	 * 		当使用https协议调用接口时，IP是平台（nginx）IP，Port是https协议的端口；
	 *     当使用http协议调用接口时，IP是artemis服务的IP，Port是artemis服务的端口（默认9016）。
	 * [2 appKey和appSecret]
	 * 		请按照技术支持提供的合作方Key和合作方Secret修改
	 * 	    appKey：合作方Key
	 * 	    appSecret：合作方Secret
	 * 调用前看清接口传入的是什么，是传入json就用doPostStringArtemis方法，是表单提交就用doPostFromArtemis方法
	 *
	 */
	/**
	 * API网关的后端服务上下文为：/artemis
	 */
	private static final String ARTEMIS_PATH = "/artemis";
	/**
	 * 根据需求调整超时时间
	 */
	static {
		//连接超时时间
		Constants.DEFAULT_TIMEOUT = 10000;
		//读取超时时间
		Constants.SOCKET_TIMEOUT = 60000;
	}

	public static void callGetImg() throws Exception {
		ArtemisConfig config = new ArtemisConfig();
		config.setHost("127.0.0.1"); // 代理API网关nginx服务器ip端口
		config.setAppKey("20469790");  // 秘钥appkey
		config.setAppSecret("lofnD6DbnBllHmk5YOyx");// 秘钥appSecret
		final String getCamsApi = "/artemis/api/v1/vqd/download/vqdPic/test.jpg";
		Map<String, String> path = new HashMap<String, String>() {
			{
				put("https://", getCamsApi);
			}
		};

		try(Response response = ArtemisHttpUtil.doGetResponse(config, path, null, null, null, null)) {
			// 1. 检查响应状态
			if (response.getStatusCode() != 200) {
				throw new IOException("下载失败: " + response.getErrorMessage());
			}
			com.hikvision.ga.Tools.savePicToDisk(response.getResponse().getEntity().getContent(), "E:\\", "test.jpg");
		}
	}

	/**
	 * 调用POST请求类型接口，这里以获取组织列表为例
	 * 接口实际url:https://ip:port/artemis/api/resource/v1/org/orgList
	 * @return
	 */
	public static String callPostApiGetOrgList() throws Exception {
		/**
		 * https://ip:port/artemis/api/resource/v1/org/orgList
		 * 通过查阅AI Cloud开放平台文档或网关门户的文档可以看到获取组织列表的接口定义,该接口为POST请求的Rest接口, 入参为JSON字符串，接口协议为https。
		 * ArtemisHttpUtil工具类提供了doPostStringArtemis调用POST请求的方法，入参可传JSON字符串, 请阅读开发指南了解方法入参，没有的参数可传null
		 */
		ArtemisConfig config = new ArtemisConfig();
		config.setHost("127.0.0.1"); // 平台nginx所在ip及https对应端口号
		config.setAppKey("20469790");  // 秘钥appkey
		config.setAppSecret("lofnD6DbnBllHmk5YOyx");// 秘钥appSecret
		final String getCamsApi = ARTEMIS_PATH + "/api/resource/v1/org/orgList";
		Map<String, String> paramMap = new HashMap<String, String>();// post请求Form表单参数
		paramMap.put("pageNo", "1");
		paramMap.put("pageSize", "2");
		String body = JSON.toJSON(paramMap).toString();
		Map<String, String> path = new HashMap<String, String>(2) {
			{
				put("https://", getCamsApi);
			}
		};
		return ArtemisHttpUtil.doPostStringArtemis(config,path, body, null, null, "application/json");
	}


	/**
	 * 调用POST请求类型接口，这里以分页获取区域列表为例
	 * 接口实际url：https://ip:port/artemis/api/api/resource/v1/regions
	 * @return
	 */
	public static String callPostApiGetRegions() throws Exception {
		/**
		 * https://ip:port/artemis/api/resource/v1/regions
		 * 过查阅AI Cloud开放平台文档或网关门户的文档可以看到分页获取区域列表的定义,这是一个POST请求的Rest接口, 入参为JSON字符串，接口协议为https。
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
				put("https://", getCamsApi);
			}
		};
		return ArtemisHttpUtil.doPostStringArtemis(config,path, body, null, null, "application/json");
	}

	/**
	 * 调用POST接口，返回图片
	 * 接口实际url：https://ip:port/artemis/api/visitor/v1/record/pictures
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
				put("https://", getSecurityApi);
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

			if (302==result.getStatusCode()) {
                /*
                获取图片数据保存到本地
                注：1.对于有时效的图片，必须尽快保存到本地
                   2.若无时效，则可以直接保存location，后续自行访问获取
                 */
				String location=  result.getHeader("Location");
				HttpGet httpget = new HttpGet(location);
				HttpClient httpClient = wrapClient(httpget.getURI().getScheme()+"://"+httpget.getURI().getHost());
				HttpResponse execute = httpClient.execute(httpget);
				HttpEntity entity = execute.getEntity();
				InputStream in = entity.getContent();
				com.hikvision.ga.Tools.savePicToDisk(in, "d:/", "test311.jpg");
				//TODO 可以返回保存后的路径
			}else{
				System.out.println("下载出错:"+result.getBody());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return getSecurityApi;
	}

	/**
	 * 调用POST接口，下载文件
	 * 接口实际url：https://ip:port/artemis/api/fedof/v1/org/downloadCameraCSV
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
				put("https://", getSecurityApi);
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
		try(Response response = ArtemisHttpUtil.doPostDownloadFileArtemis(config, path, body, null, null, null, null);) {
			// 1. 检查响应状态
			if (response.getStatusCode() != 200) {
				throw new IOException("下载失败: " + response.getErrorMessage());
			}
			String filePath = "D:\\download\\";
			com.hikvision.ga.Tools.savePicToDisk(response.getResponse().getEntity().getContent(), filePath, "test.zip");
		}
	}

	/**
	 * 调用POST请求类型接口，这里演示了外层代理服务对artemis对外接口封装的场景。例如：
	 * artemis上开放的接口实际url：https://ip:port/artemis/api/api/resource/v1/regions
	 * 外部代理服务上上开放的接口url：https://ip:port/proxy/api/api/resource/v1/regions
	 * 需保证安全认证库签名使用的path参数与artemis一致，可在请求头中设置x-ca-path参数，内容为:/artemis/api/api/resource/v1/regions
	 * @return
	 */
	public static String callPostApiGetRegionsByProxy() throws Exception {
		/**
		 * https://ip:port/proxy/api/api/resource/v1/regions
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
				put("https://", getCamsApi);
			}
		};
		Map<String, String> head = new HashMap<String, String>(2) {
			{
				put(SystemHeader.X_CA_PATH, "/artemis/api/api/resource/v1/regions");
			}
		};
		return ArtemisHttpUtil.doPostStringArtemis(config,path, body, null, null, "application/json", head);
	}

	public static void main(String[] args) throws Exception {
		String result = callPostApiGetOrgList();
		System.out.println(result);
		String VechicleDataResult = callPostApiGetRegions();
		System.out.println(VechicleDataResult);
	}

}
