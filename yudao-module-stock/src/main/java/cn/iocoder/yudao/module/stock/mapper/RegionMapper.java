package cn.iocoder.yudao.module.stock.mapper;

import cn.iocoder.yudao.module.stock.domain.Region;

import org.apache.ibatis.annotations.*;

import java.util.List;

/** 机房库存持久层。 @author vps */
@Mapper
public interface RegionMapper {
    @Select(
            "<script>SELECT * FROM vps_region <where><if test='name != null and name !="
                    + " &quot;&quot;'>name LIKE CONCAT('%', #{name}, '%')</if></where> ORDER BY"
                    + " sort_order,id</script>")
    List<Region> selectList(@Param("name") String name);

    @Select("SELECT * FROM vps_region WHERE id=#{id}")
    Region selectById(Long id);

    @Insert(
            "INSERT INTO"
                + " vps_region(product_id,name,source_name,stock_status,purchase_url,sort_order,enabled)"
                + " VALUES(#{productId},#{name},#{sourceName},#{stockStatus},#{purchaseUrl},#{sortOrder},#{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Region item);

    @Update(
            "UPDATE vps_region SET"
                + " product_id=#{productId},name=#{name},source_name=#{sourceName},stock_status=#{stockStatus},purchase_url=#{purchaseUrl},sort_order=#{sortOrder},enabled=#{enabled},update_time=NOW()"
                + " WHERE id=#{id}")
    int update(Region item);

    @Delete("DELETE FROM vps_region WHERE id=#{id}")
    int delete(Long id);

    @Select(
            "<script>SELECT * FROM vps_region <where><if test='name != null and name !="
                    + " &quot;&quot;'>name LIKE CONCAT('%', #{name}, '%')</if></where> ORDER BY"
                    + " product_id,sort_order,id LIMIT #{limit} OFFSET #{offset}</script>")
    List<Region> selectPage(
            @Param("name") String name, @Param("offset") int offset, @Param("limit") int limit);

    @Select(
            "<script>SELECT COUNT(*) FROM vps_region <where><if test='name != null and name !="
                    + " &quot;&quot;'>name LIKE CONCAT('%', #{name}, '%')</if></where></script>")
    long count(@Param("name") String name);
}
