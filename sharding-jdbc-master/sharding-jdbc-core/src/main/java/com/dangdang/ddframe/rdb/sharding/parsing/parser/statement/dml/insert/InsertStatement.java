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

package com.dangdang.ddframe.rdb.sharding.parsing.parser.statement.dml.insert;

import com.dangdang.ddframe.rdb.sharding.api.rule.ShardingRule;
import com.dangdang.ddframe.rdb.sharding.api.rule.TableRule;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.context.GeneratedKey;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.context.condition.Column;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.context.condition.Condition;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.expression.SQLNumberExpression;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.expression.SQLPlaceholderExpression;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.statement.dml.DMLStatement;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.token.GeneratedKeyToken;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.token.ItemsToken;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.token.SQLToken;
import com.google.common.base.Optional;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Insert SQL语句对象.
 *
 * @author zhangliang
 */
@Getter
@Setter
@ToString
public final class InsertStatement extends DMLStatement {
    
    private final Collection<Column> columns = new LinkedList<>();
    /**
     * 自动生成键
     */
    private GeneratedKey generatedKey;
    /**
     * 插入字段 下一个Token 开始位置
     */
    private int columnsListLastPosition;
    /**
     * 值字段 下一个Token 开始位置
     */
    private int valuesListLastPosition;

    /**
     * 追加自增主键标记对象.
     *
     * @param shardingRule 分片规则
     * @param parametersSize 参数个数
     */
    public void appendGenerateKeyToken(final ShardingRule shardingRule, final int parametersSize) {
        // SQL 里有主键列
        if (null != generatedKey) {
            return;
        }
        // TableRule 存在
        Optional<TableRule> tableRule = shardingRule.tryFindTableRule(getTables().getSingleTableName());
        if (!tableRule.isPresent()) {
            return;
        }
        // GeneratedKeyToken 存在
        Optional<GeneratedKeyToken> generatedKeysToken = findGeneratedKeyToken();
        if (!generatedKeysToken.isPresent()) {
            return;
        }
        // 处理 GenerateKeyToken
        ItemsToken valuesToken = new ItemsToken(generatedKeysToken.get().getBeginPosition());
        if (0 == parametersSize) {
            appendGenerateKeyToken(shardingRule, tableRule.get(), valuesToken);
        } else {
            appendGenerateKeyToken(shardingRule, tableRule.get(), valuesToken, parametersSize);
        }
        // 移除 generatedKeysToken
        getSqlTokens().remove(generatedKeysToken.get());
        // 新增 ItemsToken
        getSqlTokens().add(valuesToken);
    }
    
    private void appendGenerateKeyToken(final ShardingRule shardingRule, final TableRule tableRule, final ItemsToken valuesToken) {
        // 生成分布式主键
        Number generatedKey = shardingRule.generateKey(tableRule.getLogicTable());
        // 添加到 ItemsToken
        valuesToken.getItems().add(generatedKey.toString());
        // 增加 Condition，用于路由
        getConditions().add(new Condition(new Column(tableRule.getGenerateKeyColumn(), tableRule.getLogicTable()), new SQLNumberExpression(generatedKey)), shardingRule);
        // 生成 GeneratedKey
        this.generatedKey = new GeneratedKey(tableRule.getLogicTable(), -1, generatedKey);
    }
    
    private void appendGenerateKeyToken(final ShardingRule shardingRule, final TableRule tableRule, final ItemsToken valuesToken, final int parametersSize) {
        // 生成占位符
        valuesToken.getItems().add("?");
        // 增加 Condition，用于路由
        getConditions().add(new Condition(new Column(tableRule.getGenerateKeyColumn(), tableRule.getLogicTable()), new SQLPlaceholderExpression(parametersSize)), shardingRule);
        // 生成 GeneratedKey
        generatedKey = new GeneratedKey(tableRule.getGenerateKeyColumn(), parametersSize, null);
    }
    
    private Optional<GeneratedKeyToken> findGeneratedKeyToken() {
        for (SQLToken each : getSqlTokens()) {
            if (each instanceof GeneratedKeyToken) {
                return Optional.of((GeneratedKeyToken) each);
            }
        }
        return Optional.absent();
    }
}
