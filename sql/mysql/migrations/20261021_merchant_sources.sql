-- 兼容旧程序，可在新程序启动前执行。先备份数据库；仅执行一次。
ALTER TABLE vps_product
  ADD COLUMN last_source_type VARCHAR(32) NULL COMMENT '实际库存来源类型',
  ADD COLUMN last_source_url VARCHAR(2000) NULL COMMENT '实际库存来源地址',
  ADD COLUMN last_sync_error VARCHAR(1000) NULL COMMENT '官方或备用来源同步异常',
  ADD COLUMN last_attempt_at DATETIME NULL COMMENT '最近尝试同步时间';
