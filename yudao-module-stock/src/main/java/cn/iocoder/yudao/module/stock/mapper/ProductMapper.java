package cn.iocoder.yudao.module.stock.mapper;

import cn.iocoder.yudao.module.stock.domain.Product;

import org.apache.ibatis.annotations.*;

import java.util.List;

/** Product持久层。 @author vps */
@Mapper
public interface ProductMapper {
    @Select(
            "<script>SELECT id, source_page, source_key, last_synced_at, `name`, `vendor_id`,"
                + " `parent_id`, `group_flag`, `specification`, `route_tags`, `description`,"
                + " `purchase_url`, `source_url`, `speed_url`, `looking_glass_url`, `review_url`,"
                + " `stock_status`, `interval_seconds`, `last_checked_at`, `monitor_enabled`,"
                + " `sort_order`, `enabled`, create_time, update_time FROM vps_product <where><if"
                + " test='name != null and name != &quot;&quot;'>name LIKE CONCAT('%', #{name},"
                + " '%')</if></where> ORDER BY id</script>")
    List<Product> selectList(@Param("name") String name);

    @Select(
            "SELECT id, source_page, source_key, last_synced_at, `name`, `vendor_id`, `parent_id`,"
                + " `group_flag`, `specification`, `route_tags`, `description`, `purchase_url`,"
                + " `source_url`, `speed_url`, `looking_glass_url`, `review_url`, `stock_status`,"
                + " `interval_seconds`, `last_checked_at`, `monitor_enabled`, `sort_order`,"
                + " `enabled`, create_time, update_time FROM vps_product WHERE id = #{id}")
    Product selectById(Long id);

    @Insert(
            "INSERT INTO vps_product (source_page, source_key, `name`, `vendor_id`, `parent_id`,"
                + " `group_flag`, `specification`, `route_tags`, `description`, `purchase_url`,"
                + " `source_url`, `speed_url`, `looking_glass_url`, `review_url`, `stock_status`,"
                + " `interval_seconds`, `last_checked_at`, `monitor_enabled`, `sort_order`,"
                + " `enabled`) VALUES (#{sourcePage}, #{sourceKey}, #{name}, #{vendorId},"
                + " #{parentId}, #{groupFlag}, #{specification}, #{routeTags}, #{description},"
                + " #{purchaseUrl}, #{sourceUrl}, #{speedUrl}, #{lookingGlassUrl}, #{reviewUrl},"
                + " #{stockStatus}, #{intervalSeconds}, #{lastCheckedAt}, #{monitorEnabled},"
                + " #{sortOrder}, #{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Product item);

    @Update(
            "UPDATE vps_product SET source_page=#{sourcePage}, source_key=#{sourceKey}, `name` ="
                + " #{name}, `vendor_id` = #{vendorId}, `parent_id` = #{parentId}, `group_flag` ="
                + " #{groupFlag}, `specification` = #{specification}, `route_tags` = #{routeTags},"
                + " `description` = #{description}, `purchase_url` = #{purchaseUrl}, `source_url` ="
                + " #{sourceUrl}, `speed_url` = #{speedUrl}, `looking_glass_url` ="
                + " #{lookingGlassUrl}, `review_url` = #{reviewUrl}, `stock_status` ="
                + " #{stockStatus}, `interval_seconds` = #{intervalSeconds}, `last_checked_at` ="
                + " #{lastCheckedAt}, `monitor_enabled` = #{monitorEnabled}, `sort_order` ="
                + " #{sortOrder}, `enabled` = #{enabled}, update_time=NOW() WHERE id=#{id}")
    int update(Product item);

    @Delete("DELETE FROM vps_product WHERE id=#{id}")
    int delete(Long id);

    @Select(
            "<script>SELECT * FROM vps_product <where><if test='name != null and name !="
                + " &quot;&quot;'>name LIKE CONCAT('%', #{name}, '%')</if></where> ORDER BY id DESC"
                + " LIMIT #{limit} OFFSET #{offset}</script>")
    List<Product> selectPage(
            @Param("name") String name, @Param("offset") int offset, @Param("limit") int limit);

    @Select(
            "<script>SELECT COUNT(*) FROM vps_product <where><if test='name != null and name !="
                    + " &quot;&quot;'>name LIKE CONCAT('%', #{name}, '%')</if></where></script>")
    long count(@Param("name") String name);

    @Update(
            "UPDATE vps_product SET stock_status=#{status}, last_checked_at=#{checked},"
                    + " last_synced_at=NOW(), update_time=NOW() WHERE id=#{id} AND"
                    + " stock_status=#{previous} AND monitor_enabled=1")
    int updateStock(
            @Param("id") Long id,
            @Param("previous") String previous,
            @Param("status") String status,
            @Param("checked") java.time.LocalDateTime checked);
}
