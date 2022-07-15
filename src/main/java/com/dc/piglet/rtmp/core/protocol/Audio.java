
package com.dc.piglet.rtmp.core.protocol;


import com.dc.piglet.rtmp.entity.MessageType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 音频
 */
public class Audio extends DataMessage {

    @Override
    public boolean isConfig() { // TODO now hard coded for mp4a
        return data.readableBytes() > 3 && data.getInt(0) == 0xaf001310;
    }

    public Audio(final RtmpHeader header, final ByteBuf in) {
        super(header, in);
    }

    public Audio(final byte[] ... bytes) {
        super(bytes);
    }

    public Audio(final int time, final byte[] prefix, final byte[] audioData) {
        header.setTimestamp(time);
        data = Unpooled.wrappedBuffer(prefix, audioData);
        header.setMsgLength(data.readableBytes());
    }

    public Audio(final int timestamp, final ByteBuf in) {
        super(timestamp, in);
    }
    
    public static Audio empty() {
        Audio empty = new Audio();
        empty.data = Unpooled.EMPTY_BUFFER;
        return empty;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.AUDIO;
    }

}
