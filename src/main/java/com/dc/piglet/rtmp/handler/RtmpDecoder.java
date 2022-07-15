package com.dc.piglet.rtmp.handler;

import com.dc.piglet.rtmp.core.protocol.ChunkSize;
import com.dc.piglet.rtmp.core.protocol.Control;
import com.dc.piglet.rtmp.core.protocol.RtmpHeader;
import com.dc.piglet.rtmp.core.protocol.RtmpMessage;
import com.dc.piglet.rtmp.entity.DecodeState;
import com.dc.piglet.rtmp.entity.MessageType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.dc.piglet.rtmp.entity.DecodeState.GET_HEADER;
import static com.dc.piglet.rtmp.entity.DecodeState.GET_PAYLOAD;

/**
 * 以chunk为单位，粘包成一个message
 */
public class RtmpDecoder extends ReplayingDecoder<DecodeState> {
    private static final Logger log = LoggerFactory.getLogger(RtmpDecoder.class);
    public static final int MAX_CHANNEL_ID = 65600;

    private int chunkSize = 128;
    private final RtmpHeader[] incompleteHeaders = new RtmpHeader[MAX_CHANNEL_ID];
    private final ByteBuf[] incompletePayloads = new ByteBuf[MAX_CHANNEL_ID];
    private final RtmpHeader[] completedHeaders = new RtmpHeader[MAX_CHANNEL_ID];

    private ByteBuf payLoad;
    private int csId;
    private RtmpHeader header;

    public RtmpDecoder() {
        super(GET_HEADER);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        switch (state()){
            case GET_HEADER:
                header = new RtmpHeader(in, incompleteHeaders);
                csId = header.getCsId();
                if(incompletePayloads[csId] == null){
                    incompleteHeaders[csId] = header;
                    incompletePayloads[csId] = Unpooled.buffer(header.getMsgLength(),header.getMsgLength());
                }
                payLoad = incompletePayloads[csId];
                checkpoint(GET_PAYLOAD);
            case GET_PAYLOAD:
                final byte[] bytes = new byte[Math.min(payLoad.writableBytes(),chunkSize)];
                in.readBytes(bytes);
                payLoad.writeBytes(bytes);
                checkpoint(GET_HEADER);
                //粘包
                if(payLoad.isWritable()) {
                    return;
                }
                incompletePayloads[csId] = null;
                final RtmpHeader prevHeader = completedHeaders[csId];
                if (!header.isLarge()) {
                    header.setTimestamp(prevHeader.getTimestamp() + header.getDeltaTime());
                }
                //解码
                final RtmpMessage message = MessageType.decode(header, payLoad);
                if(log.isDebugEnabled()) {
                    if(message.getHeader().getMsgType() != MessageType.CONTROL || ((Control) message).getType() != Control.Type.PING_REQUEST)
                    {
                        log.debug("<< {}", message);
                    }
                }
                payLoad = null;
                if(header.isChunkSize()) {
                    final ChunkSize csMessage = (ChunkSize) message;
                    log.debug("decoder new chunk size: {}", csMessage);
                    chunkSize = csMessage.getChunkSize();
                }
                completedHeaders[csId] = header;
                ctx.fireChannelRead(message);
        }
    }
}
