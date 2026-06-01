package org.example.campusgoodstradingplatform.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DatabaseInitMapper {
    @Update("${sql}")
    int execute(@Param("sql") String sql);

    @Select("SELECT COUNT(*) FROM users")
    int countUsers();

    @Select("""
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = #{tableName} AND column_name = #{columnName}
            """)
    int countColumn(@Param("tableName") String tableName, @Param("columnName") String columnName);
}
