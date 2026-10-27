package com.un4seen.bass;

import java.nio.ByteBuffer;

/* JADX INFO: loaded from: classes2.dex */
public class BASSmix {
    public static final int BASS_ACTIVE_QUEUED = 6;
    public static final int BASS_ACTIVE_WAITING = 5;
    public static final int BASS_ATTRIB_MIXER_LATENCY = 86016;
    public static final int BASS_ATTRIB_MIXER_THREADS = 86017;
    public static final int BASS_ATTRIB_MIXER_VOL = 86018;
    public static final int BASS_ATTRIB_SPLIT_ASYNCBUFFER = 86032;
    public static final int BASS_ATTRIB_SPLIT_ASYNCPERIOD = 86033;
    public static final int BASS_CONFIG_MIXER_BUFFER = 67073;
    public static final int BASS_CONFIG_MIXER_POSEX = 67074;
    public static final int BASS_CONFIG_SPLIT_BUFFER = 67088;
    public static final int BASS_CTYPE_STREAM_MIXER = 67584;
    public static final int BASS_CTYPE_STREAM_SPLIT = 67585;
    public static final int BASS_MIXER_BUFFER = 8192;
    public static final int BASS_MIXER_CHAN_ABSOLUTE = 4096;
    public static final int BASS_MIXER_CHAN_BUFFER = 8192;
    public static final int BASS_MIXER_CHAN_DOWNMIX = 4194304;
    public static final int BASS_MIXER_CHAN_LIMIT = 16384;
    public static final int BASS_MIXER_CHAN_MATRIX = 65536;
    public static final int BASS_MIXER_CHAN_NORAMPIN = 8388608;
    public static final int BASS_MIXER_CHAN_PAUSE = 131072;
    public static final int BASS_MIXER_DOWNMIX = 4194304;
    public static final int BASS_MIXER_END = 65536;
    public static final int BASS_MIXER_ENV_FREQ = 1;
    public static final int BASS_MIXER_ENV_LOOP = 65536;
    public static final int BASS_MIXER_ENV_PAN = 3;
    public static final int BASS_MIXER_ENV_REMOVE = 131072;
    public static final int BASS_MIXER_ENV_VOL = 2;
    public static final int BASS_MIXER_LIMIT = 16384;
    public static final int BASS_MIXER_MATRIX = 65536;
    public static final int BASS_MIXER_NONSTOP = 131072;
    public static final int BASS_MIXER_NORAMPIN = 8388608;
    public static final int BASS_MIXER_NOSPEAKER = 16384;
    public static final int BASS_MIXER_PAUSE = 131072;
    public static final int BASS_MIXER_POSEX = 8192;
    public static final int BASS_MIXER_QUEUE = 32768;
    public static final int BASS_MIXER_RESUME = 4096;
    public static final int BASS_POS_MIXER_DELAY = 5;
    public static final int BASS_POS_MIXER_RESET = 65536;
    public static final int BASS_SPLIT_POS = 8192;
    public static final int BASS_SPLIT_SLAVE = 4096;
    public static final int BASS_SYNC_MIXER_ENVELOPE = 66048;
    public static final int BASS_SYNC_MIXER_ENVELOPE_NODE = 66049;
    public static final int BASS_SYNC_MIXER_QUEUE = 66050;

    public static class BASS_MIXER_NODE {
        public long pos;
        public float value;

        public BASS_MIXER_NODE() {
        }

        public BASS_MIXER_NODE(long j10, float f10) {
            this.pos = j10;
            this.value = f10;
        }
    }

    static {
        System.loadLibrary("bassmix");
    }

    public static native int BASS_Mixer_ChannelFlags(int i7, int i10, int i11);

    public static native int BASS_Mixer_ChannelGetData(int i7, ByteBuffer byteBuffer, int i10);

    public static native long BASS_Mixer_ChannelGetEnvelopePos(int i7, int i10, BASS.FloatValue floatValue);

    public static native int BASS_Mixer_ChannelGetLevel(int i7);

    public static native boolean BASS_Mixer_ChannelGetLevelEx(int i7, float[] fArr, float f10, int i10);

    public static native boolean BASS_Mixer_ChannelGetMatrix(int i7, float[][] fArr);

    public static native int BASS_Mixer_ChannelGetMixer(int i7);

    public static native long BASS_Mixer_ChannelGetPosition(int i7, int i10);

    public static native long BASS_Mixer_ChannelGetPositionEx(int i7, int i10, int i11);

    public static native int BASS_Mixer_ChannelIsActive(int i7);

    public static native boolean BASS_Mixer_ChannelRemove(int i7);

    public static native boolean BASS_Mixer_ChannelRemoveSync(int i7, int i10);

    public static native boolean BASS_Mixer_ChannelSetEnvelope(int i7, int i10, BASS_MIXER_NODE[] bass_mixer_nodeArr, int i11);

    public static native boolean BASS_Mixer_ChannelSetEnvelopePos(int i7, int i10, long j10);

    public static native boolean BASS_Mixer_ChannelSetMatrix(int i7, float[][] fArr);

    public static native boolean BASS_Mixer_ChannelSetMatrixEx(int i7, float[][] fArr, float f10);

    public static native boolean BASS_Mixer_ChannelSetPosition(int i7, long j10, int i10);

    public static native int BASS_Mixer_ChannelSetSync(int i7, int i10, long j10, BASS.SYNCPROC syncproc, Object obj);

    public static native int BASS_Mixer_GetVersion();

    public static native boolean BASS_Mixer_StreamAddChannel(int i7, int i10, int i11);

    public static native boolean BASS_Mixer_StreamAddChannelEx(int i7, int i10, int i11, long j10, long j11);

    public static native int BASS_Mixer_StreamCreate(int i7, int i10, int i11);

    public static native int BASS_Mixer_StreamGetChannels(int i7, int[] iArr, int i10);

    public static native int BASS_Split_StreamCreate(int i7, int i10, int[] iArr);

    public static native int BASS_Split_StreamGetAvailable(int i7);

    public static native int BASS_Split_StreamGetSource(int i7);

    public static native int BASS_Split_StreamGetSplits(int i7, int[] iArr, int i10);

    public static native boolean BASS_Split_StreamReset(int i7);

    public static native boolean BASS_Split_StreamResetEx(int i7, int i10);
}
