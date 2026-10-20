package cn.iocoder.yudao.module.stock.controller.app;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;

import jakarta.annotation.security.PermitAll;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/** 公共运行信息，不公开内部连接信息。 @author vps */
@RestController
@RequestMapping("/stock")
public class RuntimeController {
    private final StringRedisTemplate redis;

    public RuntimeController(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @PermitAll
    @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(enable = false)
    @GetMapping("/runtime")
    public CommonResult<Map<String, Object>> runtime() {
        Map<String, Object> result = new LinkedHashMap<>(4);
        result.put("lastSync", redis.opsForValue().get("vps:source:lastSuccess"));
        result.put("checks", redis.opsForValue().get("vps:source:checks"));
        return CommonResult.success(result);
    }
}
