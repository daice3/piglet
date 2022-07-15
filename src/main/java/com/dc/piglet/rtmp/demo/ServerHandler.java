package com.dc.piglet.rtmp.demo;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;

import java.nio.charset.StandardCharsets;

public class ServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf in = (ByteBuf) msg;
        byte[] bytes = new byte[in.readableBytes()];
        in.readBytes(bytes);
        String s = new String(bytes, StandardCharsets.UTF_8);
        System.out.println(s);
    }
}
