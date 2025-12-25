package com.hik_proxy.customized.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DownloadVideoRequestDto {
    //请求识别ID
    private String reqNo;
    //摄像头ID
    private String cameraIndexCode;
    //视频时间FROM
    private String videoDateFrom;
    //视频时间TO
    private String videoDateTo;
    //视频格式区分
    private String videoType;
    //目标文件路径
    private String videoPath;
    //目标文件名称
    private String videoFilename;
}
