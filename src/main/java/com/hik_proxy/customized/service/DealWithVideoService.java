package com.hik_proxy.customized.service;

import com.hik_proxy.customized.dto.request.CheckDownloadVideoRequestDto;
import com.hik_proxy.customized.dto.request.DownloadVideoRequestDto;

public interface DealWithVideoService {
    Object downloadVideo(DownloadVideoRequestDto downloadVideoDto);

    Object checkDownloadVideo(CheckDownloadVideoRequestDto checkDownloadVideoDto);
}
