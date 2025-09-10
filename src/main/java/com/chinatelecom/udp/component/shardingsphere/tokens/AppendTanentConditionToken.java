package com.chinatelecom.udp.component.shardingsphere.tokens;

import org.apache.shardingsphere.infra.rewrite.sql.token.common.pojo.SQLToken;

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
        return new StringBuilder(" tanent_id='")
            .append(userName).append("' and (").toString();
    }

}
