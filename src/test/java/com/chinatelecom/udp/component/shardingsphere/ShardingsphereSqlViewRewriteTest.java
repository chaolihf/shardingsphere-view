/**
 * 
 */
package com.chinatelecom.udp.component.shardingsphere;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.shardingsphere.infra.rewrite.sql.token.common.pojo.SQLToken;
import org.apache.shardingsphere.sql.parser.exception.SQLParsingException;
import org.junit.Test;

/**
 * @author lichao
 *
 */
public class ShardingsphereSqlViewRewriteTest {

	private void validMySql(MySQLViewRewriter rewriter,String sql,String targetSql){
		rewriter.analyseSql(sql);
		List<SQLToken> tokens = rewriter.generateTokens("user", sql);
		String result=rewriter.rewriteSql(sql, tokens);
		System.out.println(result);
		assertTrue(result.equals(targetSql));
	}

	@Test
	public void testMySqlRewriter() {
		String result;
		MySQLViewRewriter rewriter=new MySQLViewRewriter();
		rewriter.addRewriteTable("table1");
		rewriter.addRewriteTable("table2");

		rewriter.analyseSql("select * from (select * from view1) t where name='bbb'");
		rewriter.analyseSql("select * from (select * from view1)");
		
		validMySql(rewriter,"update table1 set name='b',name='c' where name='b' and name in (select name from table2) and name='c'",
			"update table1 set name='b',name='c' where tanent_id='user' and ( name='b' and name in (select name from (select * from table2 where tanent_id='user') table2) and name='c')"
		);

		validMySql(rewriter,"select * from table1 where name='bbb'",
						"select * from (select * from table1 where tanent_id='user') table1 where name='bbb'");

		rewriter.analyseSql("select * from (select * from view1) t where name='bbb'");
		rewriter.analyseSql("select * from (select * from view1)");
		
		rewriter.analyseSql("insert into table1 values('a','v')");
		try{
			rewriter.rewriteSql("user","insert into table1 values('a','v')");
			assertTrue("应抛出语句异常错误", false);
		} catch (SQLParsingException e){
			assertTrue(e.getMessage().indexOf("不允许插入语句不指定列")!=-1);
		}
		rewriter.analyseSql("insert into table1(name) values('a')");
		rewriter.analyseSql("insert into table1(name,id) values('a','b')");
		result= rewriter.rewriteSql("user","insert into table1(name) values('a')");
		assertTrue(("INSERT  INTO table1 (name , tanentId)\n" + //
						"VALUES\n" + //
						"\t('a', 'user')").equals(result));
		//实际执行是会报错，提示存在多个列
		rewriter.rewriteSql("user","insert into table1(name,tanentId) values('a',?)");

		rewriter.analyseSql("delete from table1 where name='b'");
		rewriter.analyseSql("delete from table1 where  tanent_id='user' and (name='b')");


		rewriter.analyseSql("delete from table1 where name='b' or name='c'");
		rewriter.analyseSql("delete from table1 where tanent_id='user' and (name='b' or name='c')");

		result=rewriter.rewriteSql("user","delete from table1 where name='b'");
		assertTrue(("DELETE  FROM table1 WHERE \n" + //
						"\ttanent_id = 'user'\n" + //
						"\tand (name = 'b')").equals(result));
		result=rewriter.rewriteSql("user","delete from table1 where name='b' or name='c'");
		assertTrue(("DELETE  FROM table1 WHERE \n" + //
						"\ttanent_id = 'user'\n" + //
						"\tand (name = 'b'\n" + //
						"\tor name = 'c')").equals(result));


		rewriter.analyseSql("update table1 set name='b',name='c' where name='b'");
		rewriter.analyseSql("update table1 set name='b' ,name='c' where tanent_id='user' and (name='b')");
		
		result=rewriter.rewriteSql("user","update table1 set name='b',name='c' where name='b'");

		assertTrue(("UPDATE  table1 SET name = 'b',\n" + //
						"\tname = 'c' WHERE \n" + //
						"\ttanent_id = 'user'\n" + //
						"\tand (name = 'b')").equals(result));

		rewriter.analyseSql("insert into table1(name) select name from table2");
		rewriter.analyseSql("insert into table1(name,tanent_id) select name,'user' from table2");
		result=rewriter.rewriteSql("user","insert into table1(name) select name from table2");
		assertTrue(("INSERT  INTO table1 (name , tanent_id) \n" + //
						"SELECT name , 'user' \n" + //
						"FROM \n" + //
						"(\n" + //
						"\tSELECT * \n" + //
						"\tFROM view1\n" + //
						"\tWHERE \n" + //
						"\t\ttanentId = 'user'\n" + //
						") table2;").equals(result));
		
	}

	@Test
	public void testPostgresRewriter() {
		String result;
		PostgresViewRewriter rewriter=new PostgresViewRewriter();
		rewriter.analyseSql("update table1 set name=min(a,1),name='c dd' where name='b' and name in (select name from table2)");
		
		
	}

}