package cn.iocoder.yudao.module.stock.service;

import cn.iocoder.yudao.module.stock.domain.*;
import cn.iocoder.yudao.module.stock.mapper.*;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 状态变更、审计及通知队列在同一事务中提交。 @author vps */
@Service
public class StockChangeService {
    private final ProductMapper products;
    private final CheckLogMapper logs;
    private final JdbcTemplate jdbc;
    private final CatalogCache cache;

    public StockChangeService(
            ProductMapper products, CheckLogMapper logs, JdbcTemplate jdbc, CatalogCache cache) {
        this.products = products;
        this.logs = logs;
        this.jdbc = jdbc;
        this.cache = cache;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean apply(Product product, SourceParser.Snapshot snapshot) {
        return apply(product, snapshot, "THIRD_PARTY_PAGE", product.getSourcePage(), null, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean apply(
            Product product,
            SourceParser.Snapshot snapshot,
            String type,
            String url,
            String error,
            boolean notify) {
        if (products.updateStock(
                        product.getId(),
                        product.getStockStatus(),
                        snapshot.status(),
                        snapshot.checked())
                == 0) {
            return false;
        }
        jdbc.update(
                "UPDATE vps_product SET"
                    + " last_source_type=?,last_source_url=?,last_sync_error=?,last_attempt_at=NOW()"
                    + " WHERE id=?",
                type,
                url,
                error,
                product.getId());
        if (type.startsWith("OFFICIAL")
                || !java.util.Objects.equals(type, product.getLastSourceType())
                || !java.util.Objects.equals(url, product.getLastSourceUrl())) {
            // 替换采集生成的机房数据，避免不同来源机房状态混用。保留手工机房。
            jdbc.update(
                    "DELETE FROM vps_region WHERE product_id=? AND source_name IS NOT NULL",
                    product.getId());
        }
        int sort = 0;
        for (SourceParser.RegionState region : snapshot.regions()) {
            jdbc.update(
                    "INSERT INTO"
                        + " vps_region(product_id,name,source_name,stock_status,purchase_url,sort_order,enabled)"
                        + " VALUES(?,?,?,?,?,?,1) ON DUPLICATE KEY UPDATE"
                        + " stock_status=VALUES(stock_status),update_time=NOW()",
                    product.getId(),
                    region.name(),
                    region.name(),
                    region.status(),
                    region.url(),
                    sort++);
        }
        CheckLog log = new CheckLog();
        log.setName(product.getName());
        log.setProductId(product.getId());
        log.setPreviousStatus(product.getStockStatus());
        log.setStockStatus(snapshot.status());
        log.setMessage("同步成功 [" + type + "] " + url + (error == null ? "" : "；官方来源异常：" + error));
        logs.insert(log);
        if (notify
                && "OUT_OF_STOCK".equals(product.getStockStatus())
                && "IN_STOCK".equals(snapshot.status())
                && !Boolean.TRUE.equals(product.getGroupFlag())
                && Boolean.TRUE.equals(product.getEnabled())) {
            jdbc.update(
                    "INSERT INTO vps_delivery(product_id,subscriber_id,name,message) SELECT"
                            + " ?,id,?,? FROM vps_subscriber WHERE status='ACTIVE'",
                    product.getId(),
                    "补货提醒：" + product.getName(),
                    product.getName() + " 已有货，购买链接：" + product.getPurchaseUrl());
            jdbc.update(
                    "INSERT INTO vps_push_delivery(product_id,subscription_id,name,message) SELECT"
                            + " ?,id,?,? FROM vps_push_subscription WHERE enabled=1",
                    product.getId(),
                    "补货提醒",
                    product.getName() + " 已有货");
        }
        cache.invalidateAfterCommit();
        return true;
    }

    public void failed(Product product, String error) {
        jdbc.update(
                "UPDATE vps_product SET last_sync_error=?,last_attempt_at=NOW() WHERE id=?",
                error,
                product.getId());
        cache.invalidateAfterCommit();
    }
}
