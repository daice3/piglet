
package com.dc.piglet.rtmp.core.protocol;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 数据消息 Message Type ID＝15或18
 * 传递一些元数据或用户自定义消息
 */
public abstract class DataMessage extends AbstractMessage {

    private boolean encoded;
    protected ByteBuf data;

    public DataMessage() {
        super();
    }

    public DataMessage(final byte[] ... bytes) {
        data = Unpooled.wrappedBuffer(bytes);
        header.setMsgLength(data.readableBytes());
    }

    public DataMessage(final RtmpHeader header, final ByteBuf in) {
        super(header, in);
    }

    public DataMessage(final int timestamp, final ByteBuf in) {
        header.setTimestamp(timestamp);
        header.setMsgLength(in.readableBytes());
        data = in;
    }

    @Override
    public ByteBuf encode() {
        if(encoded) {
            // in case used multiple times e.g. broadcast
            data.resetReaderIndex();            
        } else {
            encoded = true;
        }
        return data;
    }

    @Override
    public void decode(ByteBuf in) {
        data = in;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public abstract boolean isConfig();

}
