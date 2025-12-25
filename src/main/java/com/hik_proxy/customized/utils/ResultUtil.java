package com.hik_proxy.customized.utils;

import com.hik_proxy.customized.enums.CommonCodeEnum;

import java.util.HashMap;
import java.util.Map;

public class ResultUtil {
    public static Object success(){
        Map<String, Object> result = new HashMap<>();
        result.put("invokeCls", CommonCodeEnum.SUCCESS.getCode());
        return result;
    };
    public static Object success(String msg){
        Map<String, Object> result = new HashMap<>();
        result.put("invokeCls", CommonCodeEnum.SUCCESS.getCode());
        result.put("ngMessage", msg);
        return result;
    };
    public static Object success(Object resultObj){
        Map<String, Object> result = new HashMap<>();
        result.put("invokeCls", CommonCodeEnum.SUCCESS.getCode());
        result.put("ngMessage", "请求处理成功");
        result.put("invokeRes",resultObj);
        return result;
    };
    public static Object success(Map<String,Object> resultMap){
        Map<String, Object> result = new HashMap<>();
        result.put("invokeCls", CommonCodeEnum.SUCCESS.getCode());
        result.put("ngMessage", "请求处理成功");
        result.putAll(resultMap);
        return result;
    };
    public static Object failure(String msg){
        Map<String, Object> result = new HashMap<>();
        result.put("invokeCls", CommonCodeEnum.FAILURE.getCode());
        result.put("ngMessage", msg);
        return result;
    };
}
