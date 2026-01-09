--创建管理员账号
CREATE USER csc_admin WITH
  SUPERUSER
  CREATEDB
  CREATEROLE
  LOGIN
  PASSWORD 'Acsys_00';
--创建普通账号
CREATE USER test_user WITH PASSWORD 'test_password';
--数据库名称
csc_hik_proxy

--录像下载任务表
CREATE TABLE public.video_task (
	id serial4 NOT NULL, -- 主键ID
	req_no varchar(64) NOT NULL, -- 请求编号
	seq int4 NOT NULL, -- 序列号
	camera_index_code varchar(128) NOT NULL, -- 摄像头索引编码
	video_date_from varchar(32) NOT NULL, -- 视频开始时间（格式：yyyyMMddHHmmss 或 ISO8601）
	video_date_to varchar(32) NOT NULL, -- 视频结束时间（格式：yyyyMMddHHmmss 或 ISO8601）
	video_type varchar(32) NOT NULL, -- 视频类型
	video_path varchar(512) NULL, -- 视频存储路径
	video_filename varchar(255) NULL, -- 视频文件名
	byte_length int8 NULL, -- 文件字节长度
	status varchar(32) NOT NULL, -- 状态（如：10:下载未开始,20:下载进行中,70:下载成功,90:下载失败）
	msg varchar(1024) NULL, -- 状态描述或错误信息
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 创建时间
	update_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL, -- 更新时间
	CONSTRAINT video_task_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_camera_index_code ON public.video_task USING btree (camera_index_code);
CREATE UNIQUE INDEX uk_req_no_seq ON public.video_task USING btree (req_no, seq);
CREATE INDEX video_task_create_time_idx ON public.video_task USING btree (create_time);
COMMENT ON TABLE public.video_task IS '视频下载任务表';

-- Column comments

COMMENT ON COLUMN public.video_task.id IS '主键ID';
COMMENT ON COLUMN public.video_task.req_no IS '请求编号';
COMMENT ON COLUMN public.video_task.seq IS '序列号';
COMMENT ON COLUMN public.video_task.camera_index_code IS '摄像头索引编码';
COMMENT ON COLUMN public.video_task.video_date_from IS '视频开始时间（格式：yyyyMMddHHmmss 或 ISO8601）';
COMMENT ON COLUMN public.video_task.video_date_to IS '视频结束时间（格式：yyyyMMddHHmmss 或 ISO8601）';
COMMENT ON COLUMN public.video_task.video_type IS '视频类型';
COMMENT ON COLUMN public.video_task.video_path IS '视频存储路径';
COMMENT ON COLUMN public.video_task.video_filename IS '视频文件名';
COMMENT ON COLUMN public.video_task.byte_length IS '文件字节长度';
COMMENT ON COLUMN public.video_task.status IS '状态（如：10:下载未开始,20:下载进行中,70:下载成功,90:下载失败）';
COMMENT ON COLUMN public.video_task.msg IS '状态描述或错误信息';
COMMENT ON COLUMN public.video_task.create_time IS '创建时间';
COMMENT ON COLUMN public.video_task.update_time IS '更新时间';

