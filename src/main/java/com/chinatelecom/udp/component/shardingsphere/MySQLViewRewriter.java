package com.chinatelecom.udp.component.shardingsphere;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.apache.shardingsphere.infra.database.core.type.DatabaseType;
import org.apache.shardingsphere.infra.rewrite.sql.token.common.pojo.SQLToken;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.proxy.backend.session.ConnectionSession;
import org.apache.shardingsphere.sql.parser.api.CacheOption;
import org.apache.shardingsphere.sql.parser.api.SQLParserEngine;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.AliasContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.AssignmentContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.AssignmentValuesContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.ColumnRefContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.DeleteContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.ExprContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.FieldsContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.InsertContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.InsertSelectClauseContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.InsertValuesClauseContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.ProjectionsContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.SelectContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.SetAssignmentsClauseContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.SimpleExprContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.SubqueryContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.TableFactorContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.TableNameContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.UpdateContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.WhereClauseContext;
import org.apache.shardingsphere.sql.parser.core.ParseASTNode;
import org.apache.shardingsphere.sql.parser.exception.SQLParsingException;

public class MySQLViewRewriter extends ViewRewriter{

	private static final Logger LOGGER = Logger.getLogger(MySQLViewRewriter.class.getName());

	private static MySQLViewRewriter instance;

	public static MySQLViewRewriter getInstance(){
		if(instance == null){
			synchronized (MySQLViewRewriter.class) {
				if (instance == null) {
					instance=new MySQLViewRewriter();
				}
			}
		}
		return instance;
	}

	
	public MySQLViewRewriter(){
		super("MySQL");
	}

	
	public String rewriteSql(String userName,String sql) {
		ParseASTNode parseASTNode = parserEngine.parse(sql, false);
		ParseTree rootNode = parseASTNode.getRootNode();
		if (rootNode instanceof SelectContext){
			rewriteTableFactor(userName,rootNode,parseASTNode);
		} else if (rootNode instanceof InsertContext){
			rewriteInsertContext(userName,rootNode,parseASTNode);
		} else if (rootNode instanceof UpdateContext){
			rewriteUpdateContext(userName,rootNode,parseASTNode);
		} else if (rootNode instanceof DeleteContext){
			rewriteDeleteContext(userName,rootNode,parseASTNode);
		} else{
			return sql;
		}
		printStructure(rootNode,0);
		return printSql(parseASTNode);
	}

	public void rewriteInsertContext(String userName,ParseTree rootNode,ParseASTNode parseASTNode) {
		FieldsContext insertFieldsContext=null;
		//查找insert value子语句
		for(int i=0;i<rootNode.getChildCount();i++){
			ParseTree childContext = rootNode.getChild(i);
			if (childContext instanceof InsertValuesClauseContext){
				AssignmentValuesContext assignValueContext=null;
				InsertValuesClauseContext insertValuesContext=(InsertValuesClauseContext)childContext;
				for(int j=0;j<insertValuesContext.getChildCount();j++){
					ParseTree valueContext = insertValuesContext.getChild(j);
					if (valueContext instanceof FieldsContext){
						insertFieldsContext=(FieldsContext)valueContext;
					} else if (valueContext instanceof AssignmentValuesContext){
						assignValueContext=(AssignmentValuesContext)valueContext;
					}
				}
				if (insertFieldsContext==null){
					throw new SQLParsingException("不允许插入语句不指定列:" + printSql(parseASTNode));
				} else if (assignValueContext!=null){
					ParseTree[] appendValues=getInsertFieldAndValue(userName);
					insertFieldsContext.children.add(appendValues[0]);
					insertFieldsContext.children.add(appendValues[1]);
					assignValueContext.children.add(appendValues[2]);
					assignValueContext.children.add(appendValues[3]);
					
				}
				
			} else if (childContext instanceof InsertSelectClauseContext){
				InsertSelectClauseContext insertSelectContext=(InsertSelectClauseContext)childContext;
				ProjectionsContext insertProjectionsContext=null;
				for(int j=0;j<insertSelectContext.getChildCount();j++){
					ParseTree valueContext = insertSelectContext.getChild(j);
					if (valueContext instanceof FieldsContext){
						insertFieldsContext=(FieldsContext)valueContext;
					} else if (valueContext instanceof SelectContext){
						insertProjectionsContext=findFirstClassType(valueContext,ProjectionsContext.class);
						rewriteTableFactor(userName,valueContext,parseASTNode);
					}
				}
				if (insertFieldsContext==null){
					throw new SQLParsingException("不允许插入语句不指定列:" + printSql(parseASTNode));
				} else if (insertProjectionsContext!=null){
					ParseTree[] appendValues=getInsertFieldAndSelectValue(userName);
					insertFieldsContext.children.add(appendValues[0]);
					insertFieldsContext.children.add(appendValues[1]);
					insertProjectionsContext.children.add(appendValues[2]);
					insertProjectionsContext.children.add(appendValues[3]);
				}
			}
		}
	}


