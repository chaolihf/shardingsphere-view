/**
 * 
 */
package com.chinatelecom.udp.component.shardingsphere;

import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.shardingsphere.infra.database.core.type.DatabaseType;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.sql.parser.api.CacheOption;
import org.junit.Test;
import org.apache.shardingsphere.sql.parser.api.SQLParserEngine;

/**
 * @author lichao
 *
 */
public class ShardingsphereSqlViewRewriteTest {

	@Test
	public void testModifyTable() {
		DatabaseType databaseType = TypedSPILoader.getService(DatabaseType.class, "MySQL");
		SQLParserEngine parserEngine = new SQLParserEngine(databaseType, new CacheOption(2000, 65535L));
		SQLViewRewrite rewriter=new SQLViewRewrite();
		rewriter.analyseSql(parserEngine,"select * from table1 t where name='bbb'");
		rewriter.analyseSql(parserEngine,"select * from (select * from view1) t where name='bbb'");
		rewriter.analyseSql(parserEngine,"select * from (select * from view1)");
		
		
		
        
        
//		SQLStatementVisitorEngine visitorEngine = new SQLStatementVisitorEngine(databaseType);
//		SQLStatement statement = visitorEngine.visit(parseASTNode);
//		
//		
//		
//		if (rootNode instanceof MySQLStatementParser.ExecuteContext) {
//			MySQLStatementParser.ExecuteContext executeContext=(MySQLStatementParser.ExecuteContext)rootNode;
//			SelectContext selectContext = executeContext.select();
//			
//		}
		
		// AST替换逻辑示例
//		if (parseASTNode.getSqlStatement() instanceof org.apache.shardingsphere.sql.parser.sql.common.statement.dml.SelectStatement) {
//			org.apache.shardingsphere.sql.parser.sql.common.statement.dml.SelectStatement selectStatement =
//				(org.apache.shardingsphere.sql.parser.sql.common.statement.dml.SelectStatement) parseASTNode.getSqlStatement();
//			for (org.apache.shardingsphere.sql.parser.sql.common.segment.dml.table.TableSegment tableSegment : selectStatement.getFrom()) {
//				if (tableSegment instanceof org.apache.shardingsphere.sql.parser.sql.common.segment.dml.table.SimpleTableSegment) {
//					org.apache.shardingsphere.sql.parser.sql.common.segment.dml.table.SimpleTableSegment simpleTable =
//						(org.apache.shardingsphere.sql.parser.sql.common.segment.dml.table.SimpleTableSegment) tableSegment;
//					String tableName = simpleTable.getTableName().getIdentifier().getValue();
//					if ("table1".equalsIgnoreCase(tableName)) {
//						// 构造子查询
//						String subquerySql = "select * from view1";
//						ParseASTNode subqueryAst = parserEngine.parse(subquerySql, false);
//						org.apache.shardingsphere.sql.parser.sql.common.segment.dml.SubquerySegment subquerySegment =
//							new org.apache.shardingsphere.sql.parser.sql.common.segment.dml.SubquerySegment(
//								subqueryAst.getSqlStatement(),
//								simpleTable.getStartIndex(),
//								simpleTable.getStopIndex()
//							);
//						org.apache.shardingsphere.sql.parser.sql.common.segment.dml.table.SubqueryTableSegment subqueryTable =
//							new org.apache.shardingsphere.sql.parser.sql.common.segment.dml.table.SubqueryTableSegment(
//								subquerySegment,
//								simpleTable.getAlias().orElse(null)
//							);
//						// 替换原TableSegment
//						selectStatement.getFrom().remove(tableSegment);
//						selectStatement.getFrom().add(subqueryTable);
//						break;
//					}
//				}
//			}
//		}
	}

}