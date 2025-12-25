package com.hik_proxy.customized.task;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.hik_proxy.customized.dto.param.PlaybackParam;
import com.hik_proxy.customized.entity.DownloadVideoInfoEntity;
import com.hik_proxy.customized.enums.TaskCodeEnum;
import com.hik_proxy.customized.mapper.DownloadVideoInfoMapper;
import com.hik_proxy.customized.utils.HikHttpUtil;
import com.hik_proxy.customized.vo.BaseVo;
import com.hik_proxy.customized.vo.playback.PlaybackInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Slf4j
@Component
public class VideoDownloadTask {

    @Value("${download.host}")
    private String downloadHost;
    @Autowired
    private DownloadVideoInfoMapper mapper;

    @Autowired
    @Qualifier("videoDownExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @Scheduled(fixedRate = 60 * 1000) // 每60秒执行一次
    public void videoDownTask() {
        QueryWrapper<DownloadVideoInfoEntity> queryWrapper = new QueryWrapper();
        queryWrapper.ge("create_time", LocalDate.now());
        queryWrapper.eq("status", TaskCodeEnum.T10.getCode());
        List<DownloadVideoInfoEntity> videoInfos = mapper.selectList(queryWrapper);
        Map<String, List<DownloadVideoInfoEntity>> reqGroup = videoInfos.stream().collect(Collectors.groupingBy(DownloadVideoInfoEntity::getReqNo));
        log.info("查得待下载录像任务:{}条", reqGroup.size());

        for (String reqNo : reqGroup.keySet()) {
            List<DownloadVideoInfoEntity> taskInfos = reqGroup.get(reqNo);
            UpdateWrapper updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("req_no", reqNo);
            updateWrapper.eq("status", TaskCodeEnum.T10.getCode());
            int updateCount = mapper.update(updateWrapper);
            if (taskInfos.size() == updateCount) {
                downVideo(taskInfos);
            } else {
                log.error("reqNo:{},taskInfos.size:{} != updateCount:{},跳过", reqNo, taskInfos.size(), updateCount);
            }
        }
    }


    private void downVideo(List<DownloadVideoInfoEntity> taskInfos) {
        DownloadVideoInfoEntity v1 = taskInfos.get(0);
        String reqNo = v1.getReqNo();
        final String baseUrl;
        try {
            log.info("开始获取[{}]的录像下载链接}", reqNo);
            PlaybackParam param = buildDownParam(v1, taskInfos.get(taskInfos.size() - 1).getVideoDateTo());
            String response = HikHttpUtil.playbackURLs(param);
            BaseVo<PlaybackInfoVo> baseVo = JSON.parseObject(response, new TypeReference<>() {
            });
            String url = baseVo.getData().getUrl();
            String splitUrl = url.split("\\?")[0].split("8950")[1];
            baseUrl = downloadHost + splitUrl;
        } catch (Exception e) {
            log.error("录像任务[{}]-获取下载url失败", reqNo, e.getMessage(), e);
            throw new RuntimeException(e);
        }

        log.info("开始下载录像任务{},共计{}条", reqNo, taskInfos.size());
        List<Future> futures = new ArrayList<>();
        for (final DownloadVideoInfoEntity taskInfo : taskInfos) {
            Future<?> submit = taskExecutor.submit(() -> {
                try {
                    long fileSize = HikHttpUtil.downloadVideo(String.format("%s?beginTime=%s&endTime=%s", baseUrl, taskInfo.getVideoDateFrom(), taskInfo.getVideoDateTo()), taskInfo.getVideoPath(), taskInfo.getVideoFilename());
                    taskInfo.setStatus(TaskCodeEnum.T70.getCode());
                    taskInfo.setMsg(TaskCodeEnum.T70.getName());
                    taskInfo.setByteLength(fileSize);
                    mapper.updateById(taskInfo);
                } catch (Exception e) {
                    log.error("录像任务[{}]-{},下载失败:{}", reqNo, taskInfo.getSeq(), e.getMessage(), e);
                    taskInfo.setStatus(TaskCodeEnum.T90.getCode());
                    taskInfo.setMsg(e.getMessage());
                    mapper.updateById(taskInfo);
                }
            });
            futures.add(submit);
        }
        for (Future future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                log.error("future.get():{}", e.getMessage(), e);
            }
        }
    }

    private PlaybackParam buildDownParam(DownloadVideoInfoEntity v1, String videoDateTo) {
        PlaybackParam param = new PlaybackParam();
        param.setCameraIndexCode(v1.getCameraIndexCode());
        OffsetDateTime startTime = OffsetDateTime.of(LocalDateTime.parse(v1.getVideoDateFrom(), DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")), ZoneOffset.ofHours(8));
        param.setBeginTime(startTime);
        OffsetDateTime endTime = OffsetDateTime.of(LocalDateTime.parse(videoDateTo, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")), ZoneOffset.ofHours(8));
        param.setEndTime(endTime);
        return param;
    }

}
