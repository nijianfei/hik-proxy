package com.hik_proxy.customized.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.hik_proxy.customized.dto.param.PlaybackParam;
import com.hik_proxy.customized.dto.request.CheckDownloadVideoRequestDto;
import com.hik_proxy.customized.dto.request.DownloadVideoRequestDto;
import com.hik_proxy.customized.dto.response.CheckDownloadVideoResponseDto;
import com.hik_proxy.customized.dto.response.DownloadVideoInfoResponseDto;
import com.hik_proxy.customized.dto.response.DownloadVideoResponseDto;
import com.hik_proxy.customized.entity.DownloadVideoInfoEntity;
import com.hik_proxy.customized.enums.TaskCodeEnum;
import com.hik_proxy.customized.mapper.DownloadVideoInfoMapper;
import com.hik_proxy.customized.service.DealWithVideoService;
import com.hik_proxy.customized.utils.HikHttpUtil;
import com.hik_proxy.customized.utils.ResultUtil;
import com.hik_proxy.customized.vo.BaseVo;
import com.hik_proxy.customized.vo.playback.PlaybackInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DealWithVideoServiceImpl implements DealWithVideoService {

    @Autowired
    private DownloadVideoInfoMapper mapper;
    @Override
    public Object downloadVideo(DownloadVideoRequestDto downloadVideoDto) {
        try {
            checkParams(downloadVideoDto);
            PlaybackParam param = buildDownParam(downloadVideoDto);
            String response = HikHttpUtil.playbackURLs(param);
            BaseVo<PlaybackInfoVo> baseVo = JSON.parseObject(response, new TypeReference<>() {
            });
            Assert.isTrue(baseVo.isSuccess(), baseVo.getMsg());
            PlaybackInfoVo data = baseVo.getData();
            Assert.notNull(data.getUrl(), "指定时间区间内未查得录像信息");
            //http://172.17.121.1:8950/openUrl/vsigpfhaUBaed815f7361604316a29ab/playback.mp4?beginTime=20250924T102233&endTime=20250924T224455
            String[] splitUrl = data.getUrl().split("\\?");
            String[] splitTime = splitUrl[1].split("&");//beginTime=20250920T093501 endTime=20250924T230000
            List<OffsetDateTime> times = new ArrayList<>(2);
            for (String timeStr : splitTime) {
                times.add(LocalDateTime.parse(timeStr.split("=")[1], DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")).atOffset(param.getBeginTime().getOffset()));
            }
            Duration betweenStart = Duration.between(param.getBeginTime(), times.get(0));
            Duration betweenEnd = Duration.between(times.get(1), param.getEndTime());
            long beginDValue = betweenStart.toMinutes();
            long endDValue = betweenEnd.toMinutes();
            Assert.isTrue(!(beginDValue > 1 || endDValue > 1), String.format("指定的录像下载区间内,录像部分缺失:头部-%d(分钟),尾部-%d(分钟)", beginDValue, endDValue));

            Duration between = Duration.between(times.get(0), times.get(1));
            long hours = between.toHours();
            int minutesPart = between.toMinutesPart();
            if (minutesPart > 10) {
                hours++;
            }

            List<List<OffsetDateTime>> timeRangeList = new ArrayList<>();
            OffsetDateTime lastTime = param.getBeginTime();
            OffsetDateTime currentEndTime = param.getBeginTime();
            for (long i = 0; i < hours; i++) {

                if (i + 1 == hours) {
                    currentEndTime = param.getEndTime();
                } else {
                    currentEndTime = currentEndTime.plusHours(1);
                }
                timeRangeList.add(List.of(lastTime, currentEndTime));
                lastTime = currentEndTime;

            }

            Path dest = Paths.get(downloadVideoDto.getVideoPath());
            if (!dest.toFile().exists()) {
                Files.createDirectories(dest);
            }
            List<DownloadVideoInfoEntity> taskList = new ArrayList<>(timeRangeList.size());
            for (int i = 0; i < timeRangeList.size(); i++) {
                String fileName = String.format("%s_%d.mp4", downloadVideoDto.getVideoFilename(), i + 1);
                Assert.isTrue(!new File(downloadVideoDto.getVideoPath(),fileName ).exists(), "路径下已存在指定的文件名称");
                List<OffsetDateTime> timeRange = timeRangeList.get(i);
                String beginTime = timeRange.get(0).format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
                String endTime = timeRange.get(1).format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
                DownloadVideoInfoEntity downloadVideoInfoEntity = new DownloadVideoInfoEntity();
                downloadVideoInfoEntity.setReqNo(downloadVideoDto.getReqNo());
                downloadVideoInfoEntity.setSeq(i + 1);
                downloadVideoInfoEntity.setCameraIndexCode(downloadVideoDto.getCameraIndexCode());
                downloadVideoInfoEntity.setVideoDateFrom(beginTime);
                downloadVideoInfoEntity.setVideoDateTo(endTime);
                downloadVideoInfoEntity.setVideoType("");
                downloadVideoInfoEntity.setVideoPath(downloadVideoDto.getVideoPath());
                downloadVideoInfoEntity.setVideoFilename(fileName);
                downloadVideoInfoEntity.setByteLength(0L);
                downloadVideoInfoEntity.setStatus(TaskCodeEnum.T10.getCode());
                downloadVideoInfoEntity.setMsg("");
                downloadVideoInfoEntity.setCreateTime(new Date());
                downloadVideoInfoEntity.setUpdateTime(new Date());

                taskList.add(downloadVideoInfoEntity);
            }

            mapper.insert(taskList);
            return ResultUtil.success(DownloadVideoResponseDto.builder().reqNo(downloadVideoDto.getReqNo()).build());
        } catch (Exception e) {
            log.error("downloadVideo_Exception:{}", e.getMessage(), e);
            return ResultUtil.failure(e.getMessage());
        }
    }

    private PlaybackParam buildDownParam(DownloadVideoRequestDto downloadVideoDto) {
        PlaybackParam param = new PlaybackParam();
        param.setCameraIndexCode(downloadVideoDto.getCameraIndexCode());
        OffsetDateTime startTime = OffsetDateTime.of(LocalDateTime.parse(downloadVideoDto.getVideoDateFrom(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), ZoneOffset.ofHours(8));
        param.setBeginTime(startTime);
        OffsetDateTime endTime = OffsetDateTime.of(LocalDateTime.parse(downloadVideoDto.getVideoDateTo(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), ZoneOffset.ofHours(8));
        param.setEndTime(endTime);
        return param;
    }

    private boolean isExist(String cameraIndexCode) {
        try {
            String response = HikHttpUtil.queryCameraInfo(cameraIndexCode);
            BaseVo baseVo = JSON.parseObject(response, BaseVo.class);
            return baseVo.getData() != null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void checkParams(DownloadVideoRequestDto downloadVideoDto) {
        Assert.isTrue(StringUtils.isNotBlank(downloadVideoDto.getReqNo()), "请求识别ID,不能为空");
        Assert.isTrue(StringUtils.isNotBlank(downloadVideoDto.getCameraIndexCode()), "摄像头ID,不能为空");
        Assert.isTrue(StringUtils.isNotBlank(downloadVideoDto.getVideoDateFrom()), "视频时间FROM,不能为空");
        Assert.isTrue(StringUtils.isNotBlank(downloadVideoDto.getVideoDateTo()), "视频时间TO,不能为空");
//        Assert.isTrue(StringUtils.isNotBlank(downloadVideoDto.getVideoType()), "视频格式区分,不能为空");
        Assert.isTrue(StringUtils.isNotBlank(downloadVideoDto.getVideoPath()), "目标文件路径,不能为空");
        Assert.isTrue(StringUtils.isNotBlank(downloadVideoDto.getVideoFilename()), "目标文件名称,不能为空");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateFrom = null;
        LocalDateTime dateTo = null;
        try {
            dateFrom = LocalDateTime.parse(downloadVideoDto.getVideoDateFrom(), formatter);
            dateTo = LocalDateTime.parse(downloadVideoDto.getVideoDateTo(), formatter);
        } catch (Exception e) {
            log.error("视频时间格式错误:{}", e.getMessage(), e);
            throw new RuntimeException("视频时间格式错误:" + e.getMessage());
        }
        String reqNo = downloadVideoDto.getReqNo();
        List<DownloadVideoInfoEntity> task = mapper.selectByMap(Map.of("req_no", reqNo));
        Assert.isTrue(CollectionUtils.isEmpty(task),"reqNo已存在,禁止重复创建下载任务");
        Assert.isTrue(dateFrom.isBefore(dateTo), "视频时间TO应大于视频时间FROM");
        Assert.isTrue(isExist(downloadVideoDto.getCameraIndexCode()), "摄像头ID,不存在");
    }

    @Override
    public Object checkDownloadVideo(CheckDownloadVideoRequestDto checkDownloadVideoDto) {
        try {
            Assert.isTrue(StringUtils.isNotBlank(checkDownloadVideoDto.getReqNo()), "请求识别ID,不能为空");
            Assert.isTrue(StringUtils.isNotBlank(checkDownloadVideoDto.getCheckReqNo()), "查询识别ID,不能为空");

            String checkReqNo = checkDownloadVideoDto.getCheckReqNo();
            List<DownloadVideoInfoEntity> task = mapper.selectByMap(Map.of("req_no", checkReqNo));
            if (CollectionUtils.isNotEmpty(task)) {
                List<DownloadVideoInfoResponseDto> infos = new ArrayList<>(task.size());
                task.forEach(t->{
                    DownloadVideoInfoResponseDto dto = new DownloadVideoInfoResponseDto();
                    dto.setVideoPath(t.getVideoPath());
                    dto.setVideoFilename(t.getVideoFilename());
                    dto.setVideoSize(t.getByteLength());
                    dto.setStatus(t.getStatus());
                    dto.setMsg(t.getMsg());
                    if (TaskCodeEnum.T70.getCode().equals(t.getStatus()) || TaskCodeEnum.T90.getCode().equals(t.getStatus())) {
                        infos.add(dto);
                    }
                });
                CheckDownloadVideoResponseDto build = CheckDownloadVideoResponseDto.builder().reqNo(checkDownloadVideoDto.getReqNo())
                        .checkReqNo(checkReqNo).count(task.size()).invokeRes(infos).build();
                Map<String, Object> result = JSON.parseObject(JSON.toJSONString(build), new TypeReference<>() {
                });
                return ResultUtil.success(result);
            }else{
                return ResultUtil.failure(String.format("checkReqNo:%s,不存在",checkReqNo));
            }
        } catch (Exception e) {
            return ResultUtil.failure(e.getMessage());
        }
    }

}
