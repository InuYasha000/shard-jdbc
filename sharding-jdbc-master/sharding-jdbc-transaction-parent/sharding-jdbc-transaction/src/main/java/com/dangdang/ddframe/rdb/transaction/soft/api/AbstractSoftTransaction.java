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

package com.dangdang.ddframe.rdb.transaction.soft.api;

import com.dangdang.ddframe.rdb.sharding.executor.threadlocal.ExecutorExceptionHandler;
import com.dangdang.ddframe.rdb.sharding.jdbc.core.connection.ShardingConnection;
import com.dangdang.ddframe.rdb.transaction.soft.constants.SoftTransactionType;
import com.google.common.base.Preconditions;
import lombok.Getter;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

/**
 * 柔性事务抽象类.
 * 
 * @author zhangliang 
 */
public abstract class AbstractSoftTransaction {

    /**
     * 分片连接原自动提交状态
     */
    private boolean previousAutoCommit;
    /**
     * 分片连接
     */
    @Getter
    private ShardingConnection connection;
    /**
     * 事务类型
     */
    @Getter
    private SoftTransactionType transactionType;
    /**
     * 事务编号
     */
    @Getter
    private String transactionId;

    /**
     * 开启柔性
     *
     * @param conn 分片连接
     * @param type 事务类型
     * @throws SQLException
     */
    protected final void beginInternal(final Connection conn, final SoftTransactionType type) throws SQLException {
        // TODO 判断如果在传统事务中，则抛异常
        Preconditions.checkArgument(conn instanceof ShardingConnection, "Only ShardingConnection can support eventual consistency transaction.");
        // 设置执行错误，不抛出异常
        ExecutorExceptionHandler.setExceptionThrown(false);
        connection = (ShardingConnection) conn;
        transactionType = type;
        // 设置自动提交状态
        previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(true);
        // 生成事务编号
        // TODO 替换UUID为更有效率的id生成器
        transactionId = UUID.randomUUID().toString();
    }
    
    /**
     * 结束柔性事务.
     */
    public final void end() throws SQLException {
        if (connection != null) {
            ExecutorExceptionHandler.setExceptionThrown(true);
            connection.setAutoCommit(previousAutoCommit);
            SoftTransactionManager.closeCurrentTransactionManager();
        }
    }
}
