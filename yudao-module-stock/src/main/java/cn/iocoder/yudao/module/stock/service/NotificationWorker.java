package cn.iocoder.yudao.module.stock.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

/** 单实例批量发送，有限重试，过期记录清理。 @author vps */
@Service
public class NotificationWorker {
    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;
    private final MailGateway mail;
    private final PushGateway push;
    private final SubscriptionService subscriptions;
    private final ObjectMapper json;

    public NotificationWorker(
            JdbcTemplate jdbc,
            StringRedisTemplate redis,
            MailGateway mail,
            PushGateway push,
            SubscriptionService subscriptions,
            ObjectMapper json) {
        this.jdbc = jdbc;
        this.redis = redis;
        this.mail = mail;
        this.push = push;
        this.subscriptions = subscriptions;
        this.json = json;
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 120000)
    public void deliver() {
        String token = UUID.randomUUID().toString();
        if (!Boolean.TRUE.equals(
                redis.opsForValue()
                        .setIfAbsent("vps:delivery:lock", token, Duration.ofMinutes(10)))) {
            return;
        }
        try {
            jdbc.update(
                    "UPDATE vps_delivery SET status='EXPIRED' WHERE status='PENDING' AND"
                            + " create_time < DATE_SUB(NOW(),INTERVAL 24 HOUR)");
            jdbc.update(
                    "UPDATE vps_push_delivery SET status='EXPIRED' WHERE status='PENDING' AND"
                            + " create_time < DATE_SUB(NOW(),INTERVAL 24 HOUR)");
            if (mail.available()) {
                for (Map<String, Object> row :
                        jdbc.queryForList(
                                "SELECT d.*,s.name AS email,s.status AS subscriber_status FROM"
                                        + " vps_delivery d LEFT JOIN vps_subscriber s ON"
                                        + " s.id=d.subscriber_id WHERE d.status='PENDING' AND"
                                        + " d.attempts<3 ORDER BY d.id LIMIT 10")) {
                    if (!"ACTIVE".equals(row.get("subscriber_status"))) {
                        jdbc.update(
                                "UPDATE vps_delivery SET status='CANCELLED' WHERE id=?",
                                row.get("id"));
                        continue;
                    }
                    try {
                        String email = (String) row.get("email");
                        mail.send(
                                email,
                                (String) row.get("name"),
                                row.get("message")
                                        + "\n取消订阅："
                                        + subscriptions.unsubscribeLink(email));
                        jdbc.update(
                                "UPDATE vps_delivery SET"
                                        + " status='SENT',attempts=attempts+1,last_error=NULL WHERE"
                                        + " id=?",
                                row.get("id"));
                    } catch (Exception e) {
                        jdbc.update(
                                "UPDATE vps_delivery SET"
                                    + " status=IF(attempts>=2,'FAILED','PENDING'),attempts=attempts+1,last_error=?"
                                    + " WHERE id=?",
                                e.getClass().getSimpleName(),
                                row.get("id"));
                    }
                }
            }
            if (push.available()) {
                for (Map<String, Object> row :
                        jdbc.queryForList(
                                "SELECT d.*,s.endpoint,s.public_key,s.auth,s.enabled FROM"
                                    + " vps_push_delivery d LEFT JOIN vps_push_subscription s ON"
                                    + " s.id=d.subscription_id WHERE d.status='PENDING' AND"
                                    + " d.attempts<3 ORDER BY d.id LIMIT 5")) {
                    if (row.get("endpoint") == null
                            || ((Number) row.get("enabled")).intValue() != 1) {
                        jdbc.update(
                                "UPDATE vps_push_delivery SET status='CANCELLED' WHERE id=?",
                                row.get("id"));
                        continue;
                    }
                    try {
                        int status =
                                push.send(
                                        (String) row.get("endpoint"),
                                        (String) row.get("public_key"),
                                        (String) row.get("auth"),
                                        json.writeValueAsString(
                                                Map.of(
                                                        "title",
                                                        row.get("name"),
                                                        "body",
                                                        row.get("message"),
                                                        "url",
                                                        "/")));
                        if (status == 404 || status == 410) {
                            jdbc.update(
                                    "UPDATE vps_push_subscription SET enabled=0 WHERE id=?",
                                    row.get("subscription_id"));
                            jdbc.update(
                                    "UPDATE vps_push_delivery SET status='CANCELLED' WHERE id=?",
                                    row.get("id"));
                            continue;
                        }
                        if (status < 200 || status >= 300) {
                            throw new IllegalStateException("Push HTTP " + status);
                        }
                        jdbc.update(
                                "UPDATE vps_push_delivery SET"
                                        + " status='SENT',attempts=attempts+1,last_error=NULL WHERE"
                                        + " id=?",
                                row.get("id"));
                    } catch (Exception e) {
                        jdbc.update(
                                "UPDATE vps_push_delivery SET"
                                    + " status=IF(attempts>=2,'FAILED','PENDING'),attempts=attempts+1,last_error=?"
                                    + " WHERE id=?",
                                e.getClass().getSimpleName(),
                                row.get("id"));
                    }
                }
            }
        } finally {
            redis.execute(
                    new DefaultRedisScript<>(
                            "if redis.call('get',KEYS[1]) == ARGV[1] then return"
                                    + " redis.call('del',KEYS[1]) else return 0 end",
                            Long.class),
                    List.of("vps:delivery:lock"),
                    token);
        }
    }

    @Scheduled(cron = "0 20 3 * * *", zone = "Asia/Shanghai")
    public void cleanup() {
        for (int batch = 0; batch < 20; batch++) {
            jdbc.update(
                    "DELETE FROM vps_check_log WHERE create_time < DATE_SUB(NOW(),INTERVAL 30 DAY)"
                            + " LIMIT 10000");
            jdbc.update(
                    "DELETE FROM vps_delivery WHERE create_time < DATE_SUB(NOW(),INTERVAL 30 DAY)"
                            + " LIMIT 10000");
            jdbc.update(
                    "DELETE FROM vps_push_delivery WHERE create_time < DATE_SUB(NOW(),INTERVAL 30"
                            + " DAY) LIMIT 10000");
            jdbc.update(
                    "DELETE FROM infra_api_access_log WHERE create_time < DATE_SUB(NOW(),INTERVAL"
                            + " 14 DAY) LIMIT 10000");
        }
    }
}
