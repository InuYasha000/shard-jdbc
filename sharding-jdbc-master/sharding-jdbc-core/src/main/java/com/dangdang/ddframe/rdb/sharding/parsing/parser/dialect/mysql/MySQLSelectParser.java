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

package com.dangdang.ddframe.rdb.sharding.parsing.parser.dialect.mysql;

import com.dangdang.ddframe.rdb.sharding.parsing.lexer.dialect.mysql.MySQLKeyword;
import com.dangdang.ddframe.rdb.sharding.parsing.lexer.dialect.oracle.OracleKeyword;
import com.dangdang.ddframe.rdb.sharding.parsing.lexer.token.Assist;
import com.dangdang.ddframe.rdb.sharding.parsing.lexer.token.DefaultKeyword;
import com.dangdang.ddframe.rdb.sharding.parsing.lexer.token.Literals;
import com.dangdang.ddframe.rdb.sharding.parsing.lexer.token.Symbol;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.SQLParser;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.context.limit.Limit;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.context.limit.LimitValue;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.exception.SQLParsingException;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.exception.SQLParsingUnsupportedException;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.statement.dql.select.AbstractSelectParser;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.token.OffsetToken;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.token.RowCountToken;

public class MySQLSelectParser extends AbstractSelectParser {
    
    public MySQLSelectParser(final SQLParser sqlParser) {
        super(sqlParser);
    }

    /**
     * 查询 SQL 解析
     * SELECT Syntax：https://dev.mysql.com/doc/refman/5.7/en/select.html
     */
//    SELECT
//    [ALL | DISTINCT | DISTINCTROW ]
//            [HIGH_PRIORITY]
//            [STRAIGHT_JOIN]
//            [SQL_SMALL_RESULT] [SQL_BIG_RESULT] [SQL_BUFFER_RESULT]
//            [SQL_CACHE | SQL_NO_CACHE] [SQL_CALC_FOUND_ROWS]
//            select_expr [, select_expr ...]
//            [FROM table_references
//              [PARTITION partition_list]
//            [WHERE where_condition]
//            [GROUP BY {col_name | expr | position}
//              [ASC | DESC], ... [WITH ROLLUP]]
//            [HAVING where_condition]
//            [ORDER BY {col_name | expr | position}
//              [ASC | DESC], ...]
//            [LIMIT {[offset,] row_count | row_count OFFSET offset}]
//            [PROCEDURE procedure_name(argument_list)]
//            [INTO OUTFILE 'file_name'
//               [CHARACTER SET charset_name]
//               export_options
//              | INTO DUMPFILE 'file_name'
//              | INTO var_name [, var_name]]
//            [FOR UPDATE | LOCK IN SHARE MODE]]
    @Override
    public void query() {
        if (getSqlParser().equalAny(DefaultKeyword.SELECT)) {
            getSqlParser().getLexer().nextToken();
            parseDistinct();
            getSqlParser().skipAll(MySQLKeyword.HIGH_PRIORITY, DefaultKeyword.STRAIGHT_JOIN, MySQLKeyword.SQL_SMALL_RESULT, MySQLKeyword.SQL_BIG_RESULT, MySQLKeyword.SQL_BUFFER_RESULT,
                    MySQLKeyword.SQL_CACHE, MySQLKeyword.SQL_NO_CACHE, MySQLKeyword.SQL_CALC_FOUND_ROWS);
            // 解析 查询字段
            parseSelectList();
            // 跳到 FROM 处
            skipToFrom();
        }
        // 解析 表（JOIN ON / FROM 单&多表）
        parseFrom();
        // 解析 WHERE 条件
        parseWhere();
        // 解析 Group By 和 Having（目前不支持）条件
        parseGroupBy();
        // 解析 Order By 条件
        parseOrderBy();
        // 解析 分页 Limit 条件
        parseLimit();
        // [PROCEDURE] 暂不支持
        if (getSqlParser().equalAny(DefaultKeyword.PROCEDURE)) {
            throw new SQLParsingUnsupportedException(getSqlParser().getLexer().getCurrentToken().getType());
        }
        // TODO 疑问：待定
        queryRest();
    }

