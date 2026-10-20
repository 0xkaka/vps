package cn.iocoder.yudao.module.stock.mapper;

import cn.iocoder.yudao.module.stock.domain.Setting;

import org.apache.ibatis.annotations.*;

import java.util.List;

/** Setting持久层。 @author vps */
@Mapper
public interface SettingMapper {
    @Select(
            "<script>SELECT id, `name`, `value`, `description`, create_time, update_time FROM"
                + " vps_setting <where><if test='name != null and name != &quot;&quot;'>name LIKE"
                + " CONCAT('%', #{name}, '%')</if></where> ORDER BY id</script>")
    List<Setting> selectList(@Param("name") String name);

    @Select(
            "SELECT id, `name`, `value`, `description`, create_time, update_time FROM vps_setting"
                    + " WHERE id = #{id}")
    Setting selectById(Long id);

    @Insert(
            "INSERT INTO vps_setting (`name`, `value`, `description`) VALUES (#{name}, #{value},"
                    + " #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Setting item);

    @Update(
            "UPDATE vps_setting SET `name` = #{name}, `value` = #{value}, `description` ="
                    + " #{description}, update_time=NOW() WHERE id=#{id}")
    int update(Setting item);

    @Delete("DELETE FROM vps_setting WHERE id=#{id}")
    int delete(Long id);

    @Select(
            "<script>SELECT * FROM vps_setting <where><if test='name != null and name !="
                + " &quot;&quot;'>name LIKE CONCAT('%', #{name}, '%')</if></where> ORDER BY id DESC"
                + " LIMIT #{limit} OFFSET #{offset}</script>")
    List<Setting> selectPage(
            @Param("name") String name, @Param("offset") int offset, @Param("limit") int limit);

    @Select(
            "<script>SELECT COUNT(*) FROM vps_setting <where><if test='name != null and name !="
                    + " &quot;&quot;'>name LIKE CONCAT('%', #{name}, '%')</if></where></script>")
    long count(@Param("name") String name);
}
