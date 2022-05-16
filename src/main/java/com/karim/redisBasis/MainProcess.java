package com.karim.redisBasis;

import com.karim.redisBasis.cfg.PoolConfigurations;
import com.karim.redisBasis.cfg.Properties;
import com.karim.redisBasis.command.CommandStringType;

/**
 * Created by sblim
 * Date : 2021-12-23
 * Time : 오후 12:03
 */
public class MainProcess {

    private static final String REDIS_TYPE = "string";

    public static void main(String[] args) {

        // redis 접속 정보 pool load
        Properties.getInstance();
        PoolConfigurations.getInstance();

        if (REDIS_TYPE.equals("string")){
            CommandStringType commandStringType = new CommandStringType();
            commandStringType.setString("sync", "lim", "subin");
        }
    }
}
