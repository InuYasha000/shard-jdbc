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
import com.dangdang.ddframe.rdb.sharding.exception.ShardingJdbcException;
import com.dangdang.ddframe.rdb.sharding.parsing.lexer.token.Assist;
import com.dangdang.ddframe.rdb.sharding.parsing.lexer.token.DefaultKeyword;
import com.dangdang.ddframe.rdb.sharding.parsing.lexer.token.Symbol;
import com.dangdang.ddframe.rdb.sharding.parsing.lexer.token.TokenType;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.SQLParser;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.context.GeneratedKey;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.context.condition.Column;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.context.condition.Condition;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.exception.SQLParsingUnsupportedException;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.expression.SQLExpression;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.expression.SQLNumberExpression;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.expression.SQLPlaceholderExpression;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.statement.SQLStatementParser;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.statement.dml.DMLStatement;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.statement.dml.DMLStatement;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.token.GeneratedKeyToken;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.token.ItemsToken;
import com.dangdang.ddframe.rdb.sharding.util.SQLUtil;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Insert???????????????.
 *
 * @author zhangliang
 */
@Getter(AccessLevel.PROTECTED)
public abstract class AbstractInsertParser implements SQLStatementParser {
    
    private final SQLParser sqlParser;
    
    private final ShardingRule shardingRule;
    
    private final InsertStatement insertStatement;
    /**
     * ???????????????????????????????????????
     * index ??? 0 ??????
     */
    @Getter(AccessLevel.NONE)
    private int generateKeyColumnIndex = -1;
    
    public AbstractInsertParser(final ShardingRule shardingRule, final SQLParser sqlParser) {
        this.sqlParser = sqlParser;
        this.shardingRule = shardingRule;
        insertStatement = new InsertStatement();
    }


// https://dev.mysql.com/doc/refman/5.7/en/insert.html

// ?????????
//    INSERT [LOW_PRIORITY | DELAYED | HIGH_PRIORITY] [IGNORE]
//            [INTO] tbl_name
//    [PARTITION (partition_name,...)]
//            [(col_name,...)]
//    {VALUES | VALUE} ({expr | DEFAULT},...),(...),...
//            [ ON DUPLICATE KEY UPDATE
//    col_name=expr
//        [, col_name=expr] ... ]

// ?????????
//    INSERT [LOW_PRIORITY | DELAYED | HIGH_PRIORITY] [IGNORE]
//            [INTO] tbl_name
//    [PARTITION (partition_name,...)]
//    SET col_name={expr | DEFAULT}, ...
//            [ ON DUPLICATE KEY UPDATE
//    col_name=expr
//        [, col_name=expr] ... ]

// ?????????
//    INSERT [LOW_PRIORITY | HIGH_PRIORITY] [IGNORE]
//            [INTO] tbl_name
//    [PARTITION (partition_name,...)]
//            [(col_name,...)]
//    SELECT ...
//            [ ON DUPLICATE KEY UPDATE
//    col_name=expr
//        [, col_name=expr] ... ]

    @Override
    public final DMLStatement parse() {
        sqlParser.getLexer().nextToken(); // ?????? INSERT ?????????
        parseInto(); // ?????????
        parseColumns(); // ????????????
        if (sqlParser.equalAny(DefaultKeyword.SELECT, Symbol.LEFT_PAREN)) {
            throw new UnsupportedOperationException("Cannot support subquery");
        }
        if (getValuesKeywords().contains(sqlParser.getLexer().getCurrentToken().getType())) { // ???????????????SQL??????
            parseValues();
        } else if (getCustomizedInsertKeywords().contains(sqlParser.getLexer().getCurrentToken().getType())) { // ???????????????SQL??????
            parseCustomizedInsert();
        }
        appendGenerateKey(); // ????????????
        return insertStatement;
    }

    /**
     * ?????????
     */
    private void parseInto() {
        // ?????????Oracle???INSERT FIRST/ALL ???????????????
        if (getUnsupportedKeywords().contains(sqlParser.getLexer().getCurrentToken().getType())) {
            throw new SQLParsingUnsupportedException(sqlParser.getLexer().getCurrentToken().getType());
        }
        sqlParser.skipUntil(DefaultKeyword.INTO);
        sqlParser.getLexer().nextToken();
        // ?????????
        sqlParser.parseSingleTable(insertStatement);
        skipBetweenTableAndValues();
    }
    
    protected Set<TokenType> getUnsupportedKeywords() {
        return Collections.emptySet();
    }

    /**
     * ?????? ??? ??? ???????????? ????????? Token
     * ?????? MySQL ???[PARTITION (partition_name,...)]
     */
    private void skipBetweenTableAndValues() {
        while (getSkippedKeywordsBetweenTableAndValues().contains(sqlParser.getLexer().getCurrentToken().getType())) {
            sqlParser.getLexer().nextToken();
            if (sqlParser.equalAny(Symbol.LEFT_PAREN)) {
                sqlParser.skipParentheses();
            }
        }
    }
    
