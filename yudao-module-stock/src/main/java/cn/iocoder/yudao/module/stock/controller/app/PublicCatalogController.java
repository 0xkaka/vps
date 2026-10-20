package cn.iocoder.yudao.module.stock.controller.app;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.stock.domain.*;
import cn.iocoder.yudao.module.stock.mapper.*;
import cn.iocoder.yudao.module.stock.service.CatalogCache;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.security.PermitAll;

import org.springframework.web.bind.annotation.*;

import java.util.*;

/** 展示页面公共数据；禁止公开订阅者信息。 @author vps */
@RestController
@RequestMapping("/stock")
public class PublicCatalogController {
    private final RegionMapper regions;
    private final VendorMapper vendors;
    private final ProductMapper products;
    private final ChannelMapper channels;
    private final LinkMapper links;
    private final SettingMapper settings;
    private final CatalogCache cache;
    private final ObjectMapper json;

    public PublicCatalogController(
            RegionMapper regions,
            VendorMapper vendors,
            ProductMapper products,
            ChannelMapper channels,
            LinkMapper links,
            SettingMapper settings,
            CatalogCache cache,
            ObjectMapper json) {
        this.regions = regions;
        this.vendors = vendors;
        this.products = products;
        this.channels = channels;
        this.links = links;
        this.settings = settings;
        this.cache = cache;
        this.json = json;
    }

    @PermitAll
    @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(enable = false)
    @GetMapping("/catalog")
    public CommonResult catalog() throws java.io.IOException {
        String cached = cache.get();
        if (cached != null) {
            return CommonResult.success(json.readValue(cached, Map.class));
        }
        Map<String, Object> result = new LinkedHashMap<>(8);
        List<Vendor> visibleVendors =
                vendors.selectList(null).stream()
                        .filter(v -> Boolean.TRUE.equals(v.getEnabled()))
                        .sorted(Comparator.comparing(Vendor::getSortOrder))
                        .toList();
        Set<Long> vendorIds = new HashSet<>();
        visibleVendors.forEach(v -> vendorIds.add(v.getId()));
        List<Product> all = products.selectList(null);
        Set<Long> enabledIds = new HashSet<>();
        all.stream()
                .filter(p -> Boolean.TRUE.equals(p.getEnabled()))
                .forEach(p -> enabledIds.add(p.getId()));
        List<Product> visible =
                all.stream()
                        .filter(
                                p ->
                                        Boolean.TRUE.equals(p.getEnabled())
                                                && vendorIds.contains(p.getVendorId())
                                                && (p.getParentId() == null
                                                        || enabledIds.contains(p.getParentId())))
                        .sorted(Comparator.comparing(Product::getSortOrder))
                        .toList();
        result.put("vendors", visibleVendors);
        result.put("products", visible);
        Set<Long> productIds = new HashSet<>();
        visible.forEach(p -> productIds.add(p.getId()));
        result.put(
                "regions",
                regions.selectList(null).stream()
                        .filter(
                                r ->
                                        Boolean.TRUE.equals(r.getEnabled())
                                                && productIds.contains(r.getProductId()))
                        .toList());
        result.put(
                "channels",
                channels.selectList(null).stream()
                        .filter(
                                c ->
                                        Boolean.TRUE.equals(c.getEnabled())
                                                && (c.getProductId() == null
                                                        || productIds.contains(c.getProductId())))
                        .sorted(Comparator.comparing(Channel::getSortOrder))
                        .toList());
        result.put(
                "links",
                links.selectList(null).stream()
                        .filter(l -> Boolean.TRUE.equals(l.getEnabled()))
                        .sorted(Comparator.comparing(Link::getSortOrder))
                        .toList());
        Map<String, String> publicSettings = new LinkedHashMap<>(8);
        Set<String> allowed =
                Set.of(
                        "siteTitle",
                        "siteDescription",
                        "refreshSeconds",
                        "footerText",
                        "announcement");
        settings.selectList(null).stream()
                .filter(s -> allowed.contains(s.getName()))
                .forEach(s -> publicSettings.put(s.getName(), s.getValue()));
        result.put("settings", publicSettings);
        cache.put(json.writeValueAsString(result));
        return CommonResult.success(result);
    }
}
