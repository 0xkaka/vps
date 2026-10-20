package cn.iocoder.yudao.module.stock.service;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.stock.domain.*;
import cn.iocoder.yudao.module.stock.mapper.*;

import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

/** 固定来源库存同步，Redis 分布式锁避免重复抓取。 @author vps */
@Service
public class SourceSyncService {
    private static final Logger LOG = LoggerFactory.getLogger(SourceSyncService.class);
    private static final String LOCK = "vps:source:lock";
    private final StringRedisTemplate redis;
    private final ProductMapper products;
    private final CheckLogMapper logs;
    private final SourceParser parser;
    private final StockChangeService changes;

    public SourceSyncService(
            StringRedisTemplate redis,
            ProductMapper products,
            CheckLogMapper logs,
            SourceParser parser,
            StockChangeService changes) {
        this.redis = redis;
        this.products = products;
        this.logs = logs;
        this.parser = parser;
        this.changes = changes;
    }

    @Scheduled(fixedDelay = 120000, initialDelay = 90000)
    public void scheduled() {
        try {
            sync(false);
        } catch (Exception e) {
            LOG.warn("库存同步失败: {}", e.getMessage());
        }
    }

    public int sync(boolean force) {
        String token = UUID.randomUUID().toString();
        if (!Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(LOCK, token, Duration.ofMinutes(5)))) {
            throw new ServiceException(1_050_000_002, "正在同步，请稍后再试");
        }
        try {
            if (force
                    && !Boolean.TRUE.equals(
                            redis.opsForValue()
                                    .setIfAbsent(
                                            "vps:source:manual", token, Duration.ofSeconds(120)))) {
                throw new ServiceException(1_050_000_003, "手动同步间隔至少两分钟");
            }
            LocalDateTime now = LocalDateTime.now();
            List<Product> due =
                    products.selectList(null).stream()
                            .filter(
                                    p ->
                                            Boolean.TRUE.equals(p.getMonitorEnabled())
                                                    && Boolean.TRUE.equals(p.getEnabled())
                                                    && (force
                                                            || p.getLastSyncedAt() == null
                                                            || p.getLastSyncedAt()
                                                                    .plusSeconds(
                                                                            p.getIntervalSeconds())
                                                                    .isBefore(now)))
                            .toList();
            if (due.isEmpty()) {
                return 0;
            }
            // 仅抓取固定白名单页面；单个来源失败不阻断其他来源。
            Map<String, Map<String, SourceParser.Snapshot>> byPage = new LinkedHashMap<>();
            for (String page : due.stream().map(Product::getSourcePage).distinct().toList()) {
                if (!SourceParser.PAGES.contains(page)) {
                    continue;
                }
                try {
                    byPage.put(
                            page,
                            parser.parsePage(
                                    page,
                                    Jsoup.connect(page)
                                            .timeout(15000)
                                            .maxBodySize(4 * 1024 * 1024)
                                            .followRedirects(false)
                                            .userAgent("VPS-Stock-Monitor/1.0")
                                            .get()));
                } catch (Exception e) {
                    CheckLog log = new CheckLog();
                    log.setName("来源同步失败");
                    log.setMessage(page + " 抓取或解析失败，保留库存：" + e.getClass().getSimpleName());
                    logs.insert(log);
                    LOG.warn("来源同步失败 {}: {}", page, e.getClass().getSimpleName());
                }
            }
            if (byPage.isEmpty()) {
                throw new IllegalStateException("全部来源不可用");
            }
            int count = 0;
            for (Product p : due) {
                Map<String, SourceParser.Snapshot> states = byPage.get(p.getSourcePage());
                if (states == null) {
                    continue;
                }
                SourceParser.Snapshot state = states.get(p.getSourceKey());
                if (state != null) {
                    if (changes.apply(p, state)) {
                        count++;
                    }
                } else {
                    CheckLog log = new CheckLog();
                    log.setName(p.getName());
                    log.setProductId(p.getId());
                    log.setPreviousStatus(p.getStockStatus());
                    log.setStockStatus(p.getStockStatus());
                    log.setMessage("原站未匹配该产品，保留库存；请核对匹配键或关闭同步");
                    logs.insert(log);
                }
            }
            redis.opsForValue().set("vps:source:lastSuccess", now.toString());
            redis.opsForValue().increment("vps:source:checks", count);
            return count;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            CheckLog log = new CheckLog();
            log.setName("同步任务");
            log.setMessage("抓取或解析失败，未匹配的库存保持原值：" + e.getClass().getSimpleName());
            logs.insert(log);
            throw new ServiceException(1_050_000_004, "同步失败，请查看检查记录");
        } finally {
            redis.execute(
                    new DefaultRedisScript<>(
                            "if redis.call('get',KEYS[1]) == ARGV[1] then return"
                                    + " redis.call('del',KEYS[1]) else return 0 end",
                            Long.class),
                    List.of(LOCK),
                    token);
        }
    }
}
