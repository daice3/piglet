package com.dc.piglet.rtmp.server;


import com.dc.piglet.rtmp.core.protocol.RtmpMessage;
import com.dc.piglet.rtmp.util.Util;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.DefaultEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ServerStream {

    public enum PublishType {

        LIVE,
        APPEND,
        RECORD;

        public String asString() {
            return this.name().toLowerCase();
        }

        public static PublishType parse(final String raw) {
            return PublishType.valueOf(raw.toUpperCase());
        }

    }
    
    private final String name;
    private final PublishType publishType;
    private final ChannelGroup subscribers;
    private final List<RtmpMessage> configMessages;
    private ChannelHandlerContext publisher;

    private static final Logger logger = LoggerFactory.getLogger(ServerStream.class);

    public ServerStream(final String rawName, final String typeString) {        
        this.name = Util.trimSlashes(rawName).toLowerCase();
        if(typeString != null) {
            this.publishType = PublishType.parse(typeString); // TODO record, append
            subscribers = new DefaultChannelGroup(name,new DefaultEventExecutor());
            configMessages = new ArrayList<>();
        } else {
            this.publishType = null;
            subscribers = null;
            configMessages = null;
        }
        logger.info("Created ServerStream {}", this);
    }

    public boolean isLive() {
        return publishType != null && publishType == PublishType.LIVE;
    }

    public PublishType getPublishType() {
        return publishType;
    }

    public ChannelGroup getSubscribers() {
        return subscribers;
    }

    public String getName() {
        return name;
    }


    public List<RtmpMessage> getConfigMessages() {
        return configMessages;
    }

    public void addConfigMessage(final RtmpMessage message) {
        configMessages.add(message);
    }

    public void setPublisher(ChannelHandlerContext publisher) {
        this.publisher = publisher;
        configMessages.clear();
    }

    public ChannelHandlerContext getPublisher() {
        return publisher;
    }

    @Override
    public String toString() {
        return "[name: '" + name +
                "' type: " + publishType +
                " publisher: " + publisher +
                " subscribers: " + subscribers +
                " config: " + configMessages +
                ']';
    }

}
