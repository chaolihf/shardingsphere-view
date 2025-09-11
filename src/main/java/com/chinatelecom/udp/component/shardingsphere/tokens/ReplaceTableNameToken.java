package com.chinatelecom.udp.component.shardingsphere.tokens;

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.shardingsphere.infra.rewrite.sql.token.common.pojo.SQLToken;
import org.apache.shardingsphere.infra.rewrite.sql.token.common.pojo.Substitutable;

import com.chinatelecom.udp.component.shardingsphere.ViewRewriter;

public class ReplaceTableNameToken extends SQLToken implements Substitutable{

    private int stopIndex;
    private boolean hasAlias;
    private String tableName;
    private String userName;
    
    public ReplaceTableNameToken(ParserRuleContext tableNameContext,boolean hasAlias,String userName) {
        super(tableNameContext.start.getStartIndex());
        this.stopIndex=tableNameContext.start.getStopIndex();
        this.hasAlias=hasAlias;
        this.tableName=tableNameContext.getText();
        this.userName=userName;
    }
    
    @Override
    public int getStopIndex() {
        return stopIndex;
    }

    @Override
    public String toString() {
        StringBuilder sql=new StringBuilder(50);
        sql.append("(select * from ")
            .append(tableName)
            .append(" where ")
            .append(ViewRewriter.TANENT_FIELD_ID)
            .append("='")
            .append(userName)
            .append("')");
        
        if(!hasAlias){
            sql.append(" ").append(tableName);
        }
        return sql.toString();
    }

}
