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

package com.dangdang.ddframe.rdb.sharding.parsing.parser;

import com.dangdang.ddframe.rdb.sharding.api.rule.ShardingRule;
import com.dangdang.ddframe.rdb.sharding.parsing.lexer.Lexer;
import com.dangdang.ddframe.rdb.sharding.parsing.lexer.token.DefaultKeyword;
import com.dangdang.ddframe.rdb.sharding.parsing.lexer.token.Literals;
import com.dangdang.ddframe.rdb.sharding.parsing.lexer.token.Symbol;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.context.condition.Column;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.context.condition.Condition;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.context.limit.Limit;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.context.limit.LimitValue;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.context.table.Table;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.context.table.Tables;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.exception.SQLParsingUnsupportedException;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.expression.*;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.statement.SQLStatement;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.statement.dql.select.SelectStatement;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.token.OffsetToken;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.token.RowCountToken;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.token.TableToken;
import com.dangdang.ddframe.rdb.sharding.util.NumberUtil;
import com.dangdang.ddframe.rdb.sharding.util.SQLUtil;
import com.google.common.base.Optional;
import lombok.Getter;

import java.util.LinkedList;
import java.util.List;

/**
 * SQL解析器.
 *
 * @author zhangliang
 */
@Getter
public class SQLParser extends AbstractParser {
    
    private final ShardingRule shardingRule;
    
    public SQLParser(final Lexer lexer, final ShardingRule shardingRule) {
        super(lexer);
        this.shardingRule = shardingRule;
        getLexer().nextToken();
    }
    
    /**
     * 解析表达式.
     *
     * @param sqlStatement SQL语句对象
     * @return 表达式
     */
    public final SQLExpression parseExpression(final SQLStatement sqlStatement) {
        // 【调试代码】
        StackTraceElement stack[] = (new Throwable()).getStackTrace();
        System.out.println();
        System.out.println(stack[1].getMethodName()); // 调用方法
        System.out.println("begin：" + getLexer().getCurrentToken().getLiterals());

        int beginPosition = getLexer().getCurrentToken().getEndPosition();
        SQLExpression result = parseExpression();
        if (result instanceof SQLPropertyExpression) {
            setTableToken(sqlStatement, beginPosition, (SQLPropertyExpression) result);
        }

        // 【调试代码】
        System.out.print("end：");
        if (result instanceof SQLIdentifierExpression) {
            SQLIdentifierExpression exp = (SQLIdentifierExpression) result;
            System.out.println(exp.getClass().getSimpleName() + "：" + exp.getName());
        } else if (result instanceof SQLIgnoreExpression) {
            SQLIgnoreExpression exp = (SQLIgnoreExpression) result;
            System.out.println(exp.getClass().getSimpleName() + "：");
        } else if (result instanceof SQLNumberExpression) {
            SQLNumberExpression exp = (SQLNumberExpression) result;
            System.out.println(exp.getClass().getSimpleName() + "：" + exp.getNumber());
        } else if (result instanceof SQLPropertyExpression) {
            SQLPropertyExpression exp = (SQLPropertyExpression) result;
            System.out.println(exp.getClass().getSimpleName() + "：" + exp.getOwner().getName() + "\t" + exp.getName());
        } else if (result instanceof SQLTextExpression) {
            SQLTextExpression exp = (SQLTextExpression) result;
            System.out.println(exp.getClass().getSimpleName() + "：" + exp.getText());
        }
        System.out.println();
        System.out.println();
        return result;
    }
    
