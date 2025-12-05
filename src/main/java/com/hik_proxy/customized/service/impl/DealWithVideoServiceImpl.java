package com.hik_proxy.customized.service.impl;

import com.hik_proxy.customized.dto.CheckDownloadVideoDto;
import com.hik_proxy.customized.dto.DownloadVideoDto;
import com.hik_proxy.customized.service.DealWithVideoService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DealWithVideoServiceImpl  implements DealWithVideoService {

    String url = "/api/video/v2/cameras/playbackURLs";
    @Override
    public Map downloadVideo(DownloadVideoDto downloadVideoDto) {
        checkParams(downloadVideoDto);
        return null;
    }

    private void checkParams(DownloadVideoDto downloadVideoDto) {
    }

    @Override
    public CheckDownloadVideoDto checkDownloadVideo(CheckDownloadVideoDto checkDownloadVideoDto) {
        return null;
    }
}
