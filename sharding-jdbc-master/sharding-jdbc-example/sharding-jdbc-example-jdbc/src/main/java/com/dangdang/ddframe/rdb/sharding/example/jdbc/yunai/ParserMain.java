/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.dangdang.ddframe.rdb.sharding.example.jdbc.yunai;

import com.dangdang.ddframe.rdb.sharding.api.HintManager;
import com.dangdang.ddframe.rdb.sharding.api.ShardingValue;
import com.dangdang.ddframe.rdb.sharding.api.rule.DataSourceRule;
import com.dangdang.ddframe.rdb.sharding.api.rule.ShardingRule;
import com.dangdang.ddframe.rdb.sharding.api.rule.TableRule;
import com.dangdang.ddframe.rdb.sharding.api.strategy.database.DatabaseShardingStrategy;
import com.dangdang.ddframe.rdb.sharding.api.strategy.database.SingleKeyDatabaseShardingAlgorithm;
import com.dangdang.ddframe.rdb.sharding.api.strategy.table.TableShardingStrategy;
import com.dangdang.ddframe.rdb.sharding.example.jdbc.algorithm.ModuloTableShardingAlgorithm;
import com.dangdang.ddframe.rdb.sharding.jdbc.core.datasource.ShardingDataSource;
import com.google.common.collect.Range;
import org.apache.commons.dbcp.BasicDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@SuppressWarnings("Duplicates")
public final class ParserMain {

    // CHECKSTYLE:OFF
    public static void main(final String[] args) throws SQLException {
        // CHECKSTYLE:ON
        DataSource dataSource = getShardingDataSource();
        printSimpleSelect(dataSource);
//        System.out.println("--------------");
//        printGroupBy(dataSource);
//        System.out.println("--------------");
//        printHintSimpleSelect(dataSource);
    }

