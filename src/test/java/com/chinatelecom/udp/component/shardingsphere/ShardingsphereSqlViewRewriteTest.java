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
		MySQLViewRewriter rewriter=new MySQLViewRewriter();
		rewriter.addRewriteTable("table1");
		rewriter.addRewriteTable("table2");

		rewriter.analyseSql("insert into table1(name) select name from table2");
		rewriter.analyseSql("insert into table1(name,tanent_id) select name,'user' from table2");
		validMySql(rewriter,"insert into table1(name) select name from table2",
			"insert into table1(tanent_id,name) select 'user',name from (select * from table2 where tanent_id='user') table2");
		
		try{
			validMySql(rewriter,"insert into table1 values('a','v')","");
			assertTrue("应抛出语句异常错误", false);
		} catch (SQLParsingException e){
			assertTrue(e.getMessage().indexOf("不允许插入语句不指定列")!=-1);
		}

		rewriter.analyseSql("insert into table1(name) values('a')");
		rewriter.analyseSql("insert into table1(name,id) values('a','b')");
		validMySql(rewriter,"insert into table1(name) values('a')",
			"insert into table1(tanent_id,name) values('user','a')");
			
		//实际执行是会报错，提示存在多个列
		validMySql(rewriter,"insert into table1(name,tanent_id) values('a',?)",
			"insert into table1(tanent_id,name,tanent_id) values('user','a',?)");


		rewriter.analyseSql("delete from table1 where name='b'");
		rewriter.analyseSql("delete from table1 where  tanent_id='user' and (name='b')");
		rewriter.analyseSql("delete from table1 where name='b' or name='c'");
		rewriter.analyseSql("delete from table1 where tanent_id='user' and (name='b' or name='c')");

		validMySql(rewriter,"delete from table1 where name='b'",
			"delete from table1 where tanent_id='user' and ( name='b')");

		validMySql(rewriter,"delete from table1 where name='b' or name='c'",
			"delete from table1 where tanent_id='user' and ( name='b' or name='c')");



		rewriter.analyseSql("select * from (select * from view1) t where name='bbb'");
		rewriter.analyseSql("select * from (select * from view1)");
		
		validMySql(rewriter,"update table1 set name='b',name='c' where name='b' and name in (select name from table2) and name='c'",
			"update table1 set name='b',name='c' where tanent_id='user' and ( name='b' and name in (select name from (select * from table2 where tanent_id='user') table2) and name='c')"
		);

		validMySql(rewriter,"select * from table1 where name='bbb'",
						"select * from (select * from table1 where tanent_id='user') table1 where name='bbb'");

		rewriter.analyseSql("select * from (select * from view1) t where name='bbb'");
		rewriter.analyseSql("select * from (select * from view1)");
		
		
		
		
		rewriter.analyseSql("update table1 set name='b',name='c' where name='b'");
		rewriter.analyseSql("update table1 set name='b' ,name='c' where tanent_id='user' and (name='b')");
		
		validMySql(rewriter,"update table1 set name='b',name='c' where name='b'",
			"update table1 set name='b',name='c' where tanent_id='user' and ( name='b')");

	}

	@Test
	public void testPostgresRewriter() {
		String result;
		PostgresViewRewriter rewriter=new PostgresViewRewriter();
		rewriter.analyseSql("update table1 set name=min(a,1),name='c dd' where name='b' and name in (select name from table2)");
		
		
	}

}