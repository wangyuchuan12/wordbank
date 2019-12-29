package com.wyc.common.util;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JdbcUtil {
    public static List<Map<String,Object>> executeQuery(DataSource dataSource, String sql, List<Object> params)throws SQLException{
        System.out.println(sql);
        Statement statement = null;
        Connection connection = dataSource.getConnection();
        if(CommonUtil.isNotEmpty(params)){
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            statement = preparedStatement;
            for(int i = 0;i<params.size();i++){
                preparedStatement.setObject(i+1,params.get(i));
            }
            preparedStatement.execute();
        }else{
            try{
                statement = connection.createStatement();
                statement.execute(sql);
            }catch (SQLException e){
                statement.close();
                connection.close();
                throw e;
            }
        }

        ResultSet resultSet = statement.getResultSet();
        List<Map<String,Object>> list = null;
        if(CommonUtil.isEmpty(resultSet)){
            statement.close();
            connection.close();
        }else{
            list = new ArrayList<>();
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            Integer columnCount = resultSetMetaData.getColumnCount();
            while(resultSet.next()){
                Map<String,Object> map = new HashMap<>();
                for(int i = 0;i<columnCount;i++){
                    String columnName = resultSetMetaData.getColumnLabel(i+1);
                    map.put(columnName,resultSet.getObject(columnName));
                }
                list.add(map);
            }

            try{
                statement.close();
                connection.close();
            }catch (Exception e){
                e.printStackTrace();
                throw e;
            }
        }
        return list;
    }

    public static List<Map<String,Object>> executeQuery(DataSource dataSource,String sql,Object ... params)throws SQLException{
        List<Object> list = null;
        if(CommonUtil.isNotEmpty(params)&&params.length>0){
            list = new ArrayList<>();
            for(int i = 0;i<params.length;i++){
                list.add(params[i]);
            }
        }
        return executeQuery(dataSource,sql,list);
    }
}
