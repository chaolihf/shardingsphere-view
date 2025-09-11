package com.chinatelecom.udp.component.shardingsphere.tokens;

import org.apache.shardingsphere.infra.rewrite.sql.token.common.pojo.SQLToken;

import com.chinatelecom.udp.component.shardingsphere.ViewRewriter;

public class AppendTanentConditionToken extends SQLToken {

    private String userName;
    
    public AppendTanentConditionToken(int startIndex,String userName){
        super(startIndex);
        this.userName=userName;
    }

    @Override
    public int getStopIndex() {
        return -1;
    }

    @Override
    public String toString(){
        return new StringBuilder(" ").append(ViewRewriter.TANENT_FIELD_ID).append("='")
            .append(userName).append("' and (").toString();
    }

}
