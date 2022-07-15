
package com.dc.piglet.rtmp.core.protocol;


import com.dc.piglet.rtmp.entity.MessageType;
import com.dc.piglet.rtmp.util.Util;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.Objects;

public class Control extends AbstractMessage {


    public static enum Type{
        
        STREAM_BEGIN(0),
        STREAM_EOF(1),
        STREAM_DRY(2),
        SET_BUFFER(3),
        STREAM_IS_RECORDED(4),
        PING_REQUEST(6),
        PING_RESPONSE(7),
        SWFV_REQUEST(26),
        SWFV_RESPONSE(27),
        BUFFER_EMPTY(31),
        BUFFER_FULL(32);

        private final int value;

        private Type(int value) {
            this.value = value;
        }

        public int intValue() {
            return value;
        }

        public static Type valueToEnum(final int value) {
            Type[] values = Type.values();
            for (Type type : values) {
                if(type.intValue() == value){
                    return type;
                }
            }
            return null;
        }

    }

    private Type type;
    private int streamId;
	private int bufferLength;
    private int time;
    private byte[] bytes;

    public Control(RtmpHeader header, ByteBuf in) {
        super(header, in);
    }

    private Control(Type type, int time) {
        this.type = type;
        this.time = time;
    }

    private Control(int streamId, Type type) {
        this.streamId = streamId;
        this.type = type;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.CONTROL;
    }

    public static Control setBuffer(int streamId, int bufferLength) {
        Control control = new Control(Type.SET_BUFFER, 0);
        control.bufferLength = bufferLength;
        control.streamId = streamId;
        return control;
    }

    public static Control pingRequest(int time) {
        return new Control(Type.PING_REQUEST, time);
    }

    public static Control pingResponse(int time) {
        return new Control(Type.PING_RESPONSE, time);
    }

    public static Control swfvResponse(byte[] bytes) {
        Control control = new Control(Type.SWFV_RESPONSE, 0);
        control.bytes = bytes;
        return control;
    }

    public static Control streamBegin(int streamId) {
        Control control = new Control(Type.STREAM_BEGIN, 0);
        control.streamId = streamId;
        return control;
    }

    public static Control streamIsRecorded(int streamId) {
        return new Control(streamId, Type.STREAM_IS_RECORDED);
    }

    public static Control streamEof(int streamId) {
        return new Control(streamId, Type.STREAM_EOF);
    }

    public static Control bufferEmpty(int streamId) {
        return new Control(streamId, Type.BUFFER_EMPTY);
    }

    public static Control bufferFull(int streamId) {
        return new Control(streamId, Type.BUFFER_FULL);
    }

    public Type getType() {
        return type;
    }

    public int getTime() {
        return time;
    }

    public int getBufferLength() {
        return bufferLength;
    }

    @Override
    public ByteBuf encode() {
        final int size;
        switch(type) {
            case SWFV_RESPONSE: size = 44; break;
            case SET_BUFFER: size = 10; break;
            default: size = 6;
        }
        ByteBuf out = Unpooled.buffer(size);
        out.writeShort((short) type.value);
        switch(type) {
            case STREAM_BEGIN:
            case STREAM_EOF:
            case STREAM_DRY:
            case STREAM_IS_RECORDED:
            case BUFFER_EMPTY:
            case BUFFER_FULL:
                out.writeInt(streamId);
                break;
            case SET_BUFFER:
                out.writeInt(streamId);
                out.writeInt(bufferLength);
                break;
            case PING_REQUEST:
            case PING_RESPONSE:
                out.writeInt(time);
                break;
            case SWFV_RESPONSE:
                out.writeBytes(bytes);
                break;
            case SWFV_REQUEST:
            default:
                break;
        }
        return out;
    }

    @Override
    public void decode(ByteBuf in) {
        type = Type.valueToEnum(in.readShort());
        switch(Objects.requireNonNull(type)) {
            case STREAM_BEGIN:
            case STREAM_EOF:
            case STREAM_DRY:
            case STREAM_IS_RECORDED:
            case BUFFER_EMPTY:
            case BUFFER_FULL:
                streamId = in.readInt();
                break;
            case SET_BUFFER:
                streamId = in.readInt();
                bufferLength = in.readInt();
                break;
            case PING_REQUEST:
            case PING_RESPONSE:
                time = in.readInt();
                break;
            case SWFV_REQUEST:
                // only type (2 bytes)
                break;
            case SWFV_RESPONSE:
                bytes = new byte[42];
                in.readBytes(bytes);
                break;
            default:
                break;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append(type);
        sb.append(" streamId: ").append(streamId);
        switch(type) {
            case SET_BUFFER:
                sb.append(" bufferLength: ").append(bufferLength);
                break;
            case PING_REQUEST:
            case PING_RESPONSE:
                sb.append(" time: ").append(time);
                break;
        }
        if(bytes != null) {
            sb.append(" bytes: ").append(Util.toHex(bytes));
        }
        return sb.toString();
    }

    public int getStreamId() {
		return streamId;
	}

}
