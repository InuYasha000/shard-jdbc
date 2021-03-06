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

package com.dangdang.ddframe.rdb.sharding.parsing.parser.statement.dql.select;

import com.dangdang.ddframe.rdb.sharding.constant.AggregationType;
import com.dangdang.ddframe.rdb.sharding.constant.OrderType;
import com.dangdang.ddframe.rdb.sharding.parsing.lexer.token.*;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.SQLParser;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.context.OrderItem;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.context.selectitem.AggregationSelectItem;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.context.selectitem.CommonSelectItem;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.context.selectitem.SelectItem;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.context.table.Table;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.exception.SQLParsingUnsupportedException;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.expression.SQLExpression;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.expression.SQLIdentifierExpression;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.expression.SQLNumberExpression;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.expression.SQLPropertyExpression;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.statement.SQLStatementParser;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.token.ItemsToken;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.token.OrderByToken;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.token.TableToken;
import com.dangdang.ddframe.rdb.sharding.util.SQLUtil;
import com.google.common.base.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

@Getter(AccessLevel.PROTECTED)
public abstract class AbstractSelectParser implements SQLStatementParser {
    
    private static final String DERIVED_COUNT_ALIAS = "AVG_DERIVED_COUNT_%s";
    
    private static final String DERIVED_SUM_ALIAS = "AVG_DERIVED_SUM_%s";
    
    private static final String ORDER_BY_DERIVED_ALIAS = "ORDER_BY_DERIVED_%s";
    
    private static final String GROUP_BY_DERIVED_ALIAS = "GROUP_BY_DERIVED_%s";
    
    private final SQLParser sqlParser;
    
    private final SelectStatement selectStatement;
    
    @Setter
    private int parametersIndex;
    
    private boolean appendDerivedColumnsFlag;
    
    public AbstractSelectParser(final SQLParser sqlParser) {
        this.sqlParser = sqlParser;
        selectStatement = new SelectStatement();
    }
    
    @Override
    public final SelectStatement parse() {
        query();
        parseOrderBy();
        appendDerivedColumns();
        appendDerivedOrderBy();
        return selectStatement;
    }

    protected abstract void query();

    /**
     * ?????? DISTINCT???DISTINCTROW???UNION???ALL
     * ????????? DISTINCT ??? DISTINCT(??????) ?????????????????????????????????
     * ?????? SELECT DISTINCT user_id FROM t_order ?????????????????????????????????????????????????????????????????????????????? user_id???
     */
    protected final void parseDistinct() {
        if (sqlParser.equalAny(DefaultKeyword.DISTINCT, DefaultKeyword.DISTINCTROW, DefaultKeyword.UNION)) {
            selectStatement.setDistinct(true);
            sqlParser.getLexer().nextToken();
            if (hasDistinctOn() && sqlParser.equalAny(DefaultKeyword.ON)) { // PostgreSQL ??????????????? DISTINCT ON
                sqlParser.getLexer().nextToken();
                sqlParser.skipParentheses();
            }
        } else if (sqlParser.equalAny(DefaultKeyword.ALL)) {
            sqlParser.getLexer().nextToken();
        }
    }
    
    protected boolean hasDistinctOn() {
        return false;
    }

    /**
     * ?????????????????????
     */
    protected final void parseSelectList() {
        do {
            // ?????? ?????????
            parseSelectItem();
        } while (sqlParser.skipIfEqual(Symbol.COMMA));
        // ?????? ?????????????????????????????? Token ???????????????
        selectStatement.setSelectListLastPosition(sqlParser.getLexer().getCurrentToken().getEndPosition() - sqlParser.getLexer().getCurrentToken().getLiterals().length());
    }

