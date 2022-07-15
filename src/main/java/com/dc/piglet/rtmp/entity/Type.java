package com.dc.piglet.rtmp.entity;

/**
 * basic header解码得出的type
 * @author daice
 */

public enum Type {
    /**
     * message header可表示所有的类型，占用11个字节
     */
    ALL(0),
    /**
     * message header省去msg stream id,表示其与上一个chunk在同一个msg，占用7个字节
     */
    MID(1),
    /**
     * 相比于MID省去了表示消息长度的3个字节和表示消息类型的1个字节
     */
    SMALL(2),
    /**
     * 0字节，表示和上一个chunk的header完全相同
     */
    TINY(3);

    Type(int i) {
        this.id = i;
    }

    private int id;

    public static Type convert(int i){
        Type[] types = Type.values();
        for (Type type : types) {
            if(i == type.id){
                return type;
            }
        }
        return null;
    }

    public int getId() {
        return id;
    }
}
