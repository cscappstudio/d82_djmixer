package com.un4seen.bass;

import android.os.ParcelFileDescriptor;
import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes2.dex */
public class BASSFLAC {
    public static final int BASS_CTYPE_STREAM_FLAC = 67840;
    public static final int BASS_CTYPE_STREAM_FLAC_OGG = 67841;
    public static final int BASS_TAG_FLAC_CUE = 12;
    public static final int BASS_TAG_FLAC_METADATA = 74752;
    public static final int BASS_TAG_FLAC_PICTURE = 73728;
    public static final int TAG_FLAC_CUE_TRACK_DATA = 1;
    public static final int TAG_FLAC_CUE_TRACK_PRE = 2;

    public static class TAG_FLAC_CUE {
        public String catalog;
        public boolean iscd;
        public int leadin;
        public int ntracks;
        public TAG_FLAC_CUE_TRACK[] tracks;
    }

    public static class TAG_FLAC_CUE_TRACK {
        public int flags;
        public TAG_FLAC_CUE_TRACK_INDEX[] indexes;
        public String isrc;
        public int nindexes;
        public int number;
        public long offset;
    }

    public static class TAG_FLAC_CUE_TRACK_INDEX {
        public int number;
        public long offset;
    }

    public static class TAG_FLAC_METADATA {
        public ByteBuffer data;

        /* JADX INFO: renamed from: id, reason: collision with root package name */
        public String f5418id;
        public int length;
    }

    public static class TAG_FLAC_PICTURE {
        public int apic;
        public int colors;
        public ByteBuffer data;
        public int depth;
        public String desc;
        public int height;
        public int length;
        public String mime;
        public int width;
    }

    static {
        System.loadLibrary("bassflac");
    }

    public static native int BASS_FLAC_StreamCreateFile(ParcelFileDescriptor parcelFileDescriptor, long j10, long j11, int i7);

    public static native int BASS_FLAC_StreamCreateFile(BASS.Asset asset, long j10, long j11, int i7);

    public static native int BASS_FLAC_StreamCreateFile(String str, long j10, long j11, int i7);

    public static native int BASS_FLAC_StreamCreateFile(ByteBuffer byteBuffer, long j10, long j11, int i7);

    public static native int BASS_FLAC_StreamCreateFileUser(int i7, int i10, BASS.BASS_FILEPROCS bass_fileprocs, Object obj);

    public static native int BASS_FLAC_StreamCreateURL(String str, int i7, int i10, BASS.DOWNLOADPROC downloadproc, Object obj);
}
