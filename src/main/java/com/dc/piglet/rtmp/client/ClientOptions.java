
package com.dc.piglet.rtmp.client;


import com.dc.piglet.rtmp.core.io.RtmpReader;
import com.dc.piglet.rtmp.core.io.RtmpWriter;
import com.dc.piglet.rtmp.server.ServerStream;
import com.dc.piglet.rtmp.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class ClientOptions {

    private static final Logger log = LoggerFactory.getLogger(ClientOptions.class);
    private static final byte[] SERVER_CONST = "Genuine Adobe Flash Media Server 001".getBytes();
    public static final byte[] CLIENT_CONST = "Genuine Adobe Flash Player 001".getBytes();

    private ServerStream.PublishType publishType;
    private String host = "localhost";
    private int port = 1935;
    private String appName = "vod";
    private String streamName;
    private String fileToPublish;
    private RtmpReader readerToPublish;
    private RtmpWriter writerToSave;
    private String saveAs;
    private boolean rtmpe;
    private Map<String, Object> params;
    private Object[] args;
    private byte[] clientVersionToUse;
    private int start = -2;
    private int length = -1;
    private int buffer = 100;
    private byte[] swfHash;
    private int swfSize;
    private int load = 1;
    private int loop = 1;
    private int threads = 10;
    private List<ClientOptions> clientOptionsList;

    public ClientOptions() {}

    public ClientOptions(String host, String appName, String streamName, String saveAs) {
        this(host, 1935, appName, streamName, saveAs, false, null);
    }

    public ClientOptions(String host, int port, String appName, String streamName, String saveAs,
                         boolean rtmpe, String swfFile) {
        this.host = host;
        this.port = port;
        this.appName = appName;
        this.streamName = streamName;
        this.saveAs = saveAs;
        this.rtmpe = rtmpe;        
        if(swfFile != null) {
            initSwfVerification(swfFile);
        }
    }

    private static final Pattern URL_PATTERN = Pattern.compile(
          "(rtmp.?)://" // 1) protocol
        + "([^/:]+)(:[0-9]+)?/" // 2) host 3) port
        + "([^/]+)/" // 4) app
        + "(.*)" // 5) play
    );

    public ClientOptions(String url, String saveAs) {
        parseUrl(url);
        this.saveAs = saveAs;
    }

    public void parseUrl(String url) {
        Matcher matcher = URL_PATTERN.matcher(url);
        if (!matcher.matches()) {
            throw new RuntimeException("invalid url: " + url);
        }
        log.debug("parsing url: {}", url);
        String protocol = matcher.group(1);
        log.debug("protocol = '{}'",  protocol);
        host = matcher.group(2);
        log.debug("host = '{}'", host);
        String portString = matcher.group(3);
        if (portString == null) {
            log.debug("port is null in url, will use default 1935");
        } else {
            portString = portString.substring(1); // skip the ':'
            log.debug("port = '{}'", portString);
        }
        port = portString == null ? 1935 : Integer.parseInt(portString);
        appName = matcher.group(4);
        log.debug("app = '{}'",  appName);
        streamName = matcher.group(5);
        log.debug("playName = '{}'", streamName);
        rtmpe = protocol.equalsIgnoreCase("rtmpe");
        if(rtmpe) {
            log.debug("rtmpe requested, will use encryption");
        }        
    }
    
    public void publishLive() {
        publishType = ServerStream.PublishType.LIVE;        
    }
    
    public void publishRecord() {
        publishType = ServerStream.PublishType.RECORD;        
    }
    
    public void publishAppend() {
        publishType = ServerStream.PublishType.APPEND;        
    }

    public int getLoad() {
        return load;
    }

    public void setLoad(int load) {
        this.load = load;
    }

    public int getLoop() {
        return loop;
    }

    public void setLoop(int loop) {
        this.loop = loop;
    }

    public String getFileToPublish() {
        return fileToPublish;
    }

    public void setFileToPublish(String fileName) {
        this.fileToPublish = fileName;
    }

    public RtmpReader getReaderToPublish() {
        return readerToPublish;
    }

    public void setReaderToPublish(RtmpReader readerToPublish) {
        this.readerToPublish = readerToPublish;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getTcUrl() {
        return (rtmpe ? "rtmpe://" : "rtmp://") + host + ":" + port + "/" + appName;
    }

    public void setArgs(Object ... args) {
        this.args = args;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setClientVersionToUse(byte[] clientVersionToUse) {
        this.clientVersionToUse = clientVersionToUse;
    }

    public byte[] getClientVersionToUse() {
        return clientVersionToUse;
    }

    public void initSwfVerification(String pathToLocalSwfFile) {
        initSwfVerification(new File(pathToLocalSwfFile));
    }

    public void initSwfVerification(File localSwfFile) {
        log.debug("initializing swf verification data for: " + localSwfFile.getAbsolutePath());
        byte[] bytes = Util.readAsByteArray(localSwfFile);
        byte[] hash = Util.HmacSHA256(bytes, CLIENT_CONST);
        swfSize = bytes.length;
        swfHash = hash;
        log.debug("swf verification initialized - size: {}, hash: {}", swfSize, Util.toHex(swfHash));
    }
    
    public void putParam(String key, Object value) {
        if(params == null) {
            params = new LinkedHashMap<String, Object>();
        }
        params.put(key, value);
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public ServerStream.PublishType getPublishType() {
        return publishType;
    }

    public void setPublishType(ServerStream.PublishType publishType) {
        this.publishType = publishType;
    }

    public String getStreamName() {
        return streamName;
    }

    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getBuffer() {
        return buffer;
    }

    public void setBuffer(int buffer) {
        this.buffer = buffer;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getSaveAs() {
        return saveAs;
    }

    public void setSaveAs(String saveAs) {
        this.saveAs = saveAs;
    }

    public boolean isRtmpe() {
        return rtmpe;
    }

    public byte[] getSwfHash() {
        return swfHash;
    }

    public void setSwfHash(byte[] swfHash) {
        this.swfHash = swfHash;
    }

    public int getSwfSize() {
        return swfSize;
    }

    public void setSwfSize(int swfSize) {
        this.swfSize = swfSize;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public RtmpWriter getWriterToSave() {
        return writerToSave;
    }

    public void setWriterToSave(RtmpWriter writerToSave) {
        this.writerToSave = writerToSave;
    }

    public List<ClientOptions> getClientOptionsList() {
        return clientOptionsList;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[host: '").append(host);
        sb.append("' port: ").append(port);
        sb.append(" appName: '").append(appName);
        sb.append("' streamName: '").append(streamName);
        sb.append("' saveAs: '").append(saveAs);
        sb.append("' rtmpe: ").append(rtmpe);
        sb.append(" publish: ").append(publishType);
        if(clientVersionToUse != null) {
            sb.append(" clientVersionToUse: '").append(Util.toHex(clientVersionToUse)).append('\'');
        }
        sb.append(" start: ").append(start);
        sb.append(" length: ").append(length);
        sb.append(" buffer: ").append(buffer);
        sb.append(" params: ").append(params);
        sb.append(" args: ").append(Arrays.toString(args));
        if(swfHash != null) {
            sb.append(" swfHash: '").append(Util.toHex(swfHash));
            sb.append("' swfSize: ").append(swfSize).append('\'');
        }
        sb.append(" load: ").append(load);
        sb.append(" loop: ").append(loop);
        sb.append(" threads: ").append(threads);
        sb.append(']');
        return sb.toString();
    }
    
}
