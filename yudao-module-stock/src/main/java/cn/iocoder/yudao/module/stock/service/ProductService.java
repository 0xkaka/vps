package cn.iocoder.yudao.module.stock.service;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.stock.domain.Product;
import cn.iocoder.yudao.module.stock.mapper.ProductMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Product业务服务。 @author vps */
@Service
public class ProductService {
    private final ProductMapper mapper;
    private final CatalogCache cache;

    public ProductService(ProductMapper mapper, CatalogCache cache) {
        this.mapper = mapper;
        this.cache = cache;
    }

    public cn.iocoder.yudao.framework.common.pojo.PageResult<Product> page(
            String name, int pageNo, int pageSize) {
        return new cn.iocoder.yudao.framework.common.pojo.PageResult<>(
                mapper.selectPage(name, (pageNo - 1) * pageSize, pageSize), mapper.count(name));
    }

    public List<Product> list(String name) {
        return mapper.selectList(name);
    }

    public Product get(Long id) {
        return mapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public int save(Product item) {
        StockValidation.validate(item);
        if (Boolean.TRUE.equals(item.getMonitorEnabled())
                && (item.getSourceKey() == null || item.getSourceKey().isBlank())) {
            if (item.getSourceUrl() == null || item.getPurchaseUrl() == null) {
                throw new ServiceException(1_050_000_001, "开启原站同步需要来源地址和购买链接");
            }
            item.setSourceKey(
                    SourceParser.key(
                            item.getSourceUrl(),
                            item.getPurchaseUrl(),
                            Boolean.TRUE.equals(item.getGroupFlag())));
        }
        if (item.getParentId() != null) {
            Product parent = mapper.selectById(item.getParentId());
            if (parent == null
                    || !Boolean.TRUE.equals(parent.getGroupFlag())
                    || parent.getParentId() != null
                    || Boolean.TRUE.equals(item.getGroupFlag())
                    || !parent.getVendorId().equals(item.getVendorId())) {
                throw new ServiceException(1_050_000_001, "上级必须为同商家的一级分组，分组不能嵌套");
            }
        }
        if (item.getId() != null
                && mapper.selectList(null).stream()
                        .anyMatch(
                                p ->
                                        item.getId().equals(p.getParentId())
                                                && (!Boolean.TRUE.equals(item.getGroupFlag())
                                                        || !p.getVendorId()
                                                                .equals(item.getVendorId())))) {
            throw new ServiceException(1_050_000_001, "请先调整分组下的子产品");
        }
        if (item.getId() != null && mapper.selectById(item.getId()) == null) {
            throw new ServiceException(1_050_000_001, "记录不存在");
        }
        int result = item.getId() == null ? mapper.insert(item) : mapper.update(item);
        cache.invalidateAfterCommit();
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public int delete(Long id) {
        int result = mapper.delete(id);
        cache.invalidateAfterCommit();
        return result;
    }
}
