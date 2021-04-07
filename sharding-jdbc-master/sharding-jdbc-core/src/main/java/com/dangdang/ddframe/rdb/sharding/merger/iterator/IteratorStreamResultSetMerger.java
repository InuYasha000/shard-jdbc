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

package com.dangdang.ddframe.rdb.sharding.merger.iterator;

import com.dangdang.ddframe.rdb.sharding.merger.common.AbstractStreamResultSetMerger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

/**
 * 迭代归并结果集接口.
 *
 * @author zhangliang
 */
public final class IteratorStreamResultSetMerger extends AbstractStreamResultSetMerger {

    /**
     * ResultSet 数组迭代器
     */
    private final Iterator<ResultSet> resultSets;

    public IteratorStreamResultSetMerger(final List<ResultSet> resultSets) {
        this.resultSets = resultSets.iterator();
        // 设置当前 ResultSet，这样 #getValue() 能拿到记录
        setCurrentResultSet(this.resultSets.next());
    }

    @Override
    public boolean next() throws SQLException {
        // 当前 ResultSet 迭代下一条记录
        if (getCurrentResultSet().next()) {
            return true;
        }
        if (!resultSets.hasNext()) {
            return false;
        }
        // 获得下一个ResultSet， 设置当前 ResultSet
        setCurrentResultSet(resultSets.next());
        boolean hasNext = getCurrentResultSet().next();
        if (hasNext) {
            return true;
        }
        while (!hasNext && resultSets.hasNext()) {
            setCurrentResultSet(resultSets.next());
            hasNext = getCurrentResultSet().next();
        }
        return hasNext;
    }
}
