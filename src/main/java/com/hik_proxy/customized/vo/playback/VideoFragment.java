package com.hik_proxy.customized.vo.playback;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class VideoFragment {
    private OffsetDateTime beginTime;
    private OffsetDateTime  endTime;
    private long size;
    private int lockType;
}
