
package com.dc.piglet.rtmp.core.protocol;


import com.dc.piglet.rtmp.entity.MessageType;
import io.netty.buffer.ByteBuf;

public class Aggregate extends DataMessage {

    public Aggregate(RtmpHeader header, ByteBuf in) {
        super(header, in);
    }

    public Aggregate(int time, ByteBuf in) {
        super();
        header.setTimestamp(time);
        data = in;
        header.setMsgLength(data.readableBytes());
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.AGGREGATE;
    }

    @Override
    public boolean isConfig() {
        return false;
    }

}
