package cn.iocoder.yudao.module.stock.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;

import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.*;

/** 将约束冲突转换为可读业务错误，不暴露 SQL。 @author vps */
@RestControllerAdvice(basePackages = "cn.iocoder.yudao.module.stock.controller")
@Order(-1)
public class StockExceptionHandler {
    @ExceptionHandler(DuplicateKeyException.class)
    public CommonResult<Boolean> duplicate(DuplicateKeyException exception) {
        return CommonResult.error(1_050_000_020, "记录已存在，请检查商家标识、配置键或邮箱");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public CommonResult<Boolean> integrity(DataIntegrityViolationException exception) {
        return CommonResult.error(1_050_000_021, "关联记录不存在或仍被引用，请先处理关联数据");
    }
}
