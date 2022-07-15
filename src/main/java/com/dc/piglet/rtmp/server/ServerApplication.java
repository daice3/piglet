
package com.dc.piglet.rtmp.server;

import com.dc.piglet.rtmp.conf.ServerConfig;
import com.dc.piglet.rtmp.core.io.RtmpReader;
import com.dc.piglet.rtmp.core.io.RtmpWriter;
import com.dc.piglet.rtmp.core.io.f4v.F4vReader;
import com.dc.piglet.rtmp.core.io.flv.FlvReader;
import com.dc.piglet.rtmp.core.io.flv.FlvWriter;
import com.dc.piglet.rtmp.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerApplication {
    private static final Logger log = LoggerFactory.getLogger(ServerApplication.class);
    private final String name;
    private final Map<String, ServerStream> streams;

    public static String homePath = ServerConfig.config.getProperty("server.home");

    public ServerApplication(final String rawName) {
        this.name = cleanName(rawName);
        streams = new ConcurrentHashMap<String, ServerStream>();
    }

    public String getName() {
        return name;
    }

    public RtmpReader getReader(final String rawName) {
        final String streamName = Util.trimSlashes(rawName);
        final String path =   homePath + name + "/";
        final String readerPlayName;
        try {
            if(streamName.startsWith("mp4:")) {
                readerPlayName = streamName.substring(4);
                return new F4vReader(path + readerPlayName);
            } else {                
                if(streamName.lastIndexOf('.') < streamName.length() - 4) {
                    readerPlayName = streamName + ".flv";
                } else {
                    readerPlayName = streamName;
                }
                return new FlvReader(path + readerPlayName);
            }
        } catch(Exception e) {
            log.error("reader creation failed: {}", e.getMessage());
            return null;
        }
    }

    public RtmpWriter getWriter(final String rawName) {
        final String streamName = Util.trimSlashes(rawName);
        final String path =  homePath + name + "/";
        return new FlvWriter(path + streamName + ".flv");
    }

    public static ServerApplication get(final String rawName) {
        final String appName = cleanName(rawName);
        ServerApplication app = Server.APPLICATIONS.get(appName);
        if(app == null) {
            app = new ServerApplication(appName);
            Server.APPLICATIONS.put(appName, app);
        }
        return app;
    }

    public ServerStream getStream(final String rawName) {
        return getStream(rawName, null);
    }

    public ServerStream getStream(final String rawName, final String type) {
        final String streamName = cleanName(rawName);
        ServerStream stream = streams.get(streamName);
        if(stream == null) {
            stream = new ServerStream(streamName, type);
            streams.put(streamName, stream);
        }
        return stream;
    }

    private static String cleanName(final String raw) {
        return Util.trimSlashes(raw).toLowerCase();
    }

    @Override
    public String toString() {
        return "[name: '" + name +
                "' streams: " + streams +
                ']';
    }

}