    protected Set<TokenType> getSkippedKeywordsBetweenTableAndValues() {
        return Collections.emptySet();
    }

    /**
     * ??????????????????
     */
    private void parseColumns() {
        Collection<Column> result = new LinkedList<>();
        if (sqlParser.equalAny(Symbol.LEFT_PAREN)) {
            String tableName = insertStatement.getTables().getSingleTableName();
            Optional<String> generateKeyColumn = shardingRule.getGenerateKeyColumn(tableName); // ?????????????????????
            int count = 0;
            do {
                // Column ????????????
                sqlParser.getLexer().nextToken();
                String columnName = SQLUtil.getExactlyValue(sqlParser.getLexer().getCurrentToken().getLiterals());
                result.add(new Column(columnName, tableName));
                sqlParser.getLexer().nextToken();
                // ???????????????
                if (generateKeyColumn.isPresent() && generateKeyColumn.get().equalsIgnoreCase(columnName)) {
                    generateKeyColumnIndex = count;
                }
                count++;
            } while (!sqlParser.equalAny(Symbol.RIGHT_PAREN) && !sqlParser.equalAny(Assist.END));
            //
            insertStatement.setColumnsListLastPosition(sqlParser.getLexer().getCurrentToken().getEndPosition() - sqlParser.getLexer().getCurrentToken().getLiterals().length());
            //
            sqlParser.getLexer().nextToken();
        }
        insertStatement.getColumns().addAll(result);
    }
    
    protected Set<TokenType> getValuesKeywords() {
        return Sets.<TokenType>newHashSet(DefaultKeyword.VALUES);
    }

    /**
     * ???????????????
     */
    private void parseValues() {
        boolean parsed = false;
        do {
            if (parsed) { // ?????????INSERT INTO ??????
                throw new UnsupportedOperationException("Cannot support multiple insert");
            }
            sqlParser.getLexer().nextToken();
            sqlParser.accept(Symbol.LEFT_PAREN);
            // ???????????????
            List<SQLExpression> sqlExpressions = new LinkedList<>();
            do {
                sqlExpressions.add(sqlParser.parseExpression());
            } while (sqlParser.skipIfEqual(Symbol.COMMA));
            //
            insertStatement.setValuesListLastPosition(sqlParser.getLexer().getCurrentToken().getEndPosition() - sqlParser.getLexer().getCurrentToken().getLiterals().length());
            // ???????????????
            int count = 0;
            for (Column each : insertStatement.getColumns()) {
                SQLExpression sqlExpression = sqlExpressions.get(count);
                insertStatement.getConditions().add(new Condition(each, sqlExpression), shardingRule);
                if (generateKeyColumnIndex == count) { // ???????????????
                    insertStatement.setGeneratedKey(createGeneratedKey(each, sqlExpression));
                }
                count++;
            }
            sqlParser.accept(Symbol.RIGHT_PAREN);
            parsed = true;
        }
        while (sqlParser.equalAny(Symbol.COMMA)); // ????????? "," ??????
    }

    /**
     * ?????? ???????????????
     *
     * @param column ??????
     * @param sqlExpression ?????????
     * @return ???????????????
     */
    private GeneratedKey createGeneratedKey(final Column column, final SQLExpression sqlExpression) {
        GeneratedKey result;
        if (sqlExpression instanceof SQLPlaceholderExpression) { // ?????????
            result = new GeneratedKey(column.getName(), ((SQLPlaceholderExpression) sqlExpression).getIndex(), null);
        } else if (sqlExpression instanceof SQLNumberExpression) { // ??????
            result = new GeneratedKey(column.getName(), -1, ((SQLNumberExpression) sqlExpression).getNumber());
        } else {
            throw new ShardingJdbcException("Generated key only support number.");
        }
        return result;
    }
    
    protected Set<TokenType> getCustomizedInsertKeywords() {
        return Collections.emptySet();
    }
    
    protected void parseCustomizedInsert() {
    }

    /**
     * ??????????????????????????????????????????SQL????????????????????????????????????
     */
    private void appendGenerateKey() {
        // ??????????????????????????????????????????SQL??????????????????
        String tableName = insertStatement.getTables().getSingleTableName();
        Optional<String> generateKeyColumn = shardingRule.getGenerateKeyColumn(tableName);
        if (!generateKeyColumn.isPresent() || null != insertStatement.getGeneratedKey()) {
            return;
        }
        // ItemsToken
        ItemsToken columnsToken = new ItemsToken(insertStatement.getColumnsListLastPosition());
        columnsToken.getItems().add(generateKeyColumn.get());
        insertStatement.getSqlTokens().add(columnsToken);
        // GeneratedKeyToken
        insertStatement.getSqlTokens().add(new GeneratedKeyToken(insertStatement.getValuesListLastPosition()));
    }
}
