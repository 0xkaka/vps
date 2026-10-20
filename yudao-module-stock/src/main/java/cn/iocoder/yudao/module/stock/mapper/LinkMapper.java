package cn.iocoder.yudao.module.stock.mapper;

import cn.iocoder.yudao.module.stock.domain.Link;

import org.apache.ibatis.annotations.*;

import java.util.List;

/** Link持久层。 @author vps */
@Mapper
public interface LinkMapper {
    @Select(
            "<script>SELECT id, `name`, `url`, `position`, `sort_order`, `enabled`, create_time,"
                    + " update_time FROM vps_link <where><if test='name != null and name !="
                    + " &quot;&quot;'>name LIKE CONCAT('%', #{name}, '%')</if></where> ORDER BY"
                    + " id</script>")
    List<Link> selectList(@Param("name") String name);

    @Select(
            "SELECT id, `name`, `url`, `position`, `sort_order`, `enabled`, create_time,"
                    + " update_time FROM vps_link WHERE id = #{id}")
    Link selectById(Long id);

    @Insert(
            "INSERT INTO vps_link (`name`, `url`, `position`, `sort_order`, `enabled`) VALUES"
                    + " (#{name}, #{url}, #{position}, #{sortOrder}, #{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Link item);

    @Update(
            "UPDATE vps_link SET `name` = #{name}, `url` = #{url}, `position` = #{position},"
                + " `sort_order` = #{sortOrder}, `enabled` = #{enabled}, update_time=NOW() WHERE"
                + " id=#{id}")
    int update(Link item);

    @Delete("DELETE FROM vps_link WHERE id=#{id}")
    int delete(Long id);

    @Select(
            "<script>SELECT * FROM vps_link <where><if test='name != null and name !="
                + " &quot;&quot;'>name LIKE CONCAT('%', #{name}, '%')</if></where> ORDER BY id DESC"
                + " LIMIT #{limit} OFFSET #{offset}</script>")
    List<Link> selectPage(
            @Param("name") String name, @Param("offset") int offset, @Param("limit") int limit);

    @Select(
            "<script>SELECT COUNT(*) FROM vps_link <where><if test='name != null and name !="
                    + " &quot;&quot;'>name LIKE CONCAT('%', #{name}, '%')</if></where></script>")
    long count(@Param("name") String name);
}
