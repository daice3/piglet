
package com.dc.piglet.rtmp.core.io.f4v.box;


import com.dc.piglet.rtmp.core.io.f4v.Payload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.List;
public class FTYP implements Payload {


    private byte[] majorBrand;
    private int minorVersion;
    private List<byte[]> compatibleBrands;

    public FTYP(ByteBuf in) {
        read(in);
    }

    @Override
    public void read(ByteBuf in) {
        majorBrand = new byte[4];
        in.readBytes(majorBrand);        
        minorVersion = in.readInt();        
        compatibleBrands = new ArrayList<byte[]>();
        while (in.isReadable()) {
            final byte[] bytes = new byte[4];
            in.readBytes(bytes);            
            compatibleBrands.add(bytes);
        }
    }

    @Override
    public ByteBuf write() {
        ByteBuf out = Unpooled.buffer(256);
        out.writeBytes(majorBrand);
        out.writeInt(minorVersion);
        for (byte[] bytes : compatibleBrands) {
            out.writeBytes(bytes);
        }        
        return out;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[majorBrand: ").append(new String(majorBrand));
        sb.append(" minorVersion: ").append(minorVersion);
        if(compatibleBrands != null) {
            sb.append('[');
            for(byte[] brand : compatibleBrands) {
                sb.append(new String(brand)).append(' ');
            }
            sb.append(']');
        }
        sb.append(']');
        return super.toString();
    }
    
}
