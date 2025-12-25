package com.hik_proxy.customized.vo.playback;

import lombok.Data;

import java.util.List;

@Data
public class PlaybackInfoVo {
    private String url ;
    private List<VideoFragment> list;
}
