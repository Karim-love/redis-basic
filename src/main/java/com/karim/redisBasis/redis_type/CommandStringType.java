package com.karim.redisBasis.redis_type;

import com.karim.redisBasis.define.CommonDefine;
import com.karim.redisBasis.instance.RedisPoolInstance;
import com.karim.redisBasis.logger.SysLogger;
import com.karim.redisBasis.object.RedisPoolObject;
import com.karim.redisBasis.utils.CommonUtils;
import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by sblim
 * Date : 2021-12-27
 * Time : 오후 4:30
 * https://lettuce.io/core/release/api/io/lettuce/core/api/StatefulRedisConnection.html
 * http://redisgate.kr/redis/clients/lettuce_intro.php
 */
public class CommandStringType {

    private static Logger logger = SysLogger.getInstance().getLogger();

    // NOTE: String Set command
    public void setString(String type, String key, String value){

        RedisPoolObject redisPoolObject = RedisPoolInstance.getInstance().get(CommonDefine.REDIS_ID);

        StatefulRedisConnection<String, String> connection = null;
        StatefulRedisClusterConnection<String, String> connectionCluster = null;
        int redisTimeout = redisPoolObject.getTimeout();
        boolean singleRedis = redisPoolObject.isSingle();

        RedisAsyncCommands<String, String> asyncCommands = null;
        RedisCommands<String, String> syncCommands = null;

        RedisAdvancedClusterAsyncCommands<String, String> asyncCommandsCluster = null;
        RedisAdvancedClusterCommands<String, String> syncCommandsCluster = null;

        // async command 용
        // command 를 여러 개 몰아서 push
        List<RedisFuture<?>> futures = new ArrayList<>();

        try {
            // single 구성일 경우
            if (singleRedis){
                // sync command
                if (type.equals("sync")){
                    if (connection == null) {
                        connection = redisPoolObject.getConnectionSingleSync(redisTimeout, TimeUnit.MILLISECONDS);
                        syncCommands = connection.sync();
                    }
                    syncCommands.set(key, value);
                }else { // async command
                    if (connection == null){
                        connection = redisPoolObject.getConnectionSingleAsync(redisTimeout, TimeUnit.MILLISECONDS);
                        asyncCommands = connection.async();
                        asyncCommands.setAutoFlushCommands(false);
                    }
                    futures.add(asyncCommands.set(key, value));
                    futures.add(asyncCommands.set(key, value));

                    asyncCommands.flushCommands();

                    int futureSize = futures == null ? 0 : futures.size();
                    if (futureSize > 0) {
                        LettuceFutures.awaitAll(1000, TimeUnit.MILLISECONDS, futures.toArray(new RedisFuture[futureSize]));
                    }
                }

            }else { // cluster 구성일 경우
                // sync command
                if (type.equals("sync")){
                    if (connectionCluster == null) {
                        connectionCluster = redisPoolObject.getConnectionClusterSync(redisTimeout, TimeUnit.MILLISECONDS);
                        syncCommandsCluster = connectionCluster.sync();
                    }
                    syncCommandsCluster.set(key, value);
                }else { // async command
                    if (connectionCluster == null){
                        connectionCluster = redisPoolObject.getConnectionClusterAsync(redisTimeout, TimeUnit.MILLISECONDS);
                        asyncCommandsCluster = connectionCluster.async();
                        asyncCommandsCluster.setAutoFlushCommands(false);
                    }
                    futures.add(asyncCommandsCluster.set(key, value));
                    futures.add(asyncCommandsCluster.set(key, value));

                    asyncCommandsCluster.flushCommands();

                    int futureSize = futures == null ? 0 : futures.size();
                    if (futureSize > 0) {
                        LettuceFutures.awaitAll(1000, TimeUnit.MILLISECONDS, futures.toArray(new RedisFuture[futureSize]));
                    }
                }
            }
        }catch (Exception e){
            logger.error("Redis Exception : {}", CommonUtils.getStackTrace(e));
        }finally {
            if (connection != null) {
                redisPoolObject.returnSingleResource(true, connection);
            } else if (connectionCluster != null) {
                redisPoolObject.returnClusterResource(true, connectionCluster);
            }
        }
    }
}
