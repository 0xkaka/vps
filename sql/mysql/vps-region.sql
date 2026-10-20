CREATE TABLE vps_region (
 id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
 product_id bigint NOT NULL COMMENT '产品ID',
 name varchar(120) NOT NULL COMMENT '机房展示名称',
 source_name varchar(120) NOT NULL COMMENT '原站机房标识',
 stock_status varchar(20) NOT NULL COMMENT '库存状态',
 purchase_url varchar(2000) NOT NULL COMMENT '购买链接',
 sort_order int NOT NULL DEFAULT 0 COMMENT '排序',
 enabled tinyint NOT NULL DEFAULT 1 COMMENT '展示',
 create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
 update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
 PRIMARY KEY(id), UNIQUE KEY uk_region_source(product_id,source_name),
 FOREIGN KEY(product_id) REFERENCES vps_product(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机房库存';
INSERT INTO system_menu (id,name,permission,type,sort,parent_id,path,icon,component,component_name) VALUES (25009,'机房库存','stock:region:query',2,3,25000,'region','ep:location','stock/region/index','StockRegion');
INSERT INTO system_menu (id,name,permission,type,sort,parent_id,path) VALUES (25109,'机房库存维护','stock:region:edit',3,1,25009,'');