    /**
     * 解析 Limit
     * 一共三种情况：
     * 1. LIMIT row_count
     * 2. LIMIT offset, row_count
     * 3. LIMIT row_count, offset
     */
    private void parseLimit() {
        if (!getSqlParser().skipIfEqual(MySQLKeyword.LIMIT)) {
            return;
        }
        // 解析 第一个 位置
        int valueIndex = -1;
        int valueBeginPosition = getSqlParser().getLexer().getCurrentToken().getEndPosition();
        int value;
        boolean isParameterForValue = false;
        if (getSqlParser().equalAny(Literals.INT)) {
            value = Integer.parseInt(getSqlParser().getLexer().getCurrentToken().getLiterals());
            valueBeginPosition = valueBeginPosition - (value + "").length();
        } else if (getSqlParser().equalAny(Symbol.QUESTION)) {
            valueIndex = getParametersIndex();
            value = -1;
            valueBeginPosition--;
            isParameterForValue = true;
        } else {
            throw new SQLParsingException(getSqlParser().getLexer());
        }
        // 第二种情况：LIMIT offset, row_count
        getSqlParser().getLexer().nextToken();
        if (getSqlParser().skipIfEqual(Symbol.COMMA)) {
            getSelectStatement().setLimit(getLimitWithComma(valueIndex, valueBeginPosition, value, isParameterForValue));
            return;
        }
        // 第三种情况：LIMIT row_count, offset
        if (getSqlParser().skipIfEqual(MySQLKeyword.OFFSET)) {
            getSelectStatement().setLimit(getLimitWithOffset(valueIndex, valueBeginPosition, value, isParameterForValue));
            return;
        }
        // 第一种情况：LIMIT row_count
        //
        if (!isParameterForValue) {
            getSelectStatement().getSqlTokens().add(new RowCountToken(valueBeginPosition, value));
        }
        // Limit
        Limit limit = new Limit(true);
        limit.setRowCount(new LimitValue(value, valueIndex));
        getSelectStatement().setLimit(limit);
    }
    
    private Limit getLimitWithComma(final int index, final int valueBeginPosition, final int value, final boolean isParameterForValue) {
        int rowCountBeginPosition = getSqlParser().getLexer().getCurrentToken().getEndPosition();
        int rowCountValue;
        int rowCountIndex = -1;
        boolean isParameterForRowCount = false;
        if (getSqlParser().equalAny(Literals.INT)) {
            rowCountValue = Integer.parseInt(getSqlParser().getLexer().getCurrentToken().getLiterals());
            rowCountBeginPosition = rowCountBeginPosition - (rowCountValue + "").length();
        } else if (getSqlParser().equalAny(Symbol.QUESTION)) {
            rowCountIndex = -1 == index ? getParametersIndex() : index + 1;
            rowCountValue = -1;
            rowCountBeginPosition--;
            isParameterForRowCount = true;
        } else {
            throw new SQLParsingException(getSqlParser().getLexer());
        }
        getSqlParser().getLexer().nextToken();
        if (!isParameterForValue) {
            getSelectStatement().getSqlTokens().add(new OffsetToken(valueBeginPosition, value));
        }
        if (!isParameterForRowCount) {
            getSelectStatement().getSqlTokens().add(new RowCountToken(rowCountBeginPosition, rowCountValue));
        }
        Limit result = new Limit(true);
        result.setRowCount(new LimitValue(rowCountValue, rowCountIndex));
        result.setOffset(new LimitValue(value, index));
        return result;
    }
    
    private Limit getLimitWithOffset(final int index, final int valueBeginPosition, final int value, final boolean isParameterForValue) {
        int offsetBeginPosition = getSqlParser().getLexer().getCurrentToken().getEndPosition();
        int offsetValue = -1;
        int offsetIndex = -1;
        boolean isParameterForOffset = false; // 是否是占位符
        if (getSqlParser().equalAny(Literals.INT)) {
            offsetValue = Integer.parseInt(getSqlParser().getLexer().getCurrentToken().getLiterals());
            offsetBeginPosition = offsetBeginPosition - (offsetValue + "").length();
        } else if (getSqlParser().equalAny(Symbol.QUESTION)) {
            offsetIndex = -1 == index ? getParametersIndex() : index + 1;
            offsetBeginPosition--;
            isParameterForOffset = true;
        } else {
            throw new SQLParsingException(getSqlParser().getLexer());
        }
        getSqlParser().getLexer().nextToken();
        if (!isParameterForOffset) {
            getSelectStatement().getSqlTokens().add(new OffsetToken(offsetBeginPosition, offsetValue));
        }
        if (!isParameterForValue) {
            getSelectStatement().getSqlTokens().add(new RowCountToken(valueBeginPosition, value));
        }
        Limit result = new Limit(true);
        result.setRowCount(new LimitValue(value, index));
        result.setOffset(new LimitValue(offsetValue, offsetIndex));
        return result;
    }

