package com.chinatelecom.udp.component.shardingsphere;

import org.junit.Test;

public class PrepareStatementTest {

    @Test
    public void testMySqlPreparedStatement() {
		String url = "jdbc:mysql://localhost:33307/sharding_db?useSSL=false&useServerPrepStmts=true";
		String user = "root";
		String password = "root";
		String sql = "SELECT * FROM table2 WHERE name = ? and 1=1";
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			try (java.sql.Connection conn = java.sql.DriverManager.getConnection(url, user, password);
			     java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setString(1, "bbbbb"); 
				try (java.sql.ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						System.out.println("Result: name=" + rs.getString("name"));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
    public void testPostgresPreparedStatement() {
		String url = "jdbc:postgresql://localhost:33307/sharding_db1";
		String user = "root";
		String password = "root";
		String sql = "SELECT * FROM table1 WHERE name = ? and 1=1";
		try {
			Class.forName("org.postgresql.Driver");
			try (java.sql.Connection conn = java.sql.DriverManager.getConnection(url, user, password);
			     java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setString(1, "bbbbb"); 
				try (java.sql.ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						System.out.println("Result: name=" + rs.getString("name"));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}