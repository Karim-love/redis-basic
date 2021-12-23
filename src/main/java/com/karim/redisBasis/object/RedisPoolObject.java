package com.karim.redisBasis.object;

import ch.qos.logback.classic.Logger;
import com.karim.redisBasis.utils.CommonUtils;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by sblim
 * Date : 2021-12-23
 * Time : 오후 5:24
 */
public class RedisPoolObject {

    private Logger logger = (Logger) LoggerFactory.getLogger(RedisPoolObject.class);
    private String instanceName;
    private String id;
    private String auth;
    private boolean isSingle;
    private boolean isSentinel;

    private int workers;
    //private StatefulRedisConnection<String, String>[] singleConnectionArray;
    //private StatefulRedisConnection<String, String>[] singleAsyncConnectionArray;

    private StatefulRedisPubSubConnection<String, String> singlePubSubConnection;

    private BlockingQueue<StatefulRedisConnection<String, String>> singleSyncConnectionPool;
    private BlockingQueue<StatefulRedisConnection<String, String>> singleASyncConnectionPool;

    private BlockingQueue<StatefulRedisClusterConnection<String, String>> clusterSyncConnectionPool;
    private BlockingQueue<StatefulRedisClusterConnection<String, String>> clusterASyncConnectionPool;

    //private StatefulRedisClusterConnection<String, String>[] clusterSyncConnectionArray;
    //private StatefulRedisClusterConnection<String, String>[] clusterAsyncConnectionArray;


    private String lineBreaker = System.getProperty("line.separator") == null ? "\n" : System.getProperty("line.separator");
    private boolean isInit = false;
    private int timeout;

    public RedisPoolObject(String id,
                             boolean isSingle,
                             String auth,
                             int timeout,
                             int workers,
                             List<String> redisURIList) {

        int cores = Runtime.getRuntime().availableProcessors() * 2;
        if(workers == 0) {
            workers = Math.max(cores, 1);
        }
        this.id = id;
        this.instanceName = "redis-pool-" + id;
        this.isSingle = isSingle;
        this.workers = workers;
        this.timeout = timeout;
        this.auth = auth;

        if(this.isSentinel) {

        } else {
            if (this.isSingle) {
                initSingle(redisURIList, workers, timeout, auth);
            } else {
                initCluster(redisURIList, workers, timeout, auth);
            }
        }
    }

