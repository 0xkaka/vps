package cn.iocoder.yudao.module.stock.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.iocoder.yudao.module.stock.domain.Product;
import cn.iocoder.yudao.module.stock.mapper.CheckLogMapper;
import cn.iocoder.yudao.module.stock.mapper.ProductMapper;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Map;

class FastStockSyncTest {
    private Product product(long id, String url) {
        Product p = new Product();
        p.setId(id);
        p.setName("Basic");
        p.setPurchaseUrl(url);
        p.setMonitorEnabled(true);
        p.setEnabled(true);
        p.setIntervalSeconds(120);
        return p;
    }

    @Test
    void fastCheckOnlyTargetsDirectMerchants() {
        assertTrue(
                SourceSyncService.supportsFastCheck(
                        product(1, "https://bandwagonhost.com/cart.php?a=add&pid=1")));
        assertTrue(
                SourceSyncService.supportsFastCheck(
                        product(2, "https://vps.hosting/?cmd=cart&action=add&id=259")));
        assertFalse(
                SourceSyncService.supportsFastCheck(
                        product(3, "https://app.vmiss.com/cart.php?a=add&pid=50")));
        Product group = product(4, "https://vps.hosting/?id=259");
        group.setGroupFlag(true);
        assertFalse(SourceSyncService.supportsFastCheck(group));
    }

    @Test
    @SuppressWarnings("unchecked")
    void failedFastCheckKeepsInventoryAndDoesNotPollFallbackOrThirdParty() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        ProductMapper products = mock(ProductMapper.class);
        Product direct = product(1, "https://vps.hosting/?cmd=cart&action=add&id=259");
        Product thirdParty = product(2, "https://app.vmiss.com/cart.php?a=add&pid=50");
        when(products.selectList(any())).thenReturn(List.of(direct, thirdParty));
        CheckLogMapper logs = mock(CheckLogMapper.class);
        SourceParser parser = mock(SourceParser.class);
        StockChangeService changes = mock(StockChangeService.class);
        MerchantSourceService merchants = mock(MerchantSourceService.class);
        SpaceVpsSourceService space = mock(SpaceVpsSourceService.class);
        when(merchants.read(List.of(direct))).thenReturn(Map.of());
        SourceSyncService service =
                new SourceSyncService(redis, products, logs, parser, changes, merchants, space);
        assertEquals(0, service.sync(false, true));
        verify(merchants).read(List.of(direct));
        verifyNoInteractions(space, parser, changes, logs);
    }
}
