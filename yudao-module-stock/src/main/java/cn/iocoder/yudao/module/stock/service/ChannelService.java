package cn.iocoder.yudao.module.stock.service;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.stock.domain.Channel;
import cn.iocoder.yudao.module.stock.mapper.ChannelMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Channel业务服务。 @author vps */
@Service
public class ChannelService {
    private final ChannelMapper mapper;
    private final CatalogCache cache;

    public ChannelService(ChannelMapper mapper, CatalogCache cache) {
        this.mapper = mapper;
        this.cache = cache;
    }

    public cn.iocoder.yudao.framework.common.pojo.PageResult<Channel> page(
            String name, int pageNo, int pageSize) {
        return new cn.iocoder.yudao.framework.common.pojo.PageResult<>(
                mapper.selectPage(name, (pageNo - 1) * pageSize, pageSize), mapper.count(name));
    }

    public List<Channel> list(String name) {
        return mapper.selectList(name);
    }

    public Channel get(Long id) {
        return mapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public int save(Channel item) {
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
