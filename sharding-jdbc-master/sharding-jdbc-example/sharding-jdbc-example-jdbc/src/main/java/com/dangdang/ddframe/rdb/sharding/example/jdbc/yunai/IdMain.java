package com.dangdang.ddframe.rdb.sharding.example.jdbc.yunai;

import com.dangdang.ddframe.rdb.sharding.api.ShardingValue;
import com.dangdang.ddframe.rdb.sharding.api.rule.DataSourceRule;
import com.dangdang.ddframe.rdb.sharding.api.rule.ShardingRule;
import com.dangdang.ddframe.rdb.sharding.api.rule.TableRule;
import com.dangdang.ddframe.rdb.sharding.api.strategy.database.DatabaseShardingStrategy;
import com.dangdang.ddframe.rdb.sharding.api.strategy.database.SingleKeyDatabaseShardingAlgorithm;
import com.dangdang.ddframe.rdb.sharding.api.strategy.table.SingleKeyTableShardingAlgorithm;
import com.dangdang.ddframe.rdb.sharding.api.strategy.table.TableShardingStrategy;
import com.dangdang.ddframe.rdb.sharding.jdbc.core.datasource.ShardingDataSource;
import org.apache.commons.dbcp.BasicDataSource;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@SuppressWarnings("Duplicates")
public class IdMain {

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

    public static void main(String[] args) {
        SingleKeyDatabaseShardingAlgorithm databaseShardingAlgorithm = new SingleKeyDatabaseShardingAlgorithm<Long>() {

            @Override
            public String doEqualSharding(Collection<String> availableTargetNames, ShardingValue<Long> shardingValue) {
                List<String> list = new ArrayList<>(availableTargetNames);
                return list.get(shardingValue.getValue().intValue() % list.size());
            }

            @Override
            public Collection<String> doInSharding(Collection<String> availableTargetNames, ShardingValue<Long> shardingValue) {
                return null;
            }

            @Override
            public Collection<String> doBetweenSharding(Collection<String> availableTargetNames, ShardingValue<Long> shardingValue) {
                return null;
            }
        };

        SingleKeyTableShardingAlgorithm tableShardingAlgorithm = new SingleKeyTableShardingAlgorithm<Long>() {

            @Override
            public String doEqualSharding(Collection<String> availableTargetNames, ShardingValue<Long> shardingValue) {
                List<String> list = new ArrayList<>(availableTargetNames);
                return list.get(shardingValue.getValue().intValue() % list.size());
            }

            @Override
            public Collection<String> doInSharding(Collection<String> availableTargetNames, ShardingValue<Long> shardingValue) {
                return null;
            }

            @Override
            public Collection<String> doBetweenSharding(Collection<String> availableTargetNames, ShardingValue<Long> shardingValue) {
                return null;
            }
        };

        // 初始化
        TableRule rule = TableRule
                .builder("t_order")
                .actualTables(Arrays.asList("t_order_01"
//                        , "t_order_02"
                ))

//                .dataSourceNames(Arrays.asList("single_db_single_table"))
                .dataSourceRule(new DataSourceRule(buildDataSourceRule2("db1")))

                .databaseShardingStrategy(new DatabaseShardingStrategy("user_id", databaseShardingAlgorithm))
                .tableShardingStrategy(new TableShardingStrategy("id", tableShardingAlgorithm))
//                .generateKeyColumn("id")
                .build();

        ShardingRule shardingRule = ShardingRule.builder()
                .tableRules(Collections.singleton(rule))

                .dataSourceRule(new DataSourceRule(buildDataSourceRule("db1", "db2")))
                .build();
        DataSource dataSource = new ShardingDataSource(shardingRule);
        // 执行
        String sql = "INSERT INTO t_order(uid, nickname, pid) VALUES (1, '2', ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, 100);
            ps.execute();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                System.out.println("id:" + rs.getLong(1));
            }
            System.out.println(ps.getGeneratedKeys());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("");
    }

}
