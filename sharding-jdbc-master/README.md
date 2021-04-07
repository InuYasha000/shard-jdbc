# Sharding-JDBC - JDBC driver for shard databases and tables 

# 个人博客

[http://www.iocoder.cn](http://www.iocoder.cn/?github)

-------

![](http://www.iocoder.cn/images/common/wechat_mp.jpeg)

> 🙂🙂🙂关注**微信公众号：【芋艿的后端小屋】**有福利：  
> 1. RocketMQ / MyCAT / Sharding-JDBC **所有**源码分析文章列表  
> 2. RocketMQ / MyCAT / Sharding-JDBC **中文注释源码 GitHub 地址**  
> 3. 您对于源码的疑问每条留言**都**将得到**认真**回复。**甚至不知道如何读源码也可以请教噢**。  
> 4. **新的**源码解析文章**实时**收到通知。**每周更新一篇左右**。

-------

* 知识星球：![知识星球](http://www.iocoder.cn/images/Architecture/2017_12_29/01.png)

* 数据库中间件 **Sharding-JDBC**
    * [《Sharding-JDBC 源码分析 —— 为什么阅读 Sharding-JDBC 源码？》](http://www.iocoder.cn/Sharding-JDBC/why-read-Sharding-JDBC-source-code?github&1602)
    * [《Sharding-JDBC 源码分析 —— SQL 解析（一）之词法解析》](http://www.iocoder.cn/Sharding-JDBC/sql-parse-1?github&1602)
    * [《Sharding-JDBC 源码分析 —— SQL 解析（二）之SQL解析》](http://www.iocoder.cn/Sharding-JDBC/sql-parse-2?github&1602)
    * [《Sharding-JDBC 源码分析 —— SQL 解析（三）之查询SQL》](http://www.iocoder.cn/Sharding-JDBC/sql-parse-3?github&1602)
    * [《Sharding-JDBC 源码分析 —— SQL 解析（四）之插入SQL》](http://www.iocoder.cn/Sharding-JDBC/sql-parse-4?github&1602)
    * [《Sharding-JDBC 源码分析 —— SQL 解析（五）之更新SQL》](http://www.iocoder.cn/Sharding-JDBC/sql-parse-5?github&1602)
    * [《Sharding-JDBC 源码分析 —— SQL 解析（六）之删除SQL》](http://www.iocoder.cn/Sharding-JDBC/sql-parse-6?github&1602)
    * [《Sharding-JDBC 源码分析 —— SQL 解析（七）之DDL》](http://www.iocoder.cn/Sharding-JDBC/sql-parse-7?github&1602)
    * [《Sharding-JDBC 源码分析 —— SQL 路由（一）之分库分表配置》](http://www.iocoder.cn/Sharding-JDBC/sql-route-1?github&1602)
    * [《Sharding-JDBC 源码分析 —— SQL 路由（二）之分库分表路由》](http://www.iocoder.cn/Sharding-JDBC/sql-route-2?github&1602)
    * [《Sharding-JDBC 源码分析 —— SQL 路由（三）之Spring与YAML配置》](http://www.iocoder.cn/Sharding-JDBC/sql-route-3?github&1602)
    * [《Sharding-JDBC 源码分析 —— SQL 改写》](http://www.iocoder.cn/Sharding-JDBC/sql-rewrite?github&1602)
    * [《Sharding-JDBC 源码分析 —— 分布式主键》](http://www.iocoder.cn/Sharding-JDBC/distributed-id?github&1602)
    * [《Sharding-JDBC 源码分析 —— SQL 执行》](http://www.iocoder.cn/Sharding-JDBC/sql-execute?github&1602)
    * [《Sharding-JDBC 源码分析 —— 结果归并》](http://www.iocoder.cn/Sharding-JDBC/result-merger?github&1602)
    * [《Sharding-JDBC 源码分析 —— JDBC实现与读写分离》](http://www.iocoder.cn/Sharding-JDBC/jdbc-implement-and-read-write-splitting?github&1602)
    * [《Sharding-JDBC 源码分析 —— 分布式事务（一）之最大努力型》](http://www.iocoder.cn/Sharding-JDBC/transaction-bed?github&1602)
    * [《Sharding-JDBC 源码分析 —— 分布式事务（二）之事务补偿型》](http://www.iocoder.cn/Sharding-JDBC/transaction-tcc?github&1602)

# [中文主页](http://dangdangdotcom.github.io/sharding-jdbc)

[![Build Status](https://secure.travis-ci.org/dangdangdotcom/sharding-jdbc.png?branch=master)](https://travis-ci.org/dangdangdotcom/sharding-jdbc)
[![Maven Status](https://maven-badges.herokuapp.com/maven-central/com.dangdang/sharding-jdbc/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.dangdang/sharding-jdbc)
[![Coverage Status](https://coveralls.io/repos/dangdangdotcom/sharding-jdbc/badge.svg?branch=master&service=github)](https://coveralls.io/github/dangdangdotcom/sharding-jdbc?branch=master)
[![GitHub release](https://img.shields.io/github/release/dangdangdotcom/sharding-jdbc.svg)](https://github.com/dangdangdotcom/sharding-jdbc/releases)
[![Hex.pm](http://dangdangdotcom.github.io/sharding-jdbc/img/license.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

# Overview

Sharding-JDBC is a JDBC extension, provides distributed features such as sharding, read/write splitting and BASE transaction.

# Features

## 1. Sharding
* Aggregation functions, group by, order by and limit SQL supported in distributed database.
* Join (inner/outer) query supported.
* Sharding operator `=`, `BETWEEN` and `IN` supported.
* Sharding algorithm customization supported.
* Hint supported.

## 2. Read/Write Splitting
* Same transaction data concurrency guarantee.
* Hint supported.

## 3. BASE Transaction
* Best efforts delivery transaction.
* Try confirm cancel transaction (TBD).

## 4. Compatibility
* ORM self-adapting. JPA, Hibernate, Mybatis, Spring JDBC Template or JDBC supported.
* Connection-pool self-adapting. DBCP, C3P0, BoneCP, Druid supported.
* Any Database supported theoretically. Support MySQL, Oracle, SQLServer and PostgreSQL.

## 5. Configuration
* Java config
* Spring namespace
* YAML
* Inline expression

## 6. ID Generation
* Distributed Unique Time-Sequence Generation

# Architecture

![Architecture](http://dangdangdotcom.github.io/sharding-jdbc/img/architecture_en.png)

# [Release Notes](https://github.com/dangdangdotcom/sharding-jdbc/releases)

# [Roadmap](ROADMAP.md)

# Quick Start

## Add maven dependency

```xml
<!-- import sharding-jdbc core -->
<dependency>
    <groupId>com.dangdang</groupId>
    <artifactId>sharding-jdbc-core</artifactId>
    <version>${latest.release.version}</version>
</dependency>

<!-- import other module if need -->
```

## Rule configuration

```java
ShardingRule shardingRule = ShardingRule.builder()
        .dataSourceRule(dataSourceRule)
        .tableRules(tableRuleList)
        .databaseShardingStrategy(new DatabaseShardingStrategy("sharding_column", new XXXShardingAlgorithm()))
        .tableShardingStrategy(new TableShardingStrategy("sharding_column", new XXXShardingAlgorithm())))
        .build();
```

## Use raw JDBC API

```java
DataSource dataSource = ShardingDataSourceFactory.createDataSource(shardingRule);
String sql = "SELECT i.* FROM t_order o JOIN t_order_item i ON o.order_id=i.order_id WHERE o.user_id=? AND o.order_id=?";
try (
        Connection conn = dataSource.getConnection();
        PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
    preparedStatement.setInt(1, 10);
    preparedStatement.setInt(2, 1001);
    try (ResultSet rs = preparedStatement.executeQuery()) {
        while(rs.next()) {
            System.out.println(rs.getInt(1));
            System.out.println(rs.getInt(2));
        }
    }
}
```

## Use spring namespace

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
    xmlns:context="http://www.springframework.org/schema/context"
    xmlns:rdb="http://www.dangdang.com/schema/ddframe/rdb" 
    xsi:schemaLocation="http://www.springframework.org/schema/beans 
                        http://www.springframework.org/schema/beans/spring-beans.xsd
                        http://www.springframework.org/schema/context 
                        http://www.springframework.org/schema/context/spring-context.xsd 
                        http://www.dangdang.com/schema/ddframe/rdb 
                        http://www.dangdang.com/schema/ddframe/rdb/rdb.xsd 
                        ">
    <context:property-placeholder location="classpath:conf/rdb/conf.properties" ignore-unresolvable="true"/>
    
    <bean id="dbtbl_0" class="org.apache.commons.dbcp.BasicDataSource" destroy-method="close">
        <property name="driverClassName" value="com.mysql.jdbc.Driver"/>
        <property name="url" value="jdbc:mysql://localhost:3306/dbtbl_0"/>
        <property name="username" value="root"/>
        <property name="password" value=""/>
    </bean>
    <bean id="dbtbl_1" class="org.apache.commons.dbcp.BasicDataSource" destroy-method="close">
        <property name="driverClassName" value="com.mysql.jdbc.Driver"/>
        <property name="url" value="jdbc:mysql://localhost:3306/dbtbl_1"/>
        <property name="username" value="root"/>
        <property name="password" value=""/>
    </bean>

    <rdb:strategy id="orderTableStrategy" sharding-columns="order_id" algorithm-expression="t_order_${order_id.longValue() % 4}"/>
    <rdb:strategy id="orderItemTableStrategy" sharding-columns="order_id" algorithm-expression="t_order_item_${order_id.longValue() % 4}"/>
    <rdb:data-source id="shardingDataSource">
        <rdb:sharding-rule data-sources="dbtbl_0,dbtbl_1">
            <rdb:table-rules>
                <rdb:table-rule logic-table="t_order" actual-tables="t_order_${0..3}" table-strategy="orderTableStrategy"/>
                <rdb:table-rule logic-table="t_order_item" actual-tables="t_order_item_${0..3}" table-strategy="orderItemTableStrategy"/>
            </rdb:table-rules>
            <rdb:default-database-strategy sharding-columns="none" algorithm-class="com.dangdang.ddframe.rdb.sharding.api.strategy.database.NoneDatabaseShardingAlgorithm"/>
        </rdb:sharding-rule>
    </rdb:data-source>
</beans>
```
