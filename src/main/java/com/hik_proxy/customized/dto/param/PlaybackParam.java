package com.hik_proxy.customized.dto.param;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class PlaybackParam {
    private String cameraIndexCode;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime  beginTime;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime endTime;
    private String recordLocation = "0";
    private String protocol = "httpmp4";
    private String expand = "playBackMode=2";
}
