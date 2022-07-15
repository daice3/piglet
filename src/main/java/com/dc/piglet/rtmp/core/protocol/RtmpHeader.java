package com.dc.piglet.rtmp.core.protocol;

import com.dc.piglet.rtmp.entity.MessageType;
import com.dc.piglet.rtmp.entity.Type;
import com.dc.piglet.rtmp.util.Util;
import io.netty.buffer.ByteBuf;

import java.util.Objects;

/**
 *  basic header | message header | extended timestamp |
 *  basic header : chunkType 2 bit & csId
 *  message header : based chunkType & msgType & msgLength & timestamp  & streamId
 *  msgLength : 并不是chunkData的长度，而是chunk属于的message的总长度
 *
 */
public class RtmpHeader {


    private int timestamp;
    private int deltaTime;
    private int msgLength;
    private MessageType msgType;
    private int streamId;
    private int csId;
    private Type chunkType;

    public static final int MAX_NORMAL_HEADER_TIME = 0xFFFFFF;
    public static final int MAX_CHANNEL_ID = 65600;
    public static final int MAX_ENCODED_SIZE = 18;

    public RtmpHeader(MessageType messageType, int timestamp, int msgLength) {
        this(messageType);
        this.timestamp = timestamp;
        this.msgLength = msgLength;
    }

    public RtmpHeader(MessageType messageType) {
        this.msgType = messageType;
        chunkType = Type.ALL;
        csId = messageType.getDefaultChannelId();
    }

    /**
     *
     * @param in
     * @param incomeHeaders 已经处理的header数组
     */
    public RtmpHeader(ByteBuf in, RtmpHeader[] incomeHeaders){
        //根据第一个字节来获取chunkType
        final int fb = in.readByte();
        final int basicHeader;
        final int chunkTypeInt;
        //取出低六位
        if((fb & 0x3f) == 0){
            basicHeader = (fb & 0xff) << 8 | (in.readByte() & 0xff);
            csId = 64 + (basicHeader & 0xff);
            chunkTypeInt = basicHeader >> 14;
        }else if((fb & 0x3f) == 1){
            basicHeader = (fb & 0xff) << 16 | (in.readByte() & 0xff) << 8 | (in.readByte() & 0xff);
            csId = 64 + ((basicHeader >> 8) & 0xff) + ((basicHeader & 0xff) << 8);
            chunkTypeInt = basicHeader >> 22;
        }else{
            basicHeader = fb & 0xff;
            csId = (basicHeader & 0x3f);
            chunkTypeInt = basicHeader >> 6;
        }
        chunkType = Type.convert(chunkTypeInt);
        final RtmpHeader preHeader = incomeHeaders[csId];
        switch (Objects.requireNonNull(chunkType)){
            case ALL:
                timestamp = in.readMedium();
                msgLength = in.readMedium();
                msgType = MessageType.convert(in.readByte());
                streamId = Util.readInt32Reverse(in);
                if(timestamp == MAX_NORMAL_HEADER_TIME){
                    timestamp = in.readInt();
                }
                break;
            case MID:
                deltaTime = in.readMedium();
                msgLength = in.readMedium();
                msgType = MessageType.convert(in.readByte());
                streamId = preHeader.streamId;
                if(deltaTime == MAX_NORMAL_HEADER_TIME) {
                    deltaTime = in.readInt();
                }
                break;
            case SMALL:
                deltaTime = in.readMedium();
                msgLength = preHeader.msgLength;
                msgType = preHeader.msgType;
                streamId = preHeader.streamId;
                if(deltaTime == MAX_NORMAL_HEADER_TIME) {
                    deltaTime = in.readInt();
                }
                break;
            case TINY:
                chunkType = preHeader.chunkType;
                deltaTime = preHeader.deltaTime;
                timestamp = preHeader.timestamp;
                msgLength = preHeader.msgLength;
                msgType = preHeader.msgType;
                streamId = preHeader.streamId;
                break;
            default:
                break;
        }
    }

    public boolean isMedia() {
        switch(msgType) {
            case AUDIO:
            case VIDEO:
            case AGGREGATE:
                return true;
            default:
                return false;
        }
    }

    public boolean isMetadata() {
        return msgType == MessageType.METADATA_AMF0
                || msgType == MessageType.METADATA_AMF3;
    }


    public void encode(ByteBuf out) {
        out.writeBytes(encodeHeaderTypeAndChannel(chunkType.getId(), csId));
        if(chunkType == Type.TINY) {
            return;
        }
        final boolean extendedTime;
        if(chunkType == Type.ALL) {
            extendedTime = timestamp >= MAX_NORMAL_HEADER_TIME;
        } else {
            extendedTime = deltaTime >= MAX_NORMAL_HEADER_TIME;
        }
        if(extendedTime) {
            out.writeMedium(MAX_NORMAL_HEADER_TIME);
        } else {
            out.writeMedium(chunkType == Type.ALL ? timestamp : deltaTime);
        }
        if(chunkType != Type.SMALL) {
            out.writeMedium(msgLength);
            out.writeByte((byte) msgType.getId());
            if(chunkType == Type.ALL) {
                Util.writeInt32Reverse(out, streamId);
            }
        }
        if(extendedTime) {
            out.writeInt(chunkType == Type.ALL ? timestamp : deltaTime);
        }
    }

    public byte[] getTinyHeader() {
        return encodeHeaderTypeAndChannel(Type.TINY.getId(), csId);
    }

    private static byte[] encodeHeaderTypeAndChannel(final int headerType, final int channelId) {
        if (channelId <= 63) {
            return new byte[] {(byte) ((headerType << 6) + channelId)};
        } else if (channelId <= 320) {
            return new byte[] {(byte) (headerType << 6), (byte) (channelId - 64)};
        } else {
            return new byte[] {(byte) ((headerType << 6) | 1),
                    (byte) ((channelId - 64) & 0xff), (byte) ((channelId - 64) >> 8)};
        }
    }

    public boolean isAggregate() {
        return msgType == MessageType.AGGREGATE;
    }

    public boolean isAudio() {
        return msgType == MessageType.AUDIO;
    }

    public boolean isVideo() {
        return msgType == MessageType.VIDEO;
    }

    public boolean isLarge() {
        return chunkType == Type.ALL;
    }

    public boolean isControl() {
        return msgType == MessageType.CONTROL;
    }

    public boolean isChunkSize() {
        return msgType == MessageType.CHUNK_SIZE;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public int getDeltaTime() {
        return deltaTime;
    }

    public void setDeltaTime(int deltaTime) {
        this.deltaTime = deltaTime;
    }

    public int getMsgLength() {
        return msgLength;
    }

    public void setMsgLength(int msgLength) {
        this.msgLength = msgLength;
    }

    public MessageType getMsgType() {
        return msgType;
    }

    public void setMsgType(MessageType msgType) {
        this.msgType = msgType;
    }

    public int getStreamId() {
        return streamId;
    }

    public void setStreamId(int streamId) {
        this.streamId = streamId;
    }

    public int getCsId() {
        return csId;
    }

    public void setCsId(int csId) {
        this.csId = csId;
    }

    public Type getChunkType() {
        return chunkType;
    }

    public void setChunkType(Type chunkType) {
        this.chunkType = chunkType;
    }

    @Override
    public String toString() {
        return "RtmpHeader{" +
                "msgLength=" + msgLength +
                ", msgType=" + msgType +
                ", csId=" + csId +
                '}';
    }
}