    /**
     * ?????????????????????
     */
    private void parseSelectItem() {
        // ??????????????????SQL Server ??????
        if (isRowNumberSelectItem()) {
            selectStatement.getItems().add(parseRowNumberSelectItem());
            return;
        }
        sqlParser.skipIfEqual(DefaultKeyword.CONNECT_BY_ROOT); // Oracle ?????????https://docs.oracle.com/cd/B19306_01/server.102/b14200/operators004.htm
        String literals = sqlParser.getLexer().getCurrentToken().getLiterals();
        // ??????????????????* ??????????????????SELECT *
        if (isStarSelectItem(literals)) {
            selectStatement.getItems().add(parseStarSelectItem());
            return;
        }
        // ?????????????????????????????????
        if (isAggregationSelectItem()) {
            selectStatement.getItems().add(parseAggregationSelectItem(literals));
            return;
        }
        // ????????????????????? * ???????????????
        StringBuilder expression = new StringBuilder();
        Token lastToken = null;
        while (!sqlParser.equalAny(DefaultKeyword.AS) && !sqlParser.equalAny(Symbol.COMMA) && !sqlParser.equalAny(DefaultKeyword.FROM) && !sqlParser.equalAny(Assist.END)) {
            String value = sqlParser.getLexer().getCurrentToken().getLiterals();
            int position = sqlParser.getLexer().getCurrentToken().getEndPosition() - value.length();
            expression.append(value);
            lastToken = sqlParser.getLexer().getCurrentToken();
            sqlParser.getLexer().nextToken();
            if (sqlParser.equalAny(Symbol.DOT)) {
                selectStatement.getSqlTokens().add(new TableToken(position, value));
            }
        }
        // ?????? AS???????????????????????????????????????????????????tips?????????????????????????????????????????????????????????substring???????????????????????????
        if (hasAlias(expression, lastToken)) {
            selectStatement.getItems().add(parseSelectItemWithAlias(expression, lastToken));
            return;
        }
        // ??? AS????????????SELECT user_id AS userId??? ?????? ?????????????????????SELECT user_id???
        selectStatement.getItems().add(new CommonSelectItem(SQLUtil.getExactlyValue(expression.toString()), sqlParser.parseAlias()));
    }
    
    protected boolean isRowNumberSelectItem() {
        return false;
    }
    
    protected SelectItem parseRowNumberSelectItem() {
        throw new UnsupportedOperationException("Cannot support special select item.");
    }

    private boolean isStarSelectItem(final String literals) {
        return sqlParser.equalAny(Symbol.STAR) || Symbol.STAR.getLiterals().equals(SQLUtil.getExactlyValue(literals));
    }

    private SelectItem parseStarSelectItem() {
        sqlParser.getLexer().nextToken();
        selectStatement.setContainStar(true);
        return new CommonSelectItem(Symbol.STAR.getLiterals(), sqlParser.parseAlias());
    }

    private boolean isAggregationSelectItem() {
        return sqlParser.skipIfEqual(DefaultKeyword.MAX, DefaultKeyword.MIN, DefaultKeyword.SUM, DefaultKeyword.AVG, DefaultKeyword.COUNT);
    }

    private SelectItem parseAggregationSelectItem(final String literals) {
        return new AggregationSelectItem(AggregationType.valueOf(literals.toUpperCase()), sqlParser.skipParentheses(), sqlParser.parseAlias());
    }

    private boolean hasAlias(final StringBuilder expression, final Token lastToken) {
        return null != lastToken && Literals.IDENTIFIER == lastToken.getType()
                && !isSQLPropertyExpression(expression, lastToken) // ???????????????????????????????????????1???????????????SELECT u.user_id u.user_id FROM t_user???
                && !expression.toString().equals(lastToken.getLiterals()); // ?????????????????????????????????2???????????????SELECT user_id FROM t_user???
    }

    /**
     * ?????? ????????? ??? "." + lastToken ?????????
     * ?????????????????????SELECT u.user_id u.user_id FROM t_user ??????
     *
     * @param expression ?????????
     * @param lastToken ?????? Token
     * @return ??????
     */
    private boolean isSQLPropertyExpression(final StringBuilder expression, final Token lastToken) {
        return expression.toString().endsWith(Symbol.DOT.getLiterals() + lastToken.getLiterals());
    }
    
