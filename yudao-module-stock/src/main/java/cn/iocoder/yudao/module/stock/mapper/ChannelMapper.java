package cn.iocoder.yudao.module.stock.mapper;

import cn.iocoder.yudao.module.stock.domain.Channel;

import org.apache.ibatis.annotations.*;

import java.util.List;

/** Channel持久层。 @author vps */
@Mapper
public interface ChannelMapper {
    @Select(
            "<script>SELECT id, `name`, `type`, `product_id`, `url`, `sort_order`, `enabled`,"
                + " create_time, update_time FROM vps_channel <where><if test='name != null and"
                + " name != &quot;&quot;'>name LIKE CONCAT('%', #{name}, '%')</if></where> ORDER BY"
                + " id</script>")
    List<Channel> selectList(@Param("name") String name);

    @Select(
            "SELECT id, `name`, `type`, `product_id`, `url`, `sort_order`, `enabled`, create_time,"
                    + " update_time FROM vps_channel WHERE id = #{id}")
    Channel selectById(Long id);

    @Insert(
            "INSERT INTO vps_channel (`name`, `type`, `product_id`, `url`, `sort_order`, `enabled`)"
                    + " VALUES (#{name}, #{type}, #{productId}, #{url}, #{sortOrder}, #{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Channel item);

    @Update(
            "UPDATE vps_channel SET `name` = #{name}, `type` = #{type}, `product_id` ="
                    + " #{productId}, `url` = #{url}, `sort_order` = #{sortOrder}, `enabled` ="
                    + " #{enabled}, update_time=NOW() WHERE id=#{id}")
    int update(Channel item);

    @Delete("DELETE FROM vps_channel WHERE id=#{id}")
    int delete(Long id);

    @Select(
            "<script>SELECT * FROM vps_channel <where><if test='name != null and name !="
                + " &quot;&quot;'>name LIKE CONCAT('%', #{name}, '%')</if></where> ORDER BY id DESC"
                + " LIMIT #{limit} OFFSET #{offset}</script>")
    List<Channel> selectPage(
            @Param("name") String name, @Param("offset") int offset, @Param("limit") int limit);

    @Select(
            "<script>SELECT COUNT(*) FROM vps_channel <where><if test='name != null and name !="
                    + " &quot;&quot;'>name LIKE CONCAT('%', #{name}, '%')</if></where></script>")
    long count(@Param("name") String name);
}
