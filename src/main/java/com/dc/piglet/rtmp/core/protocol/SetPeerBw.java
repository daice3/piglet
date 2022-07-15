
package com.dc.piglet.rtmp.core.protocol;


import com.dc.piglet.rtmp.entity.MessageType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class SetPeerBw extends AbstractMessage {

    public static enum LimitType {
        HARD, // 0
        SOFT, // 1
        DYNAMIC // 2
    }

    private int value;
    private LimitType limitType;

    public SetPeerBw(RtmpHeader header, ByteBuf in) {
        super(header, in);
    }

    public SetPeerBw(int value, LimitType limitType) {
        this.value = value;
        this.limitType = limitType;
    }

    public static SetPeerBw dynamic(int value) {
        return new SetPeerBw(value, LimitType.DYNAMIC);
    }

    public static SetPeerBw hard(int value) {
        return new SetPeerBw(value, LimitType.HARD);
    }

    public int getValue() {
        return value;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.SET_PEER_BW;
    }

    @Override
    public ByteBuf encode() {
        ByteBuf out = Unpooled.buffer(5);
        out.writeInt(value);
        out.writeByte((byte) limitType.ordinal());
        return out;
    }

    @Override
    public void decode(ByteBuf in) {
        value = in.readInt();
        limitType = LimitType.values()[in.readByte()];
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append("windowSize: ").append(value);
        sb.append(" limitType: ").append(limitType);
        return sb.toString();
    }

}
