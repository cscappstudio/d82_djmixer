package com.un4seen.bass;

/* JADX INFO: loaded from: classes2.dex */
public class BASSEncrypt {
    static {
        System.loadLibrary("bass_encrypt");
    }

    public static native int BASS_StreamCreateFile(String str, int i7);
}
