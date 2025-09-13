package com.chinatelecom.udp.component.shardingsphere;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.apache.shardingsphere.infra.rewrite.sql.token.common.pojo.SQLToken;
import org.apache.shardingsphere.proxy.backend.session.ConnectionSession;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.AliasContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.AssignmentContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.AssignmentValuesContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.ColumnRefContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.DeleteContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.FieldsContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.InsertContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.InsertSelectClauseContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.InsertValuesClauseContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.ProjectionsContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.SelectContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.SetAssignmentsClauseContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.TableFactorContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.TableNameContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.UpdateContext;
import org.apache.shardingsphere.sql.parser.autogen.MySQLStatementParser.WhereClauseContext;
import org.apache.shardingsphere.sql.parser.core.ParseASTNode;
import org.apache.shardingsphere.sql.parser.exception.SQLParsingException;

import com.chinatelecom.udp.component.shardingsphere.tokens.AppendTanentConditionToken;
import com.chinatelecom.udp.component.shardingsphere.tokens.AppendTanentFieldToken;
import com.chinatelecom.udp.component.shardingsphere.tokens.AppendTanentValueToken;
import com.chinatelecom.udp.component.shardingsphere.tokens.EndConditionToken;
import com.chinatelecom.udp.component.shardingsphere.tokens.ReplaceTableNameToken;

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

	/**
	 * 更新语句的列名不能有租户字段
	 * @param childNode
	 */
	private boolean checkColumnNameValid(ParseTree assignmentNode) {
		for (int i = 0; i < assignmentNode.getChildCount(); i++) {
			ParseTree childNode = assignmentNode.getChild(i);
			if (childNode instanceof ColumnRefContext){
				TerminalNodeImpl nameNode=(TerminalNodeImpl) childNode.getChild(0).getChild(0);
				if (TANENT_FIELD_ID.equalsIgnoreCase(nameNode.getText())){
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
	private WhereClauseContext findWhereContext(ParseTree rootNode) {
		for(int i=0;i<rootNode.getChildCount();i++){
			ParseTree childContext=rootNode.getChild(i);
			if(childContext instanceof WhereClauseContext){
				return (WhereClauseContext) childContext;
			}
		}
		return null;
	}
	public static String rewriteSql(ConnectionSession connectionSession,String sql){
		MySQLViewRewriter sqlWriter = getInstance();
		String userName=connectionSession.getConnectionContext().getGrantee().getUsername();
		if(userName!=null){
			List<SQLToken> tokens = sqlWriter.generateTokens("user", sql);
			if (tokens==null){
				return sql;
			}
			String newSql=sqlWriter.rewriteSql(sql, tokens);
			return newSql;
		}
		throw new SQLParsingException("异常的数据库账号信息:" + sql);
	}
	

	@Override
	public List<SQLToken> generateTokens(String userName,String sql) {
		ParseASTNode parseASTNode = null;
		try{
			parseASTNode=parserEngine.parse(sql, true);
		} catch(SQLParsingException e){
			return null;
		}
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
				if (isTableShouldRewrite(tableNameContext.getText())){
					result.add(new ReplaceTableNameToken(tableNameContext,hasAlias,userName));
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
					result.add(new AppendTanentFieldToken(insertFieldsContext.start.getStartIndex()));
					result.add(new AppendTanentValueToken(assignValueContext.start.getStopIndex()+1, userName));
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
					result.add(new AppendTanentFieldToken(insertFieldsContext.start.getStartIndex()));
					result.add(new AppendTanentValueToken(insertProjectionsContext.start.getStartIndex(), userName));
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
		result.add(new AppendTanentConditionToken(whereClauseContext.start.getStopIndex()+1, userName));
		generateSelectTokens(result,userName,whereClauseContext,sql);
		result.add(new EndConditionToken(whereClauseContext.stop.getStopIndex()+1));
	}

	public void generateDeleteTokens(List<SQLToken> result,String userName,ParseTree rootNode,String sql) {
		WhereClauseContext whereClauseContext=findWhereContext(rootNode);
		if (whereClauseContext==null){
			throw new SQLParsingException("删除必须包含条件语句," + sql);
		}
		result.add(new AppendTanentConditionToken(whereClauseContext.start.getStopIndex()+1, userName));
		generateSelectTokens(result,userName,whereClauseContext,sql);
		result.add(new EndConditionToken(whereClauseContext.stop.getStopIndex()+1));
	}
}