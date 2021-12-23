package com.karim.redisBasis.instance;

import com.karim.redisBasis.object.RedisPoolObject;

import java.util.Arrays;
import java.util.Collection;
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
            /* 제일 처음에만 동기화 하도록 함 */
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

    public int activeCount(String pool) {
        int size = 0;
        RedisPoolObject poolObject = get(pool);
        if(poolObject != null) {
            size = poolObject.activeCount();
        }
        return size;
    }

    public int activeCount() {
        int size = 0;
        Collection<RedisPoolObject> collection = this.values();
        if(collection != null) {
            for (RedisPoolObject parentRedisPoolObject : collection) {
                size += parentRedisPoolObject.activeCount();
            }
        }
        return size;
    }

    public int activeCountSync(String pool) {
        int size = 0;
        RedisPoolObject poolObject = get(pool);
        if(poolObject != null) {
            size = poolObject.activeCountSync();
        }
        return size;
    }

    public int activeCountSync() {
        int size = 0;
        Collection<RedisPoolObject> collection = this.values();
        if(collection != null) {
            for (RedisPoolObject parentRedisPoolObject : collection) {
                size += parentRedisPoolObject.activeCountSync();
            }
        }
        return size;
    }

    public int activeCountASync(String pool) {
        int size = 0;
        RedisPoolObject poolObject = get(pool);
        if(poolObject != null) {
            size = poolObject.activeCountASync();
        }
        return size;
    }

    public int activeCountASync() {
        int size = 0;
        Collection<RedisPoolObject> collection = this.values();
        if(collection != null) {
            for (RedisPoolObject parentRedisPoolObject : collection) {
                size += parentRedisPoolObject.activeCountASync();
            }
        }
        return size;
    }

    public static void main(String[] args) {
        RedisPoolInstance.getInstance().add("test", true, null, 3000, 5, Arrays.asList("192.168.124.236:19050"));
        System.out.println();
    }
}
