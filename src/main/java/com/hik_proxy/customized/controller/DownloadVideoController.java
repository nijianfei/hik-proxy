package com.hik_proxy.customized.controller;

import com.hik_proxy.customized.dto.request.CheckDownloadVideoRequestDto;
import com.hik_proxy.customized.dto.request.DownloadVideoRequestDto;
import com.hik_proxy.customized.service.DealWithVideoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/video")
@RestController
public class DownloadVideoController {
    @Autowired
    private DealWithVideoService dealWithVideoService;

    @PostMapping("/task_create")
    public Object downloadVideo(@Valid @RequestBody DownloadVideoRequestDto downloadVideoDto) {
        return dealWithVideoService.downloadVideo(downloadVideoDto);
    }

    @PostMapping("/task_check")
    public Object checkDownloadVideo(@Valid @RequestBody CheckDownloadVideoRequestDto checkDownloadVideoDto) {
        return dealWithVideoService.checkDownloadVideo(checkDownloadVideoDto);
    }
}
