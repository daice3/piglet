package com.dc.piglet.rtmp.core.io;

import com.dc.piglet.rtmp.core.io.f4v.F4vReader;
import com.dc.piglet.rtmp.core.io.flv.FlvReader;
import com.dc.piglet.rtmp.core.protocol.RtmpHeader;
import com.dc.piglet.rtmp.core.protocol.RtmpMessage;
import io.netty.channel.*;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
public abstract class RtmpPublisher {

    private static final Logger log = LoggerFactory.getLogger(RtmpPublisher.class);
    private final Timer timer;
    private final int timerTickSize;
    private final boolean usingSharedTimer;
    private final boolean aggregateModeEnabled;

    private final RtmpReader reader;
    private int streamId;
    private long startTime;
    private long seekTime;
    private long timePosition;
    private int currentConversationId;
    private int playLength = -1;
    private boolean paused;
    private int bufferDuration;

    public Channel channel;
    public ChannelHandlerContext ctx;
    private int channelId = 8;

    public static class Event {

        private final int conversationId;
        private final int streamId;

        public Event(final int conversationId, final int streamId) {
            this.conversationId = conversationId;
            this.streamId = streamId;
        }

        public int getConversationId() {
            return conversationId;
        }

        public int getStreamId() {
            return streamId;
        }
    }

    public RtmpPublisher(final RtmpReader reader, final int streamId, final int bufferDuration,
                         boolean useSharedTimer, boolean aggregateModeEnabled) {
        this.aggregateModeEnabled = aggregateModeEnabled;
        this.usingSharedTimer = useSharedTimer;
        timer = new HashedWheelTimer(10, TimeUnit.MILLISECONDS);
        timerTickSize = 10;
        this.reader = reader;
        this.streamId = streamId;
        this.bufferDuration = bufferDuration;
        log.debug("publisher init, streamId: {}", streamId);
    }

    public static RtmpReader getReader(String path) {
        if(path.toLowerCase().startsWith("mp4:")) {
            return new F4vReader(path.substring(4));
        } else if (path.toLowerCase().endsWith(".f4v")) {
            return new F4vReader(path);
        } else {
            return new FlvReader(path);
        }
    }

    public boolean isStarted() {
        return currentConversationId > 0;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setBufferDuration(int bufferDuration) {
        this.bufferDuration = bufferDuration;
    }

    public boolean handle(final Object me,ChannelHandlerContext ctx) {
        if(me instanceof Event) {
            final Event pe = (Event) me;
            if(pe.streamId != streamId) {
                return false;
            }
            if(pe.conversationId != currentConversationId) {
                log.debug("stopping obsolete conversation id: {}, current: {}",
                        pe.getConversationId(), currentConversationId);
                return true;
            }
            write(ctx);
            return true;
        }
        return false;
    }

    public void start(final ChannelHandlerContext ctx, final int seekTime, final int playLength, final RtmpMessage ... messages) {
        this.ctx = ctx;
        this.channel = ctx.channel();
        this.playLength = playLength;
        start(ctx, seekTime, messages);
    }

    public void start(final ChannelHandlerContext ctx, final int seekTimeRequested, final RtmpMessage ... messages) {
        paused = false;
        currentConversationId++;
        startTime = System.currentTimeMillis();
        if(seekTimeRequested >= 0) {
            seekTime = reader.seek(seekTimeRequested);
        } else {
            seekTime = 0;
        }
        timePosition = seekTime;
        log.debug("publish start, seek requested: {} actual seek: {}, play length: {}, conversation: {}", new Object[]{seekTimeRequested, seekTime, playLength, currentConversationId});
        for(final RtmpMessage message : messages) {
            writeToStream(ctx, message);
        }
        for(final RtmpMessage message : reader.getStartMessages()) {
            writeToStream(ctx, message);
        }
        write(ctx);
    }

    public void writeToStream(final ChannelHandlerContext ctx, final RtmpMessage message) {
        if(message.getHeader().getCsId() > 2) {
            message.getHeader().setStreamId(streamId);
            message.getHeader().setTimestamp((int) timePosition);
        }
        ctx.write(message);
    }
    private void write(final ChannelHandlerContext ctx) {
        final long writeTime = System.currentTimeMillis();
        final RtmpMessage message;
        //从reader中读出下一条message
        synchronized(reader) {
            if(reader.hasNext()) {
                message = reader.next();
            } else {
                message = null;
            }
        }
        if (message == null || playLength >= 0 && timePosition > (seekTime + playLength)) {
            stop(ctx);
            return;
        }
        final long elapsedTime = System.currentTimeMillis() - startTime;
        final long elapsedTimePlusSeek = elapsedTime + seekTime;
        final double clientBuffer = timePosition - elapsedTimePlusSeek;
        if(aggregateModeEnabled && clientBuffer > timerTickSize) { // TODO cleanup
            reader.setAggregateDuration((int) clientBuffer);
        } else {
            reader.setAggregateDuration(0);
        }
        final RtmpHeader header = message.getHeader();
        final double compensationFactor = clientBuffer / (bufferDuration + timerTickSize);
        final long delay = (long) ((header.getTimestamp() - timePosition) * compensationFactor);
        if(log.isDebugEnabled()) {
            log.debug("elapsed: {}, streamed: {}, buffer: {}, factor: {}, delay: {}",new Object[]{elapsedTimePlusSeek, timePosition, clientBuffer, compensationFactor, delay});
        }
        timePosition = header.getTimestamp();
        header.setStreamId(streamId);
        header.setCsId(channelId);
        ChannelPromise promise = ctx.newPromise();
        promise.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(final ChannelFuture cf) {
                final long completedIn = System.currentTimeMillis() - writeTime;
                if(completedIn > 2000) {
                    log.warn("channel busy? time taken to write last message: {}", completedIn);
                }
                final long delayToUse = clientBuffer > 0 ? delay - completedIn : 0;
                fireNext(cf.channel(), delayToUse);
            }
        });
        ctx.write(message,promise);
    }
    public void fireNext(final Channel channel, final long delay) {
        final Event readyForNext = new Event(currentConversationId, streamId);
//        if(delay > timerTickSize) {
//            timer.newTimeout(new TimerTask() {
//                @Override public void run(Timeout timeout) {
//                    if(log.isDebugEnabled()) {
//                        log.debug("running after delay: {}", delay);
//                    }
//                    if(readyForNext.conversationId != currentConversationId) {
//                        log.debug("pending 'next' event found obsolete, aborting");
//                        return;
//                    }
//                    channel.pipeline().fireChannelRead(readyForNext);
//                }
//            }, delay, TimeUnit.MILLISECONDS);
//        } else {
//            channel.pipeline().fireChannelRead(readyForNext);
//        }
        channel.pipeline().fireChannelRead(readyForNext);
    }

    public void pause() {
        paused = true;
        currentConversationId++;
    }

    private void stop(final ChannelHandlerContext ctx) {
        currentConversationId++;
        final long elapsedTime = System.currentTimeMillis() - startTime;
        log.info("finished, start: {}, elapsed {}, streamed: {}",new Object[]{seekTime / 1000, elapsedTime / 1000, (timePosition - seekTime) / 1000});
        for(RtmpMessage message : getStopMessages(timePosition)) {
            writeToStream(ctx, message);
        }
    }

    public void close() {
        if(!usingSharedTimer) {
            timer.stop();
        }
        reader.close();
    }

    protected abstract RtmpMessage[] getStopMessages(long timePosition);

    public void setChannelId(int channelId) {
        this.channelId  = channelId;
    }
}
