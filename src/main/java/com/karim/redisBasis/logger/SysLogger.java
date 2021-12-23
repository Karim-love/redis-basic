package com.karim.redisBasis.logger;

import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by sblim
 * Date : 2021-12-23
 * Time : 오후 3:30
 */
public class SysLogger {

    private volatile static SysLogger _instance = null;
    private Logger logger = (Logger) LoggerFactory.getLogger(SysLogger.class);

    private SysLogger() {
    }

    public static SysLogger getInstance() {
        if (_instance == null) {
            /* 제일 처음에만 동기화 하도록 함 */
            synchronized (SysLogger.class) {
                if (_instance == null) {
                    _instance = new SysLogger();
                }
            }
        }
        return _instance;
    }

    public Logger getLogger() {
        return logger;
    }

    public static void main(String[] args) {
        SysLogger.getInstance().getLogger();
    }
}
