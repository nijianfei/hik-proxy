package com.hik_proxy.customized.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DownloadVideoResponseDto{
    //请求识别ID
    private String reqNo;
}
