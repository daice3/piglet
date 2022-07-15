package com.dc.piglet.rtmp.handler;

import com.dc.piglet.rtmp.core.io.RtmpPublisher;
import com.dc.piglet.rtmp.core.io.RtmpReader;
import com.dc.piglet.rtmp.core.io.RtmpWriter;
import com.dc.piglet.rtmp.core.protocol.*;
import com.dc.piglet.rtmp.server.ServerApplication;
import com.dc.piglet.rtmp.server.ServerStream;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ServerHandler extends ChannelDuplexHandler{

    private static final Logger log = LoggerFactory.getLogger(ServerHandler.class);

    private final int bytesReadWindow = 2500000;
    private long bytesRead;
    private long bytesReadLastSent;
    private int bufferDuration;
    private String clientId;

    private long bytesWritten;
    private final int bytesWrittenWindow = 2500000;
    private int bytesWrittenLastReceived;

    private int streamId;
    private RtmpPublisher publisher;
    private ServerApplication application;
    private String playName;
    private ServerStream subscriberStream;
    private RtmpWriter recorder;

    private boolean aggregateModeEnabled = true;

    public void setAggregateModeEnabled(boolean aggregateModeEnabled) {
        this.aggregateModeEnabled = aggregateModeEnabled;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(publisher != null && publisher.handle(msg,ctx)) {
            return;
        }
        final RtmpMessage message = (RtmpMessage) msg;
        bytesRead += message.getHeader().getMsgLength();
        if((bytesRead - bytesReadLastSent) > bytesReadWindow) {
            log.info("sending bytes read ack after: {}", bytesRead);
            BytesRead ack = new BytesRead(bytesRead);
            ctx.write(ack);
            bytesReadLastSent = bytesRead;
        }
        switch(message.getHeader().getMsgType()) {
            case CHUNK_SIZE: // handled by decoder
                break;
            case CONTROL:
                final Control control = (Control) message;
                if (control.getType() == Control.Type.SET_BUFFER) {
                    log.debug("received set buffer: {}", control);
                    bufferDuration = control.getBufferLength();
                    if(publisher != null){
                        publisher.setBufferDuration(bufferDuration);
                    }
                } else {
                    log.info("ignored control: {}", control);
                }
                break;
            case COMMAND_AMF0:
            case COMMAND_AMF3:
                final Command command = (Command) message;
                final String name = command.getName();
                switch (name) {
                    case "connect":
                        connectResponse(ctx, command);
                        break;
                    case "createStream":
                        streamId = 1;
                        ctx.write(Command.createStreamSuccess(command.getTransactionId(), streamId));
                        break;
                    case "play":
                        playResponse(ctx, command);
                        break;
                    case "deleteStream":
                        int deleteStreamId = ((Double) command.getArg(0)).intValue();
                        log.info("deleting stream id: {}", deleteStreamId);
                        break;
                    case "FCUnpublish":
                    case "closeStream":
                        final int clientStreamId = command.getHeader().getStreamId();
                        log.info("closing stream id: {}", clientStreamId); // TODO
                        unpublishIfLive();
                        break;
                    case "pause":
                        //pauseResponse(channel, command);
                        break;
                    case "seek":
                        //seekResponse(channel, command);
                        break;
                    case "publish":
                        publishResponse(ctx, command);
                        break;
                    default:
                        log.warn("ignoring command: {}", command);
                        break;
                }
                return;
            case METADATA_AMF0:
            case METADATA_AMF3:
                final Metadata meta = (Metadata) message;
                if(meta.getName().equals("onMetaData")) {
                    log.info("adding onMetaData message: {}", meta);
                    meta.setDuration(-1);
                    subscriberStream.addConfigMessage(meta);
                }
                broadcast(message);
                break;
            case AUDIO:
            case VIDEO:
                if(((DataMessage) message).isConfig()) {
                    log.info("adding config message: {}", message);
                    subscriberStream.addConfigMessage(message);
                }
            case AGGREGATE:
                broadcast(message);
                break;
            case BYTES_READ:
                final BytesRead bytesReadByClient = (BytesRead) message;
                bytesWrittenLastReceived = bytesReadByClient.getValue();
                log.debug("bytes read ack from client: {}, actual: {}", bytesReadByClient, bytesWritten);
                break;
            case WINDOW_ACK_SIZE:
                WindowAckSize was = (WindowAckSize) message;
                if(was.getValue() != bytesReadWindow) {
                    ctx.write(SetPeerBw.dynamic(bytesReadWindow));

                }
                break;
            case SET_PEER_BW:
                SetPeerBw spb = (SetPeerBw) message;
                if(spb.getValue() != bytesWrittenWindow) {
                    ctx.write(new WindowAckSize(bytesWrittenWindow));
                }
                break;
            default:
                log.warn("ignoring message: {}", message);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ByteBuf) {
            bytesWritten += ((ByteBuf) msg).writableBytes();
        }
        super.write(ctx,msg,promise);
    }

    private void broadcast(final RtmpMessage message) {
        ChannelGroup subscribers = subscriberStream.getSubscribers();
        subscribers.write(message);
        if(recorder != null) {
            recorder.write(message);
        }
    }

    private void publishResponse(final ChannelHandlerContext ctx, final Command command) {
        if(command.getArgCount() > 1) { // publish
            final String streamName = (String) command.getArg(0);
            final String publishTypeString = (String) command.getArg(1);
            log.info("publish, stream name: {}, type: {}", streamName, publishTypeString);
            subscriberStream = application.getStream(streamName, publishTypeString); // TODO append, record
            if(subscriberStream.getPublisher() != null) {
                log.info("disconnecting publisher client, stream already in use");
                ChannelFuture future = ctx.write(Command.publishBadName(streamId));
                future.addListener(ChannelFutureListener.CLOSE);
                return;
            }
            subscriberStream.setPublisher(ctx);
            ctx.write(Command.publishStart(streamName, clientId, streamId));
            ctx.write(new ChunkSize(4096));
            ctx.write(Control.streamBegin(streamId));
            ctx.flush();
            final ServerStream.PublishType publishType = subscriberStream.getPublishType();
            log.info("created publish stream: {}", subscriberStream);
            switch(publishType) {
                case LIVE:
                    final ChannelGroup subscribers = subscriberStream.getSubscribers();
                        subscribers.write(Command.publishNotify(streamId));
                        writeToStream(subscribers, Video.empty());
                        writeToStream(subscribers, Metadata.rtmpSampleAccess());
                        writeToStream(subscribers, Audio.empty());
                        writeToStream(subscribers, Metadata.dataStart());
                    break;
                case RECORD:
                    recorder = application.getWriter(streamName);
                    break;
                case APPEND:
                    log.warn("append not implemented yet, un-publishing...");
                    unpublishIfLive();
                    break;
            }
        } else { // un-publish
            final boolean publish = (Boolean) command.getArg(0);
            if(!publish) {
                unpublishIfLive();
            }
        }
    }

    private void connectResponse(final ChannelHandlerContext ctx, final Command connect) {
        final String appName = (String) connect.getObject().get("app");
        clientId = ctx.channel().id() + "";
        application = ServerApplication.get(appName);
        ctx.write(new WindowAckSize(bytesWrittenWindow));
        ctx.write(SetPeerBw.dynamic(bytesReadWindow));
        ctx.write(Control.streamBegin(streamId));
        final Command result = Command.connectSuccess(connect.getTransactionId());
        ctx.write(result);
        ctx.write(Command.onBWDone());
        ctx.flush();
    }

    private void playResponse(final ChannelHandlerContext ctx, final Command play) {
        int playStart = -2;
        int playLength = -1;
        if(play.getArgCount() > 1) {
            playStart = ((Double) play.getArg(1)).intValue();
        }
        if(play.getArgCount() > 2) {
            playLength = ((Double) play.getArg(2)).intValue();
        }
        final boolean playReset;
        if(play.getArgCount() > 3) {
            playReset = ((Boolean) play.getArg(3));
        } else {
            playReset = true;
        }
        final Command playResetCommand = playReset ? Command.playReset(playName, clientId) : null;
        final String clientPlayName = (String) play.getArg(0);
        final ServerStream stream = application.getStream(clientPlayName);
        log.debug("play name {}, start {}, length {}, reset {}",new Object[]{clientPlayName, playStart, playLength, playReset});
        if(stream.isLive()) {
            for(final RtmpMessage message : getStartMessages(playResetCommand)) {
                writeToStream(ctx, message);
            }
            boolean videoConfigPresent = false;
            for(RtmpMessage message : stream.getConfigMessages()) {
                log.info("writing start meta / config: {}", message);
                if(message.getHeader().isVideo()) {
                    videoConfigPresent = true;
                }
                writeToStream(ctx, message);
            }
            stream.getSubscribers().add(ctx.channel());
            log.info("client requested live stream: {}, added to stream: {}", clientPlayName, stream);
            return;
        }
        if(!clientPlayName.equals(playName)) {
            playName = clientPlayName;
            final RtmpReader reader = application.getReader(playName);
            if(reader == null) {
                ctx.write(Command.playFailed(playName, clientId));
                return;
            }
            publisher = new RtmpPublisher(reader, streamId, bufferDuration, true, aggregateModeEnabled) {
                @Override protected RtmpMessage[] getStopMessages(long timePosition) {
                    return new RtmpMessage[] {
                            Metadata.onPlayStatus(timePosition / (double) 1000, 10000),
                            Command.playStop(playName, clientId),
                            Control.streamEof(streamId)
                    };
                }
            };
        }
        publisher.start(ctx, playStart, playLength, getStartMessages(playResetCommand));
    }

    private RtmpMessage[] getStartMessages(final RtmpMessage variation) {
        final List<RtmpMessage> list = new ArrayList<RtmpMessage>();
        list.add(new ChunkSize(4096));
        list.add(Control.streamIsRecorded(streamId));
        list.add(Control.streamBegin(streamId));
        if(variation != null) {
            list.add(variation);
        }
        list.add(Command.playStart(playName, clientId));
        list.add(Metadata.rtmpSampleAccess());
        list.add(Audio.empty());
        list.add(Metadata.dataStart());
        return list.toArray(new RtmpMessage[list.size()]);
    }

    private void writeToStream(final ChannelHandlerContext ctx, final RtmpMessage message) {
        if(message.getHeader().getCsId() > 2) {
            message.getHeader().setStreamId(streamId);
        }
        ctx.write(message);
    }

    private void writeToStream(final ChannelGroup channelGroup, final RtmpMessage message) {
        if(message.getHeader().getCsId() > 2) {
            message.getHeader().setStreamId(streamId);
        }
        channelGroup.write(message);
    }

    private void unpublishIfLive() {
        if(subscriberStream != null && subscriberStream.getPublisher() != null) {
            final ChannelHandlerContext ctx = subscriberStream.getPublisher();
            if(ctx.channel().isWritable()) {
                ctx.write(Command.unpublishSuccess(subscriberStream.getName(), clientId, streamId));
            }
            ChannelGroup subscribers = subscriberStream.getSubscribers();
            subscribers.write(Command.unpublishNotify(streamId));
            subscriberStream.setPublisher(null);
            log.debug("publisher disconnected, stream un-published");
        }
        if(recorder != null) {
            recorder.close();
            recorder = null;
        }
    }
}
