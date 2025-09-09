package com.chinatelecom.udp.component.shardingsphere;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.apache.shardingsphere.infra.database.core.spi.DatabaseTypedSPILoader;
import org.apache.shardingsphere.infra.database.core.type.DatabaseType;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.mode.manager.ContextManager;
import org.apache.shardingsphere.proxy.backend.context.ProxyContext;
import org.apache.shardingsphere.sql.parser.api.SQLParserEngine;
import org.apache.shardingsphere.sql.parser.api.visitor.format.SQLFormatVisitor;
import org.apache.shardingsphere.sql.parser.core.ParseASTNode;

public class ViewRewriter {
	private static final Logger LOGGER = Logger.getLogger(ViewRewriter.class.getName());
    private Map<String,String> rewriteTables=new ConcurrentHashMap<>();
	private String databaseType;
	protected SQLParserEngine parserEngine;

	public ViewRewriter(){
		try {
			Class<?> clazz = ProxyContext.class;
			ProxyContext instance = ProxyContext.getInstance();
			Field field = clazz.getDeclaredField("contextManager");
			field.setAccessible(true);
			ContextManager contextManager = (ContextManager) field.get(instance);
			String tables=contextManager.getMetaDataContexts().getMetaData().getProps().getProps().getProperty("replaceTables");
			if(tables==null || tables.length()==0){
				throw new IllegalArgumentException("未在global.yaml中找到replaceTables配置");
			}
			String[] allTables=tables.split(",");
			for (String tableName : allTables) {
				rewriteTables.put(tableName, tableName);
			}
		} catch (IllegalAccessException | IllegalArgumentException
							| SecurityException
							| NoSuchFieldException e) {
			LOGGER.log(Level.SEVERE,e.getMessage());
		}
	}

	
	public ViewRewriter(SQLParserEngine parserEngine,String databaseType){
		this();
		this.parserEngine=parserEngine;
		this.databaseType=databaseType;
	}

	public void setParseEngine(SQLParserEngine parserEngine){
		this.parserEngine=parserEngine;
	}

    public String printSql(ParseTree rootNode) {
		Properties props = new Properties();
		props.setProperty("parameterized", "false");
		SQLFormatVisitor formatVisitor = DatabaseTypedSPILoader.getService(
			SQLFormatVisitor.class, TypedSPILoader.getService(DatabaseType.class, databaseType), props);
		String formattedSQL = formatVisitor.visit(rootNode);
		System.out.println(formattedSQL);
		return formattedSQL;
	}

	
	protected boolean isTableShouldRewrite(String tableName){
		return rewriteTables.containsKey(tableName);
	}

	
	protected static String generateChars(int count) {
		if(count<=0) return "";
		char[] chars = new char[count];
		Arrays.fill(chars, '-');
		return new String(chars);
	}

	
	protected void printStructure(ParseTree rootNode,int level) {
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

	protected void analyseSql(String sql) {
		ParseASTNode parseASTNode = parserEngine.parse(sql, false);
		ParseTree rootNode = parseASTNode.getRootNode();
		printStructure(rootNode,0);
		printSql(rootNode);
	}


	
	@SuppressWarnings("unchecked")
	protected <T> T findFirstClassType(ParseTree rootNode,Class<T> t){
		int childCount=rootNode.getChildCount();
		if (t.isInstance(rootNode)) {
			return (T)rootNode;
		}
		for(int i=0;i<childCount;i++) {
			ParseTree child = rootNode.getChild(i);
			if (child!=null) {
				T subQueryResult = findFirstClassType(child,t);
				if(subQueryResult!=null){
					return subQueryResult;
				}
			}
		}
		return null;
	}


}
