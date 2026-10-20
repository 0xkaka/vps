package cn.iocoder.yudao.module.stock.mapper;

import cn.iocoder.yudao.module.stock.domain.CheckLog;

import org.apache.ibatis.annotations.*;

import java.util.List;

/** CheckLog持久层。 @author vps */
@Mapper
public interface CheckLogMapper {
    @Select(
            "<script>SELECT id, `name`, `product_id`, `previous_status`, `stock_status`, `message`,"
                + " create_time, update_time FROM vps_check_log <where><if test='name != null and"
                + " name != &quot;&quot;'>name LIKE CONCAT('%', #{name}, '%')</if></where> ORDER BY"
                + " id DESC</script>")
    List<CheckLog> selectList(@Param("name") String name);

    @Select(
            "SELECT id, `name`, `product_id`, `previous_status`, `stock_status`, `message`,"
                    + " create_time, update_time FROM vps_check_log WHERE id = #{id}")
    CheckLog selectById(Long id);

    @Insert(
            "INSERT INTO vps_check_log (`name`, `product_id`, `previous_status`, `stock_status`,"
                + " `message`) VALUES (#{name}, #{productId}, #{previousStatus}, #{stockStatus},"
                + " #{message})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CheckLog item);

    @Update(
            "UPDATE vps_check_log SET `name` = #{name}, `product_id` = #{productId},"
                    + " `previous_status` = #{previousStatus}, `stock_status` = #{stockStatus},"
                    + " `message` = #{message}, update_time=NOW() WHERE id=#{id}")
    int update(CheckLog item);

    @Delete("DELETE FROM vps_check_log WHERE id=#{id}")
    int delete(Long id);

    @Select(
            "<script>SELECT * FROM vps_check_log <where><if test='name != null and name !="
                + " &quot;&quot;'>name LIKE CONCAT('%', #{name}, '%')</if></where> ORDER BY id DESC"
                + " LIMIT #{limit} OFFSET #{offset}</script>")
    List<CheckLog> selectPage(
            @Param("name") String name, @Param("offset") int offset, @Param("limit") int limit);

    @Select(
            "<script>SELECT COUNT(*) FROM vps_check_log <where><if test='name != null and name !="
                    + " &quot;&quot;'>name LIKE CONCAT('%', #{name}, '%')</if></where></script>")
    long count(@Param("name") String name);
}
