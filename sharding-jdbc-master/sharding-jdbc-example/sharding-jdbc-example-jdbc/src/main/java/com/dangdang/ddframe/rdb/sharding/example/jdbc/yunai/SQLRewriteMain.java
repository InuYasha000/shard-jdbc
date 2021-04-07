package com.dangdang.ddframe.rdb.sharding.example.jdbc.yunai;

import com.dangdang.ddframe.rdb.sharding.api.ShardingValue;
import com.dangdang.ddframe.rdb.sharding.api.rule.DataSourceRule;
import com.dangdang.ddframe.rdb.sharding.api.rule.ShardingRule;
import com.dangdang.ddframe.rdb.sharding.api.rule.TableRule;
import com.dangdang.ddframe.rdb.sharding.api.strategy.database.DatabaseShardingStrategy;
import com.dangdang.ddframe.rdb.sharding.api.strategy.database.SingleKeyDatabaseShardingAlgorithm;
import com.dangdang.ddframe.rdb.sharding.api.strategy.table.NoneTableShardingAlgorithm;
import com.dangdang.ddframe.rdb.sharding.api.strategy.table.SingleKeyTableShardingAlgorithm;
import com.dangdang.ddframe.rdb.sharding.api.strategy.table.TableShardingStrategy;
import com.dangdang.ddframe.rdb.sharding.jdbc.core.datasource.ShardingDataSource;
import org.apache.commons.dbcp.BasicDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

/**
 * 为 SQL Rewrite 准备的测试类
 */
@SuppressWarnings("Duplicates")
public class SQLRewriteMain {

    private static DataSource createDataSource(final String dataSourceName) {
        BasicDataSource result = new BasicDataSource();
        result.setDriverClassName(com.mysql.jdbc.Driver.class.getName());
        result.setUrl(String.format("jdbc:mysql://localhost:33061/%s", dataSourceName)); // TODO 修改：芋艿，数据源
        result.setUsername("root");
        result.setPassword("123456"); // TODO 修改：芋艿，增加密码
        return result;
    }

    private static Map<String, DataSource> buildDataSourceRule(String... dataSourceNames) {
        Map<String, DataSource> results = new HashMap<>();
        for (String dataSourceName : dataSourceNames) {
            results.put(dataSourceName, createDataSource(dataSourceName));
        }
        return results;
    }

    private static Map<String, DataSource> buildDataSourceRule2(String... dataSourceNames) {
        Map<String, DataSource> results = new HashMap<>();
        for (String dataSourceName : dataSourceNames) {
            results.put(dataSourceName, null);
        }
        return results;
    }

