package com.dc.piglet.rtmp.handler;

import com.dc.piglet.rtmp.core.protocol.RtmpHandshake;
import com.dc.piglet.rtmp.util.Util;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class HandShakeHandler extends ByteToMessageDecoder {

    private static final Logger logger = LoggerFactory.getLogger(HandShakeHandler.class);

    private final RtmpHandshake handshake;
    private boolean partOneDone;
    private boolean handshakeDone;

    public HandShakeHandler() {
        handshake = new RtmpHandshake();
    }

    /**
     * ｜client｜Server ｜
     * ｜－－－C0+C1---->|
     * ｜<－－S0+S1+S2-- |
     * ｜－－－C2-－－－> ｜
     *
     *  1. c0和s0很简单就是一个byte的版本号，为’\x03’
     *
     *  2. c1和s1是4个bytes的time,4个bytes的0,1528bytes的random data
     *
     *  3. c2和s2是4个bytes的time,4个bytes的time2,1528 bytes的random data
     */
    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if(!partOneDone) {
            if(in.readableBytes() < RtmpHandshake.HANDSHAKE_SIZE + 1) {
                return;
            }
            handshake.decodeClient0And1(in);
            ctx.write(handshake.encodeServer0());
            ctx.write(handshake.encodeServer1());
            ctx.write(handshake.encodeServer2());
            ctx.flush();
            partOneDone = true;
        }
        if(!handshakeDone) {
            if(in.readableBytes() < RtmpHandshake.HANDSHAKE_SIZE) {
                return;
            }
            handshake.decodeClient2(in);
            handshakeDone = true;
            if(Arrays.equals(handshake.getPeerVersion(), Util.fromHex("00000000"))) {
                final ServerHandler serverHandler = ctx.channel().pipeline().get(ServerHandler.class);
                serverHandler.setAggregateModeEnabled(false);
                logger.info("old client version, disabled 'aggregate' mode");
            }
            ctx.channel().pipeline().remove(HandShakeHandler.class);
        }
    }
}
