
package com.dc.piglet.rtmp.core.protocol;


import com.dc.piglet.rtmp.entity.MessageType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class BytesRead extends AbstractMessage {

    private int value;

    @Override
    public MessageType getMessageType() {
        return MessageType.BYTES_READ;
    }

    public BytesRead(RtmpHeader header, ByteBuf in) {
        super(header, in);
    }

    public BytesRead(long bytesRead) {        
        this.value = (int) bytesRead;
    }

    public int getValue() {
        return value;
    }

    @Override
    public ByteBuf encode() {
        ByteBuf out = Unpooled.buffer(4);
        out.writeInt(value);
        return out;
    }

    @Override
    public void decode(ByteBuf in) {
        value = in.readInt();
    }

    @Override
    public String toString() {
        return super.toString() + value;
    }

}
