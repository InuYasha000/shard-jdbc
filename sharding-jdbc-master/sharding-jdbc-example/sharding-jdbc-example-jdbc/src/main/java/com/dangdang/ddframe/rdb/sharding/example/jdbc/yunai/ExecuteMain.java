package com.dangdang.ddframe.rdb.sharding.example.jdbc.yunai;

import com.dangdang.ddframe.rdb.sharding.api.ShardingValue;
import com.dangdang.ddframe.rdb.sharding.api.rule.DataSourceRule;
import com.dangdang.ddframe.rdb.sharding.api.rule.ShardingRule;
import com.dangdang.ddframe.rdb.sharding.api.rule.TableRule;
import com.dangdang.ddframe.rdb.sharding.api.strategy.database.DatabaseShardingStrategy;
import com.dangdang.ddframe.rdb.sharding.api.strategy.database.SingleKeyDatabaseShardingAlgorithm;
import com.dangdang.ddframe.rdb.sharding.api.strategy.table.SingleKeyTableShardingAlgorithm;
import com.dangdang.ddframe.rdb.sharding.api.strategy.table.TableShardingStrategy;
import com.dangdang.ddframe.rdb.sharding.executor.event.DMLExecutionEvent;
import com.dangdang.ddframe.rdb.sharding.executor.event.DQLExecutionEvent;
import com.dangdang.ddframe.rdb.sharding.jdbc.core.datasource.ShardingDataSource;
import com.dangdang.ddframe.rdb.sharding.util.EventBusInstance;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.apache.commons.dbcp.BasicDataSource;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@SuppressWarnings("Duplicates")
public class ExecuteMain {

    private static DataSource createDataSource(final String dataSourceName) {
        BasicDataSource result = new BasicDataSource();
        result.setDriverClassName(com.mysql.jdbc.Driver.class.getName());
        result.setUrl(String.format("jdbc:mysql://localhost:33061/%s", dataSourceName)); // TODO 修改：芋艿，数据源
        result.setUsername("root");
        result.setPassword("123456"); // TODO 修改：芋艿，增加密码
//        result.setDefaultAutoCommit(false);


//        try {
//            result.getConnection().createStatement().addBatch("UPDATE t_order SET pid = 1");
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
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

    public static void main(String[] args) throws SQLException {
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
                .actualTables(Arrays.asList("t_order_01", "t_order_02"))

//                .dataSourceNames(Arrays.asList("single_db_single_table"))
                .dataSourceRule(new DataSourceRule(buildDataSourceRule2("multi_db_multi_table_01", "multi_db_multi_table_02")))

                .databaseShardingStrategy(new DatabaseShardingStrategy("user_id", databaseShardingAlgorithm))
                .tableShardingStrategy(new TableShardingStrategy("id", tableShardingAlgorithm))
                .generateKeyColumn("id")
                .build();

        ShardingRule shardingRule = ShardingRule.builder()
                .tableRules(Collections.singleton(rule))

                .dataSourceRule(new DataSourceRule(buildDataSourceRule("multi_db_multi_table_01", "multi_db_multi_table_02")))
                .build();
        ShardingDataSource dataSource = new ShardingDataSource(shardingRule);

        EventBusInstance.getInstance().register(new Runnable() {

            @Override
            public void run() {

            }

            @Subscribe // 订阅
            @AllowConcurrentEvents // 是否允许并发执行，即线程安全
            public void listen(final DMLExecutionEvent event) { // DMLExecutionEvent
                System.out.println("DMLExecutionEvent：" + event.getSql() + "\t" + event.getEventExecutionType());
            }

            @Subscribe // 订阅
            @AllowConcurrentEvents // 是否允许并发执行，即线程安全
            public void listen2(final DQLExecutionEvent event) { //DQLExecutionEvent
                System.out.println("DQLExecutionEvent：" + event.getSql() + "\t" + event.getEventExecutionType());
            }

        });

        if (true) {
            // 执行
            String sql = "SELECT * FROM t_order";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
//            ps.execute();
//            ResultSet rs = ps.getGeneratedKeys();
//            if (rs.next()) {
//                System.out.println("id:" + rs.getLong(1));
//            }
//            System.out.println(ps.getGeneratedKeys());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    System.out.println(rs.getLong(1));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            System.out.println("");
        } else if (false) {
            // 执行
            String sql = "UPDATE t_order SET pid = 1";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                conn.setAutoCommit(true);
//            ps.execute();
//            ResultSet rs = ps.getGeneratedKeys();
//            if (rs.next()) {
//                System.out.println("id:" + rs.getLong(1));
//            }
//            System.out.println(ps.getGeneratedKeys());

                ps.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }
            System.out.println("");
        } else if (false) {
            // 执行
            String sql = "UPDATE t_order SET pid = 6 WHERE pid = ? AND nickname = ?";
            Connection conn = dataSource.getConnection();
            conn.setAutoCommit(false);
            try (
                    PreparedStatement ps = conn.prepareStatement(sql)) {


                ps.setLong(1, 1);
                ps.setString(2, "nick");

//            ps.execute();
//            ResultSet rs = ps.getGeneratedKeys();
//            if (rs.next()) {
//                System.out.println("id:" + rs.getLong(1));
//            }
//            System.out.println(ps.getGeneratedKeys());

                ps.executeUpdate();

            } catch (Throwable e) {
                e.printStackTrace();
            }
            System.out.println("");
        } else if (true) {
            // 执行
            String sql = "UPDATE t_order SET pid = 6 WHERE user_id = ?";
            Connection conn = dataSource.getConnection();
            conn.setAutoCommit(false);
            try (
                    PreparedStatement ps = conn.prepareStatement(sql)) {
//            ps.execute();
//            ResultSet rs = ps.getGeneratedKeys();
//            if (rs.next()) {
//                System.out.println("id:" + rs.getLong(1));
//            }
//            System.out.println(ps.getGeneratedKeys());
                ps.setLong(1, 1L);
                ps.addBatch();
                ps.setLong(1, 2L);
                ps.addBatch();
                ps.setLong(1, 3L);
                ps.addBatch();



//                ps.addBatch("UPDATE t_order SET pid = 6");
//                ps.addBatch("UPDATE t_order SET pid = 7");
//                ps.addBatch("UPDATE t_order SET pid = 8");

                ps.executeBatch();

            } catch (Throwable e) {
                e.printStackTrace();
            }
            System.out.println("");
        } else {
// 执行
            String sql = "UPDATE t_order SET pid = 6";
            Connection conn = dataSource.getConnection();
            conn.setAutoCommit(false);
            try (
                    Statement ps = conn.createStatement()) {
//            ps.execute();
//            ResultSet rs = ps.getGeneratedKeys();
//            if (rs.next()) {
//                System.out.println("id:" + rs.getLong(1));
//            }
//            System.out.println(ps.getGeneratedKeys());

                ps.addBatch(sql);

                ps.executeBatch();

            } catch (Throwable e) {
                e.printStackTrace();
            }
            System.out.println("");
        }
    }

}
