package com.hik_proxy.customized.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("video_task")
public class DownloadVideoInfoEntity {
    @TableId(
            value = "id",
            type = IdType.AUTO
    )
    private Integer id;
    private String reqNo;
    private int seq;
    private String cameraIndexCode;
    private String videoDateFrom;
    private String videoDateTo;
    private String videoType;
    private String videoPath;
    private String videoFilename;
    private String taskCode;
    private String process;
    private Long byteLength;
    private String status;
    private String msg;
    private Date createTime;
    private Date updateTime;
}
