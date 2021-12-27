package com.karim.redisBasis.cfg;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.karim.redisBasis.define.CommonDefine;
import com.karim.redisBasis.instance.RedisPoolInstance;
import com.karim.redisBasis.logger.SysLogger;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by sblim
 * Date : 2021-12-23
 * Time : 오후 2:35
 */
public class PoolConfigurations {
    private static volatile PoolConfigurations _instance = null;
    private final String INSTANCE_NAME;
    private static Logger logger = SysLogger.getInstance().getLogger();

    private PoolConfigurations() {
        INSTANCE_NAME = "[###PoolConfigurations####" + this.hashCode() + "]";
        init();
    }

    public static PoolConfigurations getInstance() {
        if( _instance == null ) {
            synchronized(PoolConfigurations.class) {
                if( _instance == null ) {
                    _instance = new PoolConfigurations();
                }
            }
        }
        return _instance;
    }

    private void init() {

        String poolPath = Properties.getInstance().getString("common.pool.path", "config/pool.yml");
        redisLoad(poolPath);
    }

    private void redisLoad(String filePath) {
        JsonObject root = null;
        try {
            root = loadYamlToMap(filePath).getAsJsonObject();
        } catch (Exception e) {
            logger.error("{} => {}", this.getClass().getName(), e.getStackTrace());
        }
        if(root == null) {
            return;
        }
        logger.info("{} => redis configurations : {}", INSTANCE_NAME, root);

        CommonDefine.REDIS_ID = Properties.getInstance().getString("common.redis.id", "r1");

        // 1. Redis Setting
        JsonArray redisRoot = root.has("redis") ? root.get("redis").getAsJsonArray() : null;
        if (redisRoot != null){
            for (JsonElement row : redisRoot){
                if (!row.getAsJsonObject().has("id")){
                    continue;
                }
                String id = row.getAsJsonObject().get("id").getAsString();
                if (StringUtils.isEmpty(id)){
                    continue;
                }
                if (!CommonDefine.REDIS_ID.equals(id)){
                    continue;
                }

                if (!row.getAsJsonObject().has("use")){
                    continue;
                }
                boolean use = row.getAsJsonObject().get("use").getAsBoolean();
                if (!use){
                    continue;
                }
                String type = row.getAsJsonObject().get("type").getAsString();
                if (StringUtils.isEmpty(type)){
                    continue;
                }
                boolean single = true;
                if (!type.equals("single")){
                    single = false;
                }
                int timeout = row.getAsJsonObject().get("timeout").getAsInt();
                String auth = row.getAsJsonObject().has("auth") ? row.getAsJsonObject().get("auth").getAsString() : "" ;
                int workers = row.getAsJsonObject().get("workers").getAsInt();

                List<String> uri = new ArrayList<>();
                for (JsonElement uriArray : row.getAsJsonObject().get("uri").getAsJsonArray()){
                    uri.add(uriArray.getAsString());
                }
                if (uri == null || uri.size() == 0){
                    continue;
                }
                if(!RedisPoolInstance.getInstance().containsKey(id)) {
                    RedisPoolInstance.getInstance().add(id, single, auth, timeout, workers, uri);
                }
            }
        }
    }

    private static JsonElement loadYamlToMap(String filePath) {
        Map<String, Object> propMap = null;
        JsonElement jsonElement = null;
        filePath = getPath(filePath);
        try {
            InputStream inputStream = Files.newInputStream(Paths.get(filePath).toAbsolutePath());
            propMap = new Yaml().load(inputStream);
            jsonElement = new Gson().toJsonTree(propMap, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonElement;
    }

    protected static String getPath(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            String basePath = getBasePath(file);
            file = constructFile(basePath, filePath);
        }

        if (!file.exists()) {
            file = locateFromClasspath(filePath);
        }

        return !file.exists() ? null : file.getPath();
    }

    protected static String getBasePath(File file) {
        String basePath = file.getParentFile() != null ? file.getParentFile().getAbsolutePath() : null;
        if (basePath != null && basePath.startsWith("file:") && !basePath.startsWith("file://")) {
            basePath = "file://" + basePath.substring("file:".length());
        }

        return basePath;
    }

    protected static File locateFromClasspath(String resourceName) {
        URL url = null;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader != null) {
            url = loader.getResource(resourceName);
        }

        if (url == null) {
            url = ClassLoader.getSystemResource(resourceName);
        }

        return new File(url.getPath());
    }

    protected static File constructFile(String basePath, String fileName) {
        File absolute = null;
        if (fileName != null) {
            absolute = new File(fileName);
        }
        File file;
        if (StringUtils.isEmpty(basePath) || absolute != null && absolute.isAbsolute()) {
            file = new File(fileName);
        } else {
            StringBuilder fName = new StringBuilder();
            fName.append(basePath);
            if (!basePath.endsWith(File.separator)) {
                fName.append(File.separator);
            }

            if (fileName.startsWith("." + File.separator)) {
                fName.append(fileName.substring(2));
            } else {
                fName.append(fileName);
            }

            file = new File(fName.toString());
        }

        return file;
    }
}
