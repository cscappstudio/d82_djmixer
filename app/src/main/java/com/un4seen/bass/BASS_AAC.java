package com.un4seen.bass;

import android.os.ParcelFileDescriptor;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes2.dex */
public class BASS_AAC {
    public static final int BASS_AAC_FRAME960 = 4096;
    public static final int BASS_AAC_STEREO = 4194304;
    public static final int BASS_CONFIG_AAC_MP4 = 67329;
    public static final int BASS_CONFIG_AAC_PRESCAN = 67330;
    public static final int BASS_CONFIG_MP4_VIDEO = 67328;
    public static final int BASS_CTYPE_STREAM_AAC = 68352;
    public static final int BASS_CTYPE_STREAM_MP4 = 68353;
    public static final int BASS_POS_TRACK = 4;

    static {
        System.loadLibrary("bass_aac");
    }

    public static native int BASS_AAC_StreamCreateFile(ParcelFileDescriptor parcelFileDescriptor, long j10, long j11, int i7);

    public static native int BASS_AAC_StreamCreateFile(BASS.Asset asset, long j10, long j11, int i7);

    public static native int BASS_AAC_StreamCreateFile(String str, long j10, long j11, int i7);

    public static native int BASS_AAC_StreamCreateFile(ByteBuffer byteBuffer, long j10, long j11, int i7);

    public static native int BASS_AAC_StreamCreateFileUser(int i7, int i10, BASS.BASS_FILEPROCS bass_fileprocs, Object obj);

    public static native int BASS_AAC_StreamCreateURL(String str, int i7, int i10, BASS.DOWNLOADPROC downloadproc, Object obj);
}
