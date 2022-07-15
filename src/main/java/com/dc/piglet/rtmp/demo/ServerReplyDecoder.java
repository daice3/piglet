package com.dc.piglet.rtmp.demo;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

import static com.dc.piglet.rtmp.demo.ServerReplyDecoder.*;
import static com.dc.piglet.rtmp.demo.ServerReplyDecoder.State.Header;
import static com.dc.piglet.rtmp.demo.ServerReplyDecoder.State.Payload;

public class ServerReplyDecoder extends ReplayingDecoder<State> {

    enum State{
        Header,Payload
    }

    public ServerReplyDecoder() {
        super(Header);
    }

    private int length;

    private ByteBuf payload;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (state()){
            case Header:
                length = in.readInt();
                if(payload == null){
                    payload = Unpooled.buffer(length,length);
                }
                checkpoint(Payload);
            case Payload:
                //will throw REPLY error (ReplayingDecoderByteBuf)
                //catch error and resume the reader index
                //第二次过来的byteBuf多了一个header,看来利用这个机制不太好实现
                //手工来粘包
                byte[] buffer = new byte[Math.min((in.writerIndex() - in.readerIndex()),payload.writableBytes())];
                in.readBytes(buffer);
                payload.writeBytes(buffer);
                checkpoint(Header);
                if(payload.isWritable()){
                    return;
                }
                out.add(payload);
                payload = null;
                break;
            default:
                throw new RuntimeException("SHOULD NOT BE HERE");
        }
    }

}
