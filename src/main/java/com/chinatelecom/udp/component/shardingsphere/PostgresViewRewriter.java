package com.chinatelecom.udp.component.shardingsphere;

import java.util.List;
import java.util.logging.Logger;

import org.apache.shardingsphere.infra.rewrite.sql.token.common.pojo.SQLToken;

public class PostgresViewRewriter extends ViewRewriter{

	private static final Logger LOGGER = Logger.getLogger(PostgresViewRewriter.class.getName());

	private static PostgresViewRewriter instance;

	public static PostgresViewRewriter getInstance(){
		if(instance == null){
			synchronized (PostgresViewRewriter.class) {
				if (instance == null) {
					instance=new PostgresViewRewriter();
				}
			}
		}
		return instance;
	}

	
	public PostgresViewRewriter(){
		super("PostgreSQL");
	}


	@Override
	public List<SQLToken> generateTokens(String userName, String sql) {
		return null;
	}



}