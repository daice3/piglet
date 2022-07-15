package com.dc.piglet.rtmp.handler;

import com.dc.piglet.rtmp.core.protocol.ChunkSize;
import com.dc.piglet.rtmp.core.protocol.Control;
import com.dc.piglet.rtmp.core.protocol.RtmpHeader;
import com.dc.piglet.rtmp.core.protocol.RtmpMessage;
import com.dc.piglet.rtmp.entity.MessageType;
import com.dc.piglet.rtmp.entity.Type;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RtmpEncoder extends ChannelOutboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(RtmpEncoder.class);

    private int chunkSize = 128;
    private RtmpHeader[] channelPrevHeaders = new RtmpHeader[RtmpHeader.MAX_CHANNEL_ID];

    private void clearPrevHeaders() {
        logger.debug("clearing prev stream headers");
        channelPrevHeaders = new RtmpHeader[RtmpHeader.MAX_CHANNEL_ID];
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise){
        if(msg instanceof RtmpMessage){
            ctx.write(encode((RtmpMessage) msg),promise);
            ctx.flush();
        }
    }

    public ByteBuf encode(final RtmpMessage message) {
        final ByteBuf in = message.encode();
        final RtmpHeader header = message.getHeader();
        if(header.isChunkSize()) {
            final ChunkSize csMessage = (ChunkSize) message;
            logger.debug("encoder new chunk size: {}", csMessage);
            chunkSize = csMessage.getChunkSize();
        } else if(header.isControl()) {
            final Control control = (Control) message;
            if(control.getType() == Control.Type.STREAM_BEGIN) {
                clearPrevHeaders();
            }
        }
        final int csId = header.getCsId();
        header.setMsgLength(in.readableBytes());
        final RtmpHeader prevHeader = channelPrevHeaders[csId];
        if(prevHeader != null // first stream message is always large
                && header.getStreamId() > 0 // all control messages always large
                && header.getTimestamp() > 0) { // if time is zero, always large
            if(header.getMsgLength() == prevHeader.getMsgLength()) {
                header.setChunkType(Type.SMALL);
            } else {
                header.setChunkType(Type.MID);
            }
            final int deltaTime = header.getTimestamp() - prevHeader.getTimestamp();
            if(deltaTime < 0) {
                logger.warn("negative time: {}", header);
                header.setDeltaTime(0);
            } else {
                header.setDeltaTime(deltaTime);
            }
        } else {
            // otherwise force to LARGE
            header.setChunkType(Type.ALL);
        }
        channelPrevHeaders[csId] = header;
        if(logger.isDebugEnabled()) {
            if (message.getHeader().getMsgType() != MessageType.CONTROL || ((Control) message).getType() != Control.Type.PING_RESPONSE) {
                //logger.debug(">> {}", message);
            }
        }
        final ByteBuf out = Unpooled.buffer(
                RtmpHeader.MAX_ENCODED_SIZE + header.getMsgLength() + header.getMsgLength() / chunkSize);
        boolean first = true;
        while(in.isReadable()) {
            final int size = Math.min(chunkSize, in.readableBytes());
            if(first) {
                header.encode(out);
                first = false;
            } else {
                out.writeBytes(header.getTinyHeader());
            }
            in.readBytes(out, size);
        }
        return out;
    }


}
