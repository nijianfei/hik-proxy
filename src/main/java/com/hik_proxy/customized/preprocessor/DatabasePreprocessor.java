package com.hik_proxy.customized.preprocessor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.hik_proxy.customized.entity.DownloadVideoInfoEntity;
import com.hik_proxy.customized.enums.TaskCodeEnum;
import com.hik_proxy.customized.mapper.DownloadVideoInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DatabasePreprocessor implements ApplicationRunner {

    @Autowired
    private DownloadVideoInfoMapper mapper;

    @Override
    public void run(ApplicationArguments args) {
        try {
            // 查询数据库并执行预处理
            QueryWrapper<DownloadVideoInfoEntity> queryWrapper = new QueryWrapper();
            LocalDateTime lastDate = LocalDateTime.now().minusDays(1);
            //查询一天内,待下载的录像
            queryWrapper.ge("create_time", lastDate);
            queryWrapper.eq("status", TaskCodeEnum.T20.getCode());
            List<DownloadVideoInfoEntity> downTasks = mapper.selectList(queryWrapper);
            Map<String, List<DownloadVideoInfoEntity>> reqNoGroup = downTasks.stream().collect(Collectors.groupingBy(DownloadVideoInfoEntity::getReqNo));
            log.info("任务状态预处理,查得下载中任务:{}项,共计{}条", reqNoGroup.size(), downTasks.size());
            reqNoGroup.forEach((reqNo, tasks) -> {
                for (DownloadVideoInfoEntity task : tasks) {
                    File file = new File(task.getVideoPath(), task.getVideoFilename());
                    boolean isDeleteSuccess = FileUtils.deleteQuietly(file);
                    log.info("开始删除任务目录下文件:{} - {}", file, isDeleteSuccess ? "成功" : "失败");
                }
                // 预处理逻辑（如更新状态、缓存数据等）
                log.info("开始更新任务下载状态:{}");
                UpdateWrapper updateWrapper = new UpdateWrapper();
                updateWrapper.eq("status", TaskCodeEnum.T20.getCode());
                updateWrapper.eq("req_no", reqNo);
                updateWrapper.set("status", TaskCodeEnum.T10.getCode());
                updateWrapper.set("update_time", new Date());
                int update = mapper.update(updateWrapper);
                log.info("任务[{}]状态由[{}]更新为:[{}],共计查得:{}条,更新:{}条！",reqNo, TaskCodeEnum.T20.getName(), TaskCodeEnum.T10.getName(), tasks.size(), update);
            });
            log.info("任务状态预处理完成！");
        } catch (Exception e) {
            log.info("任务状态预处理异常！{}", e.getMessage(), e);
        }
    }
}
