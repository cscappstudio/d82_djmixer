package com.un4seen.bass;

import android.os.ParcelFileDescriptor;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes2.dex */
public class BASSOPUS {
    public static final int BASS_ATTRIB_OPUS_GAIN = 77825;
    public static final int BASS_ATTRIB_OPUS_ORIGFREQ = 77824;
    public static final int BASS_CTYPE_STREAM_OPUS = 70144;
    public static final int BASS_STREAMPROC_OPUS_LOSS = 1073741824;

    public static class BASS_OPUS_HEAD {
        public byte[] chanmap;
        public byte channels;
        public byte coupled;
        public short gain;
        public int inputrate;
        public byte mapping;
        public short preskip;
        public byte streams;
        public byte version;
    }

    static {
        System.loadLibrary("bassopus");
    }

    public static native int BASS_OPUS_StreamCreate(BASS_OPUS_HEAD bass_opus_head, int i7, int i10, Object obj);

    public static native int BASS_OPUS_StreamCreate(BASS_OPUS_HEAD bass_opus_head, int i7, BASS.STREAMPROC streamproc, Object obj);

    public static native int BASS_OPUS_StreamCreateFile(ParcelFileDescriptor parcelFileDescriptor, long j10, long j11, int i7);

    public static native int BASS_OPUS_StreamCreateFile(BASS.Asset asset, long j10, long j11, int i7);

    public static native int BASS_OPUS_StreamCreateFile(String str, long j10, long j11, int i7);

    public static native int BASS_OPUS_StreamCreateFile(ByteBuffer byteBuffer, long j10, long j11, int i7);

    public static native int BASS_OPUS_StreamCreateFileUser(int i7, int i10, BASS.BASS_FILEPROCS bass_fileprocs, Object obj);

    public static native int BASS_OPUS_StreamCreateURL(String str, int i7, int i10, BASS.DOWNLOADPROC downloadproc, Object obj);

    public static native int BASS_OPUS_StreamPutData(int i7, ByteBuffer byteBuffer, int i10);
}
