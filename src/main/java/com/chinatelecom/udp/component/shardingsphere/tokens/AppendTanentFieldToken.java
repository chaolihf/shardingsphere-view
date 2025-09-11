package com.chinatelecom.udp.component.shardingsphere.tokens;

import org.apache.shardingsphere.infra.rewrite.sql.token.common.pojo.SQLToken;

import com.chinatelecom.udp.component.shardingsphere.ViewRewriter;

public class AppendTanentFieldToken extends SQLToken {
    
    public AppendTanentFieldToken(int startIndex){
        super(startIndex);
    }

    @Override
    public int getStopIndex() {
        return -1;
    }

    @Override
    public String toString(){
        return ViewRewriter.TANENT_FIELD_ID + ",";
    }

}
