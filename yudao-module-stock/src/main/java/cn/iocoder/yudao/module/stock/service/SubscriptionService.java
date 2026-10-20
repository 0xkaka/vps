package cn.iocoder.yudao.module.stock.service;

import cn.iocoder.yudao.framework.common.exception.ServiceException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

/** 邮件双重确认，退订同样验证邮箱归属。 @author vps */
@Service
public class SubscriptionService {
    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;
    private final MailGateway mail;
    private final String baseUrl;

    public SubscriptionService(
            JdbcTemplate jdbc,
            StringRedisTemplate redis,
            MailGateway mail,
            @Value("${stock.public-url:http://68.68.249.248}") String baseUrl) {
        this.jdbc = jdbc;
        this.redis = redis;
        this.mail = mail;
        this.baseUrl = baseUrl;
    }

    public void rateLimit(String ip) {
        Long count =
                redis.execute(
                        new DefaultRedisScript<>(
                                "local n=redis.call('incr',KEYS[1]); if n==1 then"
                                        + " redis.call('expire',KEYS[1],60) end; return n",
                                Long.class),
                        List.of("vps:rate:" + ip));
        if (count != null && count > 10) {
            throw new ServiceException(1_050_000_005, "操作过于频繁，请稍后再试");
        }
    }

    public void request(String email, boolean unsubscribe) {
        if (!mail.available()) {
            throw new ServiceException(1_050_000_006, "邮件通知暂未启用，请选择其他订阅渠道");
        }
        email = email.trim().toLowerCase(Locale.ROOT);
        if (!Boolean.TRUE.equals(
                redis.opsForValue()
                        .setIfAbsent(
                                "vps:mail:rate:" + SourceParser.key(email, "", false),
                                "1",
                                Duration.ofMinutes(1)))) {
            throw new ServiceException(1_050_000_007, "确认邮件已请求，请稍后再试");
        }
        String token = UUID.randomUUID().toString() + UUID.randomUUID();
        redis.opsForValue()
                .set(
                        "vps:mail:token:" + token,
                        (unsubscribe ? "UNSUBSCRIBED" : "ACTIVE") + "\n" + email,
                        Duration.ofHours(24));
        if (!unsubscribe) {
            jdbc.update(
                    "INSERT IGNORE INTO vps_subscriber(name,status) VALUES(?,'PENDING')", email);
        }
        try {
            mail.send(
                    email,
                    unsubscribe ? "确认取消库存订阅" : "确认库存订阅",
                    "请在24小时内打开以下链接并确认：\n"
                            + baseUrl
                            + "/?emailToken="
                            + token
                            + "\n如果不是你本人操作，请忽略此邮件。");
        } catch (Exception e) {
            redis.delete("vps:mail:token:" + token);
            throw new ServiceException(1_050_000_008, "邮件发送失败，请稍后重试");
        }
    }

    public void confirm(String token) {
        String value = redis.opsForValue().get("vps:mail:token:" + token);
        if (value == null) {
            throw new ServiceException(1_050_000_009, "确认链接无效或已过期");
        }
        String[] parts = value.split("\n", 2);
        jdbc.update(
                "INSERT INTO vps_subscriber(name,status) VALUES(?,?) ON DUPLICATE KEY UPDATE"
                        + " status=VALUES(status),update_time=NOW()",
                parts[1],
                parts[0]);
        redis.delete("vps:mail:token:" + token);
    }

    public String unsubscribeLink(String email) {
        String token = UUID.randomUUID().toString() + UUID.randomUUID();
        redis.opsForValue()
                .set("vps:mail:token:" + token, "UNSUBSCRIBED\n" + email, Duration.ofDays(30));
        return baseUrl + "/?emailToken=" + token;
    }
}
