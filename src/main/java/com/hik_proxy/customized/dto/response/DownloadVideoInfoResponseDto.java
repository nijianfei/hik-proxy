package com.hik_proxy.customized.dto.response;

import lombok.Data;

@Data
public class DownloadVideoInfoResponseDto {
    private String videoPath;
    private String videoFilename;
    private int videoSize;
    private String status;
    private String msg;
}