    private static void printSimpleSelect(final DataSource dataSource) throws SQLException {
        // String sql = "SELECT o.*, i.item_id, i.create_time FROM t_order o JOIN t_order_item i ON o.order_id=i.order_id WHERE o.user_id=? AND o.order_id=?";
//        String sql = "SELECT i.* FROM t_order o JOIN t_order_item i ON o.order_id=i.order_id JOIN t_order_item2 i2 ON o.order_id=i2.order_id WHERE o.user_id=? AND o.order_id=?";
//        String sql = "SELECT i.* FROM t_order o JOIN t_order_item i ON o.order_id=i.order_id ORDER BY i.order_id DESC";
//        String sql = "SELECT @@VERSION"; // 带变量的 SQL
//        String sql = "SELECT 0x123 AS SEQ"; // 数字 和 AS
//        String sql = "SELECT -.2 as C"; // 数字 和 AS
//        String sql = "SELECT 1.13E10 as C"; // 数字 和 AS
//        String sql = "SELECT 1.321312FE100;;"; // 科学计数法 + 浮点数
//        String sql = "select 1.321312E10;;"; // 数字 和 AS
//        String sql = "/** sb **/ SELECT `id  + `` 1`, group FROM t_user GROUP BY id"; // group by
//        String sql = "SELECT o.user_id, COUNT(DISTINCT i.item_id) AS item_count\n" +
//                "FROM t_order o JOIN t_order_item i ON o.order_id = i.order_id\n" +
//                "WHERE o.user_id % 2 = 1\n" +
//                "GROUP BY o.user_id\n" +
//                "ORDER BY COUNT(DISTINCT i.item_id) DESC, user_id DESC\n" +
//                "LIMIT 2";
//        String sql = "SELECT DISTINCT(o.user_id) `uid`, COUNT(DISTINCT i.item_id) AS item_count\n" +
////        String sql = "SELECT u.user_id user_id, COUNT(DISTINCT i.item_id) AS item_count\n" +
//                "FROM t_order o JOIN t_order_item i ON o.order_id = i.order_id\n" +
//                "WHERE o.user_id % 2 = 1\n" +
//                "GROUP BY o.user_id\n" +
//                "ORDER BY COUNT(DISTINCT i.item_id) DESC, user_id DESC\n" +
//                "LIMIT 2";
//        String sql = "SELECT `*` FROM t_order o ORDER BY o.order_id % 2"; // 测试解析表达式，复合表达式
//        String sql = "SELECT '*', order_id FROM t_order ORDER order_id";
//        String sql = "SELECT o.user_id, uid, COUNT(DISTINCT i.item_id) AS item_count, MAX(i.item_id)\n" +
//                "FROM t_order o JOIN t_order_item i ON o.order_id = i.order_id\n" +
//                "WHERE o.user_id % 2 = 1\n" +
//                "GROUP BY DATE(o.user_id)\n" +
//                "ORDER BY COUNT(DISTINCT i.item_id) DESC, user_id DESC\n" +
//                "LIMIT 2;\n";
//        String sql = "SELECT o3.* FROM (SELECT * FROM (SELECT * FROM t_order o) o2) o3 JOIN t_order_item i ON o3.order_id = i.order_id LIMIT 0, 1"; // 不报错
//        String sql = "SELECT o3.* FROM (SELECT * FROM t_order) o3 JOIN t_order_item i ON o3.order_id = i.order_id LIMIT 0, 1";
//        String sql = "SELECT o3.* FROM t_order o3 JOIN t_order_item i ON o3.order_id = i.order_id LIMIT 0, 1";
//        String sql = "SELECT o3.* FROM t_order_item i JOIN (SELECT * FROM (SELECT * FROM t_order o) o2) o3 ON o3.order_id = i.order_id ";
//        String sql = "SELECT * FROM t_user u, t_order o WHERE u.user_id = o.user_id";
//        String sql = "SELECT * FROM t_order o JOIN t_order_item i ON o.order_id = i.order_id;";
//        String sql = "SELECT * FROM t_order JOIN t_order_item_ USING (order_id);";
//        String sql = "SELECT * FROM t_order o JOIN t_order_item i ON o.order_id = i.order_id LEFT JOIN t_order_item i2 ON o.order_id = i2.order_id;";
//        String sql = "SELECT o2.* FROM (SELECT * FROM t_order o) o2";
//        String sql = "SELECT * FROM t_order WHERE (order_id = ? AND time > 0) GROUP BY user_id";
//        String sql = "SELECT * FROM t_order WHERE (order_id = ? ) GROUP BY user_id";
//        String sql = "SELECT * FROM t_order GROUP BY user_id";
//        String sql = "SELECT user_id FROM t_order GROUP BY user_id ORDER BY create_time DESC";
//        String sql = "SELECT COUNT(order_id) FROM t_order GROUP BY user_id";
//        String sql = "SELECT 1E100";
//        String sql = "SELECT * FROM a JOIN b ON a.x = b.x JOIN c ON a.x = b.x;";
//        String sql = "INSERT INTO t_user(id, name) VALUES(1, 2)";
//        String sql = "UPDATE t_user SET name = 'xiaoming'";
//        String sql = "SELECT * FROM t_user u, t_order o WHERE u.user_id = o.user_id";
//        String sql = "SELECT * FROM t_user u, t_order o WHERE 1 = o.user_id";
//        String sql = "SELECT * FROM tbl_name1 WHERE (val1=?) AND (val2=?)  ";
//        String sql = "SELECT order_id FROM t_order o WHERE o.order_id = 1";
//        String sql = "INSERT INTO tbl_temp2 (fld_id)\n" +
//                "  SELECT tbl_temp1.fld_order_id\n" +
//                "  FROM tbl_temp1 WHERE tbl_temp1.fld_order_id > 100;";
//        String sql = "UPDATE t_user set nickname = ? WHERE id = 123";
//        String sql = "SELECT COUNT(user_id) FROM t_user";
        // f
//        String sql = "SELECT o.id FROM t_order o";
//        String sql = "SELECT o3.* FROM (SELECT * FROM (SELECT * FROM t_order o) o2) o3 JOIN t_order_item i ON o3.order_id = i.order_id;";
        String sql = "SELECT o.* FROM t_order o JOIN t_order_item i ON o.order_id = i.order_id;";
//        String sql = "SELECT o3.* FROM t_order_item i JOIN (SELECT * FROM (SELECT * FROM t_order o) o2) o3 ON o3.order_id = i.order_id;";
//        String sql = "SELECT order_id FROM t_order ORDER BY order_id";
//        String sql = "SELECT order_id FROM t_order o ORDER BY o.order_id";
//        String sql = "INSERT INTO t_order (order_id, uid, nickname) VALUES (?, ?, ?)";
//        String sql = "INSERT INTO t_order (uid, nickname) VALUES (?, ?)";
//        String sql = "INSERT INTO test SET id = 4  ON DUPLICATE KEY UPDATE name = 'doubi', name = 'hehe'";
//        String sql = "UPDATE t_user SET nickname = ?, age = ? WHERE user_id = ?";
//        String sql = "DELETE IGNORE FROM t_user\n" +
//                "WHERE user_id = ?";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
//            preparedStatement.setInt(1, 10);
//            preparedStatement.setInt(2, 1001);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                    System.out.println(rs.getInt(1));
                    System.out.println(rs.getInt(2));
//                    System.out.println(rs.getInt(3));
                }
            }
        }
    }

    private static void printGroupBy(final DataSource dataSource) throws SQLException {
        String sql = "SELECT o.user_id, COUNT(*) FROM t_order o JOIN t_order_item i ON o.order_id=i.order_id GROUP BY o.user_id";
        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement preparedStatement = conn.prepareStatement(sql)
        ) {
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                System.out.println("user_id: " + rs.getInt(1) + ", count: " + rs.getInt(2));
            }
        }
    }

    private static void printHintSimpleSelect(final DataSource dataSource) throws SQLException {
        String sql = "SELECT i.* FROM t_order o JOIN t_order_item i ON o.order_id=i.order_id";
        try (
                HintManager hintManager = HintManager.getInstance();
                Connection conn = dataSource.getConnection();
                PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            hintManager.addDatabaseShardingValue("t_order", "user_id", 10);
            hintManager.addTableShardingValue("t_order", "order_id", 1001);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                    System.out.println(rs.getInt(1));
                    System.out.println(rs.getInt(2));
                    System.out.println(rs.getInt(3));
                }
            }
        }
    }

