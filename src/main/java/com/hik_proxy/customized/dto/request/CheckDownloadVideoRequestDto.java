package com.hik_proxy.customized.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CheckDownloadVideoRequestDto {
    //请求识别ID
    private String reqNo;
    //查询识别ID
    private String checkReqNo;
}