	/**
	 * 
	 * @return 固定长度为4，前两个为字段，后两个为值
	 */
	private ParseTree[] getInsertFieldAndValue(String userName) {
		ParseTree[] result=new ParseTree[4];
		String insertSql="insert into t(a,tanentId) values('','" + userName +"')";
		ParseASTNode parseASTNode = parserEngine.parse(insertSql, false);
		ParseTree rootNode = parseASTNode.getRootNode();
		InsertValuesClauseContext insertValuesContext=(InsertValuesClauseContext)rootNode.getChild(4);
		FieldsContext fieldsContext=(FieldsContext)insertValuesContext.getChild(1);
		AssignmentValuesContext valuesContext=(AssignmentValuesContext)insertValuesContext.getChild(4);
		result[0]=fieldsContext.getChild(1);
		result[1]=fieldsContext.getChild(2);
		result[2]=valuesContext.getChild(2);
		result[3]=valuesContext.getChild(3);
		return result;
	}

	/**
	 * 
	 * @return 固定长度为4，前两个为字段，后两个为值
	 */
	private ParseTree[] getInsertFieldAndSelectValue(String userName) {
		ParseTree[] result=new ParseTree[4];
		String insertSql="insert into table1(name,tanent_id) select name,'" + userName + "' from table2";
		ParseASTNode parseASTNode = parserEngine.parse(insertSql, false);
		ParseTree rootNode = parseASTNode.getRootNode();
		InsertSelectClauseContext insertSelectContext=(InsertSelectClauseContext)rootNode.getChild(4);
		FieldsContext fieldsContext=(FieldsContext)insertSelectContext.getChild(1);
		SelectContext selectContext=(SelectContext)insertSelectContext.getChild(3);
		ProjectionsContext projectsContext=findFirstClassType(selectContext,ProjectionsContext.class);
		result[0]=fieldsContext.getChild(1);
		result[1]=fieldsContext.getChild(2);
		result[2]=projectsContext.getChild(1);
		result[3]=projectsContext.getChild(2);
		return result;
	}


	public void rewriteUpdateContext(String userName,ParseTree rootNode,ParseASTNode parseASTNode) {
		WhereClauseContext whereClauseContext=findWhereContext(rootNode);
		if (whereClauseContext==null){
			throw new SQLParsingException("更新必须包含条件语句," + printSql(parseASTNode));
		}
		SetAssignmentsClauseContext setAssignContext=findSetAssignContext(rootNode);
		for(int i=0;i<setAssignContext.getChildCount();i++){
			ParseTree childNode = setAssignContext.getChild(i);
			if(childNode instanceof AssignmentContext){
				if (!checkColumnNameValid(childNode)){
					throw new SQLParsingException("更新语句不允许包含租户字段," + printSql(parseASTNode));
				}
			}
		}
		ExprContext expressContext=getConditionExpressionContext(userName,whereClauseContext.getChild(1));
		whereClauseContext.children.set(1, expressContext);
		rewriteTableFactor(userName,whereClauseContext,parseASTNode);
	}

	/**
	 * 更新语句的列名不能有租户字段
	 * @param childNode
	 */
	private boolean checkColumnNameValid(ParseTree assignmentNode) {
		for (int i = 0; i < assignmentNode.getChildCount(); i++) {
			ParseTree childNode = assignmentNode.getChild(i);
			if (childNode instanceof ColumnRefContext){
				TerminalNodeImpl nameNode=(TerminalNodeImpl) childNode.getChild(0).getChild(0).getChild(0);
				if ("tanent_id".equalsIgnoreCase(nameNode.getText())){
					return false;
				}
			}
		}
		return true;
	}


	private SetAssignmentsClauseContext findSetAssignContext(ParseTree rootNode) {
		for(int i=0;i<rootNode.getChildCount();i++){
			ParseTree childContext=rootNode.getChild(i);
			if(childContext instanceof SetAssignmentsClauseContext){
				return (SetAssignmentsClauseContext) childContext;
			}
		}
		return null;
	}

	public void rewriteDeleteContext(String userName,ParseTree rootNode,ParseASTNode parseASTNode) {
		WhereClauseContext whereClauseContext=findWhereContext(rootNode);
		if (whereClauseContext==null){
			throw new SQLParsingException("删除必须包含条件语句," + printSql(parseASTNode));
		}
		ExprContext expressContext=getConditionExpressionContext(userName,whereClauseContext.getChild(1));
		whereClauseContext.children.set(1, expressContext);
		rewriteTableFactor(userName,whereClauseContext,parseASTNode);
	}

