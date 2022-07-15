
package com.dc.piglet.rtmp.core.io.f4v;


import io.netty.buffer.ByteBuf;
import com.dc.piglet.rtmp.core.io.f4v.box.*;

public enum BoxType {

    FTYP,
    MOOV,
    /**/ MVHD,
    /**/ TRAK,
    /*    */ TKHD,
    /*    */ MDIA,
    /*        */ MDHD,
    /*        */ HDLR,
    /*        */ MINF,
    /*           */ VMHD,
    /*           */ SMHD,
    /*           */ DINF,
    /*           */ STBL,
    /*               */ STSD,
    /*               */ STTS,
    /*               */ CTTS,
    /*               */ STSC,
    /*               */ STSZ,
    /*               */ STCO,
    /*               */ CO64,
    /*               */ STSS,
    MDAT,           //======
    UNKNOWN;

    private String typeString;

    public void setTypeString(String typeString) {
        this.typeString = typeString;
    }

    public BoxType[] getChildren() {
        switch(this) {            
            case MOOV: return array(MVHD, TRAK);
            case TRAK: return array(TKHD, MDIA);
            case MDIA: return array(MDHD, HDLR, MINF);
            case MINF: return array(VMHD, SMHD, DINF, STBL);
            case STBL: return array(STSD, STTS, CTTS, STSC, STSZ, STCO, CO64, STSS);
            default: return null;
        }
    }

    public Payload read(ByteBuf in) {
        switch(this) {
            case FTYP: return new FTYP(in);
            case MVHD: return new MVHD(in);
            case TKHD: return new TKHD(in);
            case MDHD: return new MDHD(in);
            case STSD: return new STSD(in);
            case STTS: return new STTS(in);
            case CTTS: return new CTTS(in);
            case STSC: return new STSC(in);
            case STSZ: return new STSZ(in);
            case STCO: return new STCO(in);
            case CO64: return new STCO(in, true);
            case STSS: return new STSS(in);            
            default: return new UnknownPayload(in, this);
        }
    }

    private static BoxType[] array(BoxType ... types) {
        return types;
    }

    public static BoxType parse(String typeString) {
        BoxType type;
        try {
            type = BoxType.valueOf(typeString.toUpperCase());
        } catch(Exception e) {
            type = UNKNOWN;
        }
        type.setTypeString(typeString);
        return type;
    }

    @Override
    public String toString() {
        if(this != UNKNOWN) {
            return super.toString();
        }
        return "(" + typeString + ")";
    }
    
}
