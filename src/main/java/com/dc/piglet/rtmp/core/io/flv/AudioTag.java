
package com.dc.piglet.rtmp.core.io.flv;


public class AudioTag {

    private final CodecType codecType;
    private final SampleRate sampleRate;
    private final boolean sampleSize16Bit;
    private final boolean stereo;

    public AudioTag(final byte byteValue) {
        final int unsigned = 0xFF & byteValue;
        codecType = CodecType.valueToEnum(unsigned >> 4);
        sampleSize16Bit = (0x02 & unsigned) > 0;
        if(codecType == CodecType.AAC) {
            sampleRate = SampleRate.KHZ_44;
            stereo = true;
            return;
        }
        sampleRate = SampleRate.valueToEnum((0x0F & unsigned) >> 2);
        stereo = (0x01 & unsigned) > 0;
    }

    public CodecType getCodecType() {
        return codecType;
    }

    public SampleRate getSampleRate() {
        return sampleRate;
    }

    public boolean isSampleSize16Bit() {
        return sampleSize16Bit;
    }

    public boolean isStereo() {
        return stereo;
    }

    public static enum CodecType{

        ADPCM(1),
        MP3(2),
        PCM(3),
        NELLY_16(4),
        NELLY_8(5),
        NELLY(6),
        G711_A(7),
        G711_U(8),
        RESERVED(9),
        AAC(10),
        SPEEX(11),
        MP3_8(14),
        DEVICE_SPECIFIC(15);

        private final int value;

        CodecType(final int value) {
            this.value = value;
        }

        public int intValue() {
            return value;
        }

        public static CodecType valueToEnum(final int value) {
            CodecType[] values = CodecType.values();
            for (CodecType codecType : values) {
                if(codecType.value == value){
                    return codecType;
                }
            }
            return null;
        }

    }

    public static enum SampleRate {

        KHZ_5(0),
        KHZ_11(1),
        KHZ_22(2),
        KHZ_44(3);

        private final int value;

        SampleRate(final int value) {
            this.value = value;
        }

        public int intValue() {
            return value;
        }


        public static SampleRate valueToEnum(final int value) {
            SampleRate[] values = SampleRate.values();
            for (SampleRate sampleRate : values) {
                if(sampleRate.value == value){
                    return sampleRate;
                }
            }
            return null;
        }

    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("[format: ").append(codecType);
        sb.append(", sampleRate: ").append(sampleRate);
        sb.append(", sampleSize16bit: ").append(sampleSize16Bit);
        sb.append(", stereo: ").append(stereo);
        sb.append(']');
        return sb.toString();
    }

}
