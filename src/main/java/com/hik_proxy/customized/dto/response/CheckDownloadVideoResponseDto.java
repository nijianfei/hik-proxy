package com.hik_proxy.customized.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CheckDownloadVideoResponseDto {
    //请求识别ID
    private String reqNo;
    //查询识别ID
    private String checkReqNo;
    private int count;
    private List<DownloadVideoInfoResponseDto> invokeRes;
}