    /**
     * 跳到 FROM 处
     */
    private void skipToFrom() {
        while (!getSqlParser().equalAny(DefaultKeyword.FROM) && !getSqlParser().equalAny(Assist.END)) {
            getSqlParser().getLexer().nextToken();
        }
    }

    /**
     * 解析 JoinTable
     * https://dev.mysql.com/doc/refman/5.7/en/join.html
     */
//    table_references:
//    escaped_table_reference [, escaped_table_reference] ...
//
//    escaped_table_reference:
//    table_reference
//  | { OJ table_reference }
//
//    table_reference:
//    table_factor
//  | join_table
//
//    table_factor:
//    tbl_name [PARTITION (partition_names)]
//            [[AS] alias] [index_hint_list]
//            | table_subquery [AS] alias
//  | ( table_references )
//
//    join_table:
//    table_reference [INNER | CROSS] JOIN table_factor [join_condition]
//            | table_reference STRAIGHT_JOIN table_factor
//  | table_reference STRAIGHT_JOIN table_factor ON conditional_expr
//  | table_reference {LEFT|RIGHT} [OUTER] JOIN table_reference join_condition
//  | table_reference NATURAL [{LEFT|RIGHT} [OUTER]] JOIN table_factor
//
//    join_condition:
//    ON conditional_expr
//  | USING (column_list)
//
//    index_hint_list:
//    index_hint [, index_hint] ...
//
//    index_hint:
//    USE {INDEX|KEY}
//      [FOR {JOIN|ORDER BY|GROUP BY}] ([index_list])
//            | IGNORE {INDEX|KEY}
//      [FOR {JOIN|ORDER BY|GROUP BY}] (index_list)
//            | FORCE {INDEX|KEY}
//      [FOR {JOIN|ORDER BY|GROUP BY}] (index_list)
//
//    index_list:
//    index_name [, index_name] ...
    @Override
    protected void parseJoinTable() {
        if (getSqlParser().equalAny(DefaultKeyword.USING)) {
            return;
        }
        if (getSqlParser().equalAny(DefaultKeyword.USE)) {
            getSqlParser().getLexer().nextToken();
            parseIndexHint();
        }
        if (getSqlParser().equalAny(OracleKeyword.IGNORE)) {
            getSqlParser().getLexer().nextToken();
            parseIndexHint();
        }
        if (getSqlParser().equalAny(OracleKeyword.FORCE)) {
            getSqlParser().getLexer().nextToken();
            parseIndexHint();
        }
        super.parseJoinTable();
    }

    /**
     * 解析 Index Hint
     */
    private void parseIndexHint() {
        if (getSqlParser().equalAny(DefaultKeyword.INDEX)) {
            getSqlParser().getLexer().nextToken();
        } else {
            getSqlParser().accept(DefaultKeyword.KEY);
        }
        if (getSqlParser().equalAny(DefaultKeyword.FOR)) {
            getSqlParser().getLexer().nextToken();
            if (getSqlParser().equalAny(DefaultKeyword.JOIN)) {
                getSqlParser().getLexer().nextToken();
            } else if (getSqlParser().equalAny(DefaultKeyword.ORDER)) {
                getSqlParser().getLexer().nextToken();
                getSqlParser().accept(DefaultKeyword.BY);
            } else {
                getSqlParser().accept(DefaultKeyword.GROUP);
                getSqlParser().accept(DefaultKeyword.BY);
            }
        }
        getSqlParser().skipParentheses();
    }
}