    /**
     * 解析表达式.
     *
     * @return 表达式
     */
    // TODO 完善Expression解析的各种场景
    public final SQLExpression parseExpression() {
        if (!(new Throwable()).getStackTrace()[1].getMethodName().equals("parseExpression")) {
            System.out.println();
        }
        // 解析表达式
        String literals = getLexer().getCurrentToken().getLiterals();
        final SQLExpression expression = getExpression(literals);
        // SQLIdentifierExpression 需要特殊处理。考虑自定义函数，表名.属性情况。
        if (skipIfEqual(Literals.IDENTIFIER)) {
            if (skipIfEqual(Symbol.DOT)) { // 例如，ORDER BY o.uid 中的 "o.uid"
                String property = getLexer().getCurrentToken().getLiterals();
                getLexer().nextToken();
                return skipIfCompositeExpression() ? new SQLIgnoreExpression() : new SQLPropertyExpression(new SQLIdentifierExpression(literals), property);
            }
            if (equalAny(Symbol.LEFT_PAREN)) { // 例如，GROUP BY DATE(create_time) 中的 "DATE(create_time)"
                skipParentheses();
                skipRestCompositeExpression();
                return new SQLIgnoreExpression();
            }
            return skipIfCompositeExpression() ? new SQLIgnoreExpression() : expression;
        }
        getLexer().nextToken();
        return skipIfCompositeExpression() ? new SQLIgnoreExpression() : expression;
    }

    /**
     * 获得 词法Token 对应的 SQLExpression
     *
     * @param literals 词法字面量标记
     * @return SQLExpression
     */
    private SQLExpression getExpression(final String literals) {
        if (equalAny(Symbol.QUESTION)) {
            increaseParametersIndex();
            return new SQLPlaceholderExpression(getParametersIndex() - 1);
        }
        if (equalAny(Literals.CHARS)) {
            return new SQLTextExpression(literals);
        }
        if (equalAny(Literals.INT)) {
            return new SQLNumberExpression(NumberUtil.getExactlyNumber(literals, 10));
        }
        if (equalAny(Literals.FLOAT)) {
            return new SQLNumberExpression(Double.parseDouble(literals));
        }
        if (equalAny(Literals.HEX)) {
            return new SQLNumberExpression(NumberUtil.getExactlyNumber(literals, 16));
        }
        if (equalAny(Literals.IDENTIFIER)) {
            return new SQLIdentifierExpression(SQLUtil.getExactlyValue(literals));
        }
        return new SQLIgnoreExpression();
    }

    /**
     * 如果是 复合表达式，跳过。
     *
     * @return 是否跳过
     */
    private boolean skipIfCompositeExpression() {
        if (equalAny(Symbol.PLUS, Symbol.SUB, Symbol.STAR, Symbol.SLASH, Symbol.PERCENT, Symbol.AMP, Symbol.BAR, Symbol.DOUBLE_AMP, Symbol.DOUBLE_BAR, Symbol.CARET, Symbol.DOT, Symbol.LEFT_PAREN)) {
            skipParentheses();
            skipRestCompositeExpression();
            return true;
        }
        return false;
    }

    /**
     * 跳过剩余复合表达式
     */
    private void skipRestCompositeExpression() {
        while (skipIfEqual(Symbol.PLUS, Symbol.SUB, Symbol.STAR, Symbol.SLASH, Symbol.PERCENT, Symbol.AMP, Symbol.BAR, Symbol.DOUBLE_AMP, Symbol.DOUBLE_BAR, Symbol.CARET, Symbol.DOT)) {
            if (equalAny(Symbol.QUESTION)) {
                increaseParametersIndex();
            }
            getLexer().nextToken();
            skipParentheses();
        }
    }
    
    private void setTableToken(final SQLStatement sqlStatement, final int beginPosition, final SQLPropertyExpression propertyExpr) {
        String owner = propertyExpr.getOwner().getName();
        if (!sqlStatement.getTables().isEmpty() && sqlStatement.getTables().getSingleTableName().equalsIgnoreCase(SQLUtil.getExactlyValue(owner))) {
            sqlStatement.getSqlTokens().add(new TableToken(beginPosition - owner.length(), owner));
        }
    }
    
    /**
     * 解析别名.不仅仅是字段的别名，也可以是表的别名。
     *
     * @return 别名
     */
    public Optional<String> parseAlias() {
        // 解析带 AS 情况
        if (skipIfEqual(DefaultKeyword.AS)) {
            if (equalAny(Symbol.values())) {
                return Optional.absent();
            }
            String result = SQLUtil.getExactlyValue(getLexer().getCurrentToken().getLiterals());
            getLexer().nextToken();
            return Optional.of(result);
        }
        // 解析别名
        // TODO 增加哪些数据库识别哪些关键字作为别名的配置
        if (equalAny(Literals.IDENTIFIER, Literals.CHARS, DefaultKeyword.USER, DefaultKeyword.END, DefaultKeyword.CASE, DefaultKeyword.KEY, DefaultKeyword.INTERVAL, DefaultKeyword.CONSTRAINT)) {
            String result = SQLUtil.getExactlyValue(getLexer().getCurrentToken().getLiterals());
            getLexer().nextToken();
            return Optional.of(result);
        }
        return Optional.absent();
    }
    
