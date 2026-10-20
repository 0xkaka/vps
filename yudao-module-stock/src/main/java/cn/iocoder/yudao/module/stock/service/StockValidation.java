package cn.iocoder.yudao.module.stock.service;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.stock.domain.*;

import java.net.URI;

/** 业务输入校验。 @author vps */
public final class StockValidation {
    private StockValidation() {}

    public static void validate(Object item) {
        if (item instanceof Product p) {
            checkUrl(p.getPurchaseUrl());
            checkUrl(p.getSourceUrl());
            checkUrl(p.getSpeedUrl());
            checkUrl(p.getLookingGlassUrl());
            checkUrl(p.getReviewUrl());
            if (p.getId() != null && p.getId().equals(p.getParentId())) {
                throw new ServiceException(1_050_000_001, "上级分组不能为自身");
            }
        }
        if (item instanceof Region r) {
            checkUrl(r.getPurchaseUrl());
        }
        if (item instanceof Channel c) {
            checkUrl(c.getUrl());
        }
        if (item instanceof Link l) {
            checkUrl(l.getUrl());
        }
        if (item instanceof Setting s && "refreshSeconds".equals(s.getName())) {
            try {
                if (Integer.parseInt(s.getValue()) < 30) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                throw new ServiceException(1_050_000_001, "刷新间隔必须为至少30秒的整数");
            }
        }
    }

    private static void checkUrl(String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        try {
            URI uri = URI.create(value);
            if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
                    || uri.getHost() == null
                    || uri.getUserInfo() != null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            throw new ServiceException(1_050_000_001, "链接必须是有效的 HTTP/HTTPS 地址");
        }
    }
}
