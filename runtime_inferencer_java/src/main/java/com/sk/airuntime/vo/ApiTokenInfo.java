package com.sk.airuntime.vo;

import java.util.Date;

public class ApiTokenInfo {

    private String token;
    private long tokenValidTime;

    public ApiTokenInfo(String token, long issueTime, long tokenValidMinutes) {
        this.token = token;
        this.tokenValidTime = issueTime + 60 * (tokenValidMinutes- 1);
    }

    public boolean valid(){
        long currTime = System.currentTimeMillis()/1000;
        return currTime < tokenValidTime;
    }

    public String getToken(){
        return this.token;
    }

}