    /**
     * 解析单表.
     *
     * @param sqlStatement SQL语句对象
     */
    public final void parseSingleTable(final SQLStatement sqlStatement) {
        boolean hasParentheses = false;
        if (skipIfEqual(Symbol.LEFT_PAREN)) {
            if (equalAny(DefaultKeyword.SELECT)) { // multiple-update 或者 multiple-delete
                throw new UnsupportedOperationException("Cannot support subquery");
            }
            hasParentheses = true;
        }
        Table table;
        final int beginPosition = getLexer().getCurrentToken().getEndPosition() - getLexer().getCurrentToken().getLiterals().length();
        String literals = getLexer().getCurrentToken().getLiterals();
        getLexer().nextToken();
        if (skipIfEqual(Symbol.DOT)) {
            getLexer().nextToken();
            if (hasParentheses) {
                accept(Symbol.RIGHT_PAREN);
            }
            table = new Table(SQLUtil.getExactlyValue(literals), parseAlias());
        } else {
            if (hasParentheses) {
                accept(Symbol.RIGHT_PAREN);
            }
            table = new Table(SQLUtil.getExactlyValue(literals), parseAlias());
        }
        if (skipJoin()) { // multiple-update 或者 multiple-delete
            throw new UnsupportedOperationException("Cannot support Multiple-Table.");
        }
        sqlStatement.getSqlTokens().add(new TableToken(beginPosition, literals));
        sqlStatement.getTables().add(table);
    }
    
