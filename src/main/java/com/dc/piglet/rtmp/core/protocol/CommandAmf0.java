
package com.dc.piglet.rtmp.core.protocol;


import com.dc.piglet.rtmp.core.protocol.amf.Amf0Object;
import com.dc.piglet.rtmp.core.protocol.amf.Amf0Value;
import com.dc.piglet.rtmp.entity.MessageType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.List;

/**
 * amf0格式指令
 */
public class CommandAmf0 extends Command {

    public CommandAmf0(RtmpHeader header, ByteBuf in) {
        super(header, in);        
    }

    public CommandAmf0(int transactionId, String name, Amf0Object object, Object ... args) {
        super(transactionId, name, object, args);
    }

    public CommandAmf0(String name, Amf0Object object, Object ... args) {
        super(name, object, args);
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.COMMAND_AMF0;
    }

    @Override
    public ByteBuf encode() {
        ByteBuf out = Unpooled.buffer(256);
        Amf0Value.encode(out, name, transactionId, object);
        if(args != null) {
            for(Object o : args) {
                Amf0Value.encode(out, o);
            }
        }
        return out;
    }

    @Override
    public void decode(ByteBuf in) {                
        name = (String) Amf0Value.decode(in);
        transactionId = ((Double) Amf0Value.decode(in)).intValue();
        object = (Amf0Object) Amf0Value.decode(in);
        List<Object> list = new ArrayList<Object>();
        while(in.isReadable()) {
            list.add(Amf0Value.decode(in));
        }
        args = list.toArray();
    }

}
