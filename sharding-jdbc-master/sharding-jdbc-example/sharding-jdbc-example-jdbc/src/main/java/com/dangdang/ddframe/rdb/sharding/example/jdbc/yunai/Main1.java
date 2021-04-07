package com.dangdang.ddframe.rdb.sharding.example.jdbc.yunai;

import com.dangdang.ddframe.rdb.sharding.api.rule.DataSourceRule;
import com.dangdang.ddframe.rdb.sharding.api.rule.ShardingRule;
import com.dangdang.ddframe.rdb.sharding.api.rule.TableRule;
import com.dangdang.ddframe.rdb.sharding.jdbc.core.datasource.ShardingDataSource;
import org.apache.commons.dbcp.BasicDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by yunai on 2017/7/20.
 */
public class Main1 {

    private static DataSourceRule createDataSourceRule(String dataSourceNameX) {
        Map<String, DataSource> dataSourceHashMap = new HashMap<>();
        for (String dataSourceName : new String[]{"ds_jdbc_0", "ds_jdbc_1"}) {
            BasicDataSource dataSource = new BasicDataSource();
            dataSource.setDriverClassName(com.mysql.jdbc.Driver.class.getName());
            dataSource.setUrl(String.format("jdbc:mysql://localhost:33061/%s", dataSourceName)); // TODO 修改：芋艿，数据源
            dataSource.setUsername("root");
            dataSource.setPassword("123456"); // TODO 修改：芋艿，增加密码
            dataSourceHashMap.put(dataSourceName, dataSource);
        }

        DataSourceRule dataSourceRule = new DataSourceRule(dataSourceHashMap);
        return dataSourceRule;
    }

    @SuppressWarnings("Duplicates")
    public static void main(String[] args) {
        // A 库 t_order
//        DataSourceRule orderRule = createDataSourceRule("ds_jdbc_0");
        TableRule orderTableRule = TableRule.builder("t_order")
                .actualTables(Collections.singletonList("t_order"))
                .dataSourceNames(Collections.singleton("ds_jdbc_0"))
                .build()
                ;
        // B 库 t_order_item
//        DataSourceRule itemRule = createDataSourceRule("ds_jdbc_1");
        TableRule itemTableRule = TableRule.builder("t_order_item")
                .actualTables(Collections.singletonList("t_order_item"))
                .dataSourceNames(Collections.singleton("ds_jdbc_1")).build();
        //
        ShardingRule shardingRule = ShardingRule.builder()
                .dataSourceRule(createDataSourceRule(null))
                .tableRules(Arrays.asList(orderTableRule, itemTableRule))
//                .bindingTableRules(Arrays.asList(new BindingTableRule(Collections.singletonList(orderTableRule)),
//                        new BindingTableRule(Collections.singletonList(itemTableRule))))
//                .bindingTableRules(Collections.singletonList(new BindingTableRule(Arrays.asList(orderTableRule, itemTableRule))))
//                .databaseShardingStrategy(new DatabaseShardingStrategy("user_id", new ModuloDatabaseShardingAlgorithm()))
//                .tableShardingStrategy(new TableShardingStrategy("order_id", new ModuloTableShardingAlgorithm()))

                .build();
        DataSource dataSource = new ShardingDataSource(shardingRule);
        String sql = "SELECT i.* FROM t_order o JOIN t_order_item i ON o.order_id=i.order_id";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
//            preparedStatement.setInt(1, 10);
//            preparedStatement.setInt(2, 1001);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                    System.out.println(rs.getInt(1));
                    System.out.println(rs.getInt(2));
                    System.out.println(rs.getInt(3));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

}
