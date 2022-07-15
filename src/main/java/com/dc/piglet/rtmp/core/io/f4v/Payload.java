
package com.dc.piglet.rtmp.core.io.f4v;

import io.netty.buffer.ByteBuf;

public interface Payload {

    void read(ByteBuf in);

    ByteBuf write();

}
