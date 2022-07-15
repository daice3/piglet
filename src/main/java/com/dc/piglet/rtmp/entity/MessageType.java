package com.dc.piglet.rtmp.entity;

import com.dc.piglet.rtmp.core.protocol.*;
import io.netty.buffer.ByteBuf;

/**
 * 协议控制消息
 * @author daice
 */
public enum MessageType {
    /**
     * 设置chunk的最大字节数，默认128b
     */
    CHUNK_SIZE(0x01),
    /**
     * 丢弃该CSID的所有已接收到的chunk
     */
    ABORT(0x02),
    /**
     * 窗口大小，当收到的数据等于该值时发送ack告知对方可继续发送数据
     */
    BYTES_READ(0x03),
    CONTROL(0x04),
    /**
     * 发送端在接收到接受端返回的两个ACK间最多可以发送的字节数
     */
    WINDOW_ACK_SIZE(0x05),
    /**
     * 限制对端的输出带宽
     */
    SET_PEER_BW(0x06),
    /**
     * 音频
     */
    AUDIO(0x08),
    /**
     * 视频
     */
    VIDEO(0x09),
    METADATA_AMF3(0x0F),
    SHARED_OBJECT_AMF3(0x10),
    /**
     * AMF3时的操作命令
     */
    COMMAND_AMF3(0x11),
    METADATA_AMF0(0x12),
    SHARED_OBJECT_AMF0(0x13),
    /**
     * AMF0时的操作命令
     */
    COMMAND_AMF0(0x14),
    /**
     * 聚集信息
     */
    AGGREGATE(0x16);

    private int id;

    MessageType(int i) {
        this.id = i;
    }

    public static MessageType convert(int id){
        MessageType[] values = MessageType.values();
        for (MessageType value : values) {
            if(value.id == id){
                return value;
            }
        }
        return null;
    }

    public int getId(){
        return this.id;
    }

    /**
     * 根据msgType解码
     */
    public static RtmpMessage decode(final RtmpHeader header, final ByteBuf in) {
        switch(header.getMsgType()) {
            case ABORT: return new Abort(header, in);
            case BYTES_READ: return new BytesRead(header, in);
            case CHUNK_SIZE: return new ChunkSize(header, in);
            case COMMAND_AMF0: return new CommandAmf0(header, in);
            case METADATA_AMF0: return new MetadataAmf0(header, in);
            case CONTROL: return new Control(header, in);
            case WINDOW_ACK_SIZE: return new WindowAckSize(header, in);
            case SET_PEER_BW: return new SetPeerBw(header, in);
            case AUDIO: return new Audio(header, in);
            case VIDEO: return new Video(header, in);
            case AGGREGATE: return new Aggregate(header, in);
            case SHARED_OBJECT_AMF0:
            case SHARED_OBJECT_AMF3:
                //return new SharedObjectMessage(header, in);
            default: throw new RuntimeException("unable to create message for: " + header);
        }
    }

    public int getDefaultChannelId() {
        switch(this) {
            case CHUNK_SIZE:
            case CONTROL:
            case ABORT:
            case BYTES_READ:
            case WINDOW_ACK_SIZE:
            case SET_PEER_BW:
                return 2;
            case SHARED_OBJECT_AMF0:
            case SHARED_OBJECT_AMF3:
            case COMMAND_AMF0:
            case COMMAND_AMF3:
                return 3;
            case METADATA_AMF0:
            case METADATA_AMF3:
            case AUDIO:
            case VIDEO:
            case AGGREGATE:
            default:
                return 5;
        }
    }

    public static MessageType valueToEnum(final int value) {
        MessageType[] values = MessageType.values();
        for (MessageType messageType : values) {
            if(messageType.getId() == value){
                return messageType;
            }
        }
        return null;
    }
}
