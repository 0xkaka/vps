package cn.iocoder.yudao.module.stock.mapper;

import cn.iocoder.yudao.module.stock.domain.Subscriber;

import org.apache.ibatis.annotations.*;

import java.util.List;

/** Subscriber持久层。 @author vps */
@Mapper
public interface SubscriberMapper {
    @Select(
            "<script>SELECT id, `name`, `status`, create_time, update_time FROM vps_subscriber"
                + " <where><if test='name != null and name != &quot;&quot;'>name LIKE CONCAT('%',"
                + " #{name}, '%')</if></where> ORDER BY id</script>")
    List<Subscriber> selectList(@Param("name") String name);

    @Select(
            "SELECT id, `name`, `status`, create_time, update_time FROM vps_subscriber WHERE id ="
                    + " #{id}")
    Subscriber selectById(Long id);

    @Insert("INSERT INTO vps_subscriber (`name`, `status`) VALUES (#{name}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Subscriber item);

    @Update(
            "UPDATE vps_subscriber SET `name` = #{name}, `status` = #{status}, update_time=NOW()"
                    + " WHERE id=#{id}")
    int update(Subscriber item);

    @Delete("DELETE FROM vps_subscriber WHERE id=#{id}")
    int delete(Long id);

    @Select(
            "<script>SELECT * FROM vps_subscriber <where><if test='name != null and name !="
                + " &quot;&quot;'>name LIKE CONCAT('%', #{name}, '%')</if></where> ORDER BY id DESC"
                + " LIMIT #{limit} OFFSET #{offset}</script>")
    List<Subscriber> selectPage(
            @Param("name") String name, @Param("offset") int offset, @Param("limit") int limit);

    @Select(
            "<script>SELECT COUNT(*) FROM vps_subscriber <where><if test='name != null and name !="
                    + " &quot;&quot;'>name LIKE CONCAT('%', #{name}, '%')</if></where></script>")
    long count(@Param("name") String name);
}
