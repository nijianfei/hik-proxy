package com.hik_proxy.customized.aspect;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

@Aspect
@Component
public class ControllerLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(ControllerLogAspect.class);

    // 定义切入点：拦截所有Controller层的公共方法
    @Pointcut("execution(public * com.hik_proxy.customized.controller..*.*(..))")
    public void controllerLogPointcut() {
    }

    // 前置通知：在方法执行前记录请求信息
    @Before("controllerLogPointcut()")
    public void doBefore(JoinPoint joinPoint) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();

            // 构建请求信息
            String requestInfo = buildRequestInfo(request, joinPoint);
            logger.info("【请求】: {}", requestInfo);
        }
    }

    // 环绕通知：在方法执行前后记录日志
    @Around("controllerLogPointcut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 执行目标方法
        Object result = joinPoint.proceed();

        // 计算执行时间
        long executionTime = System.currentTimeMillis() - startTime;

        // 构建响应信息
        String responseInfo = buildResponseInfo(joinPoint, result, executionTime);
        logger.info("【响应】: {}", responseInfo);

        return result;
    }

    /**
     * 构建请求信息
     */
    private String buildRequestInfo(HttpServletRequest request, JoinPoint joinPoint) {
        StringBuilder sb = new StringBuilder();

        // 获取基本信息
        String method = request.getMethod();
        String url = request.getRequestURL().toString();
        String uri = request.getRequestURI();
        String clientIP = getClientIp(request);

        sb.append("URL=").append(url)
                .append(", Method=").append(method)
                .append(", ClientIP=").append(clientIP)
                .append(", URI=").append(uri)
                .append(", Class=").append(joinPoint.getSignature().getDeclaringTypeName())
                .append(", Method=").append(joinPoint.getSignature().getName());


        // 获取请求参数
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (parameterMap != null && !parameterMap.isEmpty()) {
            String params = parameterMap.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + String.join(",", entry.getValue()))
                    .collect(Collectors.joining(", "));
            sb.append(", Params=[").append(params).append("]");
        }

        String requestBody = "";
        if (request instanceof ContentCachingRequestWrapper) {
            ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) request;
            byte[] buf = wrapper.getContentAsByteArray();
            if (buf.length > 0) {
                requestBody = new String(buf, StandardCharsets.UTF_8);
            }
        }

        if (StringUtils.isNotEmpty(requestBody)) {
            sb.append(", Body=").append(System.lineSeparator()).append(requestBody).append(System.lineSeparator());
        }
        return sb.toString();
    }

    /**
     * 构建响应信息
     */
    private String buildResponseInfo(ProceedingJoinPoint joinPoint, Object result, long executionTime) {
        StringBuilder sb = new StringBuilder();

        sb.append("Class=").append(joinPoint.getSignature().getDeclaringTypeName())
                .append(", Method=").append(joinPoint.getSignature().getName())
                .append(", ExecutionTime=").append(executionTime).append("ms");

        // 处理返回结果（避免循环引用等问题）
        if (result != null) {
            sb.append(", Response=");
            try {
                // 简化返回结果，避免打印过大的对象
                if (result.toString().length() > 500) {
                    sb.append(result.getClass().getSimpleName()).append("(size too large)");
                } else {
                    sb.append(result);
                }
            } catch (Exception e) {
                sb.append("Unable to log response: ").append(e.getMessage());
            }
        }

        return sb.toString();
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果是多级代理，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    // 异常通知：记录异常信息
    @AfterThrowing(value = "controllerLogPointcut()", throwing = "ex")
    public void doAfterThrowing(JoinPoint joinPoint, Exception ex) {
        logger.error("【异常】: Class={}, Method={}, Error={}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                ex.getMessage(),
                ex);
    }
}