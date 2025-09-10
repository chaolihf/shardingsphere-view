package com.chinatelecom.udp.component.shardingsphere.tokens;

import org.apache.shardingsphere.infra.rewrite.sql.token.common.pojo.SQLToken;

public class EndConditionToken extends SQLToken {
    
    public EndConditionToken(int startIndex){
        super(startIndex);
    }

    @Override
    public int getStopIndex() {
        return -1;
    }

    @Override
    public String toString(){
        return ")";
    }

}
