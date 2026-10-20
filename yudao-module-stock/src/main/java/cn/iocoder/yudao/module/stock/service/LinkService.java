package cn.iocoder.yudao.module.stock.service;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.stock.domain.Link;
import cn.iocoder.yudao.module.stock.mapper.LinkMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Link业务服务。 @author vps */
@Service
public class LinkService {
    private final LinkMapper mapper;
    private final CatalogCache cache;

    public LinkService(LinkMapper mapper, CatalogCache cache) {
        this.mapper = mapper;
        this.cache = cache;
    }

    public cn.iocoder.yudao.framework.common.pojo.PageResult<Link> page(
            String name, int pageNo, int pageSize) {
        return new cn.iocoder.yudao.framework.common.pojo.PageResult<>(
                mapper.selectPage(name, (pageNo - 1) * pageSize, pageSize), mapper.count(name));
    }

    public List<Link> list(String name) {
        return mapper.selectList(name);
    }

    public Link get(Long id) {
        return mapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public int save(Link item) {
        StockValidation.validate(item);
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