    /**
     * 跳过表关联词法.
     *
     * @return 是否表关联.
     */
    public final boolean skipJoin() {
        if (skipIfEqual(DefaultKeyword.LEFT, DefaultKeyword.RIGHT, DefaultKeyword.FULL)) {
            skipIfEqual(DefaultKeyword.OUTER);
            accept(DefaultKeyword.JOIN);
            return true;
        } else if (skipIfEqual(DefaultKeyword.INNER)) {
            accept(DefaultKeyword.JOIN);
            return true;
        } else if (skipIfEqual(DefaultKeyword.JOIN, Symbol.COMMA, DefaultKeyword.STRAIGHT_JOIN)) {
            return true;
        } else if (skipIfEqual(DefaultKeyword.CROSS)) {
            if (skipIfEqual(DefaultKeyword.JOIN, DefaultKeyword.APPLY)) {
                return true;
            }
        } else if (skipIfEqual(DefaultKeyword.OUTER)) {
            if (skipIfEqual(DefaultKeyword.APPLY)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 解析查询条件.
     *
     * @param sqlStatement SQL语句对象
     */
    public final void parseWhere(final SQLStatement sqlStatement) {
        parseAlias();
        if (skipIfEqual(DefaultKeyword.WHERE)) {
            parseConditions(sqlStatement);
        }
    }

    /**
     * 解析所有查询条件。
     * 目前不支持 OR 条件。
     *
     * @param sqlStatement SQL
     */
    private void parseConditions(final SQLStatement sqlStatement) {
        // AND 查询
        do {
            parseComparisonCondition(sqlStatement);
        } while (skipIfEqual(DefaultKeyword.AND));
        // 目前不支持 OR 条件
        if (equalAny(DefaultKeyword.OR)) {
            throw new SQLParsingUnsupportedException(getLexer().getCurrentToken().getType());
        }
    }
    
    // TODO 解析组合expr
    /**
     * 解析单个查询条件
     *
     * @param sqlStatement SQL
     */
    public final void parseComparisonCondition(final SQLStatement sqlStatement) {
        skipIfEqual(Symbol.LEFT_PAREN);
        SQLExpression left = parseExpression(sqlStatement);
        if (equalAny(Symbol.EQ)) {
            parseEqualCondition(sqlStatement, left);
            skipIfEqual(Symbol.RIGHT_PAREN);
            return;
        }
        if (equalAny(DefaultKeyword.IN)) {
            parseInCondition(sqlStatement, left);
            skipIfEqual(Symbol.RIGHT_PAREN);
            return;
        }
        if (equalAny(DefaultKeyword.BETWEEN)) {
            parseBetweenCondition(sqlStatement, left);
            skipIfEqual(Symbol.RIGHT_PAREN);
            return;
        }
        if (equalAny(Symbol.LT, Symbol.GT, Symbol.LT_EQ, Symbol.GT_EQ)) {
            if (left instanceof SQLIdentifierExpression && sqlStatement instanceof SelectStatement
                    && isRowNumberCondition((SelectStatement) sqlStatement, ((SQLIdentifierExpression) left).getName())) {
                parseRowNumberCondition((SelectStatement) sqlStatement);
            } else if (left instanceof SQLPropertyExpression && sqlStatement instanceof SelectStatement
                    && isRowNumberCondition((SelectStatement) sqlStatement, ((SQLPropertyExpression) left).getName())) {
                parseRowNumberCondition((SelectStatement) sqlStatement);
            } else {
                parseOtherCondition(sqlStatement);
            }
        } else if (equalAny(Symbol.LT_GT, DefaultKeyword.LIKE)) {
            parseOtherCondition(sqlStatement);
        }
        skipIfEqual(Symbol.RIGHT_PAREN);
    }

    /**
     * 解析 = 条件
     *
     * @param sqlStatement SQL
     * @param left 左SQLExpression
     */
    private void parseEqualCondition(final SQLStatement sqlStatement, final SQLExpression left) {
        getLexer().nextToken();
        SQLExpression right = parseExpression(sqlStatement);
        // 添加列
        // TODO 如果有多表,且找不到column是哪个表的,则不加入condition,以后需要解析binding table
        if ((sqlStatement.getTables().isSingleTable() || left instanceof SQLPropertyExpression)
                // 只有对路由结果有影响的才会添加到 conditions。SQLPropertyExpression 和 SQLIdentifierExpression 无法判断，所以未加入 conditions
                && (right instanceof SQLNumberExpression || right instanceof SQLTextExpression || right instanceof SQLPlaceholderExpression)) {
            Optional<Column> column = find(sqlStatement.getTables(), left);
            if (column.isPresent()) {
                sqlStatement.getConditions().add(new Condition(column.get(), right), shardingRule);
            }
        }
    }

    /**
     * 解析 IN 条件
     *
     * @param sqlStatement SQL
     * @param left 左SQLExpression
     */
    private void parseInCondition(final SQLStatement sqlStatement, final SQLExpression left) {
        // 解析 IN 条件
        getLexer().nextToken();
        accept(Symbol.LEFT_PAREN);
        List<SQLExpression> rights = new LinkedList<>();
        do {
            if (equalAny(Symbol.COMMA)) {
                getLexer().nextToken();
            }
            rights.add(parseExpression(sqlStatement));
        } while (!equalAny(Symbol.RIGHT_PAREN));
        // 添加列
        Optional<Column> column = find(sqlStatement.getTables(), left);
        if (column.isPresent()) {
            sqlStatement.getConditions().add(new Condition(column.get(), rights), shardingRule);
        }
        // 解析下一个 TOKEN
        getLexer().nextToken();
    }

    /**
     * 解析 BETWEEN 条件
     *
     * @param sqlStatement SQL
     * @param left 左SQLExpression
     */
    private void parseBetweenCondition(final SQLStatement sqlStatement, final SQLExpression left) {
        // 解析 BETWEEN 条件
        getLexer().nextToken();
        List<SQLExpression> rights = new LinkedList<>();
        rights.add(parseExpression(sqlStatement));
        accept(DefaultKeyword.AND);
        rights.add(parseExpression(sqlStatement));
        // 添加查询条件
        Optional<Column> column = find(sqlStatement.getTables(), left);
        if (column.isPresent()) {
            sqlStatement.getConditions().add(new Condition(column.get(), rights.get(0), rights.get(1)), shardingRule);
        }
    }
    
    protected boolean isRowNumberCondition(final SelectStatement selectStatement, final String columnLabel) {
        return false;
    }
    
    private void parseRowNumberCondition(final SelectStatement selectStatement) {
        Symbol symbol = (Symbol) getLexer().getCurrentToken().getType();
        getLexer().nextToken();
        SQLExpression sqlExpression = parseExpression(selectStatement);
        if (null == selectStatement.getLimit()) {
            selectStatement.setLimit(new Limit(false));
        }
        if (Symbol.LT == symbol || Symbol.LT_EQ == symbol) {
            if (sqlExpression instanceof SQLNumberExpression) {
                int rowCount = ((SQLNumberExpression) sqlExpression).getNumber().intValue();
                selectStatement.getLimit().setRowCount(new LimitValue(rowCount, -1));
                selectStatement.getSqlTokens().add(
                        new RowCountToken(getLexer().getCurrentToken().getEndPosition() - String.valueOf(rowCount).length() - getLexer().getCurrentToken().getLiterals().length(), rowCount));
            } else if (sqlExpression instanceof SQLPlaceholderExpression) {
                selectStatement.getLimit().setRowCount(new LimitValue(-1, ((SQLPlaceholderExpression) sqlExpression).getIndex()));
            }
        } else if (Symbol.GT == symbol || Symbol.GT_EQ == symbol) {
            if (sqlExpression instanceof SQLNumberExpression) {
                int offset = ((SQLNumberExpression) sqlExpression).getNumber().intValue();
                selectStatement.getLimit().setOffset(new LimitValue(offset, -1));
                selectStatement.getSqlTokens().add(
                        new OffsetToken(getLexer().getCurrentToken().getEndPosition() - String.valueOf(offset).length() - getLexer().getCurrentToken().getLiterals().length(), offset));
            } else if (sqlExpression instanceof SQLPlaceholderExpression) {
                selectStatement.getLimit().setOffset(new LimitValue(-1, ((SQLPlaceholderExpression) sqlExpression).getIndex()));
            }
        }
    }

    /**
     * 解析其他条件。目前其他条件包含 LIKE, <, <=, >, >=
     *
     * @param sqlStatement SQL
     */
    private void parseOtherCondition(final SQLStatement sqlStatement) {
        getLexer().nextToken();
        parseExpression(sqlStatement);
    }

    /**
     *
     *
     * @param tables 表
     * @param sqlExpression SqlExpression
     * @return 列
     */
    private Optional<Column> find(final Tables tables, final SQLExpression sqlExpression) {
        if (sqlExpression instanceof SQLPropertyExpression) {
            return getColumnWithOwner(tables, (SQLPropertyExpression) sqlExpression);
        }
        if (sqlExpression instanceof SQLIdentifierExpression) {
            return getColumnWithoutOwner(tables, (SQLIdentifierExpression) sqlExpression);
        }
        return Optional.absent();
    }

    /**
     * 获得列
     *
     * @param tables 表
     * @param propertyExpression SQLPropertyExpression
     * @return 列
     */
    private Optional<Column> getColumnWithOwner(final Tables tables, final SQLPropertyExpression propertyExpression) {
        Optional<Table> table = tables.find(SQLUtil.getExactlyValue((propertyExpression.getOwner()).getName()));
        return propertyExpression.getOwner() instanceof SQLIdentifierExpression && table.isPresent()
                ? Optional.of(new Column(SQLUtil.getExactlyValue(propertyExpression.getName()), table.get().getName())) : Optional.<Column>absent();
    }

    /**
     * 获得列
     * 只有是单表的情况下，才能获得到列
     *
     * @param tables 表
     * @param identifierExpression SQLIdentifierExpression
     * @return 列
     */
    private Optional<Column> getColumnWithoutOwner(final Tables tables, final SQLIdentifierExpression identifierExpression) {
        return tables.isSingleTable() ? Optional.of(new Column(SQLUtil.getExactlyValue(identifierExpression.getName()), tables.getSingleTableName())) : Optional.<Column>absent();
    }
}