	private ExprContext getConditionExpressionContext(String userName,ParseTree originParseTree){
		String deleteSql="delete from table1 where  tanent_id='" + userName +"' and (name='b')";
		ParseASTNode parseASTNode = parserEngine.parse(deleteSql, false);
		ParseTree rootNode = parseASTNode.getRootNode();
		WhereClauseContext whereContext=(WhereClauseContext) rootNode.getChild(3);
		ExprContext result=(ExprContext) whereContext.getChild(1);
		ExprContext subsituteContext=(ExprContext) result.getChild(2);
		//找到()的父节点
		SimpleExprContext simpleExprContext=(SimpleExprContext) subsituteContext.getChild(0).getChild(0).getChild(0).getChild(0);
		simpleExprContext.children.set(1, originParseTree);
		return result;
	}
	
	private WhereClauseContext findWhereContext(ParseTree rootNode) {
		for(int i=0;i<rootNode.getChildCount();i++){
			ParseTree childContext=rootNode.getChild(i);
			if(childContext instanceof WhereClauseContext){
				return (WhereClauseContext) childContext;
			}
		}
		return null;
	}

	public void rewriteTableFactor(String userName,ParseTree rootNode,ParseASTNode parseASTNode) {
		int childCount=rootNode.getChildCount();
		if (rootNode instanceof TableFactorContext) {
			TableFactorContext factor=(TableFactorContext)rootNode;
			int tableFactorCount= factor.getChildCount();
			TableNameContext tableNameContext=null;
			boolean hasAlias=false;
			for(int i=0;i<tableFactorCount;i++){
				ParseTree childContext = factor.getChild(i);
				if (childContext instanceof TableNameContext){
					tableNameContext=(TableNameContext)childContext;
				} else if (childContext instanceof AliasContext){
					hasAlias=true;
				}
			}
			//如果没有别名那就使用原表名增加一个别名，如果有别名那就直接将表替换为子查询
			if (tableNameContext!=null){
				TerminalNodeImpl tableNode= getTableName(tableNameContext);
				String tableName=tableNode.getText();
				if (isTableShouldRewrite(tableName)){
					SubqueryContext subqueryContext = findFirstClassType(createReplaceSubQuery(userName,tableName,hasAlias), SubqueryContext.class);
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
					rewriteTableFactor(userName,child,parseASTNode);
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
	private ParseTree createReplaceSubQuery(String userName,String tableName,boolean hasAlias) {
		String subQuery="select * from (select * from " + tableName +" where tanentId='" + userName +"')" + (hasAlias?"":" " + tableName);
		ParseASTNode parseASTNode = parserEngine.parse(subQuery, false);
		ParseTree rootNode = parseASTNode.getRootNode();
		printStructure(rootNode, 0);
		return rootNode;
	}

	private TerminalNodeImpl getTableName(TableNameContext tableNameContext) {
		ParseTree tableNode = tableNameContext.getChild(0).getChild(0).getChild(0);
		return (TerminalNodeImpl)tableNode;
	}

	public static String rewriteSql(ConnectionSession connectionSession,String sql){
		MySQLViewRewriter sqlWriter = getInstance();
		String userName=connectionSession.getConnectionContext().getGrantee().getUsername();
		if(userName!=null){
			String newSql= sqlWriter.rewriteSql(userName,sql);
			return newSql;
		}
		throw new SQLParsingException("异常的数据库账号信息:" + sql);
	}


	@Override
	public List<SQLToken> generateTokens(String userName,String sql) {
		ParseASTNode parseASTNode = parserEngine.parse(sql, false);
		ParseTree rootNode = parseASTNode.getRootNode();
		List<SQLToken> result=new ArrayList<>();
		if (rootNode instanceof SelectContext){
			generateSelectTokens(result,userName,rootNode,sql);
		} else if (rootNode instanceof InsertContext){
			generateInsertTokens(result,userName,rootNode,sql);
		} else if (rootNode instanceof UpdateContext){
			generateUpdateTokens(result,userName,rootNode,sql);
		} else if (rootNode instanceof DeleteContext){
			generateDeleteTokens(result,userName,rootNode,sql);
		} 
		return result;
	}

	public void generateSelectTokens(List<SQLToken> result,String userName,ParseTree rootNode,String sql) {
		int childCount=rootNode.getChildCount();
		if (rootNode instanceof TableFactorContext) {
			TableFactorContext factor=(TableFactorContext)rootNode;
			int tableFactorCount= factor.getChildCount();
			TableNameContext tableNameContext=null;
			boolean hasAlias=false;
			for(int i=0;i<tableFactorCount;i++){
				ParseTree childContext = factor.getChild(i);
				if (childContext instanceof TableNameContext){
					tableNameContext=(TableNameContext)childContext;
				} else if (childContext instanceof AliasContext){
					hasAlias=true;
				}
			}
			//如果没有别名那就使用原表名增加一个别名，如果有别名那就直接将表替换为子查询
			if (tableNameContext!=null){
				TerminalNodeImpl tableNode= getTableName(tableNameContext);
				String tableName=tableNode.getText();
				if (isTableShouldRewrite(tableName)){
					SubqueryContext subqueryContext = findFirstClassType(createReplaceSubQuery(userName,tableName,hasAlias), SubqueryContext.class);
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
					generateSelectTokens(result,userName,child,sql);
				}
			}
		}
	}
	public void generateInsertTokens(List<SQLToken> result,String userName,ParseTree rootNode,String sql) {
		FieldsContext insertFieldsContext=null;
		//查找insert value子语句
		for(int i=0;i<rootNode.getChildCount();i++){
			ParseTree childContext = rootNode.getChild(i);
			if (childContext instanceof InsertValuesClauseContext){
				AssignmentValuesContext assignValueContext=null;
				InsertValuesClauseContext insertValuesContext=(InsertValuesClauseContext)childContext;
				for(int j=0;j<insertValuesContext.getChildCount();j++){
					ParseTree valueContext = insertValuesContext.getChild(j);
					if (valueContext instanceof FieldsContext){
						insertFieldsContext=(FieldsContext)valueContext;
					} else if (valueContext instanceof AssignmentValuesContext){
						assignValueContext=(AssignmentValuesContext)valueContext;
					}
				}
				if (insertFieldsContext==null){
					throw new SQLParsingException("不允许插入语句不指定列:" + sql);
				} else if (assignValueContext!=null){
					ParseTree[] appendValues=getInsertFieldAndValue(userName);
					insertFieldsContext.children.add(appendValues[0]);
					insertFieldsContext.children.add(appendValues[1]);
					assignValueContext.children.add(appendValues[2]);
					assignValueContext.children.add(appendValues[3]);
					
				}
				
			} else if (childContext instanceof InsertSelectClauseContext){
				InsertSelectClauseContext insertSelectContext=(InsertSelectClauseContext)childContext;
				ProjectionsContext insertProjectionsContext=null;
				for(int j=0;j<insertSelectContext.getChildCount();j++){
					ParseTree valueContext = insertSelectContext.getChild(j);
					if (valueContext instanceof FieldsContext){
						insertFieldsContext=(FieldsContext)valueContext;
					} else if (valueContext instanceof SelectContext){
						insertProjectionsContext=findFirstClassType(valueContext,ProjectionsContext.class);
						generateSelectTokens(result,userName,valueContext,sql);
					}
				}
				if (insertFieldsContext==null){
					throw new SQLParsingException("不允许插入语句不指定列:" + sql);
				} else if (insertProjectionsContext!=null){
					ParseTree[] appendValues=getInsertFieldAndSelectValue(userName);
					insertFieldsContext.children.add(appendValues[0]);
					insertFieldsContext.children.add(appendValues[1]);
					insertProjectionsContext.children.add(appendValues[2]);
					insertProjectionsContext.children.add(appendValues[3]);
				}
			}
		}
	}

	public void generateUpdateTokens(List<SQLToken> result,String userName,ParseTree rootNode,String sql) {
		WhereClauseContext whereClauseContext=findWhereContext(rootNode);
		if (whereClauseContext==null){
			throw new SQLParsingException("更新必须包含条件语句," + sql);
		}
		SetAssignmentsClauseContext setAssignContext=findSetAssignContext(rootNode);
		for(int i=0;i<setAssignContext.getChildCount();i++){
			ParseTree childNode = setAssignContext.getChild(i);
			if(childNode instanceof AssignmentContext){
				if (!checkColumnNameValid(childNode)){
					throw new SQLParsingException("更新语句不允许包含租户字段," + sql);
				}
			}
		}
		ExprContext expressContext=getConditionExpressionContext(userName,whereClauseContext.getChild(1));
		whereClauseContext.children.set(1, expressContext);
		generateSelectTokens(result,userName,whereClauseContext,sql);
	}

	public void generateDeleteTokens(List<SQLToken> result,String userName,ParseTree rootNode,String sql) {
		WhereClauseContext whereClauseContext=findWhereContext(rootNode);
		if (whereClauseContext==null){
			throw new SQLParsingException("删除必须包含条件语句," + sql);
		}
		ExprContext expressContext=getConditionExpressionContext(userName,whereClauseContext.getChild(1));
		whereClauseContext.children.set(1, expressContext);
		generateSelectTokens(result,userName,whereClauseContext,sql);
	}
}