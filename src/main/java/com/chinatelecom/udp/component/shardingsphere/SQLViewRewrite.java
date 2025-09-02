package com.chinatelecom.udp.component.shardingsphere;

import java.util.Arrays;
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
import org.apache.shardingsphere.sql.parser.core.ParseASTNode;
import org.apache.shardingsphere.sql.parser.statement.core.statement.SQLStatement;

public class SQLViewRewrite {
	
	private void printSql(ParseTree rootNode) {
        Properties props = new Properties();
        props.setProperty("parameterized", "false");
        SQLFormatVisitor formatVisitor = DatabaseTypedSPILoader.getService(
            SQLFormatVisitor.class, TypedSPILoader.getService(DatabaseType.class, "MySQL"), props);
        String formattedSQL = formatVisitor.visit(rootNode);
        System.out.println(formattedSQL);
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

	void analyseSql(SQLParserEngine parserEngine,String sql) {
		ParseASTNode parseASTNode = parserEngine.parse(sql, false);
		ParseTree rootNode = parseASTNode.getRootNode();
		printStructure(rootNode,0);
		printSql(rootNode);
	}
	
	

}