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

package com.dangdang.ddframe.rdb.sharding.parsing.parser.context.selectitem;

import com.dangdang.ddframe.rdb.sharding.constant.AggregationType;
import com.dangdang.ddframe.rdb.sharding.util.SQLUtil;
import com.google.common.base.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * 聚合选择项.
 *
 * @author zhangliang
 */
@RequiredArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public final class AggregationSelectItem implements SelectItem {

    /**
     * 聚合类型
     */
    private final AggregationType type;
    /**
     * 聚合内部表达式
     */
    private final String innerExpression;
    /**
     * 别名
     */
    private final Optional<String> alias;
    /**
     * 推导字段
     * 目前只有 AVG 聚合选择项需要用到：AVG 改写成 SUM + COUNT 查询，内存计算出 AVG 结果。
     */
    private final List<AggregationSelectItem> derivedAggregationSelectItems = new ArrayList<>(2);
    
    @Setter
    private int index = -1;
    
    @Override
    public String getExpression() {
        return SQLUtil.getExactlyValue(type.name() + innerExpression);
    }
    
    /**
     * 获取列标签.
     * 
     * @return 列标签
     */
    public String getColumnLabel() {
        return alias.isPresent() ? alias.get() : getExpression();
    }
}
