package com.chinatelecom.udp.component.shardingsphere;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.apache.shardingsphere.infra.database.core.spi.DatabaseTypedSPILoader;
import org.apache.shardingsphere.infra.database.core.type.DatabaseType;
import org.apache.shardingsphere.infra.database.mysql.type.MySQLDatabaseType;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.sql.parser.api.CacheOption;
import org.apache.shardingsphere.sql.parser.api.SQLParserEngine;
import org.apache.shardingsphere.sql.parser.api.SQLStatementVisitorEngine;
import org.apache.shardingsphere.sql.parser.api.visitor.format.SQLFormatVisitor;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.SelectContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.SubqueryContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.TableFactorContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.TableNameContext;
import org.apache.shardingsphere.sql.parser.core.ParseASTNode;
import org.apache.shardingsphere.sql.parser.statement.core.statement.SQLStatement;

public class SQLViewRewrite {

	private SQLParserEngine parserEngine;
	
	public void setParseEngine(SQLParserEngine parserEngine){
		this.parserEngine=parserEngine;
	}
	
	private String printSql(ParseTree rootNode) {
		Properties props = new Properties();
		props.setProperty("parameterized", "false");
		SQLFormatVisitor formatVisitor = DatabaseTypedSPILoader.getService(
			SQLFormatVisitor.class, TypedSPILoader.getService(DatabaseType.class, "MySQL"), props);
		String formattedSQL = formatVisitor.visit(rootNode);
		System.out.println(formattedSQL);
		return formattedSQL;
	}
	
	public static String generateChars(int count) {
		if(count<=0) return "";
		char[] chars = new char[count];
		Arrays.fill(chars, '-');
		return new String(chars);
	}
	
	private void printStructure(ParseTree rootNode,int level) {
		int childCount=rootNode.getChildCount();
		String concat=generateChars(level*3);
		String text="";
		if (rootNode instanceof TerminalNodeImpl) {
			TerminalNodeImpl valueNode=(TerminalNodeImpl)rootNode;
			text= " ------ " + valueNode.getText();
		}
		System.out.println( concat + rootNode.getClass().getSimpleName() + "(" + childCount + ")" + text);
		for(int i=0;i<childCount;i++) {
			ParseTree child = rootNode.getChild(i);
			if (child!=null) {
				printStructure(child,level+1);
			}
		}
	}

	void analyseSql(String sql) {
		ParseASTNode parseASTNode = parserEngine.parse(sql, false);
		ParseTree rootNode = parseASTNode.getRootNode();
		printStructure(rootNode,0);
		printSql(rootNode);
	}

	public String rewriteSql(String sql) {
		ParseASTNode parseASTNode = parserEngine.parse(sql, false);
		ParseTree rootNode = parseASTNode.getRootNode();
		rewriteTableFactor(rootNode);
		printStructure(rootNode,0);
		return printSql(rootNode);
	}
	
	public void rewriteTableFactor(ParseTree rootNode) {
		int childCount=rootNode.getChildCount();
		if (rootNode instanceof MySQLStatementParser.TableFactorContext) {
			MySQLStatementParser.TableFactorContext factor=(MySQLStatementParser.TableFactorContext)rootNode;
			int tableFactorCount= factor.getChildCount();
			MySQLStatementParser.TableNameContext tableNameContext=null;
			boolean hasAlias=false;
			for(int i=0;i<tableFactorCount;i++){
				ParseTree childContext = factor.getChild(i);
				if (childContext instanceof MySQLStatementParser.TableNameContext){
					tableNameContext=(MySQLStatementParser.TableNameContext)childContext;
				} else if (childContext instanceof MySQLStatementParser.AliasContext){
					hasAlias=true;
				}
			}
			//如果没有别名那就使用原表名增加一个别名，如果有别名那就直接将表替换为子查询
			if (tableNameContext!=null){
				TerminalNodeImpl tableNode= getTableName(tableNameContext);
				if (isTableShouldRewrite(tableNode)){
					SubqueryContext subqueryContext = getSubustituteSubQuery(createReplaceSubQuery(tableNode,hasAlias));
					if (hasAlias){
						factor.children.set(0, subqueryContext);
					} 
					else {
						factor.children.remove(0);
						factor.children.add(subqueryContext.getParent());
					}
				}
			}

		}
		else {
			for(int i=0;i<childCount;i++) {
				ParseTree child = rootNode.getChild(i);
				if (child!=null) {
					rewriteTableFactor(child);
				}
			}
		}
	}

	/**
	 * 
	 * @param tableNode
	 * @param hasAlias 原始表有别名的话，替换的子查询不需要别名
	 * @return
	 */
	private ParseTree createReplaceSubQuery(TerminalNodeImpl tableNode,boolean hasAlias) {
		String subQuery="select * from (select * from view1)" + (hasAlias?"":" " + tableNode.getText());
		ParseASTNode parseASTNode = parserEngine.parse(subQuery, false);
		ParseTree rootNode = parseASTNode.getRootNode();
		printStructure(rootNode, 0);
		return rootNode;
	}

	private boolean isTableShouldRewrite(TerminalNodeImpl tableNode){
		return true;
	}

	private TerminalNodeImpl getTableName(TableNameContext tableNameContext) {
		ParseTree tableNode = tableNameContext.getChild(0).getChild(0).getChild(0);
		return (TerminalNodeImpl)tableNode;
	}

	private MySQLStatementParser.SubqueryContext getSubustituteSubQuery(ParseTree rootNode){
		int childCount=rootNode.getChildCount();
		if (rootNode instanceof MySQLStatementParser.SubqueryContext) {
			return (MySQLStatementParser.SubqueryContext)rootNode;
		}
		for(int i=0;i<childCount;i++) {
			ParseTree child = rootNode.getChild(i);
			if (child!=null) {
				SubqueryContext subQueryResult = getSubustituteSubQuery(child);
				if(subQueryResult!=null){
					return subQueryResult;
				}
			}
		}
		return null;
	}

}