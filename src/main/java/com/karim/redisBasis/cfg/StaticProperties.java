package com.karim.redisBasis.cfg;


import com.karim.redisBasis.logger.SysLogger;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.List;

public class StaticProperties {
    private static final String PROPERTIES_FILE_PATH = "config/static.properties";
    private static volatile StaticProperties _instance = null;
    private static PropertiesConfiguration configuration;
    private Logger logger = SysLogger.getInstance().getLogger();
    private static String lineBreaker = System.getProperty("line.separator") == null ? "\n" : System.getProperty("line.separator");

    public static StaticProperties getInstance() {
        if (_instance == null) {
            /* 제일 처음에만 동기화 하도록 함 */
            synchronized (StaticProperties.class) {
                if (_instance == null) {
                    _instance = new StaticProperties();
                }
            }
        }
        return _instance;
    }

    private StaticProperties() {
        try {
            logger.info("Loading the static properties file : {}", PROPERTIES_FILE_PATH);
            configuration = new PropertiesConfiguration(PROPERTIES_FILE_PATH);
        } catch (ConfigurationException e) {
            logger.error("StaticProperties:init => {}",e.getStackTrace());
        }
    }

    public static void main(String[] args) {
        StaticProperties.getInstance();
    }


    public String getString(String key) {
        return configuration.getString(key);
    }

    public String getString(String key, String defaultValue) {
        return configuration.getString(key, defaultValue);
    }

    public int getInt(String key) {
        int returnValue = 0;
        try {
            returnValue = configuration.getInt(key);
        } catch (Exception e) {
        }
        return returnValue;
    }

    public int getInt(String key, int defaultValue) {
        int returnValue;
        try {
            returnValue = configuration.getInt(key, defaultValue);
        } catch (Exception e) {
            returnValue = 0;
        }
        return returnValue;
    }

    public boolean getBoolean(String key) {
        boolean returnValue = false;
        try {
            returnValue = configuration.getBoolean(key);
        } catch (Exception e) {
        }
        return returnValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        boolean returnValue;
        try {
            returnValue = configuration.getBoolean(key, defaultValue);
        } catch (Exception e) {
            returnValue = false;
        }
        return returnValue;
    }

    public List<Object> getList(String key) {
        List<Object> returnValue = null;
        try {
            returnValue = configuration.getList(key);
        } catch (Exception e) {
        }
        return returnValue;
    }

    public List<Object> getList(String key, List<?> defaultValue) {
        List<Object> returnValue = null;
        try {
            returnValue = configuration.getList(key, defaultValue);
        } catch (Exception e) {
        }
        return returnValue;
    }

    public String[] getStringArray(String key) {
        String[] returnValue = null;
        try {
            returnValue = configuration.getStringArray(key);
        } catch (Exception e) {
        }
        return returnValue;
    }

    public Object getProperty(String key) {
        return configuration.getProperty(key);
    }

    public void setProperty(String key, Object value) {
        configuration.setProperty(key, value);
    }

    public void save() throws ConfigurationException {
        configuration.save();
    }

    public Iterator<String> getKeys(String prefix) {
        return configuration.getKeys(prefix);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        Iterator<String> iterator = configuration.getKeys();
        String key;
        stringBuilder.append(lineBreaker);
        while (iterator.hasNext()) {
            key = iterator.next();
            stringBuilder.append(key).append("=").append(configuration.getString(key, "")).append(lineBreaker);
        }
        return stringBuilder.toString();
    }

}
