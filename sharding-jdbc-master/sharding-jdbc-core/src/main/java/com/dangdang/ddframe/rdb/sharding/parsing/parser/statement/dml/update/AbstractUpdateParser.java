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

package com.dangdang.ddframe.rdb.sharding.parsing.parser.statement.dml.update;

import com.dangdang.ddframe.rdb.sharding.parsing.lexer.token.DefaultKeyword;
import com.dangdang.ddframe.rdb.sharding.parsing.lexer.token.Symbol;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.SQLParser;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.statement.SQLStatementParser;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.statement.dml.DMLStatement;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.token.TableToken;
import com.dangdang.ddframe.rdb.sharding.util.SQLUtil;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * Update语句解析器.
 *
 * @author zhangliang
 */
@Getter(AccessLevel.PROTECTED)
public abstract class AbstractUpdateParser implements SQLStatementParser {
    
    private final SQLParser sqlParser;
    
    private final DMLStatement updateStatement;

    /**
     *
     */
    @Getter(AccessLevel.NONE)
    private int parametersIndex;
    
    public AbstractUpdateParser(final SQLParser sqlParser) {
        this.sqlParser = sqlParser;
        updateStatement = new DMLStatement();
    }

//https://dev.mysql.com/doc/refman/5.7/en/update.html

// Single-table syntax:
//    UPDATE [LOW_PRIORITY] [IGNORE] table_reference
//    SET col_name1={expr1|DEFAULT} [, col_name2={expr2|DEFAULT}] ...
//            [WHERE where_condition]
//            [ORDER BY ...]
//            [LIMIT row_count]

// Multiple-table syntax:
//    UPDATE [LOW_PRIORITY] [IGNORE] table_references
//    SET col_name1={expr1|DEFAULT} [, col_name2={expr2|DEFAULT}] ...
//            [WHERE where_condition]

    @Override
    public DMLStatement parse() {
        sqlParser.getLexer().nextToken(); // 跳过 UPDATE
        skipBetweenUpdateAndTable(); // 跳过关键字，例如：MYSQL 里的 LOW_PRIORITY、IGNORE
        sqlParser.parseSingleTable(updateStatement); // 解析表
        parseSetItems(); // 解析 SET
        sqlParser.skipUntil(DefaultKeyword.WHERE);
        sqlParser.setParametersIndex(parametersIndex);
        sqlParser.parseWhere(updateStatement);
        return updateStatement; // 解析 WHERE
    }

    /**
     * 跳过 Update 和 表名 之间的Token
     */
    protected abstract void skipBetweenUpdateAndTable();

    /**
     * 解析多个 SET 项
     */
    private void parseSetItems() {
        sqlParser.accept(DefaultKeyword.SET);
        do {
            parseSetItem();
        } while (sqlParser.skipIfEqual(Symbol.COMMA)); // 以 "," 分隔
    }

    /**
     * 解析单个 SET 项
     */
    private void parseSetItem() {
        parseSetColumn();
        sqlParser.skipIfEqual(Symbol.EQ, Symbol.COLON_EQ);
        parseSetValue();
    }

    /**
     * 解析单个 SET 项
     */
    private void parseSetColumn() {
        if (sqlParser.equalAny(Symbol.LEFT_PAREN)) {
            sqlParser.skipParentheses();
            return;
        }
        int beginPosition = sqlParser.getLexer().getCurrentToken().getEndPosition();
        String literals = sqlParser.getLexer().getCurrentToken().getLiterals();
        sqlParser.getLexer().nextToken();
        if (sqlParser.skipIfEqual(Symbol.DOT)) {
            if (updateStatement.getTables().getSingleTableName().equalsIgnoreCase(SQLUtil.getExactlyValue(literals))) {
                updateStatement.getSqlTokens().add(new TableToken(beginPosition - literals.length(), literals));
            }
            sqlParser.getLexer().nextToken();
        }
    }

    /**
     * 解析单个 SET 值
     */
    private void parseSetValue() {
        sqlParser.parseExpression(updateStatement);
        parametersIndex = sqlParser.getParametersIndex();
    }
}