//    private static ShardingDataSource getShardingDataSource() {
//        DataSourceRule dataSourceRule = new DataSourceRule(createDataSourceMap());
////        TableRule orderTableRule = TableRule.builder("t_order").actualTables(Arrays.asList("t_order_0", "t_order_1"))
//        TableRule orderTableRule = TableRule.builder("t_order").actualTables(Arrays.asList("t_order"))
//                .dataSourceRule(dataSourceRule)
//                .generateKeyColumn("order_id")
////                .dataSourceRule(new DataSourceRule(createDataSourceMap01()))
////                .databaseShardingStrategy(new DatabaseShardingStrategy("user_id", new ModuloDatabaseShardingAlgorithm()))
//                .build();
////        TableRule orderItemTableRule = TableRule.builder("t_order_item").actualTables(Arrays.asList("t_order_item_0", "t_order_item_1")).dataSourceRule(dataSourceRule).build();
////        TableRule orderItemTableRule = TableRule.builder("t_order_item").actualTables(Arrays.asList("t_order_item_0", "t_order_item_1"))
//        TableRule orderItemTableRule = TableRule.builder("t_order_item").actualTables(Arrays.asList("t_order_item"))
//                .dataSourceRule(dataSourceRule)
////                .dataSourceRule(new DataSourceRule(createDataSourceMap02()))
//                .build();
//        ShardingRule shardingRule = ShardingRule.builder()
//                .dataSourceRule(dataSourceRule)
//                .tableRules(Arrays.asList(orderTableRule, orderItemTableRule))
//                .bindingTableRules(Collections.singletonList(new BindingTableRule(Arrays.asList(orderTableRule, orderItemTableRule))))
////                .bindingTableRules(Arrays.asList(new BindingTableRule(Collections.singletonList(orderTableRule)),
////                        new BindingTableRule(Collections.singletonList(orderItemTableRule))))
//                .databaseShardingStrategy(new DatabaseShardingStrategy("user_id", new ModuloDatabaseShardingAlgorithm()))
//                .tableShardingStrategy(new TableShardingStrategy("order_id", new ModuloTableShardingAlgorithm())).build();
//        return new ShardingDataSource(shardingRule);
//    }

    private static ShardingDataSource getShardingDataSource() {
        DataSourceRule dataSourceRule = new DataSourceRule(createDataSourceMap());
//        TableRule orderTableRule = TableRule.builder("t_order").actualTables(Arrays.asList("t_order_0", "t_order_1"))
        TableRule orderTableRule = TableRule.builder("t_order").actualTables(Arrays.asList("t_order"))
                .dataSourceRule(dataSourceRule)
                .generateKeyColumn("order_id")
//                .dataSourceRule(new DataSourceRule(createDataSourceMap01()))
//                .databaseShardingStrategy(new DatabaseShardingStrategy("user_id", new ModuloDatabaseShardingAlgorithm()))
                .build();
//        TableRule orderItemTableRule = TableRule.builder("t_order_item").actualTables(Arrays.asList("t_order_item_0", "t_order_item_1")).dataSourceRule(dataSourceRule).build();
        TableRule orderItemTableRule = TableRule.builder("t_order_item").actualTables(Arrays.asList("t_order_item_0", "t_order_item_1", "t_order_item_2", "t_order_item_3"))
//        TableRule orderItemTableRule = TableRule.builder("t_order_item").actualTables(Arrays.asList("t_order_item"))
                .dataSourceRule(dataSourceRule)
//                .dataSourceRule(new DataSourceRule(createDataSourceMap02()))
                .build();
        ShardingRule shardingRule = ShardingRule.builder()
                .dataSourceRule(dataSourceRule)
                .tableRules(Arrays.asList(orderTableRule, orderItemTableRule))
//                .bindingTableRules(Collections.singletonList(new BindingTableRule(Arrays.asList(orderTableRule, orderItemTableRule))))
//                .bindingTableRules(Arrays.asList(new BindingTableRule(Collections.singletonList(orderTableRule)),
//                        new BindingTableRule(Collections.singletonList(orderItemTableRule))))
                .databaseShardingStrategy(new DatabaseShardingStrategy("order_id", new SingleKeyDatabaseShardingAlgorithm<Integer>() {
                    @Override
                    public String doEqualSharding(final Collection<String> dataSourceNames, final ShardingValue<Integer> shardingValue) {
                        for (String each : dataSourceNames) {
                            if (each.endsWith(shardingValue.getValue() % 2 + "")) {
                                return each;
                            }
                        }
                        throw new IllegalArgumentException();
                    }

                    @Override
                    public Collection<String> doInSharding(final Collection<String> dataSourceNames, final ShardingValue<Integer> shardingValue) {
                        Collection<String> result = new LinkedHashSet<>(dataSourceNames.size());
                        for (Integer value : shardingValue.getValues()) {
                            for (String dataSourceName : dataSourceNames) {
                                if (dataSourceName.endsWith(value % 2 + "")) {
                                    result.add(dataSourceName);
                                }
                            }
                        }
                        return result;
                    }

                    @Override
                    public Collection<String> doBetweenSharding(final Collection<String> dataSourceNames, final ShardingValue<Integer> shardingValue) {
                        Collection<String> result = new LinkedHashSet<>(dataSourceNames.size());
                        Range<Integer> range = shardingValue.getValueRange();
                        for (Integer i = range.lowerEndpoint(); i <= range.upperEndpoint(); i++) {
                            for (String each : dataSourceNames) {
                                if (each.endsWith(i % 2 + "")) {
                                    result.add(each);
                                }
                            }
                        }
                        return result;
                    }
                }))
                .tableShardingStrategy(new TableShardingStrategy("order_id", new ModuloTableShardingAlgorithm())).build();
        return new ShardingDataSource(shardingRule);
    }

    private static Map<String, DataSource> createDataSourceMap() {
        Map<String, DataSource> result = new HashMap<>(2);
        result.put("ds_jdbc_0", createDataSource("ds_jdbc_0"));
        result.put("ds_jdbc_1", createDataSource("ds_jdbc_1"));
        return result;
    }

    private static Map<String, DataSource> createDataSourceMap01() {
        Map<String, DataSource> result = new HashMap<>(2);
        result.put("ds_jdbc_0", createDataSource("ds_jdbc_0"));
        return result;
    }

    private static Map<String, DataSource> createDataSourceMap02() {
        Map<String, DataSource> result = new HashMap<>(2);
        result.put("ds_jdbc_1", createDataSource("ds_jdbc_1"));
        return result;
    }

    private static DataSource createDataSource(final String dataSourceName) {
        BasicDataSource result = new BasicDataSource();
        result.setDriverClassName(com.mysql.jdbc.Driver.class.getName());
        result.setUrl(String.format("jdbc:mysql://localhost:33061/%s", dataSourceName)); // TODO 修改：芋艿，数据源
        result.setUsername("root");
        result.setPassword("123456"); // TODO 修改：芋艿，增加密码
        return result;
    }
}


