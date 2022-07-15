
package com.dc.piglet.rtmp.core.protocol;


import io.netty.buffer.ByteBuf;

public interface RtmpMessage {

    RtmpHeader getHeader();
    
    ByteBuf encode();

    void decode(ByteBuf in);

}
