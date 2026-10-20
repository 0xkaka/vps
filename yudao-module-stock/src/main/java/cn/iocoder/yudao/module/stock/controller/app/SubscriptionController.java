package cn.iocoder.yudao.module.stock.controller.app;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.stock.service.*;

import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/** 面向访客的订阅接口。 @author vps */
@RestController
@RequestMapping("/stock")
public class SubscriptionController {
    private final SubscriptionService subscriptions;
    private final MailGateway mail;
    private final PushGateway push;
    private final JdbcTemplate jdbc;

    public SubscriptionController(
            SubscriptionService subscriptions,
            MailGateway mail,
            PushGateway push,
            JdbcTemplate jdbc) {
        this.subscriptions = subscriptions;
        this.mail = mail;
        this.push = push;
        this.jdbc = jdbc;
    }

    public record EmailRequest(@NotBlank @Email @Size(max = 254) String email) {}

    public record ConfirmRequest(@NotBlank @Size(max = 100) String token) {}

    public record Keys(
            @NotBlank @Size(max = 180) String p256dh, @NotBlank @Size(max = 100) String auth) {}

    public record PushRequest(
            @NotBlank @Size(max = 2000) String endpoint, @Valid @NotNull Keys keys) {}

    @PermitAll
    @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(enable = false)
    @GetMapping("/capabilities")
    public CommonResult<Map<String, Object>> capabilities() {
        return CommonResult.success(
                Map.of(
                        "email",
                        mail.available(),
                        "webpush",
                        push.available(),
                        "publicKey",
                        push.getPublicKey()));
    }

    private void limit(HttpServletRequest req) {
        subscriptions.rateLimit(
                Optional.ofNullable(req.getHeader("X-Real-IP")).orElse(req.getRemoteAddr()));
    }

    @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable = false)
    @PermitAll
    @PostMapping("/email/subscribe")
    public CommonResult<Boolean> subscribe(
            @Valid @RequestBody EmailRequest body, HttpServletRequest req) {
        limit(req);
        subscriptions.request(body.email(), false);
        return CommonResult.success(true);
    }

    @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable = false)
    @PermitAll
    @PostMapping("/email/unsubscribe")
    public CommonResult<Boolean> unsubscribe(
            @Valid @RequestBody EmailRequest body, HttpServletRequest req) {
        limit(req);
        subscriptions.request(body.email(), true);
        return CommonResult.success(true);
    }

    @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable = false)
    @PermitAll
    @PostMapping("/email/confirm")
    public CommonResult<Boolean> confirm(
            @Valid @RequestBody ConfirmRequest body, HttpServletRequest req) {
        limit(req);
        subscriptions.confirm(body.token());
        return CommonResult.success(true);
    }

    @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable = false)
    @PermitAll
    @PostMapping("/webpush/subscribe")
    public CommonResult<Boolean> push(
            @Valid @RequestBody PushRequest body, HttpServletRequest req) {
        limit(req);
        if (!push.available()) {
            throw new ServiceException(1_050_000_010, "浏览器推送未配置");
        }
        try {
            PushGateway.validate(body.endpoint(), body.keys().p256dh(), body.keys().auth());
        } catch (IllegalArgumentException e) {
            throw new ServiceException(1_050_000_011, "无效的浏览器订阅信息");
        }
        jdbc.update(
                "INSERT INTO vps_push_subscription(endpoint_hash,endpoint,public_key,auth)"
                    + " VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE"
                    + " public_key=VALUES(public_key),auth=VALUES(auth),enabled=1,update_time=NOW()",
                SourceParser.key(body.endpoint(), "", false),
                body.endpoint(),
                body.keys().p256dh(),
                body.keys().auth());
        return CommonResult.success(true);
    }

    @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable = false)
    @PermitAll
    @PostMapping("/webpush/unsubscribe")
    public CommonResult<Boolean> unpush(
            @Valid @RequestBody PushRequest body, HttpServletRequest req) {
        limit(req);
        jdbc.update(
                "UPDATE vps_push_subscription SET enabled=0 WHERE endpoint_hash=? AND public_key=?"
                        + " AND auth=?",
                SourceParser.key(body.endpoint(), "", false),
                body.keys().p256dh(),
                body.keys().auth());
        return CommonResult.success(true);
    }
}
