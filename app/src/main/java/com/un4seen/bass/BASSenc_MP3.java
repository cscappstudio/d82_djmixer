package com.un4seen.bass;

/* JADX INFO: loaded from: classes2.dex */
public class BASSenc_MP3 {
    static {
        System.loadLibrary("bassenc");
        System.loadLibrary("bassenc_mp3");
    }

    public static native int BASS_Encode_MP3_GetVersion();

    public static native int BASS_Encode_MP3_Start(int i7, String str, int i10, BASSenc.ENCODEPROCEX encodeprocex, Object obj);

    public static native int BASS_Encode_MP3_StartFile(int i7, String str, int i10, String str2);
}
