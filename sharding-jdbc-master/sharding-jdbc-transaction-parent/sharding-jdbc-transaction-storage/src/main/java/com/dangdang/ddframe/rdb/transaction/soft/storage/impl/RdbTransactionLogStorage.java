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

package com.dangdang.ddframe.rdb.transaction.soft.storage.impl;

import com.dangdang.ddframe.rdb.transaction.soft.constants.SoftTransactionType;
import com.dangdang.ddframe.rdb.transaction.soft.exception.TransactionCompensationException;
import com.dangdang.ddframe.rdb.transaction.soft.exception.TransactionLogStorageException;
import com.dangdang.ddframe.rdb.transaction.soft.storage.TransactionLog;
import com.dangdang.ddframe.rdb.transaction.soft.storage.TransactionLogStorage;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于数据库的事务日志存储器接口.
 *
 * @author zhangliang
 */
@RequiredArgsConstructor
public final class RdbTransactionLogStorage implements TransactionLogStorage {

    /**
     * 存储事务日志的数据源
     */
    private final DataSource dataSource;
    
    @Override
    public void add(final TransactionLog transactionLog) {
        String sql = "INSERT INTO `transaction_log` (`id`, `transaction_type`, `data_source`, `sql`, `parameters`, `creation_time`) VALUES (?, ?, ?, ?, ?, ?);";
        try (
            Connection conn = dataSource.getConnection();
            PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, transactionLog.getId());
            preparedStatement.setString(2, SoftTransactionType.BestEffortsDelivery.name());
            preparedStatement.setString(3, transactionLog.getDataSource());
            preparedStatement.setString(4, transactionLog.getSql());
            preparedStatement.setString(5, new Gson().toJson(transactionLog.getParameters()));
            preparedStatement.setLong(6, transactionLog.getCreationTime());
            preparedStatement.executeUpdate();
        } catch (final SQLException ex) {
            throw new TransactionLogStorageException(ex);
        } // TODO 疑问：为啥没close()
    }
    
    @Override
    public void remove(final String id) {
        String sql = "DELETE FROM `transaction_log` WHERE `id`=?;";
        try (
            Connection conn = dataSource.getConnection();
            PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, id);
            preparedStatement.executeUpdate();
        } catch (final SQLException ex) {
            throw new TransactionLogStorageException(ex);
        } // TODO 疑问：为啥没close()
    }
    
    @Override
    public List<TransactionLog> findEligibleTransactionLogs(final int size, final int maxDeliveryTryTimes, final long maxDeliveryTryDelayMillis) {
        List<TransactionLog> result = new ArrayList<>(size);
        String sql = "SELECT `id`, `transaction_type`, `data_source`, `sql`, `parameters`, `creation_time`, `async_delivery_try_times` "
            + "FROM `transaction_log` WHERE `async_delivery_try_times`<? AND `transaction_type`=? AND `creation_time`<? LIMIT ?;";
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                preparedStatement.setInt(1, maxDeliveryTryTimes); // 最大重试次数
                preparedStatement.setString(2, SoftTransactionType.BestEffortsDelivery.name()); // 柔性事务类型
                preparedStatement.setLong(3, System.currentTimeMillis() - maxDeliveryTryDelayMillis); // 早于异步处理的间隔时间
                preparedStatement.setInt(4, size);
                try (ResultSet rs = preparedStatement.executeQuery()) {
                    while (rs.next()) {
                        Gson gson = new Gson();
                        //TODO 对于批量执行的参数需要解析成两层列表
                        List<Object> parameters = gson.fromJson(rs.getString(5), new TypeToken<List<Object>>() { }.getType());
                        result.add(new TransactionLog(rs.getString(1), "", SoftTransactionType.valueOf(rs.getString(2)), rs.getString(3), rs.getString(4), parameters, rs.getLong(6), rs.getInt(7)));
                    }
                }
            }
        } catch (final SQLException ex) {
            throw new TransactionLogStorageException(ex);
        }
        return result;
    }
    
    @Override
    public void increaseAsyncDeliveryTryTimes(final String id) {
        String sql = "UPDATE `transaction_log` SET `async_delivery_try_times`=`async_delivery_try_times`+1 WHERE `id`=?;";
        try (
            Connection conn = dataSource.getConnection();
            PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, id);
            preparedStatement.executeUpdate();
        } catch (final SQLException ex) {
            throw new TransactionLogStorageException(ex);
        }
    }
    
    @Override
    public boolean processData(final Connection connection, final TransactionLog transactionLog, final int maxDeliveryTryTimes) {
        // 重试执行失败 SQL
        try (
            Connection conn = connection;
            PreparedStatement preparedStatement = conn.prepareStatement(transactionLog.getSql())) {
            for (int parameterIndex = 0; parameterIndex < transactionLog.getParameters().size(); parameterIndex++) {
                preparedStatement.setObject(parameterIndex + 1, transactionLog.getParameters().get(parameterIndex));
            }
            preparedStatement.executeUpdate();
        } catch (final SQLException ex) {
            // 重试失败，更新事务日志，增加已异步重试次数
            increaseAsyncDeliveryTryTimes(transactionLog.getId());
            throw new TransactionCompensationException(ex);
        }
        // 移除重试执行成功 SQL 对应的事务日志
        remove(transactionLog.getId());
        return true;
    }
}
