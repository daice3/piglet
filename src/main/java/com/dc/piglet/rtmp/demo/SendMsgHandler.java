package com.dc.piglet.rtmp.demo;

import com.dc.piglet.rtmp.util.Util;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.SneakyThrows;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
public class SendMsgHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        final int length = 64;
        final String[] s = {"床前明月光\n", "疑是地上霜\n", "举头望明月\n", "低头思故乡\n"};
        ctx.channel().eventLoop().schedule(new Runnable() {
            @Override
            @SneakyThrows
            public void run() {
                int i = 0;
                while (true) {
                    byte[] header = Util.intToByte(length);
                    byte[] payload = s[i].getBytes(StandardCharsets.UTF_8);
                    byte[] combine = Util.combine(header, payload);
                    ctx.writeAndFlush(Unpooled.wrappedBuffer(combine));
                    if ((i = i + 1) == 4) {
                        i = 0;
                    }
                    TimeUnit.SECONDS.sleep(2L);
                }
            }
        },2L, TimeUnit.SECONDS);
    }
}
