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
		rewriter.setParseEngine(parserEngine);
		rewriter.analyseSql("select * from table1 t where name='bbb'");
		rewriter.analyseSql("select * from (select * from view1) t where name='bbb'");
		rewriter.analyseSql("select * from (select * from view1)");
		
		String result=rewriter.rewriteSql("select * from table1 where name='bbb'");
		assertTrue(("SELECT * \n" + //
						"FROM \n" + //
						"(\n" + //
						"\tSELECT * \n" + //
						"\tFROM view1\n" + //
						") table1\n" + //
						"WHERE \n" + //
						"\tname = 'bbb';").equals(result));

		result=rewriter.rewriteSql("select * from table1 t where name='bbb'");
		assertTrue(("SELECT * \n" + //
						"FROM \n" + //
						"(\n" + //
						"\tSELECT * \n" + //
						"\tFROM view1\n" + //
						") t\n" + //
						"WHERE \n" + //
						"\tname = 'bbb';").equals(result));
		
        
	}

}