    private CommonSelectItem parseSelectItemWithAlias(final StringBuilder expression, final Token lastToken) {
        return new CommonSelectItem(SQLUtil.getExactlyValue(expression.substring(0, expression.lastIndexOf(lastToken.getLiterals()))), Optional.of(lastToken.getLiterals()));
    }

    protected void queryRest() {
        if (sqlParser.equalAny(DefaultKeyword.UNION, DefaultKeyword.EXCEPT, DefaultKeyword.INTERSECT, DefaultKeyword.MINUS)) {
            throw new SQLParsingUnsupportedException(sqlParser.getLexer().getCurrentToken().getType());
        }
    }
    
    protected final void parseWhere() {
        if (selectStatement.getTables().isEmpty()) {
            return;
        }
        sqlParser.parseWhere(selectStatement);
        parametersIndex = sqlParser.getParametersIndex();
    }
    
    protected final void parseOrderBy() {
        if (!sqlParser.skipIfEqual(DefaultKeyword.ORDER)) {
            return;
        }
        List<OrderItem> result = new LinkedList<>();
        sqlParser.skipIfEqual(DefaultKeyword.SIBLINGS);
        sqlParser.accept(DefaultKeyword.BY);
        do {
            // ???????????? OrderBy
            Optional<OrderItem> orderItem = parseSelectOrderByItem();
            if (orderItem.isPresent() && !selectStatement.isContainSubQuery()) {
                result.add(orderItem.get());
            }
        }
        while (sqlParser.skipIfEqual(Symbol.COMMA));
        // OrderItem
        selectStatement.getOrderByItems().addAll(result);
    }

    /**
     * ?????????????????????
     *
     * @return ?????????
     */
    protected Optional<OrderItem> parseSelectOrderByItem() {
        // ASC / DESC
        SQLExpression sqlExpression = sqlParser.parseExpression(selectStatement);
        OrderType orderByType = OrderType.ASC;
        if (sqlParser.skipIfEqual(DefaultKeyword.ASC)) {
            orderByType = OrderType.ASC;
        } else if (sqlParser.skipIfEqual(DefaultKeyword.DESC)) {
            orderByType = OrderType.DESC;
        }
        // ?????? OrderItem
        OrderItem result;
        if (sqlExpression instanceof SQLNumberExpression) { // ORDER BY ?????? ??? ????????????????????????????????????
            result = new OrderItem(((SQLNumberExpression) sqlExpression).getNumber().intValue(), orderByType);
        } else if (sqlExpression instanceof SQLIdentifierExpression) {
            result = new OrderItem(
                    SQLUtil.getExactlyValue(((SQLIdentifierExpression) sqlExpression).getName()), orderByType, getAlias(SQLUtil.getExactlyValue(((SQLIdentifierExpression) sqlExpression).getName())));
        } else if (sqlExpression instanceof SQLPropertyExpression) {
            SQLPropertyExpression sqlPropertyExpression = (SQLPropertyExpression) sqlExpression;
            result = new OrderItem(SQLUtil.getExactlyValue(sqlPropertyExpression.getOwner().getName()), SQLUtil.getExactlyValue(sqlPropertyExpression.getName()), orderByType, 
                    getAlias(SQLUtil.getExactlyValue(sqlPropertyExpression.getOwner().getName()) + "." + SQLUtil.getExactlyValue(sqlPropertyExpression.getName())));
        } else {
            return Optional.absent();
        }
        return Optional.of(result);
    }

