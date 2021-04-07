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

package com.dangdang.ddframe.rdb.transaction.soft.storage;

import com.dangdang.ddframe.rdb.transaction.soft.constants.SoftTransactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 事务日志.
 * 
 * @author zhangliang
 */
@AllArgsConstructor
@Getter
public final class TransactionLog {

    /**
     * 事务日志编号
     */
    private final String id;
    /**
     * 事务编号
     * tips：非存储字段
     */
    private final String transactionId;
    /**
     * 柔性事务类型
     */
    private final SoftTransactionType transactionType;
    /**
     * 真实数据源名
     */
    private final String dataSource;
    /**
     * 执行 SQL（已改写）
     */
    private final String sql;
    /**
     * 占位符参数
     */
    private final List<Object> parameters;
    /**
     * 记录时间
     */
    private final long creationTime;
    /**
     * 已异步重试次数
     */
    @Setter
    private int asyncDeliveryTryTimes;
}
