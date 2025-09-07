/**
 * 
 */
package com.chinatelecom.udp.component.shardingsphere;

import static org.junit.Assert.assertTrue;

import org.apache.shardingsphere.infra.database.core.type.DatabaseType;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.sql.parser.api.CacheOption;
import org.apache.shardingsphere.sql.parser.api.SQLParserEngine;
import org.apache.shardingsphere.sql.parser.exception.SQLParsingException;
import org.junit.Test;

/**
 * @author lichao
 *
 */
public class ShardingsphereSqlViewRewriteTest {

	@Test
	public void testModifyTable() {
		String result;
		DatabaseType databaseType = TypedSPILoader.getService(DatabaseType.class, "MySQL");
		SQLParserEngine parserEngine = new SQLParserEngine(databaseType, new CacheOption(2000, 65535L));
		SQLViewRewrite rewriter=new SQLViewRewrite();
		rewriter.setParseEngine(parserEngine);
		
		rewriter.rewriteSql("insert into table1(name) select name from table2");

		rewriter.analyseSql("select * from table1 t where name='bbb'");



		//rewriter.analyseSql("select * from (select * from view1) t where name='bbb'");
		//rewriter.analyseSql("select * from (select * from view1)");
		// 
		// result=rewriter.rewriteSql("select * from table1 where name='bbb'");
		// assertTrue(("SELECT * \n" + //
		// 				"FROM \n" + //
		// 				"(\n" + //
		// 				"\tSELECT * \n" + //
		// 				"\tFROM view1\n" + //
		// 				"\tWHERE \n" + //
		// 				"\t\ttanentId = 'ddd'\n" + //
		// 				") table1\n" + //
		// 				"WHERE \n" + //
		// 				"\tname = 'bbb';").equals(result));

		// result=rewriter.rewriteSql("select * from table1 t where name='bbb'");
		// assertTrue(("SELECT * \n" + //
		// 				"FROM \n" + //
		// 				"(\n" + //
		// 				"\tSELECT * \n" + //
		// 				"\tFROM view1\n" + //
		// 				"\tWHERE \n" + //
		// 				"\t\ttanentId = 'ddd'\n" + //
		// 				") t\n" + //
		// 				"WHERE \n" + //
		// 				"\tname = 'bbb';").equals(result));
		
		// rewriter.analyseSql("insert into table1 values('a','v')");
		// try{
		// 	rewriter.rewriteSql("insert into table1 values('a','v')");
		// 	assertTrue("应抛出语句异常错误", false);
		// } catch (SQLParsingException e){
		// 	assertTrue(e.getMessage().indexOf("不允许插入语句不指定列")!=-1);
		// }
		// rewriter.analyseSql("insert into table1(name) values('a')");
		// rewriter.analyseSql("insert into table1(name,id) values('a','b')");
		// result= rewriter.rewriteSql("insert into table1(name) values('a')");
		// assertTrue(("INSERT  INTO table1 (name , tanentId)\n" + //
		// 				"VALUES\n" + //
		// 				"\t('a', 'userId')").equals(result));
		// //实际执行是会报错，提示存在多个列
		// rewriter.rewriteSql("insert into table1(name,tanentId) values('a',?)");

		// rewriter.analyseSql("delete from table1 where name='b'");
		// rewriter.analyseSql("delete from table1 where  tanent_id='user' and (name='b')");


		// rewriter.analyseSql("delete from table1 where name='b' or name='c'");
		// rewriter.analyseSql("delete from table1 where tanent_id='user' and (name='b' or name='c')");

		// result=rewriter.rewriteSql("delete from table1 where name='b'");
		// assertTrue(("DELETE  FROM table1 WHERE \n" + //
		// 				"\ttanent_id = 'user'\n" + //
		// 				"\tand (name = 'b')").equals(result));
		// result=rewriter.rewriteSql("delete from table1 where name='b' or name='c'");
		// assertTrue(("DELETE  FROM table1 WHERE \n" + //
		// 				"\ttanent_id = 'user'\n" + //
		// 				"\tand (name = 'b'\n" + //
		// 				"\tor name = 'c')").equals(result));


		// rewriter.analyseSql("update table1 set name='b',name='c' where name='b'");
		// rewriter.analyseSql("update table1 set name='b' ,name='c' where tanent_id='user' and (name='b')");
		
		// result=rewriter.rewriteSql("update table1 set name='b',name='c' where name='b'");

		// assertTrue(("UPDATE  table1 SET name = 'b',\n" + //
		// 				"\tname = 'c' WHERE \n" + //
		// 				"\ttanent_id = 'user'\n" + //
		// 				"\tand (name = 'b')").equals(result));

		
	}

}