    /**
     * ?????? Group By ??? Having?????????????????????
     */
    protected void parseGroupBy() {
        if (sqlParser.skipIfEqual(DefaultKeyword.GROUP)) {
            sqlParser.accept(DefaultKeyword.BY);
            // ?????? Group By ????????????
            while (true) {
                addGroupByItem(sqlParser.parseExpression(selectStatement));
                if (!sqlParser.equalAny(Symbol.COMMA)) {
                    break;
                }
                sqlParser.getLexer().nextToken();
            }
            while (sqlParser.equalAny(DefaultKeyword.WITH) || sqlParser.getLexer().getCurrentToken().getLiterals().equalsIgnoreCase("ROLLUP")) {
                sqlParser.getLexer().nextToken();
            }
            // Having?????????????????????
            if (sqlParser.skipIfEqual(DefaultKeyword.HAVING)) {
                throw new UnsupportedOperationException("Cannot support Having");
            }
            selectStatement.setGroupByLastPosition(sqlParser.getLexer().getCurrentToken().getEndPosition());
        } else if (sqlParser.skipIfEqual(DefaultKeyword.HAVING)) {
            throw new UnsupportedOperationException("Cannot support Having");
        }
    }

    /**
     * ?????? Group By ????????????
     * Group By ????????????????????????????????????ASC
     *
     * @param sqlExpression ?????????
     */
    protected final void addGroupByItem(final SQLExpression sqlExpression) {
        // Group By ?????? DESC / ASC / ;????????? ASC???
        OrderType orderByType = OrderType.ASC;
        if (sqlParser.equalAny(DefaultKeyword.ASC)) {
            sqlParser.getLexer().nextToken();
        } else if (sqlParser.skipIfEqual(DefaultKeyword.DESC)) {
            orderByType = OrderType.DESC;
        }
        // ?????? OrderItem
        OrderItem orderItem;
        if (sqlExpression instanceof SQLPropertyExpression) {
            SQLPropertyExpression sqlPropertyExpression = (SQLPropertyExpression) sqlExpression;
            orderItem = new OrderItem(SQLUtil.getExactlyValue(sqlPropertyExpression.getOwner().getName()), SQLUtil.getExactlyValue(sqlPropertyExpression.getName()), orderByType,
                    getAlias(SQLUtil.getExactlyValue(sqlPropertyExpression.getOwner() + "." + SQLUtil.getExactlyValue(sqlPropertyExpression.getName()))));
        } else if (sqlExpression instanceof SQLIdentifierExpression) {
            SQLIdentifierExpression sqlIdentifierExpression = (SQLIdentifierExpression) sqlExpression;
            orderItem = new OrderItem(SQLUtil.getExactlyValue(sqlIdentifierExpression.getName()), orderByType, getAlias(SQLUtil.getExactlyValue(sqlIdentifierExpression.getName())));
        } else {
            return;
        }
        if (!selectStatement.isContainSubQuery()) {
            selectStatement.getGroupByItems().add(orderItem);
        }
    }

    /**
     * ??????????????????????????????
     *
     * @param name ??????
     * @return ??????
     */
    private Optional<String> getAlias(final String name) {
        if (selectStatement.isContainStar()) {
            return Optional.absent();
        }
        String rawName = SQLUtil.getExactlyValue(name);
        for (SelectItem each : selectStatement.getItems()) {
            if (rawName.equalsIgnoreCase(SQLUtil.getExactlyValue(each.getExpression()))) {
                return each.getAlias();
            }
            if (rawName.equalsIgnoreCase(each.getAlias().orNull())) {
                return Optional.of(rawName);
            }
        }
        return Optional.absent();
    }

    /**
     * ??????????????????????????????
     */
    public final void parseFrom() {
        if (sqlParser.skipIfEqual(DefaultKeyword.FROM)) {
            parseTable();
        }
    }

    /**
     * ??????????????????????????????
     */
    public void parseTable() {
        // ???????????????
        if (sqlParser.skipIfEqual(Symbol.LEFT_PAREN)) {
            if (!selectStatement.getTables().isEmpty()) {
                throw new UnsupportedOperationException("Cannot support subquery for nested tables.");
            }
            selectStatement.setContainSubQuery(true);
            selectStatement.setContainStar(false); // TODO ??????
            // ????????????????????????
            sqlParser.skipUselessParentheses();
            // ??????????????? SQL
            parse();
            // ????????????????????????
            sqlParser.skipUselessParentheses();
            //
            if (!selectStatement.getTables().isEmpty()) {
                return;
            }
        }
        // ???????????????
        parseTableFactor();
        // ??????????????????
        parseJoinTable();
    }

