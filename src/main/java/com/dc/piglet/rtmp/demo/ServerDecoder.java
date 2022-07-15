package com.dc.piglet.rtmp.demo;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.LinkedList;
import java.util.List;

public class ServerDecoder extends ByteToMessageDecoder {

    private LinkedList<ByteBuf> comePayload = new LinkedList<>();
    private int add = 0;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        //前32位表示长度
        int length = in.readInt();
        if((add +=in.readableBytes()) <= length){
            comePayload.addLast(in.readBytes(in.readableBytes()));
        }
        //已经收到了完整的包
        if(add == length){
            ByteBuf buf = combineBuf();
            out.add(buf);
        }
    }

    private ByteBuf combineBuf(){
        ByteBuf buffer = Unpooled.buffer();
        for (ByteBuf byteBuf : comePayload) {
            buffer.writeBytes(byteBuf);
        }
        comePayload.clear();
        add = 0;
        return buffer;
    }
}
