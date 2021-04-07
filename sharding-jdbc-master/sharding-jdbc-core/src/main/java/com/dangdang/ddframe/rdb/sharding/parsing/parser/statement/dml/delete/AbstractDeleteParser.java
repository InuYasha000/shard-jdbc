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

package com.dangdang.ddframe.rdb.sharding.parsing.parser.statement.dml.delete;

import com.dangdang.ddframe.rdb.sharding.parsing.lexer.token.DefaultKeyword;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.SQLParser;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.statement.SQLStatementParser;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.statement.dml.DMLStatement;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Delete语句解析器.
 *
 * @author zhangliang
 */
@RequiredArgsConstructor
@Getter(AccessLevel.PROTECTED)
public abstract class AbstractDeleteParser implements SQLStatementParser {
    
    private final SQLParser sqlParser;
    
    private final DMLStatement deleteStatement;
    
    public AbstractDeleteParser(final SQLParser sqlParser) {
        this.sqlParser = sqlParser;
        deleteStatement = new DMLStatement();
    }

// Single-Table Syntax ：
//    DELETE [LOW_PRIORITY] [QUICK] [IGNORE] FROM tbl_name
//    [PARTITION (partition_name,...)]
//            [WHERE where_condition]
//            [ORDER BY ...]
//            [LIMIT row_count]

// Multiple-Table Syntax ：
//    DELETE [LOW_PRIORITY] [QUICK] [IGNORE]
//    tbl_name[.*] [, tbl_name[.*]] ...
//    FROM table_references
//    [WHERE where_condition]
// OR
//    DELETE [LOW_PRIORITY] [QUICK] [IGNORE]
//    FROM tbl_name[.*] [, tbl_name[.*]] ...
//    USING table_references
//    [WHERE where_condition]

    @Override
    public DMLStatement parse() {
        sqlParser.getLexer().nextToken(); // 跳过 DELETE
        skipBetweenDeleteAndTable(); // // 跳过关键字，例如：MYSQL 里的 LOW_PRIORITY、IGNORE 和 FROM
        sqlParser.parseSingleTable(deleteStatement); // 解析表
        sqlParser.skipUntil(DefaultKeyword.WHERE); // 跳到 WHERE
        sqlParser.parseWhere(deleteStatement); // 解析 WHERE
        return deleteStatement;
    }
    
    protected abstract void skipBetweenDeleteAndTable();
}