    private void initSingle(List<String> redisURIList, int workers, int timeout, String auth) {

        int listSize = redisURIList == null ? 0 : redisURIList.size();

        if(listSize > 0) {
            String uri = redisURIList.get(0);
            if(!uri.startsWith("redis://")) {
                uri = "redis://" + uri;
            }

            Timer timer = new HashedWheelTimer();

            try {
                RedisURI redisURI = RedisURI.create(uri);
                if(timeout > 0) {
                    redisURI.setTimeout(Duration.ofMillis(timeout));
                }
                if (StringUtils.isNotEmpty(auth)) {
                    redisURI.setPassword(auth);
                }
                redisURI.setDatabase(0);

                if(singleSyncConnectionPool == null) {
                    singleSyncConnectionPool = new LinkedBlockingQueue<>();
                }
                if(singleASyncConnectionPool == null) {
                    singleASyncConnectionPool = new LinkedBlockingQueue<>();
                }

                int syncPingOkCount = 0;
                int asyncPingOkCount = 0;
                for(int i=0; i<workers; i++) {

                    ClientResources clientResources = DefaultClientResources.builder()
                            .ioThreadPoolSize(3)
                            .computationThreadPoolSize(3)
                            .timer(timer)
                            .build();

                    RedisClient redisClientSync = RedisClient.create(clientResources, redisURI);
                    RedisClient redisClientASync = RedisClient.create(clientResources, redisURI);

                    try {
                        singlePubSubConnection = redisClientSync.connectPubSub();
                    } catch (Exception e) {
                        logger.error("redis connect exception : {} => {}", e.toString(), e.getCause().getMessage());
                    }

                    StatefulRedisConnection<String, String> connectionSync = null;
                    try {
                        connectionSync = redisClientSync.connect();
                    } catch (Exception e) {
                        logger.error("redis connect exception : {} => {}", e.toString(), e.getCause().getMessage());
                    }
                    StatefulRedisConnection<String, String> connectionASync = null;
                    try {
                        connectionASync = redisClientASync.connect();
                    } catch (Exception e) {
                        logger.error("redis connect exception : {} => {}", e.toString(), e.getCause().getMessage());
                    }

                    try {
                        connectionSync.sync().ping();
                        syncPingOkCount++;
                    } catch (Exception e) {}

                    try {
                        connectionASync.async().ping();
                        asyncPingOkCount++;
                    } catch (Exception e) {}

                    if(connectionSync != null) {
                        singleSyncConnectionPool.offer(connectionSync);
                    }
                    if(connectionASync != null) {
                        singleASyncConnectionPool.offer(connectionASync);
                    }
                }

                if(testActionSingle()) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(lineBreaker);
                    stringBuilder.append("================ [").append(instanceName).append("] ================").append(lineBreaker);
                    stringBuilder.append(" 1. Redis Type            : single").append(lineBreaker);
                    stringBuilder.append(" 2. Redis Host            : ").append(redisURI.getHost()).append(lineBreaker);
                    stringBuilder.append(" 3. Redis Port            : ").append(redisURI.getPort()).append(lineBreaker);
                    stringBuilder.append(" 4. Redis Timeout(ms)     : ").append(redisURI.getTimeout().toMillis()).append(" ms").append(lineBreaker);
                    stringBuilder.append(" 5. Redis Auth            : ").append(auth).append(lineBreaker);
                    stringBuilder.append(" 6. Redis ConnectionCount : ").append(workers).append(lineBreaker);
                    stringBuilder.append(" 7. Redis Checker         :").append(lineBreaker);
                    stringBuilder.append("   -  sync [").append(syncPingOkCount).append("/").append(workers).append("]").append(lineBreaker);
                    stringBuilder.append("   - async [").append(asyncPingOkCount).append("/").append(workers).append("]").append(lineBreaker);
                    stringBuilder.append("============================================================");
                    stringBuilder.append(lineBreaker);
                    logger.info("{} => POOL SETTINGS INFO {}", instanceName, stringBuilder.toString());
                }

            } catch (Exception e) {
                logger.error("{} => Exception : {}", instanceName, CommonUtils.getStackTrace(e));
            }
        }
    }


    private void initCluster(List<String> redisURIList, int workers, int timeout, String auth) {

        int listSize = redisURIList == null ? 0 : redisURIList.size();

        if(listSize > 0) {

            Timer timer = new HashedWheelTimer();

            List<RedisURI> redisClusterURIList = new ArrayList<>();
            for(int i=0; i<listSize; i++) {
                String uri = redisURIList.get(i);
                if(!uri.startsWith("redis://")) {
                    uri = "redis://" + uri;
                }

                RedisURI redisURI = RedisURI.create(uri);
                if(timeout > 0) {
                    redisURI.setTimeout(Duration.ofMillis(timeout));
                }
                if (StringUtils.isNotEmpty(auth)) {
                    redisURI.setPassword(auth);
                }
                redisURI.setDatabase(0);
                redisClusterURIList.add(redisURI);
            }

            try {

                if(clusterSyncConnectionPool == null) {
                    clusterSyncConnectionPool = new LinkedBlockingQueue<>();
                }
                if(clusterASyncConnectionPool == null) {
                    clusterASyncConnectionPool = new LinkedBlockingQueue<>();
                }
                int syncPingOkCount = 0;
                int asyncPingOkCount = 0;

                for(int i=0; i<workers; i++) {

                    ClientResources clientResources = DefaultClientResources.builder()
                            .ioThreadPoolSize(3)
                            .computationThreadPoolSize(3)
                            .timer(timer)
                            .build();

                    RedisClusterClient redisClientClusterSync = RedisClusterClient.create(clientResources, redisClusterURIList);
                    RedisClusterClient redisClientClusterASync = RedisClusterClient.create(clientResources, redisClusterURIList);

                    StatefulRedisClusterConnection<String, String> connectionSync = null;
                    try {
                        connectionSync = redisClientClusterSync.connect();
                    } catch (Exception e) {
                        logger.error("redis connect exception : {} => {}", e.toString(), e.getCause().getMessage());
                    }
                    StatefulRedisClusterConnection<String, String> connectionASync = null;
                    try {
                        connectionASync = redisClientClusterASync.connect();
                    } catch (Exception e) {
                        logger.error("redis connect exception : {} => {}", e.toString(), e.getCause().getMessage());
                    }

                    try {
                        connectionSync.sync().ping();
                        syncPingOkCount++;
                    } catch (Exception e) {}

                    try {
                        connectionASync.async().ping();
                        asyncPingOkCount++;
                    } catch (Exception e) {}

                    if(connectionSync != null) {
                        clusterSyncConnectionPool.offer(connectionSync);
                    }
                    if(connectionASync != null) {
                        clusterASyncConnectionPool.offer(connectionASync);
                    }
                }

                if(testActionCluster()) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(lineBreaker);
                    stringBuilder.append("================ [").append(instanceName).append("] ================").append(lineBreaker);
                    stringBuilder.append(" 1. Redis Type            : cluster").append(lineBreaker);
                    stringBuilder.append(" 2. Redis Cluster Nodes   : ").append(lineBreaker);
                    for(RedisURI redisURI : redisClusterURIList) {
                        stringBuilder.append("   - ").append(redisURI).append(lineBreaker);
                    }
                    stringBuilder.append(" 3. Redis ConnectionCount : ").append(workers).append(lineBreaker);
                    stringBuilder.append(" 4. Redis Checker         :").append(lineBreaker);
                    stringBuilder.append("   -  sync [").append(syncPingOkCount).append("/").append(workers).append("]").append(lineBreaker);
                    stringBuilder.append("   - async [").append(asyncPingOkCount).append("/").append(workers).append("]").append(lineBreaker);
                    stringBuilder.append("============================================================");
                    stringBuilder.append(lineBreaker);
                    logger.info("{} => POOL SETTINGS INFO {}", instanceName, stringBuilder.toString());
                }

            } catch (Exception e) {
                logger.error("{} => Exception : {}", instanceName, CommonUtils.getStackTrace(e));
            }
        }
    }
    
    private boolean testActionSingle() {

        StatefulRedisConnection<String, String> connection = null;
        boolean result = false;
        try {
            connection = singleSyncConnectionPool.poll();
            logger.info("{} => * TEST POOL ATTEMPT !! ", instanceName);
            logger.info("{} =>  - Server \n{}", instanceName, connection.sync().info("Server"));
            logger.info("{} =>  - Memory \n{}", instanceName, connection.sync().info("Memory"));
            logger.info("{} =>  - Space  \n{}", instanceName, connection.sync().info("Keyspace"));
            result = true;
            this.isInit = true;
        } catch(Exception e) {
            logger.error("{} => redis exception : {}", instanceName, CommonUtils.getStackTrace(e));
        } finally {
            if(connection != null) {
                this.singleSyncConnectionPool.offer(connection);
            }
        }
        if(!result) {
            logger.info("{} => Redis Connection Pool Fail, Verify Redis Server....", instanceName);
        } else {
            logger.info("{} => Redis Connection Pool Success Loading", instanceName);
        }
        return result;
    }

    private boolean testActionCluster() {

        StatefulRedisClusterConnection<String, String> connection = null;
        boolean result = false;
        try {
            connection = clusterSyncConnectionPool.poll();

            logger.info("{} => * TEST POOL ATTEMPT !! ", instanceName);
            logger.info("{} =>  - Info  \n{}", instanceName, connection.sync().clusterInfo());
            logger.info("{} =>  - Nodes \n{}", instanceName, connection.sync().clusterNodes());

            result = true;
            this.isInit = true;

        } catch(Exception e) {
            logger.error("{} => redis exception : {}", instanceName, CommonUtils.getStackTrace(e));
        } finally {
            if(connection != null) {
                this.clusterSyncConnectionPool.offer(connection);
            }
        }
        if(!result) {
            logger.info("{} => Redis Connection Pool Fail, Verify Redis Server....", instanceName);
        } else {
            logger.info("{} => Redis Connection Pool Success Loading", instanceName);
        }
        return result;
    }

    private int randomRange(int n1, int n2) {
        return n1==n2 ? n1 : (int) (Math.random() * (n2 - n1 + 1)) + n1;
    }


    public StatefulRedisConnection<String, String> getConnectionSingleSync() {
        if(singleSyncConnectionPool == null) {
            return null;
        }
        return this.singleSyncConnectionPool.poll();
    }
    public StatefulRedisConnection<String, String> getConnectionSingleSync(int timeout, TimeUnit timeUnit) {
        if(singleSyncConnectionPool == null) {
            return null;
        }
        try {
            return this.singleSyncConnectionPool.poll(timeout, timeUnit);
        } catch (InterruptedException e) {
            logger.error("{} => {}", instanceName, CommonUtils.getStackTrace(e));
        }
        return null;
    }
    public StatefulRedisConnection<String, String> getConnectionSingleAsync() {
        if(singleASyncConnectionPool == null) {
            return null;
        }
        return this.singleASyncConnectionPool.poll();
    }
    public StatefulRedisConnection<String, String> getConnectionSingleAsync(int timeout, TimeUnit timeUnit) {
        if(singleASyncConnectionPool == null) {
            return null;
        }
        try {
            return this.singleASyncConnectionPool.poll(timeout, timeUnit);
        } catch (InterruptedException e) {
            logger.error("{} => {}", instanceName, CommonUtils.getStackTrace(e));
        }
        return null;
    }
    public void returnSingleResource(boolean isSync, StatefulRedisConnection<String, String> connection) {
        if(connection == null) {
            return;
        }
        if(isSync) {
            this.singleSyncConnectionPool.offer(connection);
        } else {
            this.singleASyncConnectionPool.offer(connection);
        }
    }


    public StatefulRedisClusterConnection<String, String> getConnectionClusterSync() {
        if(clusterSyncConnectionPool == null) {
            return null;
        }
        return this.clusterSyncConnectionPool.poll();
    }
    public StatefulRedisClusterConnection<String, String> getConnectionClusterSync(int timeout, TimeUnit timeUnit) {
        if(clusterSyncConnectionPool == null) {
            return null;
        }
        try {
            return this.clusterSyncConnectionPool.poll(timeout, timeUnit);
        } catch (InterruptedException e) {
            logger.error("{} => {}", instanceName, CommonUtils.getStackTrace(e));
        }
        return null;
    }
    public StatefulRedisClusterConnection<String, String> getConnectionClusterAsync() {
        if(clusterASyncConnectionPool == null) {
            return null;
        }
        return this.clusterASyncConnectionPool.poll();
    }
    public StatefulRedisClusterConnection<String, String> getConnectionClusterAsync(int timeout, TimeUnit timeUnit) {
        if(clusterASyncConnectionPool == null) {
            return null;
        }
        try {
            return this.clusterASyncConnectionPool.poll(timeout, timeUnit);
        } catch (InterruptedException e) {
            logger.error("{} => {}", instanceName, CommonUtils.getStackTrace(e));
        }
        return null;
    }
    public void returnClusterResource(boolean isSync, StatefulRedisClusterConnection<String, String> connection) {
        if(connection == null) {
            return;
        }
        if(isSync) {
            this.clusterSyncConnectionPool.offer(connection);
        } else {
            this.clusterASyncConnectionPool.offer(connection);
        }
    }

    public StatefulRedisPubSubConnection<String, String> getSinglePubSubConnection() {
        return singlePubSubConnection;
    }

    public boolean isSingle() {
        return isSingle;
    }

    public boolean isInit() {
        return isInit;
    }

    public String getId() {
        return id;
    }

    public int getWorkers() {
        return workers;
    }

    public int getTimeout() {
        return timeout;
    }

    public int activeCount() {
        if(this.isSingle) {
            return singleSyncConnectionPool.size() + singleASyncConnectionPool.size();
        } else {
            return clusterSyncConnectionPool.size() + clusterASyncConnectionPool.size();
        }
    }
    public int activeCountSync() {
        if(this.isSingle) {
            return singleSyncConnectionPool.size();
        } else {
            return clusterSyncConnectionPool.size();
        }
    }
    public int activeCountASync() {
        if(this.isSingle) {
            return singleASyncConnectionPool.size();
        } else {
            return clusterASyncConnectionPool.size();
        }
    }
}
