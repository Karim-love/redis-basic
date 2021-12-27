package com.karim.redisBasis.instance;

import com.karim.redisBasis.object.RedisPoolObject;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by sblim
 * Date : 2021-12-23
 * Time : 오후 5:24
 */
public class RedisPoolInstance extends ConcurrentHashMap<String, RedisPoolObject> {

    private static volatile RedisPoolInstance _instance = null;
    public static RedisPoolInstance getInstance() {
        if( _instance == null ) {
            synchronized(RedisPoolInstance.class) {
                if( _instance == null ) {
                    _instance = new RedisPoolInstance();
                }
            }
        }
        return _instance;
    }

    public synchronized boolean add(String id, boolean isSingle, String auth, int timeout, int workers, List<String> redisURIList) {
        if(!this.containsKey(id)) {
            RedisPoolObject RedisPoolObject = new RedisPoolObject(id, isSingle, auth, timeout, workers, redisURIList);
            this.put(id, RedisPoolObject);
            return true;
        }
        return false;
    }
}
