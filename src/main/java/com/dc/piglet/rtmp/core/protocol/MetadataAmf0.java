
package com.dc.piglet.rtmp.core.protocol;


import com.dc.piglet.rtmp.core.protocol.amf.Amf0Value;
import com.dc.piglet.rtmp.entity.MessageType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.List;

public class MetadataAmf0 extends Metadata {        

    public MetadataAmf0(String name, Object... data) {
        super(name, data);
    }

    public MetadataAmf0(RtmpHeader header, ByteBuf in) {
        super(header, in);
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.METADATA_AMF0;
    }

    @Override
    public ByteBuf encode() {
        ByteBuf out = Unpooled.buffer(256);
        Amf0Value.encode(out, name);
        Amf0Value.encode(out, data);
        return out;
    }

    @Override
    public void decode(ByteBuf in) {
        name = (String) Amf0Value.decode(in);
        List<Object> list = new ArrayList<Object>();
        while(in.isReadable()) {
            list.add(Amf0Value.decode(in));
        }
        data = list.toArray();
    }

}
