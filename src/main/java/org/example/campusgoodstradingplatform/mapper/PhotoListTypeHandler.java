package org.example.campusgoodstradingplatform.mapper;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class PhotoListTypeHandler extends BaseTypeHandler<List<String>> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, join(parameter));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return split(rs.getString(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return split(rs.getString(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return split(cs.getString(columnIndex));
    }

    private List<String> split(String photos) {
        if (photos == null || photos.isBlank()) {
            return List.of("/images/product-default.svg");
        }
        return Arrays.stream(photos.split("\\|")).filter(value -> !value.isBlank()).toList();
    }

    private String join(List<String> photos) {
        if (photos == null || photos.isEmpty()) {
            return "/images/product-default.svg";
        }
        return String.join("|", photos);
    }
}
