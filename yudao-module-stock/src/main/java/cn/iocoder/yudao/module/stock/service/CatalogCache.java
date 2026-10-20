package cn.iocoder.yudao.module.stock.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.TimeUnit;

/** 公共数据缓存，事务提交后失效。 @author vps */
@Component
public class CatalogCache {
    private static final String KEY = "vps:catalog:v1";
    private final StringRedisTemplate redis;

    public CatalogCache(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String get() {
        return redis.opsForValue().get(KEY);
    }

    public void put(String json) {
        redis.opsForValue().set(KEY, json, 30, TimeUnit.SECONDS);
    }

    public void invalidate() {
        redis.delete(KEY);
    }

    public void invalidateAfterCommit() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            invalidate();
                        }
                    });
        } else {
            invalidate();
        }
    }
}
