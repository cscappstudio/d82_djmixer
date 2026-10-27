package com.un4seen.bass;

import android.os.ParcelFileDescriptor;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes2.dex */
public class BASSAPE {
    public static final int BASS_CTYPE_STREAM_APE = 67328;

    static {
        System.loadLibrary("bassape");
    }

    public static native int BASS_APE_StreamCreateFile(ParcelFileDescriptor parcelFileDescriptor, long j10, long j11, int i7);

    public static native int BASS_APE_StreamCreateFile(BASS.Asset asset, long j10, long j11, int i7);

    public static native int BASS_APE_StreamCreateFile(String str, long j10, long j11, int i7);

    public static native int BASS_APE_StreamCreateFile(ByteBuffer byteBuffer, long j10, long j11, int i7);

    public static native int BASS_APE_StreamCreateFileUser(int i7, int i10, BASS.BASS_FILEPROCS bass_fileprocs, Object obj);

    public static native int BASS_APE_StreamCreateURL(String str, int i7, int i10, BASS.DOWNLOADPROC downloadproc, Object obj);
}
