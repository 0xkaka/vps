package cn.iocoder.yudao.module.stock.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.stock.service.*;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/** 通知运行状态，不返回凭据或浏览器认证值。 @author vps */
@RestController
@RequestMapping("/stock/notification")
public class NotificationController {
    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;
    private final MailGateway mail;
    private final PushGateway push;

    public NotificationController(
            JdbcTemplate jdbc, StringRedisTemplate redis, MailGateway mail, PushGateway push) {
        this.jdbc = jdbc;
        this.redis = redis;
        this.mail = mail;
        this.push = push;
    }

    @GetMapping("/status")
    @PreAuthorize("@ss.hasPermission('stock:notification:query')")
    public CommonResult<Map<String, Object>> status() {
        Map<String, Object> result = new LinkedHashMap<>(8);
        result.put("emailConfigured", mail.available());
        result.put("pushConfigured", push.available());
        result.put("lastSync", redis.opsForValue().get("vps:source:lastSuccess"));
        result.put("checks", redis.opsForValue().get("vps:source:checks"));
        result.put(
                "deliveries",
                jdbc.queryForList(
                        "SELECT id,name,status,attempts,last_error,create_time FROM vps_delivery"
                                + " ORDER BY id DESC LIMIT 100"));
        result.put(
                "pushDeliveries",
                jdbc.queryForList(
                        "SELECT id,name,status,attempts,last_error,create_time FROM"
                                + " vps_push_delivery ORDER BY id DESC LIMIT 100"));
        result.put(
                "subscriptions",
                jdbc.queryForList(
                        "SELECT id,enabled,create_time,update_time FROM vps_push_subscription ORDER"
                                + " BY id DESC LIMIT 100"));
        return CommonResult.success(result);
    }

    @PostMapping("/retry")
    @PreAuthorize("@ss.hasPermission('stock:notification:edit')")
    public CommonResult<Boolean> retry(
            @RequestParam Long id, @RequestParam(defaultValue = "email") String type) {
        if ("push".equals(type)) {
            jdbc.update(
                    "UPDATE vps_push_delivery SET status='PENDING',attempts=0 WHERE id=? AND"
                            + " status='FAILED'",
                    id);
        } else {
            jdbc.update(
                    "UPDATE vps_delivery SET status='PENDING',attempts=0 WHERE id=? AND"
                            + " status='FAILED'",
                    id);
        }
        return CommonResult.success(true);
    }

    @DeleteMapping("/subscription")
    @PreAuthorize("@ss.hasPermission('stock:notification:edit')")
    public CommonResult<Boolean> remove(@RequestParam Long id) {
        jdbc.update("UPDATE vps_push_subscription SET enabled=0 WHERE id=?", id);
        return CommonResult.success(true);
    }
}
