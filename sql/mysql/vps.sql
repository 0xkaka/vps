-- VPS业务表：MySQL 8；所有时间均为 Asia/Shanghai。

SET NAMES utf8mb4;

CREATE TABLE vps_vendor (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(120) NOT NULL COMMENT '商家名称',
  `code` varchar(60) NOT NULL COMMENT '商家标识',
  `sort_order` int NOT NULL COMMENT '排序',
  `enabled` tinyint NOT NULL COMMENT '启用',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_vendor (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Vendor管理';

CREATE TABLE vps_product (
  source_page varchar(255) NOT NULL DEFAULT 'https://stock.vpszdm.com/' COMMENT '库存来源页面',
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) NOT NULL COMMENT '产品名称',
  `vendor_id` bigint NOT NULL COMMENT '商家ID',
  `parent_id` bigint NULL COMMENT '上级分组ID',
  `group_flag` tinyint NOT NULL COMMENT '是否分组',
  `specification` varchar(500) NULL COMMENT '配置',
  `route_tags` varchar(255) NULL COMMENT '线路标签',
  `description` varchar(1000) NULL COMMENT '说明',
  `purchase_url` varchar(2000) NULL COMMENT '购买链接',
  `source_url` varchar(2000) NULL COMMENT '原站匹配地址',
  `speed_url` varchar(2000) NULL COMMENT '测速链接',
  `looking_glass_url` varchar(2000) NULL COMMENT '透镜链接',
  `review_url` varchar(2000) NULL COMMENT '测评链接',
  `stock_status` varchar(20) NOT NULL COMMENT '库存状态',
  `interval_seconds` int NOT NULL COMMENT '检查间隔(秒)',
  `last_checked_at` datetime NULL COMMENT '原站检查时间',
  `monitor_enabled` tinyint NOT NULL COMMENT '同步库存',
  `sort_order` int NOT NULL COMMENT '排序',
  `enabled` tinyint NOT NULL COMMENT '展示',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_product_vendor (vendor_id),
  KEY idx_product_parent (parent_id),
  FOREIGN KEY (vendor_id) REFERENCES vps_vendor(id),
  FOREIGN KEY (parent_id) REFERENCES vps_product(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Product管理';

CREATE TABLE vps_channel (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(120) NOT NULL COMMENT '渠道名称',
  `type` varchar(30) NOT NULL COMMENT '渠道类型',
  `product_id` bigint NULL COMMENT '产品ID(空为全局)',
  `url` varchar(2000) NOT NULL COMMENT '订阅链接',
  `sort_order` int NOT NULL COMMENT '排序',
  `enabled` tinyint NOT NULL COMMENT '启用',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_channel_product (product_id),
  FOREIGN KEY (product_id) REFERENCES vps_product(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Channel管理';

CREATE TABLE vps_link (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(120) NOT NULL COMMENT '链接名称',
  `url` varchar(2000) NOT NULL COMMENT '链接地址',
  `position` varchar(20) NOT NULL COMMENT '位置',
  `sort_order` int NOT NULL COMMENT '排序',
  `enabled` tinyint NOT NULL COMMENT '启用',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Link管理';

CREATE TABLE vps_setting (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(100) NOT NULL COMMENT '配置键',
  `value` varchar(2000) NOT NULL COMMENT '配置值',
  `description` varchar(255) NULL COMMENT '说明',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_setting (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Setting管理';

CREATE TABLE vps_subscriber (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(254) NOT NULL COMMENT '邮箱',
  `status` varchar(20) NOT NULL COMMENT '状态',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_subscriber (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Subscriber管理';

CREATE TABLE vps_check_log (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) NULL COMMENT '产品名称',
  `product_id` bigint NULL COMMENT '产品ID',
  `previous_status` varchar(20) NULL COMMENT '原状态',
  `stock_status` varchar(20) NULL COMMENT '现状态',
  `message` varchar(1000) NULL COMMENT '结果',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_check_product_time (product_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CheckLog管理';


CREATE TABLE vps_delivery (
 id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
 product_id bigint NOT NULL COMMENT '产品',
 subscriber_id bigint NOT NULL COMMENT '订阅者',
 name varchar(255) NOT NULL COMMENT '标题',
 message text NOT NULL COMMENT '正文',
 status varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
 attempts int NOT NULL DEFAULT 0 COMMENT '尝试次数',
 last_error varchar(500) NULL COMMENT '失败原因',
 create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
 update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
 PRIMARY KEY(id), KEY idx_delivery_status(status,update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件投递队列';
INSERT INTO system_menu (id,name,type,sort,parent_id,path,icon) VALUES (25000,'库存运营',1,0,0,'/stock','ep:goods');
INSERT INTO system_menu (id,name,permission,type,sort,parent_id,path,icon,component,component_name) VALUES (25001,'商家分类','stock:vendor:query',2,0,25000,'vendor','ep:list','stock/vendor/index','StockVendor');
INSERT INTO system_menu (id,name,permission,type,sort,parent_id,path) VALUES (25101,'商家分类维护','stock:vendor:edit',3,1,25001,'');
INSERT INTO system_menu (id,name,permission,type,sort,parent_id,path,icon,component,component_name) VALUES (25002,'产品与库存','stock:product:query',2,1,25000,'product','ep:list','stock/product/index','StockProduct');
INSERT INTO system_menu (id,name,permission,type,sort,parent_id,path) VALUES (25102,'产品与库存维护','stock:product:edit',3,1,25002,'');
INSERT INTO system_menu (id,name,permission,type,sort,parent_id,path,icon,component,component_name) VALUES (25003,'订阅渠道','stock:channel:query',2,2,25000,'channel','ep:list','stock/channel/index','StockChannel');
INSERT INTO system_menu (id,name,permission,type,sort,parent_id,path) VALUES (25103,'订阅渠道维护','stock:channel:edit',3,1,25003,'');
INSERT INTO system_menu (id,name,permission,type,sort,parent_id,path,icon,component,component_name) VALUES (25004,'导航链接','stock:link:query',2,3,25000,'link','ep:list','stock/link/index','StockLink');
INSERT INTO system_menu (id,name,permission,type,sort,parent_id,path) VALUES (25104,'导航链接维护','stock:link:edit',3,1,25004,'');
INSERT INTO system_menu (id,name,permission,type,sort,parent_id,path,icon,component,component_name) VALUES (25005,'站点配置','stock:setting:query',2,4,25000,'setting','ep:list','stock/setting/index','StockSetting');
INSERT INTO system_menu (id,name,permission,type,sort,parent_id,path) VALUES (25105,'站点配置维护','stock:setting:edit',3,1,25005,'');
INSERT INTO system_menu (id,name,permission,type,sort,parent_id,path,icon,component,component_name) VALUES (25006,'邮件订阅者','stock:subscriber:query',2,5,25000,'subscriber','ep:list','stock/subscriber/index','StockSubscriber');
INSERT INTO system_menu (id,name,permission,type,sort,parent_id,path) VALUES (25106,'邮件订阅者维护','stock:subscriber:edit',3,1,25006,'');
INSERT INTO system_menu (id,name,permission,type,sort,parent_id,path,icon,component,component_name) VALUES (25007,'库存检查记录','stock:checklog:query',2,6,25000,'checklog','ep:list','stock/checklog/index','StockCheckLog');

ALTER TABLE vps_product ADD COLUMN source_key varchar(64) NULL COMMENT '原站匹配键', ADD COLUMN last_synced_at datetime NULL COMMENT '最近同步时间', ADD KEY idx_source_key(source_key);

CREATE TABLE vps_push_subscription (
 id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
 endpoint_hash varchar(64) NOT NULL COMMENT '端点摘要',
 endpoint varchar(2000) NOT NULL COMMENT '浏览器推送端点',
 public_key varchar(180) NOT NULL COMMENT '浏览器公钥',
 auth varchar(100) NOT NULL COMMENT '浏览器认证值',
 enabled tinyint NOT NULL DEFAULT 1 COMMENT '启用',
 create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
 update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
 PRIMARY KEY(id), UNIQUE KEY uk_push_endpoint(endpoint_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='浏览器订阅';
CREATE TABLE vps_push_delivery (
 id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
 product_id bigint NOT NULL COMMENT '产品',
 subscription_id bigint NOT NULL COMMENT '浏览器订阅',
 name varchar(255) NOT NULL COMMENT '通知标题',
 message text NOT NULL COMMENT '通知内容',
 status varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
 attempts int NOT NULL DEFAULT 0 COMMENT '尝试次数',
 last_error varchar(500) NULL COMMENT '失败原因',
 create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
 update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
 PRIMARY KEY(id), KEY idx_push_delivery_status(status,update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='浏览器投递队列';
INSERT INTO system_menu (id,name,permission,type,sort,parent_id,path,icon,component,component_name) VALUES (25008,'通知与运行状态','stock:notification:query',2,8,25000,'notification','ep:bell','stock/notification/index','StockNotification');
INSERT INTO system_menu (id,name,permission,type,sort,parent_id,path) VALUES (25108,'通知维护','stock:notification:edit',3,1,25008,'');
