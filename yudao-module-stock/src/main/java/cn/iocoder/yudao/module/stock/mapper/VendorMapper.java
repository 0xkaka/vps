package cn.iocoder.yudao.module.stock.mapper;

import cn.iocoder.yudao.module.stock.domain.Vendor;

import org.apache.ibatis.annotations.*;

import java.util.List;

/** Vendor持久层。 @author vps */
@Mapper
public interface VendorMapper {
    @Select(
            "<script>SELECT id, `name`, `code`, `sort_order`, `enabled`, create_time, update_time"
                + " FROM vps_vendor <where><if test='name != null and name != &quot;&quot;'>name"
                + " LIKE CONCAT('%', #{name}, '%')</if></where> ORDER BY id</script>")
    List<Vendor> selectList(@Param("name") String name);

    @Select(
            "SELECT id, `name`, `code`, `sort_order`, `enabled`, create_time, update_time FROM"
                    + " vps_vendor WHERE id = #{id}")
    Vendor selectById(Long id);

    @Insert(
            "INSERT INTO vps_vendor (`name`, `code`, `sort_order`, `enabled`) VALUES (#{name},"
                    + " #{code}, #{sortOrder}, #{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Vendor item);

    @Update(
            "UPDATE vps_vendor SET `name` = #{name}, `code` = #{code}, `sort_order` = #{sortOrder},"
                    + " `enabled` = #{enabled}, update_time=NOW() WHERE id=#{id}")
    int update(Vendor item);

    @Delete("DELETE FROM vps_vendor WHERE id=#{id}")
    int delete(Long id);

    @Select(
            "<script>SELECT * FROM vps_vendor <where><if test='name != null and name !="
                + " &quot;&quot;'>name LIKE CONCAT('%', #{name}, '%')</if></where> ORDER BY id DESC"
                + " LIMIT #{limit} OFFSET #{offset}</script>")
    List<Vendor> selectPage(
            @Param("name") String name, @Param("offset") int offset, @Param("limit") int limit);

    @Select(
            "<script>SELECT COUNT(*) FROM vps_vendor <where><if test='name != null and name !="
                    + " &quot;&quot;'>name LIKE CONCAT('%', #{name}, '%')</if></where></script>")
    long count(@Param("name") String name);
}
