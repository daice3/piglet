package com.dc.piglet.rtmp.conf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

public class ServerConfig {
    private static final Logger log = LoggerFactory.getLogger(ServerConfig.class);

    public static final Properties config;

    static {
        config = loadConfig();
    }

    private static Properties loadConfig(){
        try{
            InputStream inputStream = new BufferedInputStream(new FileInputStream(new File("src/main/resources/server.properties")));
            Properties prop = new Properties();
            prop.load(new InputStreamReader(inputStream));
            return prop;
        }catch(Exception e) {
            log.error("get server config error",e);
            return null;
        }
    }
}