    /**
     * 测试多库多表
     * 无路由字段 + 相同路由算法 + 无bind
     */
    public static void testMultiDbMultiTable3() throws SQLException {

        SingleKeyDatabaseShardingAlgorithm databaseShardingAlgorithm = new SingleKeyDatabaseShardingAlgorithm<Integer>() {

            @Override
            public String doEqualSharding(Collection<String> availableTargetNames, ShardingValue<Integer> shardingValue) {
                int value = 0;
                if (shardingValue.getValue() instanceof Number) {
                    value = ((Number) shardingValue.getValue()).intValue();
                }
                List<String> list = new ArrayList<>(availableTargetNames);
                return list.get(value % list.size());
            }

            @Override
            public Collection<String> doInSharding(Collection<String> availableTargetNames, ShardingValue<Integer> shardingValue) {
                return null;
            }

            @Override
            public Collection<String> doBetweenSharding(Collection<String> availableTargetNames, ShardingValue<Integer> shardingValue) {
                return null;
            }
        };

        SingleKeyTableShardingAlgorithm tableShardingAlgorithm = new SingleKeyTableShardingAlgorithm<Integer>() {

            @Override
            public String doEqualSharding(Collection<String> availableTargetNames, ShardingValue<Integer> shardingValue) {
                List<String> list = new ArrayList<>(availableTargetNames);
                return list.get(shardingValue.getValue().intValue() % list.size());
            }

            @Override
            public Collection<String> doInSharding(Collection<String> availableTargetNames, ShardingValue<Integer> shardingValue) {
                return null;
            }

            @Override
            public Collection<String> doBetweenSharding(Collection<String> availableTargetNames, ShardingValue<Integer> shardingValue) {
                return null;
            }
        };

        // 初始化
        TableRule rule = TableRule
                .builder("t_order")
                .actualTables(Arrays.asList("t_order_01", "t_order_02"))

//                .dataSourceNames(Arrays.asList("single_db_single_table"))
                .dataSourceRule(new DataSourceRule(buildDataSourceRule2("multi_db_multi_table_01", "multi_db_multi_table_02")))

                .databaseShardingStrategy(new DatabaseShardingStrategy("user_id", databaseShardingAlgorithm))
                .tableShardingStrategy(new TableShardingStrategy("user_id", tableShardingAlgorithm))

                .generateKeyColumn("order_id")

                .build();

        TableRule rule2 = TableRule
                .builder("t_order_item")
                .actualTables(Arrays.asList("t_order_item_01", "t_order_item_02"))

//                .dataSourceNames(Arrays.asList("single_db_single_table"))

                .dataSourceRule(new DataSourceRule(buildDataSourceRule2("multi_db_multi_table_01", "multi_db_multi_table_02")))
                .databaseShardingStrategy(new DatabaseShardingStrategy(new NoneKeyModuloDatabaseShardingAlgorithm()))
                .tableShardingStrategy(new TableShardingStrategy("", new NoneTableShardingAlgorithm()))
                .build();

        ShardingRule shardingRule = ShardingRule.builder()
                .tableRules(Arrays.asList(rule, rule2))

                .dataSourceRule(new DataSourceRule(buildDataSourceRule("multi_db_multi_table_01", "multi_db_multi_table_02")))
                .build();
        ShardingDataSource dataSource = new ShardingDataSource(shardingRule);
        // 执行
        Connection conn = dataSource.getConnection();
        int type = 100;
        String sql = "";
        if (type == 100) { // TableToken
//            sql = "SELECT * FROM t_order o";
            sql = "SELECT o.* FROM t_order o";
//            sql = "SELECT o.order_id, o.user_id FROM t_order o";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.executeQuery();
        } else if (type == 200) { // ItemsToken AVG
            sql = "SELECT AVG(order_id) FROM t_order o";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.executeQuery();
        } else if (type == 201) { // ItemsToken GROUP BY
            sql = "SELECT order_id FROM t_order o GROUP BY order_id";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.executeQuery();
        } else if (type == 202) { // ItemsToken ORDER BY
            sql = "SELECT userId FROM t_order o ORDER BY order_id";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.executeQuery();
        } else if (type == 300) { // Row： Limit 10, 20
//            sql = "SELECT userId FROM t_order o Limit 10, 20";
//            PreparedStatement ps = conn.prepareStatement(sql);
//            ps.executeQuery();

//            sql = "SELECT userId FROM t_order o Limit ?, ?";
//            PreparedStatement ps = conn.prepareStatement(sql);
//            ps.setInt(1, 10);
//            ps.setInt(2, 10);
//            ps.executeQuery();

            sql = "SELECT userId FROM t_order o Limit ?, 100";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, 10);
//            ps.setInt(2, 10);
            ps.executeQuery();
        } else if (type == 301) { // Row： Limit 10
            sql = "SELECT userId FROM t_order o Limit ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, 10);
            ps.executeQuery();
        } else if (type == 400) { // 插入 + 空
            sql = "INSERT INTO t_order(user_id) VALUES (1)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.execute();
        } else if (type == 401) { // 插入 + 占位符
            sql = "INSERT INTO t_order(order_id, user_id) VALUES (?, 1)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setLong(1, 100);
            ps.execute();
        } else if (type == 402) { // 插入 + 数字
            sql = "INSERT INTO t_order(order_id, user_id) VALUES (100, 1)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.execute();
        } else if (type == 403) { // 插入 + 数字 + 有其他占位符
            sql = "INSERT INTO t_order(order_id, user_id) VALUES (100, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setLong(1, 1);
            ps.execute();
        } else if (type == 404) { // 插入 + 占位 + 有占位符
            sql = "INSERT INTO t_order(user_id) VALUES (?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setLong(1, 1);
            ps.execute();
        } else if (type == 405) { // 插入 + 占位符（不设置）
            sql = "INSERT INTO t_order(order_id, user_id, nickname) VALUES (?, 1, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(2, "nickname");
            ps.execute();
        }
//        try (
//             ) {
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//
//        if (true) {
//            String sql = "SELECT * FROM t_order o WHERE o.order_id = ?";
//            try (Connection conn = dataSource.getConnection();
//                 PreparedStatement ps = conn.prepareStatement(sql)) {
//                ps.setInt(1, 1);
//                try (ResultSet rs = ps.executeQuery()) {
//                    while (rs.next()) {
//                        System.out.println(rs.getLong(1) + "\t" + rs.getLong(2) + "\t" + rs.getString(3));
//                    }
//                }
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
//            System.out.println("");
//        } else {
//            String sql = "SELECT * FROM t_order o join t_order_item i ON o.order_id = i.order_id WHERE o.order_id = 10";
//            try (Connection conn = dataSource.getConnection();
//                 PreparedStatement ps = conn.prepareStatement(sql)) {
//                try (ResultSet rs = ps.executeQuery()) {
//                    while (rs.next()) {
//                        System.out.println(rs.getLong(1) + "\t" + rs.getLong(2) + "\t" + rs.getString(3));
//                    }
//                }
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
//            System.out.println("");
//        }
    }

    public static void main(String[] args) throws SQLException {
        testMultiDbMultiTable3();
    }

}