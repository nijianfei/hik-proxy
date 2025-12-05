package com.hik_proxy.customized.controller;

import com.hik_proxy.customized.dto.CheckDownloadVideoDto;
import com.hik_proxy.customized.dto.DownloadVideoDto;
import com.hik_proxy.customized.service.DealWithVideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DownloadVideoController {
    @Autowired
    private DealWithVideoService dealWithVideoService;

    public DownloadVideoController() {
    }

    @RequestMapping(
            value = {"/api/v1/download/video/ZXJT_HK_VIDEO_DL"},
            method = {RequestMethod.POST}
    )
    public Map downloadVideo(@RequestBody DownloadVideoDto downloadVideoDto) {
        return this.dealWithVideoService.downloadVideo(downloadVideoDto);
    }

    @RequestMapping(
            value = {"/api/v1/download/video/ZXJT_HK_VIDEO_DL_CK"},
            method = {RequestMethod.POST}
    )
    public CheckDownloadVideoDto checkDownloadVideo(@RequestBody CheckDownloadVideoDto checkDownloadVideoDto) {
        return this.dealWithVideoService.checkDownloadVideo(checkDownloadVideoDto);
    }
}
