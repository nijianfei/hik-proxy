package com.hik_proxy.customized.service;

import com.hik_proxy.customized.dto.CheckDownloadVideoDto;
import com.hik_proxy.customized.dto.DownloadVideoDto;

import java.util.Map;

public interface DealWithVideoService {
    Map downloadVideo(DownloadVideoDto downloadVideoDto);

    CheckDownloadVideoDto checkDownloadVideo(CheckDownloadVideoDto checkDownloadVideoDto);
}