    /**
     * ??????????????????????????????
     */
    protected final void parseTableFactor() {
        int beginPosition = sqlParser.getLexer().getCurrentToken().getEndPosition() - sqlParser.getLexer().getCurrentToken().getLiterals().length();
        String literals = sqlParser.getLexer().getCurrentToken().getLiterals();
        sqlParser.getLexer().nextToken();
        // TODO ??????Schema??????
        if (sqlParser.skipIfEqual(Symbol.DOT)) { // https://dev.mysql.com/doc/refman/5.7/en/information-schema.html ???SELECT table_name, table_type, engine FROM information_schema.tables
            sqlParser.getLexer().nextToken();
            sqlParser.parseAlias();
            return;
        }
        // FIXME ??????shardingRule??????table
        selectStatement.getSqlTokens().add(new TableToken(beginPosition, literals));
        // ??? ?????? ?????????
        selectStatement.getTables().add(new Table(SQLUtil.getExactlyValue(literals), sqlParser.parseAlias()));
    }

    /**
     * ?????? Join Table ?????? FROM ????????? Table
     */
    protected void parseJoinTable() {
        if (sqlParser.skipJoin()) {
            // ???????????? parseJoinTable() ????????? parseTableFactor() ???????????? Table ??????????????????
            // ?????????SELECT * FROM t_order JOIN (SELECT * FROM t_order_item JOIN t_order_other ON ) .....
            parseTable();
            if (sqlParser.skipIfEqual(DefaultKeyword.ON)) { // JOIN ?????? ON ??????
                do {
                    parseTableCondition(sqlParser.getLexer().getCurrentToken().getEndPosition());
                    sqlParser.accept(Symbol.EQ);
                    parseTableCondition(sqlParser.getLexer().getCurrentToken().getEndPosition() - sqlParser.getLexer().getCurrentToken().getLiterals().length());
                } while (sqlParser.skipIfEqual(DefaultKeyword.AND));
            } else if (sqlParser.skipIfEqual(DefaultKeyword.USING)) { // JOIN ?????? USING ??????????????????????????????????????? ON ?????????????????????????????? SQL ?????????
                                                                        // SELECT * FROM t_order o JOIN t_order_item i USING (order_id);
                                                                        // SELECT * FROM t_order o JOIN t_order_item i ON o.order_id = i.order_id
                sqlParser.skipParentheses();
            }
            parseJoinTable(); // ????????????
        }
    }

    /**
     * ?????? ON ???????????? TableToken
     *
     * @param startPosition ????????????
     */
    private void parseTableCondition(final int startPosition) {
        SQLExpression sqlExpression = sqlParser.parseExpression();
        if (!(sqlExpression instanceof SQLPropertyExpression)) {
            return;
        }
        SQLPropertyExpression sqlPropertyExpression = (SQLPropertyExpression) sqlExpression;
        if (selectStatement.getTables().getTableNames().contains(SQLUtil.getExactlyValue(sqlPropertyExpression.getOwner().getName()))) {
            selectStatement.getSqlTokens().add(new TableToken(startPosition, sqlPropertyExpression.getOwner().getName()));
        }
    }

    /**
     * ??????????????????
     */
    private void appendDerivedColumns() {
        if (appendDerivedColumnsFlag) {
            return;
        }
        appendDerivedColumnsFlag = true;
        ItemsToken itemsToken = new ItemsToken(selectStatement.getSelectListLastPosition());
        // AVG ????????????
        appendAvgDerivedColumns(itemsToken);
        // ORDER BY
        appendDerivedOrderColumns(itemsToken, selectStatement.getOrderByItems(), ORDER_BY_DERIVED_ALIAS);
        // GROUP BY
        appendDerivedOrderColumns(itemsToken, selectStatement.getGroupByItems(), GROUP_BY_DERIVED_ALIAS);
        if (!itemsToken.getItems().isEmpty()) {
            selectStatement.getSqlTokens().add(itemsToken);
        }
    }

