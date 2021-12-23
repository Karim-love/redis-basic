package com.karim.redisBasis;

import com.karim.redisBasis.cfg.PoolConfigurations;
import com.karim.redisBasis.cfg.StaticProperties;

/**
 * Created by sblim
 * Date : 2021-12-23
 * Time : 오후 12:03
 */
public class MainProcess {
    public static void main(String[] args) {

        // redis 접속 정보 pool load
        StaticProperties.getInstance();
        PoolConfigurations.getInstance();
    }
}