    /**
     * ?????? AVG ?????????????????????????????????
     * AVG ????????? SUM + COUNT ???????????????????????? AVG ?????????
     *
     * @param itemsToken ?????????????????????
     */
    private void appendAvgDerivedColumns(final ItemsToken itemsToken) {
        int derivedColumnOffset = 0;
        for (SelectItem each : selectStatement.getItems()) {
            if (!(each instanceof AggregationSelectItem) || AggregationType.AVG != ((AggregationSelectItem) each).getType()) {
                continue;
            }
            AggregationSelectItem avgItem = (AggregationSelectItem) each;
            // COUNT ??????
            String countAlias = String.format(DERIVED_COUNT_ALIAS, derivedColumnOffset);
            AggregationSelectItem countItem = new AggregationSelectItem(AggregationType.COUNT, avgItem.getInnerExpression(), Optional.of(countAlias));
            // SUM ??????
            String sumAlias = String.format(DERIVED_SUM_ALIAS, derivedColumnOffset);
            AggregationSelectItem sumItem = new AggregationSelectItem(AggregationType.SUM, avgItem.getInnerExpression(), Optional.of(sumAlias));
            // AggregationSelectItem ??????
            avgItem.getDerivedAggregationSelectItems().add(countItem);
            avgItem.getDerivedAggregationSelectItems().add(sumItem);
            // TODO ???AVG??????????????????????????????????????????????????????AVG??????
            // ItemsToken
            itemsToken.getItems().add(countItem.getExpression() + " AS " + countAlias + " ");
            itemsToken.getItems().add(sumItem.getExpression() + " AS " + sumAlias + " ");
            //
            derivedColumnOffset++;
        }
    }

    /**
     * ?????? GROUP BY ??? ORDER BY ???????????????????????????
     * ????????????????????????????????????????????????????????????????????????????????????????????? GROUP BY ??? ORDER BY
     *
     * @param itemsToken ?????????????????????
     * @param orderItems ????????????
     * @param aliasPattern ????????????
     */
    private void appendDerivedOrderColumns(final ItemsToken itemsToken, final List<OrderItem> orderItems, final String aliasPattern) {
        int derivedColumnOffset = 0;
        for (OrderItem each : orderItems) {
            if (!isContainsItem(each)) {
                String alias = String.format(aliasPattern, derivedColumnOffset++);
                each.setAlias(Optional.of(alias));
                itemsToken.getItems().add(each.getQualifiedName().get() + " AS " + alias + " ");
            }
        }
    }

    /**
     * ????????????????????????????????????
     *
     * @param orderItem ????????????
     * @return ??????
     */
    private boolean isContainsItem(final OrderItem orderItem) {
        if (selectStatement.isContainStar()) { // SELECT *
            return true;
        }
        for (SelectItem each : selectStatement.getItems()) {
            if (-1 != orderItem.getIndex()) { // ORDER BY ????????????
                return true;
            }
            if (each.getAlias().isPresent() && orderItem.getAlias().isPresent() && each.getAlias().get().equalsIgnoreCase(orderItem.getAlias().get())) { // ??????????????????
                return true;
            }
            if (!each.getAlias().isPresent() && orderItem.getQualifiedName().isPresent() && each.getExpression().equalsIgnoreCase(orderItem.getQualifiedName().get())) { // ??????????????????
                return true;
            }
        }
        return false;
    }

    /**
     * ?????? Order By ?????????????????? Group By ?????????????????????????????????????????????
     */
    private void appendDerivedOrderBy() {
        if (!getSelectStatement().getGroupByItems().isEmpty() && getSelectStatement().getOrderByItems().isEmpty()) {
            getSelectStatement().getOrderByItems().addAll(getSelectStatement().getGroupByItems());
            getSelectStatement().getSqlTokens().add(new OrderByToken(getSelectStatement().getGroupByLastPosition()));
        }
    }
}
