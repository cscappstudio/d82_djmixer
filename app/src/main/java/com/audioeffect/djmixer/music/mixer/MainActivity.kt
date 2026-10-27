package com.audioeffect.djmixer.music.mixer

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.SoundPool
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import androidx.core.widget.TextViewCompat
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.audioeffect.djmixer.music.mixer.core.utils.EnumSelectLanguage
import com.audioeffect.djmixer.music.mixer.core.utils.SystemUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.ijoysoft.mix.view.BPMSeekBar
import com.ijoysoft.mix.view.BpmSettingPreviewView
import com.ijoysoft.mix.view.CrossFadeSeekBar
import com.ijoysoft.mix.view.CustomSeekBar
import com.ijoysoft.mix.view.RotateAlbumView
import com.ijoysoft.mix.view.SelectBox
import com.ijoysoft.mix.view.VolumeSeekBar
import com.ijoysoft.mix.view.WaveProgressView
import com.ijoysoft.mix.view.WaveView
import com.ijoysoft.mix.view.visualizer.VisualizerView
import com.ijoysoft.music.view.RotateStepBar
import com.un4seen.bass.BASS
import com.un4seen.bass.BASSEncrypt
import com.audioeffect.djmixer.music.mixer.core.ads.AdsManager
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.audioeffect.djmixer.music.mixer.core.utils.SystemUtil.wrapContext(newBase))
    }

    private val tag = "DJMixer"

    // Audio stream channels (Deck A and Deck B)
    private var mediaPlayerA: MediaPlayer? = null
    private var mediaPlayerB: MediaPlayer? = null
    private var durationMsA: Int = 0
    private var durationMsB: Int = 0
    private var channelA: Int = 0
    private var channelB: Int = 0

    private var isPlayingA = false
    private var isPlayingB = false

    private var volumeA = 0.8f
    private var volumeB = 0.8f
    private var isMutedA = false
    private var isMutedB = false
    private var crossfadeLeftGain = 1.0f
    private var crossfadeRightGain = 1.0f

    // Pitch & BPM
    private var baseBpmA = 128.0f
    private var baseBpmB = 128.0f
    private var pitchFactorA = 1.0f
    private var pitchFactorB = 1.0f
    private var isKeyLockA = false
    private var isKeyLockB = false

    // Tap BPM timestamps
    private val tapTimesA = mutableListOf<Long>()
    private val tapTimesB = mutableListOf<Long>()

    // Cue points
    private var tempCueA: Long = 0L
    private var tempCueB: Long = 0L
    private val hotCuesA = LongArray(6) { -1L }
    private val hotCuesB = LongArray(6) { -1L }
    private var isCueDeleteModeLeft = false
    private var isCueDeleteModeRight = false

    // Loop points
    private var loopStartA: Long = -1L
    private var loopEndA: Long = -1L
    private var isLoopingA = false
    private var currentLoopBeatsA = 0
    private var loopStartB: Long = -1L
    private var loopEndB: Long = -1L
    private var isLoopingB = false
    private var currentLoopBeatsB = 0

    // Recording
    private var isRecording = false
    private var recordStartTime = 0L

    // Turntables & Containers
    private lateinit var containerLeft: FrameLayout
    private lateinit var containerRight: FrameLayout
    private lateinit var turntableRootLeft: View
    private lateinit var turntableRootRight: View
    private lateinit var turntableLeft: RotateAlbumView
    private lateinit var turntableRight: RotateAlbumView

    // Volume & Crossfade controls
    private lateinit var volSeekBarLeft: VolumeSeekBar
    private lateinit var volSeekBarRight: VolumeSeekBar
    private lateinit var crossFadeSeekBar: CrossFadeSeekBar
    private lateinit var soundMuteLeft: SelectBox
    private lateinit var soundMuteRight: SelectBox

    // Visualizers (VU meters)
    private lateinit var vuLeft: VisualizerView
    private lateinit var vuRight: VisualizerView

    // Bottom Transport buttons
    private lateinit var btnPlayLeft: View
    private lateinit var btnPlayRight: View
    private lateinit var btnAddMusicLeft: View
    private lateinit var btnAddMusicRight: View
    private lateinit var btnCueLeft: View
    private lateinit var btnCueRight: View
    private lateinit var callCuesLeft: View
    private lateinit var callCuesRight: View
    private lateinit var btnResetLeft: View
    private lateinit var btnResetRight: View
    private lateinit var arrowCrossLeft: View
    private lateinit var arrowCrossRight: View

    // Top Wave & Recording
    private lateinit var txtTitleLeft: TextView
    private lateinit var txtTitleRight: TextView
    private lateinit var txtDurationLeft: TextView
    private lateinit var txtDurationRight: TextView
    private lateinit var keyLeft: TextView
    private lateinit var keyRight: TextView
    private lateinit var waveLeft: WaveProgressView
    private lateinit var waveRight: WaveProgressView
    private lateinit var waveViewLeft: WaveView
    private lateinit var waveViewRight: WaveView
    private lateinit var recordLayout: View
    private lateinit var recordIcon: ImageView
    private lateinit var recordTime: TextView

    // Top Pitch Views (Deck A & Deck B)
    private lateinit var pitchLayout1Left: View
    private lateinit var pitchLayout2Left: View
    private lateinit var pitchLayout1Right: View
    private lateinit var pitchLayout2Right: View
    private lateinit var txtBpmLeft: TextView
    private lateinit var txtBpmRight: TextView
    private lateinit var txtBpm2Left: TextView
    private lateinit var txtBpm2Right: TextView
    private lateinit var btnPitchLeft: TextView
    private lateinit var btnPitchRight: TextView
    private lateinit var btnOkPitchLeft: TextView
    private lateinit var btnOkPitchRight: TextView
    private lateinit var btnResetPitchLeft: TextView
    private lateinit var btnResetPitchRight: TextView
    private lateinit var btnMusicLeft: ImageView
    private lateinit var btnMusicRight: ImageView
    private lateinit var pitchSeekBarLeft: BPMSeekBar
    private lateinit var pitchSeekBarRight: BPMSeekBar

    // BPM Setting Layouts
    private lateinit var bpmSettingLayoutLeft: View
    private lateinit var bpmSettingLayoutRight: View
    private lateinit var bpmPreviewLeft: BpmSettingPreviewView
    private lateinit var bpmPreviewRight: BpmSettingPreviewView

    // Active tool views inside containers
    private var activeToolViewLeft: View? = null
    private var activeToolViewRight: View? = null
    private var activeToolBtnLeft: TextView? = null
    private var activeToolBtnRight: TextView? = null

    // Sampler pack pagination & routing
    private var samplerPackIndexLeft = 0
    private var samplerPackIndexRight = 0
    private var samplerVolumeLeft = 0.8f
    private var samplerVolumeRight = 0.8f
    private var samplerRouteCrossfadeLeft = true
    private var samplerRouteCrossfadeRight = true

    // SoundPool for multi-voice instant Sampler and Scratch playback
    data class SamplerDef(val assetPath: String, val name: String)
    private val samplerList = listOf(
        SamplerDef("sampler/audio_1.mp3", "Come on"),
        SamplerDef("sampler/audio_2.mp3", "Horn"),
        SamplerDef("sampler/audio_3.mp3", "Oh yeah"),
        SamplerDef("sampler/audio_4.mp3", "Lazer"),
        SamplerDef("sampler/audio_5.mp3", "Gunshot"),
        SamplerDef("sampler/audio_28.mp3", "Trumpet"),
        SamplerDef("sampler/audio_6.mp3", "Boing"),
        SamplerDef("sampler/audio_18.mp3", "Alarm"),
        SamplerDef("sampler/audio_13.mp3", "Clap"),
        SamplerDef("sampler/audio_14.mp3", "Cymbal"),
        SamplerDef("sampler/audio_15.mp3", "Boom"),
        SamplerDef("sampler/audio_16.mp3", "Prick"),
        SamplerDef("sampler/audio_11.mp3", "Rhythm"),
        SamplerDef("sampler/audio_12.mp3", "Walker"),
        SamplerDef("sampler/audio_21.mp3", "Scratch"),
        SamplerDef("sampler/audio_22.mp3", "DJScratch"),
        SamplerDef("sampler/audio_17.mp3", "Squeak"),
        SamplerDef("sampler/audio_19.mp3", "Whoosh"),
        SamplerDef("sampler/audio_20.mp3", "Yes"),
        SamplerDef("sampler/audio_27.mp3", "Ohh oh"),
        SamplerDef("sampler/audio_24.mp3", "Beat"),
        SamplerDef("sampler/audio_10.mp3", "Crunchy"),
        SamplerDef("sampler/audio_8.mp3", "Downlifter"),
        SamplerDef("sampler/audio_9.mp3", "Uplifter"),
        SamplerDef("sampler/audio_7.mp3", "Drums"),
        SamplerDef("sampler/audio_23.mp3", "Countdown"),
        SamplerDef("sampler/audio_25.mp3", "Applause"),
        SamplerDef("sampler/audio_26.mp3", "DJing")
    )
    private val samplerChannelMap = HashMap<String, Int>()
    private val scratchSounds = arrayOf("disc_scraping_1.mp3", "disc_scraping_2.mp3", "disc_scraping_3.mp3", "disc_scraping_4.mp3")
    private val scratchChannelMap = HashMap<String, Int>()
    private val scratchRandom = java.util.Random()
    private val scratchStopHandler = Handler(Looper.getMainLooper())
    private val scratchStopRunnableA = Runnable { stopScratchStream(1) }
    private val scratchStopRunnableB = Runnable { stopScratchStream(2) }

    // Low-Latency Scratch Engine
    private var scratchSoundPool: SoundPool? = null
    private val scratchFwdSoundIds = IntArray(4)
    private val scratchRevSoundIds = IntArray(4)
    private val loadedScratchIds = HashSet<Int>()
    private var activeScratchStreamA = 0
    private var activeScratchStreamB = 0
    private var lastScratchDirA = 0
    private var lastScratchDirB = 0
    private var scratchPairA = 0
    private var scratchPairB = 1
    private var wasPlayingBeforeScratchA = false
    private var wasPlayingBeforeScratchB = false
    private var scratchCurrentPosA = 0L
    private var scratchCurrentPosB = 0L

    // Equalizer (EQ) DSP state (DX8_PARAMEQ)
    private val eqFreqs = floatArrayOf(60f, 230f, 910f, 3000f, 14000f)
    private val eqGainsLeft = floatArrayOf(0f, 0f, 0f, 0f, 0f)
    private val eqGainsRight = floatArrayOf(0f, 0f, 0f, 0f, 0f)
    private val eqHandlesLeft = IntArray(5) { 0 }
    private val eqHandlesRight = IntArray(5) { 0 }
    private val eqParamsLeft = Array(5) { BASS.BASS_DX8_PARAMEQ().apply { fBandwidth = 18.0f } }
    private val eqParamsRight = Array(5) { BASS.BASS_DX8_PARAMEQ().apply { fBandwidth = 18.0f } }

    // Bass Boost (FX) DSP state (DX8_PARAMEQ at 80Hz)
    private var bassBoostProgressLeft = 0
    private var bassBoostProgressRight = 0
    private var bassHandleLeft = 0
    private var bassHandleRight = 0
    private val bassParamLeft = BASS.BASS_DX8_PARAMEQ().apply {
        fCenter = 80f
        fBandwidth = 24.0f
    }
    private val bassParamRight = BASS.BASS_DX8_PARAMEQ().apply {
        fCenter = 80f
        fBandwidth = 24.0f
    }

    // Pickers for loading custom audio
    private var pickingForDeckA = true
    private val openAudioLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { loadAudioFromUri(it, pickingForDeckA) }
    }
    private val audioLibraryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val isDeckA = result.data?.getIntExtra(ActivityAudioLibrary.EXTRA_DECK, 0) == 0
            val path = result.data?.getStringExtra(ActivityAudioLibrary.EXTRA_AUDIO_PATH)
            val title = result.data?.getStringExtra(ActivityAudioLibrary.EXTRA_AUDIO_TITLE)
            if (!path.isNullOrEmpty()) {
                loadAudioFromFile(path, title, isDeckA)
            }
        }
    }

    // Settings & Display state
    private var timeMode = 0 // 0 = Elapsed, 1 = Remaining, 2 = Total
    private var pitchSliderRange = 0.08f // default ±8%
    private var isScratchSoundEnabled = true
    private var crossfaderCurve = 0 // 0 = Smooth, 1 = Linear, 2 = Cut
    private var recFormat = "WAV"
    private var currentLanguageCode: String = ""

    private val handler = Handler(Looper.getMainLooper())
    private val uiUpdateRunnable = object : Runnable {
        override fun run() {
            updateVuMeters()
            updateWaveAndNeedle()
            updateRecordingTimer()
            checkLoopBoundaries()
            handler.postDelayed(this, 50)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentLanguageCode = SystemUtil.getPreLanguage(this) ?: "en"
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        val decorView = window.decorView
        decorView.systemUiVisibility = 5378
        decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if ((visibility and View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                decorView.systemUiVisibility = 5378
            }
        }
        setContentView(R.layout.activity_main)

        initAudioEngine()
        preloadSamplerChannels()
        initViews()
        setupTurntables()
        setupAudioChannels()
        setupControls()
        setupTopLayout()
        initAdInspectorShakeDetector()
        setupSideTools()
        val flTopBanner = findViewById<FrameLayout>(R.id.fl_top_banner)
        AdsManager.showNativeBanner(this, flTopBanner)
        checkAndRequestPermissions()
        handleLoadIntent(intent)

        handler.post(uiUpdateRunnable)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLoadIntent(intent)
    }

    private fun handleLoadIntent(targetIntent: Intent?) {
        val target = targetIntent ?: return
        val pathA = target.getStringExtra("LOAD_DECK_A")
        val pathB = target.getStringExtra("LOAD_DECK_B")
        val playA = target.getBooleanExtra("PLAY_A", false)
        val playB = target.getBooleanExtra("PLAY_B", false)
        if (!pathA.isNullOrEmpty()) {
            loadAudioFromFile(pathA, java.io.File(pathA).nameWithoutExtension, isDeckA = true)
            if (playA && !isPlayingA) toggleDeckA()
        }
        if (!pathB.isNullOrEmpty()) {
            loadAudioFromFile(pathB, java.io.File(pathB).nameWithoutExtension, isDeckA = false)
            if (playB && !isPlayingB) toggleDeckB()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.RECORD_AUDIO)
        }
        if (permissions.isNotEmpty()) {
            androidx.core.app.ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 101)
        }
    }

    override fun onResume() {
        super.onResume()
        window.decorView.systemUiVisibility = 5378
        val latestLang = SystemUtil.getPreLanguage(this) ?: "en"
        if (currentLanguageCode.isNotEmpty() && latestLang != currentLanguageCode) {
            currentLanguageCode = latestLang
            recreate()
            return
        }
        if (activeToolBtnLeft?.id == R.id.btn_samples) {
            openSamplesPanel(true)
        }
        if (activeToolBtnRight?.id == R.id.btn_samples) {
            openSamplesPanel(false)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = 5378
        }
    }

    private fun initAudioEngine() {
        try {
            BASS.BASS_SetConfig(BASS.BASS_CONFIG_BUFFER, 100)
            BASS.BASS_SetConfig(BASS.BASS_CONFIG_UPDATEPERIOD, 10)
            BASS.BASS_SetConfig(BASS.BASS_CONFIG_DEV_BUFFER, 25)
        } catch (e: Exception) {
            Log.e(tag, "BASS_SetConfig failed", e)
        }

        val success = BASS.BASS_Init(-1, 44100, 0)
        if (success || BASS.BASS_ErrorGetCode() == BASS.BASS_ERROR_ALREADY) {
            Log.d(tag, "BASS Engine initialized successfully")
        } else {
            Log.e(tag, "BASS Engine init failed with error: ${BASS.BASS_ErrorGetCode()}")
        }

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            scratchSoundPool = SoundPool.Builder()
                .setMaxStreams(8)
                .setAudioAttributes(audioAttributes)
                .build()
            scratchSoundPool?.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) {
                    loadedScratchIds.add(sampleId)
                }
            }
            loadScratchSounds()
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize scratch SoundPool", e)
        }
    }

    private fun loadScratchSounds() {
        val sp = scratchSoundPool ?: return
        for (i in 0 until 4) {
            try {
                val fwdAfd = assets.openFd("scratch/scratch_fwd_$i.ogg")
                scratchFwdSoundIds[i] = sp.load(fwdAfd, 1)
            } catch (e: Exception) {
                val cached = copyAssetToCache("scratch/scratch_fwd_$i.ogg")
                if (cached != null) {
                    scratchFwdSoundIds[i] = sp.load(cached, 1)
                }
            }

            try {
                val revAfd = assets.openFd("scratch/scratch_rev_$i.ogg")
                scratchRevSoundIds[i] = sp.load(revAfd, 1)
            } catch (e: Exception) {
                val cached = copyAssetToCache("scratch/scratch_rev_$i.ogg")
                if (cached != null) {
                    scratchRevSoundIds[i] = sp.load(cached, 1)
                }
            }
        }
        Log.d(tag, "Scratch sounds loaded into SoundPool")
    }

    private fun preloadSamplerChannels() {
        Thread {
            for (sound in scratchSounds) {
                if (!scratchChannelMap.containsKey(sound)) {
                    val scratchFile = copyAssetToCache(sound)
                    if (scratchFile != null) {
                        val ch = BASSEncrypt.BASS_StreamCreateFile(scratchFile, 0)
                        if (ch != 0) {
                            scratchChannelMap[sound] = ch
                        } else {
                            Log.e(tag, "BASSEncrypt failed to load scratch $sound, error=${BASS.BASS_ErrorGetCode()}")
                        }
                    }
                }
            }
            for (item in samplerList) {
                if (!samplerChannelMap.containsKey(item.assetPath)) {
                    val sampleFile = copyAssetToCache(item.assetPath)
                    if (sampleFile != null) {
                        val ch = BASSEncrypt.BASS_StreamCreateFile(sampleFile, 0)
                        if (ch != 0) {
                            samplerChannelMap[item.assetPath] = ch
                        } else {
                            Log.e(tag, "BASSEncrypt failed to load ${item.assetPath}, error=${BASS.BASS_ErrorGetCode()}")
                        }
                    }
                }
            }
            Log.d(tag, "Preloaded ${scratchChannelMap.size} scratch channels and ${samplerChannelMap.size} sampler channels")
        }.start()
    }

    private fun applyDeckEq(isLeft: Boolean) {
        val channel = if (isLeft) channelA else channelB
        if (channel == 0) return
        val handles = if (isLeft) eqHandlesLeft else eqHandlesRight
        val params = if (isLeft) eqParamsLeft else eqParamsRight
        val gains = if (isLeft) eqGainsLeft else eqGainsRight

        for (i in 0..4) {
            if (handles[i] != 0) {
                BASS.BASS_ChannelRemoveFX(channel, handles[i])
                handles[i] = 0
            }
            val handle = BASS.BASS_ChannelSetFX(channel, BASS.BASS_FX_DX8_PARAMEQ, 0)
            if (handle != 0) {
                params[i].fCenter = eqFreqs[i]
                params[i].fBandwidth = 18.0f
                params[i].fGain = gains[i]
                BASS.BASS_FXSetParameters(handle, params[i])
                handles[i] = handle
            }
        }
    }

    private fun setEqBandGain(isLeft: Boolean, band: Int, gainDb: Float) {
        val gains = if (isLeft) eqGainsLeft else eqGainsRight
        val handles = if (isLeft) eqHandlesLeft else eqHandlesRight
        val params = if (isLeft) eqParamsLeft else eqParamsRight

        gains[band] = gainDb
        params[band].fGain = gainDb
        if (handles[band] != 0) {
            BASS.BASS_FXSetParameters(handles[band], params[band])
        }
    }

    private fun applyDeckBass(isLeft: Boolean) {
        val channel = if (isLeft) channelA else channelB
        if (channel == 0) return
        val progress = if (isLeft) bassBoostProgressLeft else bassBoostProgressRight
        val param = if (isLeft) bassParamLeft else bassParamRight

        var handle = if (isLeft) bassHandleLeft else bassHandleRight
        if (handle != 0) {
            BASS.BASS_ChannelRemoveFX(channel, handle)
            handle = 0
        }
        handle = BASS.BASS_ChannelSetFX(channel, BASS.BASS_FX_DX8_PARAMEQ, 1)
        if (handle != 0) {
            param.fCenter = 80f
            param.fBandwidth = 24.0f
            param.fGain = (progress / 1000f) * 15.0f
            BASS.BASS_FXSetParameters(handle, param)
            if (isLeft) bassHandleLeft = handle else bassHandleRight = handle
        }
    }

    private fun setBassBoostProgress(isLeft: Boolean, progress: Int) {
        if (isLeft) bassBoostProgressLeft = progress else bassBoostProgressRight = progress
        val handle = if (isLeft) bassHandleLeft else bassHandleRight
        val param = if (isLeft) bassParamLeft else bassParamRight
        param.fGain = (progress / 1000f) * 15.0f
        if (handle != 0) {
            BASS.BASS_FXSetParameters(handle, param)
        }
    }

    private fun initViews() {
        containerLeft = findViewById(R.id.container_left)
        containerRight = findViewById(R.id.container_right)
        turntableRootLeft = containerLeft.findViewById(R.id.play_rotate_layout_root)
        turntableRootRight = containerRight.findViewById(R.id.play_rotate_layout_root)

        turntableLeft = containerLeft.findViewById(R.id.play_rotate_layout)
        turntableRight = containerRight.findViewById(R.id.play_rotate_layout)

        val soundLayout = findViewById<View>(R.id.sound_layout)
        volSeekBarLeft = soundLayout.findViewById(R.id.seekbar_sound_left)
        volSeekBarRight = soundLayout.findViewById(R.id.seekbar_sound_right)
        vuLeft = soundLayout.findViewById(R.id.sound_visualizer_left)
        vuRight = soundLayout.findViewById(R.id.sound_visualizer_right)
        soundMuteLeft = soundLayout.findViewById(R.id.sound_left)
        soundMuteRight = soundLayout.findViewById(R.id.sound_right)
        val btnSetting = soundLayout.findViewById<View>(R.id.setting)
        btnSetting?.setOnClickListener {
            startActivity(Intent(this, ActivitySetting::class.java))
        }

        val bottomLayout = findViewById<View>(R.id.bottom_layout)
        crossFadeSeekBar = bottomLayout.findViewById(R.id.seekbar_cross)
        arrowCrossLeft = bottomLayout.findViewById(R.id.arrow_left)
        arrowCrossRight = bottomLayout.findViewById(R.id.arrow_right)

        val bottomLeft = bottomLayout.findViewById<View>(R.id.bottom_layout_left)
        val bottomRight = bottomLayout.findViewById<View>(R.id.bottom_layout_right)

        btnAddMusicLeft = bottomLeft.findViewById(R.id.add_music)
        btnPlayLeft = bottomLeft.findViewById(R.id.play_pause)
        btnCueLeft = bottomLeft.findViewById(R.id.set_cues)
        callCuesLeft = bottomLeft.findViewById(R.id.call_cues)
        btnResetLeft = bottomLeft.findViewById(R.id.reset)

        btnAddMusicRight = bottomRight.findViewById(R.id.add_music)
        btnPlayRight = bottomRight.findViewById(R.id.play_pause)
        btnCueRight = bottomRight.findViewById(R.id.set_cues)
        callCuesRight = bottomRight.findViewById(R.id.call_cues)
        btnResetRight = bottomRight.findViewById(R.id.reset)

        // Top layouts
        val topLayout = findViewById<View>(R.id.top_layout)
        val waveLayout = topLayout.findViewById<View>(R.id.top_layout_wave)
        txtTitleLeft = waveLayout.findViewById(R.id.title_left)
        txtTitleRight = waveLayout.findViewById(R.id.title_right)
        txtDurationLeft = waveLayout.findViewById(R.id.duration_left)
        txtDurationRight = waveLayout.findViewById(R.id.duration_right)
        keyLeft = waveLayout.findViewById(R.id.key_left)
        keyRight = waveLayout.findViewById(R.id.key_right)
        waveLeft = waveLayout.findViewById(R.id.wave_left)
        waveRight = waveLayout.findViewById(R.id.wave_right)
        waveViewLeft = waveLayout.findViewById(R.id.waveView_left)
        waveViewRight = waveLayout.findViewById(R.id.waveView_right)

        recordLayout = waveLayout.findViewById(R.id.record_layout)
        recordIcon = recordLayout.findViewById(R.id.record_icon)
        recordTime = recordLayout.findViewById(R.id.record_time)

        // Pitch Left
        val pitchLeft = topLayout.findViewById<View>(R.id.pitch_layout_left)
        pitchLayout1Left = pitchLeft.findViewById(R.id.layout_1)
        pitchLayout2Left = pitchLeft.findViewById(R.id.layout_2)
        txtBpmLeft = pitchLayout1Left.findViewById(R.id.bpm)
        txtBpm2Left = pitchLayout2Left.findViewById(R.id.bpm_2)
        btnPitchLeft = pitchLayout1Left.findViewById(R.id.btn_pitch)
        btnOkPitchLeft = pitchLayout2Left.findViewById(R.id.btn_ok)
        btnResetPitchLeft = pitchLayout2Left.findViewById(R.id.btn_reset)
        btnMusicLeft = pitchLayout2Left.findViewById(R.id.btn_music)
        pitchSeekBarLeft = pitchLayout2Left.findViewById(R.id.seekbar_pitch)
        bpmSettingLayoutLeft = pitchLayout1Left.findViewById(R.id.bpm_setting_layout)
        bpmPreviewLeft = bpmSettingLayoutLeft.findViewById(R.id.bpm_setting_preview)

        // Pitch Right
        val pitchRight = topLayout.findViewById<View>(R.id.pitch_layout_right)
        pitchLayout1Right = pitchRight.findViewById(R.id.layout_1)
        pitchLayout2Right = pitchRight.findViewById(R.id.layout_2)
        txtBpmRight = pitchLayout1Right.findViewById(R.id.bpm)
        txtBpm2Right = pitchLayout2Right.findViewById(R.id.bpm_2)
        btnPitchRight = pitchLayout1Right.findViewById(R.id.btn_pitch)
        btnOkPitchRight = pitchLayout2Right.findViewById(R.id.btn_ok)
        btnResetPitchRight = pitchLayout2Right.findViewById(R.id.btn_reset)
        btnMusicRight = pitchLayout2Right.findViewById(R.id.btn_music)
        pitchSeekBarRight = pitchLayout2Right.findViewById(R.id.seekbar_pitch)
        bpmSettingLayoutRight = pitchLayout1Right.findViewById(R.id.bpm_setting_layout)
        bpmPreviewRight = bpmSettingLayoutRight.findViewById(R.id.bpm_setting_preview)

        btnMusicLeft.setBackgroundResource(R.drawable.pitch_btn_music_selector_left)
        btnMusicRight.setBackgroundResource(R.drawable.pitch_btn_music_selector_right)
        btnMusicLeft.setColorFilter(ContextCompat.getColor(this, R.color.theme_color_left))
        btnMusicRight.setColorFilter(ContextCompat.getColor(this, R.color.theme_color_right))
    }

    private fun setupTurntables() {
        // Deck A (Left)
        turntableLeft.logoSizeFactor = 0.60f
        turntableLeft.deckThemeColor = ContextCompat.getColor(this, R.color.theme_color_left)
        turntableLeft.setIdleAlbumResource(R.drawable.rotate_cd_idle)
        turntableLeft.setIdleLogoResource(R.drawable.rotate_cd_logo_idle)
        turntableLeft.setDefaultAlbumResource(R.drawable.rotate_cd_default)
        turntableLeft.setAlbumTextureResource(R.drawable.rotate_cd_texture)
        turntableLeft.setLogoResource(R.drawable.rotate_cd_logo_idle)
        turntableLeft.setIndicatorResource(R.drawable.rotate_cd_indicator_left)
        turntableLeft.isDragEnable = false

        // Deck B (Right)
        turntableRight.logoSizeFactor = 0.60f
        turntableRight.deckThemeColor = ContextCompat.getColor(this, R.color.theme_color_right)
        turntableRight.setIdleAlbumResource(R.drawable.rotate_cd_idle)
        turntableRight.setIdleLogoResource(R.drawable.rotate_cd_logo_idle)
        turntableRight.setDefaultAlbumResource(R.drawable.rotate_cd_default)
        turntableRight.setAlbumTextureResource(R.drawable.rotate_cd_texture)
        turntableRight.setLogoResource(R.drawable.rotate_cd_logo_idle)
        turntableRight.setIndicatorResource(R.drawable.rotate_cd_indicator_right)
        turntableRight.isDragEnable = false

        // Scratch physics on Left Turntable
        turntableLeft.setOnAlbumRotateListener(object : RotateAlbumView.OnAlbumRotateListener {
            override fun onScratchStart() {
                wasPlayingBeforeScratchA = isPlayingA
                if (isPlayingA) {
                    if (channelA != 0) BASS.BASS_ChannelPause(channelA) else mediaPlayerA?.pause()
                    turntableLeft.isRotating = false
                }
                scratchCurrentPosA = if (channelA != 0) {
                    BASS.BASS_ChannelGetPosition(channelA, BASS.BASS_POS_BYTE)
                } else {
                    mediaPlayerA?.currentPosition?.toLong() ?: 0L
                }
                stopScratchStream(1)
                scratchPairA = scratchRandom.nextInt(4)
            }

            override fun onScratch(deltaDegrees: Float, velocity: Float) {
                handleScratchMove(deck = 1, deltaDegrees = deltaDegrees, velocity = velocity)

                // Scrub position smoothly in memory & visual
                if (channelA != 0) {
                    val len = BASS.BASS_ChannelGetLength(channelA, BASS.BASS_POS_BYTE)
                    if (len > 0) {
                        val deltaBytes = ((deltaDegrees / 360f) * 317520).toLong()
                        scratchCurrentPosA = (scratchCurrentPosA + deltaBytes).coerceIn(0L, len)
                        val ratio = (scratchCurrentPosA.toFloat() / len).coerceIn(0f, 1f)
                        turntableLeft.progress = ratio
                        waveLeft.progress = ratio
                        val posMsA = (BASS.BASS_ChannelBytes2Seconds(channelA, scratchCurrentPosA) * 1000).toInt()
                        waveViewLeft.playOffset = posMsA * 0.25f

                        val posSecA = posMsA / 1000
                        val totalSecA = BASS.BASS_ChannelBytes2Seconds(channelA, len).toInt()
                        val remSecA = (totalSecA - posSecA).coerceAtLeast(0)
                        txtDurationLeft.text = when (timeMode) {
                            0 -> String.format("%02d:%02d", posSecA / 60, posSecA % 60)
                            1 -> String.format("-%02d:%02d", remSecA / 60, remSecA % 60)
                            else -> String.format("%02d:%02d / %02d:%02d", posSecA / 60, posSecA % 60, totalSecA / 60, totalSecA % 60)
                        }
                    }
                } else {
                    mediaPlayerA?.let { mp ->
                        if (durationMsA > 0) {
                            val deltaMs = ((deltaDegrees / 360f) * 2000).toLong()
                            scratchCurrentPosA = (scratchCurrentPosA + deltaMs).coerceIn(0L, durationMsA.toLong())
                            val ratio = (scratchCurrentPosA.toFloat() / durationMsA).coerceIn(0f, 1f)
                            turntableLeft.progress = ratio
                            waveLeft.progress = ratio
                            waveViewLeft.playOffset = scratchCurrentPosA.toFloat() * 0.25f

                            val posSecA = (scratchCurrentPosA / 1000).toInt()
                            val totalSecA = durationMsA / 1000
                            val remSecA = (totalSecA - posSecA).coerceAtLeast(0)
                            txtDurationLeft.text = when (timeMode) {
                                0 -> String.format("%02d:%02d", posSecA / 60, posSecA % 60)
                                1 -> String.format("-%02d:%02d", remSecA / 60, remSecA % 60)
                                else -> String.format("%02d:%02d / %02d:%02d", posSecA / 60, posSecA % 60, totalSecA / 60, totalSecA % 60)
                            }
                        }
                    }
                }
            }

            override fun onScratchEnd() {
                stopScratchStream(1)

                if (channelA != 0) {
                    BASS.BASS_ChannelSetPosition(channelA, scratchCurrentPosA, BASS.BASS_POS_BYTE)
                } else {
                    mediaPlayerA?.seekTo(scratchCurrentPosA.toInt())
                }

                if (wasPlayingBeforeScratchA) {
                    isPlayingA = true
                    if (channelA != 0) {
                        BASS.BASS_ChannelPlay(channelA, false)
                    } else {
                        mediaPlayerA?.start()
                    }
                    turntableLeft.isRotating = true
                    turntableLeft.setIndicatorState(1)
                }
            }
        })

        // Scratch physics on Right Turntable
        turntableRight.setOnAlbumRotateListener(object : RotateAlbumView.OnAlbumRotateListener {
            override fun onScratchStart() {
                wasPlayingBeforeScratchB = isPlayingB
                if (isPlayingB) {
                    if (channelB != 0) BASS.BASS_ChannelPause(channelB) else mediaPlayerB?.pause()
                    turntableRight.isRotating = false
                }
                scratchCurrentPosB = if (channelB != 0) {
                    BASS.BASS_ChannelGetPosition(channelB, BASS.BASS_POS_BYTE)
                } else {
                    mediaPlayerB?.currentPosition?.toLong() ?: 0L
                }
                stopScratchStream(2)
                scratchPairB = scratchRandom.nextInt(4)
            }

            override fun onScratch(deltaDegrees: Float, velocity: Float) {
                handleScratchMove(deck = 2, deltaDegrees = deltaDegrees, velocity = velocity)

                // Scrub position smoothly in memory & visual
                if (channelB != 0) {
                    val len = BASS.BASS_ChannelGetLength(channelB, BASS.BASS_POS_BYTE)
                    if (len > 0) {
                        val deltaBytes = ((deltaDegrees / 360f) * 317520).toLong()
                        scratchCurrentPosB = (scratchCurrentPosB + deltaBytes).coerceIn(0L, len)
                        val ratio = (scratchCurrentPosB.toFloat() / len).coerceIn(0f, 1f)
                        turntableRight.progress = ratio
                        waveRight.progress = ratio
                        val posMsB = (BASS.BASS_ChannelBytes2Seconds(channelB, scratchCurrentPosB) * 1000).toInt()
                        waveViewRight.playOffset = posMsB * 0.25f

                        val posSecB = posMsB / 1000
                        val totalSecB = BASS.BASS_ChannelBytes2Seconds(channelB, len).toInt()
                        val remSecB = (totalSecB - posSecB).coerceAtLeast(0)
                        txtDurationRight.text = when (timeMode) {
                            0 -> String.format("%02d:%02d", posSecB / 60, posSecB % 60)
                            1 -> String.format("-%02d:%02d", remSecB / 60, remSecB % 60)
                            else -> String.format("%02d:%02d / %02d:%02d", posSecB / 60, posSecB % 60, totalSecB / 60, totalSecB % 60)
                        }
                    }
                } else {
                    mediaPlayerB?.let { mp ->
                        if (durationMsB > 0) {
                            val deltaMs = ((deltaDegrees / 360f) * 2000).toLong()
                            scratchCurrentPosB = (scratchCurrentPosB + deltaMs).coerceIn(0L, durationMsB.toLong())
                            val ratio = (scratchCurrentPosB.toFloat() / durationMsB).coerceIn(0f, 1f)
                            turntableRight.progress = ratio
                            waveRight.progress = ratio
                            waveViewRight.playOffset = scratchCurrentPosB.toFloat() * 0.25f

                            val posSecB = (scratchCurrentPosB / 1000).toInt()
                            val totalSecB = durationMsB / 1000
                            val remSecB = (totalSecB - posSecB).coerceAtLeast(0)
                            txtDurationRight.text = when (timeMode) {
                                0 -> String.format("%02d:%02d", posSecB / 60, posSecB % 60)
                                1 -> String.format("-%02d:%02d", remSecB / 60, remSecB % 60)
                                else -> String.format("%02d:%02d / %02d:%02d", posSecB / 60, posSecB % 60, totalSecB / 60, totalSecB % 60)
                            }
                        }
                    }
                }
            }

            override fun onScratchEnd() {
                stopScratchStream(2)

                if (channelB != 0) {
                    BASS.BASS_ChannelSetPosition(channelB, scratchCurrentPosB, BASS.BASS_POS_BYTE)
                } else {
                    mediaPlayerB?.seekTo(scratchCurrentPosB.toInt())
                }

                if (wasPlayingBeforeScratchB) {
                    isPlayingB = true
                    if (channelB != 0) {
                        BASS.BASS_ChannelPlay(channelB, false)
                    } else {
                        mediaPlayerB?.start()
                    }
                    turntableRight.isRotating = true
                    turntableRight.setIndicatorState(1)
                }
            }
        })

        // Pitch Bend +/- buttons on Turntable corners (Continuous Touch Nudge)
        turntableRootLeft.findViewById<View>(R.id.pitch_bend_add)?.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    nudgePitchContinuous(channelA, 1.05f, isLeft = true)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    restorePitch(channelA, isLeft = true)
                    true
                }
                else -> false
            }
        }
        turntableRootLeft.findViewById<View>(R.id.pitch_bend_reduce)?.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    nudgePitchContinuous(channelA, 0.95f, isLeft = true)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    restorePitch(channelA, isLeft = true)
                    true
                }
                else -> false
            }
        }
        turntableRootRight.findViewById<View>(R.id.pitch_bend_add)?.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    nudgePitchContinuous(channelB, 1.05f, isLeft = false)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    restorePitch(channelB, isLeft = false)
                    true
                }
                else -> false
            }
        }
        turntableRootRight.findViewById<View>(R.id.pitch_bend_reduce)?.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    nudgePitchContinuous(channelB, 0.95f, isLeft = false)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    restorePitch(channelB, isLeft = false)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupAudioChannels() {
        channelA = 0
        channelB = 0
        applyVolumes()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupControls() {
        // Initial state matching original app idle
        btnPlayLeft.isEnabled = false
        btnPlayRight.isEnabled = false
        btnResetLeft.isSelected = true
        btnResetRight.isSelected = true
        callCuesLeft.isEnabled = false
        callCuesRight.isEnabled = false

        volSeekBarLeft.progress = 1000
        volSeekBarRight.progress = 1000
        volumeA = 1.0f
        volumeB = 1.0f
        crossFadeSeekBar.progress = 500

        // Deck A Play / Pause
        btnPlayLeft.setOnClickListener { toggleDeckA() }

        // Deck B Play / Pause
        btnPlayRight.setOnClickListener { toggleDeckB() }

        // Load Music buttons (Dedicated DJ Library Activity)
        val openLibraryForDeckA = {
            AdsManager.showInterAll(this) {
                val intent = Intent(this, ActivityAudioLibrary::class.java).apply {
                    putExtra(ActivityAudioLibrary.EXTRA_DECK, 0)
                }
                audioLibraryLauncher.launch(intent)
            }
        }
        val openLibraryForDeckB = {
            AdsManager.showInterAll(this) {
                val intent = Intent(this, ActivityAudioLibrary::class.java).apply {
                    putExtra(ActivityAudioLibrary.EXTRA_DECK, 1)
                }
                audioLibraryLauncher.launch(intent)
            }
        }
        btnAddMusicLeft.setOnClickListener { openLibraryForDeckA() }
        txtTitleLeft.setOnClickListener { openLibraryForDeckA() }
        btnAddMusicRight.setOnClickListener { openLibraryForDeckB() }
        txtTitleRight.setOnClickListener { openLibraryForDeckB() }

        // CUE buttons ("SET"): saves current playback position
        btnCueLeft.setOnClickListener {
            val hasSong = (channelA != 0 || mediaPlayerA != null)
            if (!hasSong) {
                Toast.makeText(this, getString(R.string.toast_select_song_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            tempCueA = if (channelA != 0) BASS.BASS_ChannelGetPosition(channelA, BASS.BASS_POS_BYTE) else (mediaPlayerA?.currentPosition?.toLong() ?: 0L)
            val len = if (channelA != 0) BASS.BASS_ChannelGetLength(channelA, BASS.BASS_POS_BYTE) else (mediaPlayerA?.duration?.toLong() ?: 1L)
            if (len > 0) {
                waveLeft.cuePosition = (tempCueA.toFloat() / len).coerceIn(0f, 1f)
            }
            btnCueLeft.isPressed = true
            btnCueLeft.postDelayed({ btnCueLeft.isPressed = false }, 200)
            callCuesLeft.isEnabled = true
            Toast.makeText(this, getString(R.string.toast_cue_set, "A"), Toast.LENGTH_SHORT).show()
        }
        btnCueRight.setOnClickListener {
            val hasSong = (channelB != 0 || mediaPlayerB != null)
            if (!hasSong) {
                Toast.makeText(this, getString(R.string.toast_select_song_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            tempCueB = if (channelB != 0) BASS.BASS_ChannelGetPosition(channelB, BASS.BASS_POS_BYTE) else (mediaPlayerB?.currentPosition?.toLong() ?: 0L)
            val len = if (channelB != 0) BASS.BASS_ChannelGetLength(channelB, BASS.BASS_POS_BYTE) else (mediaPlayerB?.duration?.toLong() ?: 1L)
            if (len > 0) {
                waveRight.cuePosition = (tempCueB.toFloat() / len).coerceIn(0f, 1f)
            }
            btnCueRight.isPressed = true
            btnCueRight.postDelayed({ btnCueRight.isPressed = false }, 200)
            callCuesRight.isEnabled = true
            Toast.makeText(this, getString(R.string.toast_cue_set, "B"), Toast.LENGTH_SHORT).show()
        }

        // Call CUE buttons (arrow to dot): tap jumps immediately to CUE point
        callCuesLeft.setOnClickListener {
            val hasSong = (channelA != 0 || mediaPlayerA != null)
            if (!hasSong) {
                Toast.makeText(this, getString(R.string.toast_select_song_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            seekDeckToByte(isLeft = true, targetByte = tempCueA)
        }
        callCuesRight.setOnClickListener {
            val hasSong = (channelB != 0 || mediaPlayerB != null)
            if (!hasSong) {
                Toast.makeText(this, getString(R.string.toast_select_song_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            seekDeckToByte(isLeft = false, targetByte = tempCueB)
        }

        // Reset buttons: stop playback and rewind to start
        btnResetLeft.setOnClickListener {
            if (isPlayingA) toggleDeckA()
            if (channelA != 0) {
                BASS.BASS_ChannelSetPosition(channelA, 0, BASS.BASS_POS_BYTE)
            } else {
                mediaPlayerA?.seekTo(0)
            }
            turntableLeft.progress = 0f
            waveLeft.progress = 0f
            waveViewLeft.playOffset = 0f
        }
        btnResetRight.setOnClickListener {
            if (isPlayingB) toggleDeckB()
            if (channelB != 0) {
                BASS.BASS_ChannelSetPosition(channelB, 0, BASS.BASS_POS_BYTE)
            } else {
                mediaPlayerB?.seekTo(0)
            }
            turntableRight.progress = 0f
            waveRight.progress = 0f
            waveViewRight.playOffset = 0f
        }

        // Mute / Sound Boost buttons
        soundMuteLeft.setOnSelectChangedListener(object : SelectBox.OnSelectChangedListener {
            override fun onSelectChanged(fromUser: Boolean, isSelected: Boolean) {
                isMutedA = isSelected
                applyVolumes()
            }
        })
        soundMuteRight.setOnSelectChangedListener(object : SelectBox.OnSelectChangedListener {
            override fun onSelectChanged(fromUser: Boolean, isSelected: Boolean) {
                isMutedB = isSelected
                applyVolumes()
            }
        })

        // Volume Fader Left (Deck A)
        volSeekBarLeft.setOnSeekBarChangeListener(object : CustomSeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: CustomSeekBar, progress: Int, fromUser: Boolean) {
                volumeA = progress / 1000f
                applyVolumes()
            }
        })

        // Volume Fader Right (Deck B)
        volSeekBarRight.setOnSeekBarChangeListener(object : CustomSeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: CustomSeekBar, progress: Int, fromUser: Boolean) {
                volumeB = progress / 1000f
                applyVolumes()
            }
        })

        // Crossfader
        crossFadeSeekBar.setOnSeekBarChangeListener(object : CustomSeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: CustomSeekBar, progress: Int, fromUser: Boolean) {
                crossfadeLeftGain = crossFadeSeekBar.getLeftGain()
                crossfadeRightGain = crossFadeSeekBar.getRightGain()
                applyVolumes()
            }
        })

        // Crossfader nudge arrows (auto-fade left / right)
        arrowCrossLeft.setOnClickListener {
            animateCrossfader(0)
        }
        arrowCrossRight.setOnClickListener {
            animateCrossfader(1000)
        }

        // SYNC buttons
        findViewById<View>(R.id.sync_left).setOnClickListener {
            // Deck A syncs to Deck B
            pitchFactorA = pitchFactorB
            baseBpmA = baseBpmB
            val progress = (500 + ((pitchFactorA - 1.0f) / 0.08f * 500)).toInt().coerceIn(0, 1000)
            pitchSeekBarLeft.progress = progress
            if (channelA != 0) {
                BASS.BASS_ChannelSetAttribute(channelA, BASS.BASS_ATTRIB_FREQ, 44100f * pitchFactorA)
            }
            updatePitchUi(isLeft = true)
        }
        findViewById<View>(R.id.sync_right).setOnClickListener {
            // Deck B syncs to Deck A
            pitchFactorB = pitchFactorA
            baseBpmB = baseBpmA
            val progress = (500 + ((pitchFactorB - 1.0f) / 0.08f * 500)).toInt().coerceIn(0, 1000)
            pitchSeekBarRight.progress = progress
            if (channelB != 0) {
                BASS.BASS_ChannelSetAttribute(channelB, BASS.BASS_ATTRIB_FREQ, 44100f * pitchFactorB)
            }
            updatePitchUi(isLeft = false)
        }

        // Setting button in center console
        findViewById<View>(R.id.setting)?.setOnClickListener {
            startActivity(Intent(this, ActivitySetting::class.java))
        }
    }

    private fun setupTopLayout() {
        // Initial visibility
        pitchLayout2Left.visibility = View.GONE
        pitchLayout2Right.visibility = View.GONE
        bpmSettingLayoutLeft.visibility = View.INVISIBLE
        bpmSettingLayoutRight.visibility = View.INVISIBLE

        // Style Deck A
        val colorLeft = ContextCompat.getColor(this, R.color.theme_color_left)
        txtTitleLeft.text = getString(R.string.music)
        txtTitleLeft.setTextColor(colorLeft)
        txtDurationLeft.text = "00:00"
        txtDurationLeft.setTextColor(colorLeft)
        txtBpmLeft.setTextColor(colorLeft)
        txtBpm2Left.setTextColor(colorLeft)
        btnPitchLeft.text = getString(R.string.pitch)
        keyLeft.visibility = View.GONE
        waveLeft.hasAudio = false
        waveViewLeft.hasAudio = false

        // Style Deck B
        val colorRight = ContextCompat.getColor(this, R.color.theme_color_right)
        txtTitleRight.text = getString(R.string.music)
        txtTitleRight.setTextColor(colorRight)
        txtDurationRight.text = "00:00"
        txtDurationRight.setTextColor(colorRight)
        txtBpmRight.setTextColor(colorRight)
        txtBpm2Right.setTextColor(colorRight)
        btnPitchRight.text = getString(R.string.pitch)
        keyRight.visibility = View.GONE
        waveRight.hasAudio = false
        waveViewRight.hasAudio = false
        pitchSeekBarRight.progressColor = colorRight
        pitchSeekBarRight.progressColorRight = colorRight
        pitchSeekBarRight.progressColorLeft = colorRight
        pitchSeekBarRight.thumbDrawable = ContextCompat.getDrawable(this, R.drawable.seekbar_thumb_h_pitch_selector_right)

        btnPitchRight.setTextColor(ContextCompat.getColorStateList(this, R.color.pitch_text_color_selector_right))
        btnPitchRight.setBackgroundResource(R.drawable.pitch_bg_reset_selector_right)
        btnOkPitchRight.setTextColor(ContextCompat.getColorStateList(this, R.color.pitch_text_color_selector_right))
        btnOkPitchRight.setBackgroundResource(R.drawable.pitch_bg_reset_selector_right)
        btnMusicRight.setBackgroundResource(R.drawable.pitch_btn_music_selector_right)
        btnMusicRight.setColorFilter(colorRight)
        btnResetPitchRight.setTextColor(ContextCompat.getColorStateList(this, R.color.pitch_text_color_selector_right))
        btnResetPitchRight.setBackgroundResource(R.drawable.pitch_bg_reset_selector_right)

        bpmPreviewRight.setLineSelectedColor(colorRight)
        val bpmResetRight = bpmSettingLayoutRight.findViewById<TextView>(R.id.bpm_setting_reset)
        bpmResetRight?.setTextColor(ContextCompat.getColorStateList(this, R.color.pitch_text_color_selector_right))
        bpmResetRight?.setBackgroundResource(R.drawable.pitch_bg_selector_right)
        val bpmApplyRight = bpmSettingLayoutRight.findViewById<TextView>(R.id.bpm_setting_apply)
        bpmApplyRight?.setTextColor(ContextCompat.getColorStateList(this, R.color.pitch_text_color_selector_right))
        bpmApplyRight?.setBackgroundResource(R.drawable.pitch_bg_selector_right)

        // Deck A Pitch Expand / Collapse
        btnPitchLeft.setOnClickListener {
            pitchLayout1Left.visibility = View.GONE
            pitchLayout2Left.visibility = View.VISIBLE
            bpmSettingLayoutLeft.visibility = View.INVISIBLE
        }
        btnOkPitchLeft.setOnClickListener {
            pitchLayout2Left.visibility = View.GONE
            pitchLayout1Left.visibility = View.VISIBLE
        }
        btnResetPitchLeft.setOnClickListener {
            pitchSeekBarLeft.progress = 500
            pitchFactorA = 1.0f
            if (channelA != 0) BASS.BASS_ChannelSetAttribute(channelA, BASS.BASS_ATTRIB_FREQ, 44100f)
            updatePitchUi(isLeft = true)
        }
        btnMusicLeft.setOnClickListener {
            isKeyLockA = !isKeyLockA
            btnMusicLeft.setColorFilter(if (isKeyLockA) ContextCompat.getColor(this, R.color.theme_color_left) else Color.parseColor("#88ffffff"))
            Toast.makeText(this, if (isKeyLockA) "Key Lock (Deck A): ON" else "Key Lock (Deck A): OFF", Toast.LENGTH_SHORT).show()
        }
        pitchSeekBarLeft.setOnSeekBarChangeListener(object : CustomSeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: CustomSeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    pitchFactorA = 1.0f + ((progress - 500) / 500.0f) * pitchSliderRange
                    if (channelA != 0) {
                        BASS.BASS_ChannelSetAttribute(channelA, BASS.BASS_ATTRIB_FREQ, 44100f * pitchFactorA)
                    }
                    updatePitchUi(isLeft = true)
                }
            }
        })

        // Deck B Pitch Expand / Collapse
        btnPitchRight.setOnClickListener {
            pitchLayout1Right.visibility = View.GONE
            pitchLayout2Right.visibility = View.VISIBLE
            bpmSettingLayoutRight.visibility = View.INVISIBLE
        }
        btnOkPitchRight.setOnClickListener {
            pitchLayout2Right.visibility = View.GONE
            pitchLayout1Right.visibility = View.VISIBLE
        }
        btnResetPitchRight.setOnClickListener {
            pitchSeekBarRight.progress = 500
            pitchFactorB = 1.0f
            if (channelB != 0) BASS.BASS_ChannelSetAttribute(channelB, BASS.BASS_ATTRIB_FREQ, 44100f)
            updatePitchUi(isLeft = false)
        }
        btnMusicRight.setOnClickListener {
            isKeyLockB = !isKeyLockB
            btnMusicRight.setColorFilter(if (isKeyLockB) ContextCompat.getColor(this, R.color.theme_color_right) else Color.parseColor("#88ffffff"))
            Toast.makeText(this, if (isKeyLockB) "Key Lock (Deck B): ON" else "Key Lock (Deck B): OFF", Toast.LENGTH_SHORT).show()
        }
        pitchSeekBarRight.setOnSeekBarChangeListener(object : CustomSeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: CustomSeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    pitchFactorB = 1.0f + ((progress - 500) / 500.0f) * pitchSliderRange
                    if (channelB != 0) {
                        BASS.BASS_ChannelSetAttribute(channelB, BASS.BASS_ATTRIB_FREQ, 44100f * pitchFactorB)
                    }
                    updatePitchUi(isLeft = false)
                }
            }
        })

        // Deck A BPM Tap / Settings
        pitchLayout1Left.findViewById<View>(R.id.bpm_layout).setOnClickListener {
            val visible = bpmSettingLayoutLeft.visibility == View.VISIBLE
            bpmSettingLayoutLeft.visibility = if (visible) View.INVISIBLE else View.VISIBLE
            bpmPreviewLeft.reset()
            tapTimesA.clear()
        }
        bpmSettingLayoutLeft.findViewById<View>(R.id.bpm_setting_apply).setOnClickListener {
            bpmSettingLayoutLeft.visibility = View.INVISIBLE
        }
        bpmSettingLayoutLeft.findViewById<View>(R.id.bpm_setting_reset).setOnClickListener {
            baseBpmA = 128.0f
            updatePitchUi(isLeft = true)
            bpmPreviewLeft.reset()
        }
        bpmSettingLayoutLeft.findViewById<View>(R.id.bpm_setting_minus).setOnClickListener {
            baseBpmA = (baseBpmA / 2f).coerceAtLeast(30f)
            updatePitchUi(isLeft = true)
        }
        bpmSettingLayoutLeft.findViewById<View>(R.id.bpm_setting_plus).setOnClickListener {
            baseBpmA = (baseBpmA * 2f).coerceAtMost(300f)
            updatePitchUi(isLeft = true)
        }
        bpmSettingLayoutLeft.findViewById<View>(R.id.bpm_setting_tap).setOnClickListener {
            handleBpmTap(isLeft = true)
        }

        // Deck B BPM Tap / Settings
        pitchLayout1Right.findViewById<View>(R.id.bpm_layout).setOnClickListener {
            val visible = bpmSettingLayoutRight.visibility == View.VISIBLE
            bpmSettingLayoutRight.visibility = if (visible) View.INVISIBLE else View.VISIBLE
            bpmPreviewRight.reset()
            tapTimesB.clear()
        }
        bpmSettingLayoutRight.findViewById<View>(R.id.bpm_setting_apply).setOnClickListener {
            bpmSettingLayoutRight.visibility = View.INVISIBLE
        }
        bpmSettingLayoutRight.findViewById<View>(R.id.bpm_setting_reset).setOnClickListener {
            baseBpmB = 128.0f
            updatePitchUi(isLeft = false)
            bpmPreviewRight.reset()
        }
        bpmSettingLayoutRight.findViewById<View>(R.id.bpm_setting_minus).setOnClickListener {
            baseBpmB = (baseBpmB / 2f).coerceAtLeast(30f)
            updatePitchUi(isLeft = false)
        }
        bpmSettingLayoutRight.findViewById<View>(R.id.bpm_setting_plus).setOnClickListener {
            baseBpmB = (baseBpmB * 2f).coerceAtMost(300f)
            updatePitchUi(isLeft = false)
        }
        bpmSettingLayoutRight.findViewById<View>(R.id.bpm_setting_tap).setOnClickListener {
            handleBpmTap(isLeft = false)
        }

        // Wave scrubbing
        waveLeft.seekListener = object : WaveProgressView.OnSeekListener {
            override fun onSeek(progressRatio: Float) {
                if (channelA != 0) {
                    val len = BASS.BASS_ChannelGetLength(channelA, BASS.BASS_POS_BYTE)
                    val target = (len * progressRatio).toLong()
                    BASS.BASS_ChannelSetPosition(channelA, target, BASS.BASS_POS_BYTE)
                } else {
                    mediaPlayerA?.let { mp ->
                        if (durationMsA > 0) {
                            mp.seekTo((durationMsA * progressRatio).toInt())
                        }
                    }
                }
                turntableLeft.progress = progressRatio
            }
        }
        waveRight.seekListener = object : WaveProgressView.OnSeekListener {
            override fun onSeek(progressRatio: Float) {
                if (channelB != 0) {
                    val len = BASS.BASS_ChannelGetLength(channelB, BASS.BASS_POS_BYTE)
                    val target = (len * progressRatio).toLong()
                    BASS.BASS_ChannelSetPosition(channelB, target, BASS.BASS_POS_BYTE)
                } else {
                    mediaPlayerB?.let { mp ->
                        if (durationMsB > 0) {
                            mp.seekTo((durationMsB * progressRatio).toInt())
                        }
                    }
                }
                turntableRight.progress = progressRatio
            }
        }

        // Time Mode cycle on duration text click
        txtDurationLeft.setOnClickListener { cycleTimeMode() }
        txtDurationRight.setOnClickListener { cycleTimeMode() }

        // Recording
        recordLayout.setOnClickListener {
            AdsManager.showInterAll(this) {
                if (!isRecording) {
                    isRecording = true
                    recordIcon.isSelected = true
                    recordStartTime = SystemClock.elapsedRealtime()
                    recordTime.text = "00:00"
                    Toast.makeText(this, getString(R.string.recording_started), Toast.LENGTH_SHORT).show()
                } else {
                    isRecording = false
                    recordIcon.isSelected = false
                    recordTime.text = getString(R.string.rec)
                    showSaveRecordingDialog()
                }
            }
        }

        updatePitchUi(isLeft = true)
        updatePitchUi(isLeft = false)
    }

    private fun handleBpmTap(isLeft: Boolean) {
        val tapTimes = if (isLeft) tapTimesA else tapTimesB
        val bpmPreview = if (isLeft) bpmPreviewLeft else bpmPreviewRight
        val now = SystemClock.elapsedRealtime()

        if (tapTimes.isNotEmpty() && now - tapTimes.last() > 2000L) {
            tapTimes.clear()
        }
        tapTimes.add(now)
        bpmPreview.tapCount = tapTimes.size

        if (tapTimes.size >= 4) {
            val totalDurationMs = tapTimes.last() - tapTimes.first()
            val avgIntervalSec = (totalDurationMs / 3.0f) / 1000f
            if (avgIntervalSec > 0) {
                val computedBpm = (60f / avgIntervalSec).coerceIn(40f, 300f)
                if (isLeft) baseBpmA = computedBpm else baseBpmB = computedBpm
                updatePitchUi(isLeft)
            }
            tapTimes.clear()
            bpmPreview.postDelayed({ bpmPreview.reset() }, 800)
        }
    }

    private fun updatePitchUi(isLeft: Boolean) {
        if (isLeft) {
            if (channelA != 0) {
                BASS.BASS_ChannelSetAttribute(channelA, BASS.BASS_ATTRIB_FREQ, 44100f * pitchFactorA)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mediaPlayerA != null) {
                try {
                    val params = mediaPlayerA?.playbackParams ?: PlaybackParams()
                    params.pitch = if (isKeyLockA) 1.0f else pitchFactorA
                    params.speed = pitchFactorA
                    mediaPlayerA?.playbackParams = params
                } catch (e: Exception) {}
            }
            if (channelA != 0 || mediaPlayerA != null) {
                val effBpm = baseBpmA * pitchFactorA
                val strBpm = String.format(Locale.US, "%.1f", effBpm)
                txtBpmLeft.text = strBpm
                txtBpm2Left.text = strBpm
                btnPitchLeft.text = getString(R.string.pitch)
            } else {
                txtBpmLeft.text = "-"
                txtBpm2Left.text = "-"
                btnPitchLeft.text = getString(R.string.pitch)
            }
        } else {
            if (channelB != 0) {
                BASS.BASS_ChannelSetAttribute(channelB, BASS.BASS_ATTRIB_FREQ, 44100f * pitchFactorB)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mediaPlayerB != null) {
                try {
                    val params = mediaPlayerB?.playbackParams ?: PlaybackParams()
                    params.pitch = if (isKeyLockB) 1.0f else pitchFactorB
                    params.speed = pitchFactorB
                    mediaPlayerB?.playbackParams = params
                } catch (e: Exception) {}
            }
            if (channelB != 0 || mediaPlayerB != null) {
                val effBpm = baseBpmB * pitchFactorB
                val strBpm = String.format(Locale.US, "%.1f", effBpm)
                txtBpmRight.text = strBpm
                txtBpm2Right.text = strBpm
                btnPitchRight.text = getString(R.string.pitch)
            } else {
                txtBpmRight.text = "-"
                txtBpm2Right.text = "-"
                btnPitchRight.text = getString(R.string.pitch)
            }
        }
    }

    private fun setupSideTools() {
        val toolLayoutLeft = findViewById<View>(R.id.activity_tool_layout_left)
        val toolLayoutRight = findViewById<View>(R.id.activity_tool_layout_right)

        setupToolButtons(toolLayoutLeft, isLeft = true)
        setupToolButtons(toolLayoutRight, isLeft = false)
    }

    private fun setupToolButtons(toolLayout: View, isLeft: Boolean) {
        val btnEq = toolLayout.findViewById<TextView>(R.id.btn_eq)
        val btnFx = toolLayout.findViewById<TextView>(R.id.btn_fx)
        val btnLoop = toolLayout.findViewById<TextView>(R.id.btn_loop)
        val btnCues = toolLayout.findViewById<TextView>(R.id.btn_cues)
        val btnSamples = toolLayout.findViewById<TextView>(R.id.btn_samples)

        val buttons = listOf(btnEq, btnFx, btnLoop, btnCues, btnSamples)
        val colorStateList = ContextCompat.getColorStateList(
            this,
            if (isLeft) R.color.tool_btn_text_color_selector_left else R.color.tool_btn_text_color_selector_right
        )
        val bgRes = if (isLeft) R.drawable.tool_button_bg_selector_left else R.drawable.tool_button_bg_selector_right

        for (btn in buttons) {
            btn.setBackgroundResource(bgRes)
            btn.setTextColor(colorStateList)
            TextViewCompat.setCompoundDrawableTintList(btn, colorStateList)

            btn.setOnClickListener {
                AdsManager.showInterAll(this) {
                    val isCurrentlyActive = if (isLeft) activeToolBtnLeft == btn else activeToolBtnRight == btn
                    if (isCurrentlyActive) {
                        closeToolPanel(isLeft)
                        btn.isSelected = false
                        if (isLeft) activeToolBtnLeft = null else activeToolBtnRight = null
                    } else {
                        buttons.forEach { it.isSelected = false }
                        btn.isSelected = true
                        if (isLeft) activeToolBtnLeft = btn else activeToolBtnRight = btn

                        when (btn.id) {
                            R.id.btn_eq -> openEqPanel(isLeft)
                            R.id.btn_samples -> openSamplesPanel(isLeft)
                            R.id.btn_loop -> openLoopPanel(isLeft)
                            R.id.btn_cues -> openCuesPanel(isLeft)
                            R.id.btn_fx -> openFxPanel(isLeft)
                        }
                    }
                }
            }
        }
    }

    private fun openEqPanel(isLeft: Boolean) {
        val container = if (isLeft) containerLeft else containerRight
        val toolView = LayoutInflater.from(this).inflate(R.layout.activity_main_tool_eq_layout, container, false)

        val itemIds = listOf(R.id.item_1, R.id.item_2, R.id.item_3, R.id.item_4, R.id.item_5)
        val freqs = listOf("60HZ", "230HZ", "910HZ", "3KHZ", "14KHZ")
        val themeColor = ContextCompat.getColor(this, if (isLeft) R.color.theme_color_left else R.color.theme_color_right)
        val gains = if (isLeft) eqGainsLeft else eqGainsRight

        for (i in itemIds.indices) {
            toolView.findViewById<View>(itemIds[i])?.let { item ->
                val txtFreq = item.findViewById<TextView>(R.id.equalizer_item_text)
                val txtVal = item.findViewById<TextView>(R.id.equalizer_item_value)
                val seekBar = item.findViewById<CustomSeekBar>(R.id.equalizer_item_seek)

                txtFreq?.text = freqs[i]
                txtVal?.setTextColor(themeColor)
                val curGain = gains[i]
                txtVal?.text = "${curGain.toInt()}"
                seekBar?.progressColor = themeColor
                if (!isLeft) {
                    seekBar?.thumbDrawable = ContextCompat.getDrawable(this, R.drawable.seekbar_thumb_v_right)
                }
                // Map -15..+15 dB to 0..1000 progress (center 0 dB = 500)
                seekBar?.progress = (((curGain + 15f) / 30f) * 1000f).toInt().coerceIn(0, 1000)
                seekBar?.setOnSeekBarChangeListener(object : CustomSeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(sb: CustomSeekBar, progress: Int, fromUser: Boolean) {
                        val db = ((progress - 500) / 500f * 15f)
                        txtVal?.text = "${db.toInt()}"
                        setEqBandGain(isLeft, i, db)
                    }
                })
            }
        }

        showToolView(container, toolView, isLeft)
    }

    private fun getSamplerListForDeck(isLeft: Boolean): List<SamplerDef> {
        val items = com.audioeffect.djmixer.music.mixer.sampler.SamplerManager.getSamplerItemsForDeck(this, isLeft)
        return items.map { SamplerDef(it.soundPath, it.title) }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun openSamplesPanel(isLeft: Boolean) {
        val container = if (isLeft) containerLeft else containerRight
        val layoutRes = if (isLeft) R.layout.activity_main_tool_samples_layout_left else R.layout.activity_main_tool_samples_layout_right
        val toolView = LayoutInflater.from(this).inflate(layoutRes, container, false)

        val padBgs = listOf(
            R.drawable.sample_bg_selector_1,
            R.drawable.sample_bg_selector_2,
            R.drawable.sample_bg_selector_3,
            R.drawable.sample_bg_selector_4,
            R.drawable.sample_bg_selector_5,
            R.drawable.sample_bg_selector_6,
            R.drawable.sample_bg_selector_7
        )
        val padIds = listOf(R.id.samples_1, R.id.samples_2, R.id.samples_3, R.id.samples_4)

        val themeColor = ContextCompat.getColor(this, if (isLeft) R.color.theme_color_left else R.color.theme_color_right)
        toolView.findViewById<ImageView>(R.id.btn_more_samplers_image)?.imageTintList = ColorStateList.valueOf(themeColor)
        val txtMore = toolView.findViewById<TextView>(R.id.btn_more_samplers_text)
        txtMore?.setTextColor(themeColor)
        txtMore?.text = getString(R.string.more_samples)
        toolView.findViewById<View>(R.id.btn_more_samplers)?.setOnClickListener {
            startActivity(Intent(this, ActivitySoundSampleStore::class.java))
        }

        val currentSamplerList = getSamplerListForDeck(isLeft)
        val totalPages = (currentSamplerList.size + 3) / 4
        var packIndex = if (isLeft) samplerPackIndexLeft else samplerPackIndexRight

        fun bindPads(root: View, page: Int) {
            val baseIdx = page * 4
            for (i in padIds.indices) {
                val pad = root.findViewById<TextView>(padIds[i]) ?: continue
                val itemIdx = baseIdx + i
                if (itemIdx < currentSamplerList.size) {
                    val item = currentSamplerList[itemIdx]
                    pad.visibility = View.VISIBLE
                    pad.text = item.name
                    pad.setBackgroundResource(padBgs[itemIdx % padBgs.size])
                    pad.setOnTouchListener { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                v.isPressed = true
                                playSampleSound(item.assetPath, isLeft)
                                true
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                v.isPressed = false
                                true
                            }
                            else -> false
                        }
                    }
                } else {
                    pad.visibility = View.INVISIBLE
                }
            }
        }

        val viewPager = toolView.findViewById<androidx.viewpager.widget.ViewPager>(R.id.viewPager)
        if (viewPager != null) {
            viewPager.adapter = object : androidx.viewpager.widget.PagerAdapter() {
                override fun getCount(): Int = totalPages
                override fun isViewFromObject(view: View, obj: Any): Boolean = view === obj
                override fun instantiateItem(vpContainer: ViewGroup, position: Int): Any {
                    val pageView = LayoutInflater.from(this@MainActivity).inflate(
                        R.layout.activity_main_tool_samples_layout_item,
                        vpContainer,
                        false
                    )
                    bindPads(pageView, position)
                    vpContainer.addView(pageView)
                    return pageView
                }
                override fun destroyItem(vpContainer: ViewGroup, position: Int, obj: Any) {
                    vpContainer.removeView(obj as View)
                }
            }
            viewPager.addOnPageChangeListener(object : androidx.viewpager.widget.ViewPager.OnPageChangeListener {
                override fun onPageScrolled(pos: Int, off: Float, offPixels: Int) {}
                override fun onPageScrollStateChanged(state: Int) {}
                override fun onPageSelected(position: Int) {
                    if (isLeft) samplerPackIndexLeft = position else samplerPackIndexRight = position
                }
            })
            viewPager.currentItem = packIndex.coerceIn(0, totalPages - 1)
        }

        // Page arrows
        toolView.findViewById<View>(R.id.arrow_left)?.setOnClickListener {
            viewPager?.let {
                val next = (it.currentItem - 1).coerceAtLeast(0)
                it.setCurrentItem(next, true)
            }
        }
        toolView.findViewById<View>(R.id.arrow_right)?.setOnClickListener {
            viewPager?.let {
                val next = (it.currentItem + 1).coerceAtMost(totalPages - 1)
                it.setCurrentItem(next, true)
            }
        }

        // Sampler volume fader
        toolView.findViewById<CustomSeekBar>(R.id.seekbar_samples)?.let { sb ->
            sb.progressColor = themeColor
            if (!isLeft) {
                sb.thumbDrawable = ContextCompat.getDrawable(this, R.drawable.seekbar_thumb_v_right)
            }
            sb.progress = ((if (isLeft) samplerVolumeLeft else samplerVolumeRight) * 1000).toInt()
            sb.setOnSeekBarChangeListener(object : CustomSeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: CustomSeekBar, progress: Int, fromUser: Boolean) {
                    val vol = progress / 1000f
                    if (isLeft) samplerVolumeLeft = vol else samplerVolumeRight = vol
                }
            })
        }

        // Sampler crossfade switch
        toolView.findViewById<SelectBox>(R.id.samples_switch)?.let { sb ->
            sb.isSelected = if (isLeft) samplerRouteCrossfadeLeft else samplerRouteCrossfadeRight
            sb.setOnSelectChangedListener(object : SelectBox.OnSelectChangedListener {
                override fun onSelectChanged(fromUser: Boolean, isSelected: Boolean) {
                    if (isLeft) samplerRouteCrossfadeLeft = isSelected else samplerRouteCrossfadeRight = isSelected
                }
            })
        }

        showToolView(container, toolView, isLeft)
    }

    private fun openLoopPanel(isLeft: Boolean) {
        val container = if (isLeft) containerLeft else containerRight
        val layoutRes = if (isLeft) R.layout.activity_main_tool_loop_layout_left else R.layout.activity_main_tool_loop_layout_right
        val toolView = LayoutInflater.from(this).inflate(layoutRes, container, false)

        val channel = if (isLeft) channelA else channelB
        val txtCurrentLoop = toolView.findViewById<TextView>(R.id.current_loop)
        val txtLoopIn = toolView.findViewById<TextView>(R.id.loop_in)
        val txtLoopOut = toolView.findViewById<TextView>(R.id.loop_out)

        val loopButtons = listOf(
            R.id.loop_8 to "8", R.id.loop_4 to "4", R.id.loop_2 to "2",
            R.id.loop_1 to "1", R.id.loop_1_2 to "1/2", R.id.loop_1_4 to "1/4"
        )
        val beatValues = mapOf("8" to 8f, "4" to 4f, "2" to 2f, "1" to 1f, "1/2" to 0.5f, "1/4" to 0.25f)

        var selectedBeatStr = if (isLeft) (if (currentLoopBeatsA > 0) "$currentLoopBeatsA" else "8") else (if (currentLoopBeatsB > 0) "$currentLoopBeatsB" else "8")

        fun updateLoopUi() {
            val isLooping = if (isLeft) isLoopingA else isLoopingB
            txtCurrentLoop?.isSelected = isLooping
            txtLoopOut?.isSelected = isLooping
            txtCurrentLoop?.text = "Loop:$selectedBeatStr"

            if (channel != 0) {
                val startPos = if (isLeft) loopStartA else loopStartB
                val endPos = if (isLeft) loopEndA else loopEndB
                val inSec = BASS.BASS_ChannelBytes2Seconds(channel, startPos.coerceAtLeast(0L)).toInt()
                val outSec = BASS.BASS_ChannelBytes2Seconds(channel, endPos.coerceAtLeast(0L)).toInt()
                txtLoopIn?.text = String.format("IN %02d:%02d", inSec / 60, inSec % 60)
                txtLoopOut?.text = String.format("OUT %02d:%02d", outSec / 60, outSec % 60)
            } else {
                txtLoopIn?.text = "IN 00:00"
                txtLoopOut?.text = "OUT 00:00"
            }
        }

        fun activateLoop(beatsStr: String) {
            selectedBeatStr = beatsStr
            val beats = beatValues[beatsStr] ?: 8f
            if (channel != 0) {
                val bpm = if (isLeft) baseBpmA * pitchFactorA else baseBpmB * pitchFactorB
                val beatSeconds = (60f / bpm) * beats
                val startPos = BASS.BASS_ChannelGetPosition(channel, BASS.BASS_POS_BYTE)
                val endPos = startPos + BASS.BASS_ChannelSeconds2Bytes(channel, beatSeconds.toDouble())
                if (isLeft) {
                    loopStartA = startPos
                    loopEndA = endPos
                    isLoopingA = true
                    currentLoopBeatsA = beats.toInt()
                } else {
                    loopStartB = startPos
                    loopEndB = endPos
                    isLoopingB = true
                    currentLoopBeatsB = beats.toInt()
                }
            }
            updateLoopUi()
        }

        for ((btnId, beatStr) in loopButtons) {
            val btn = toolView.findViewById<TextView>(btnId)
            btn?.isSelected = ((if (isLeft) isLoopingA else isLoopingB) && selectedBeatStr == beatStr)
            btn?.setOnClickListener { v ->
                val isCurrentlySelected = v.isSelected
                loopButtons.forEach { toolView.findViewById<View>(it.first)?.isSelected = false }
                if (!isCurrentlySelected) {
                    v.isSelected = true
                    activateLoop(beatStr)
                } else {
                    if (isLeft) isLoopingA = false else isLoopingB = false
                    updateLoopUi()
                }
            }
        }

        txtCurrentLoop?.setOnClickListener {
            val wasLooping = if (isLeft) isLoopingA else isLoopingB
            if (wasLooping) {
                if (isLeft) isLoopingA = false else isLoopingB = false
                loopButtons.forEach { toolView.findViewById<View>(it.first)?.isSelected = false }
            } else {
                activateLoop(selectedBeatStr)
                val targetId = loopButtons.firstOrNull { it.second == selectedBeatStr }?.first
                if (targetId != null) toolView.findViewById<View>(targetId)?.isSelected = true
            }
            updateLoopUi()
        }

        txtLoopIn?.setOnClickListener {
            if (channel != 0) {
                val pos = BASS.BASS_ChannelGetPosition(channel, BASS.BASS_POS_BYTE)
                if (isLeft) loopStartA = pos else loopStartB = pos
                val sec = BASS.BASS_ChannelBytes2Seconds(channel, pos).toInt()
                txtLoopIn.text = String.format("IN %02d:%02d", sec / 60, sec % 60)
            }
        }

        txtLoopOut?.setOnClickListener {
            if (channel != 0) {
                val pos = BASS.BASS_ChannelGetPosition(channel, BASS.BASS_POS_BYTE)
                if (isLeft) {
                    loopEndA = pos
                    isLoopingA = true
                } else {
                    loopEndB = pos
                    isLoopingB = true
                }
                updateLoopUi()
            }
        }

        val beatOrder = listOf("1/4", "1/2", "1", "2", "4", "8")
        toolView.findViewById<View>(R.id.arrow_left)?.setOnClickListener {
            val curIdx = beatOrder.indexOf(selectedBeatStr).takeIf { it >= 0 } ?: 5
            if (curIdx > 0) {
                val newBeat = beatOrder[curIdx - 1]
                loopButtons.forEach { toolView.findViewById<View>(it.first)?.isSelected = (it.second == newBeat) }
                activateLoop(newBeat)
            }
        }
        toolView.findViewById<View>(R.id.arrow_right)?.setOnClickListener {
            val curIdx = beatOrder.indexOf(selectedBeatStr).takeIf { it >= 0 } ?: 5
            if (curIdx < beatOrder.size - 1) {
                val newBeat = beatOrder[curIdx + 1]
                loopButtons.forEach { toolView.findViewById<View>(it.first)?.isSelected = (it.second == newBeat) }
                activateLoop(newBeat)
            }
        }

        updateLoopUi()
        showToolView(container, toolView, isLeft)
    }

    private fun updateWaveCues(isLeft: Boolean) {
        val channel = if (isLeft) channelA else channelB
        val mp = if (isLeft) mediaPlayerA else mediaPlayerB
        val wave = if (isLeft) waveLeft else waveRight
        val hotCues = if (isLeft) hotCuesA else hotCuesB
        val len = if (channel != 0) BASS.BASS_ChannelGetLength(channel, BASS.BASS_POS_BYTE) else (mp?.duration?.toLong() ?: 1L)
        if (len > 0) {
            wave.hotCuePositions = FloatArray(hotCues.size) { i ->
                if (hotCues[i] >= 0) (hotCues[i].toFloat() / len).coerceIn(0f, 1f) else -1f
            }
        }
    }

    private fun seekDeckToByte(isLeft: Boolean, targetByte: Long) {
        val channel = if (isLeft) channelA else channelB
        val mp = if (isLeft) mediaPlayerA else mediaPlayerB
        val turntable = if (isLeft) turntableLeft else turntableRight
        val wave = if (isLeft) waveLeft else waveRight
        val waveView = if (isLeft) waveViewLeft else waveViewRight

        val len = if (channel != 0) BASS.BASS_ChannelGetLength(channel, BASS.BASS_POS_BYTE) else (mp?.duration?.toLong() ?: 1L)
        val posRatio = if (len > 0) (targetByte.toFloat() / len).coerceIn(0f, 1f) else 0f

        if (channel != 0) {
            BASS.BASS_ChannelSetPosition(channel, targetByte, BASS.BASS_POS_BYTE)
            val isPlaying = if (isLeft) isPlayingA else isPlayingB
            if (isPlaying) {
                BASS.BASS_ChannelPlay(channel, false)
            }
        } else {
            mp?.seekTo(targetByte.toInt())
        }

        turntable.progress = posRatio
        wave.progress = posRatio
        val posMs = if (channel != 0) (BASS.BASS_ChannelBytes2Seconds(channel, targetByte) * 1000).toInt() else targetByte.toInt()
        waveView.playOffset = posMs * 0.25f

        val curSec = posMs / 1000
        val totalSec = if (channel != 0) BASS.BASS_ChannelBytes2Seconds(channel, len).toInt() else ((mp?.duration ?: 0) / 1000)
        val remSec = (totalSec - curSec).coerceAtLeast(0)
        val timeStr = when (timeMode) {
            0 -> String.format("%02d:%02d", curSec / 60, curSec % 60)
            1 -> String.format("-%02d:%02d", remSec / 60, remSec % 60)
            else -> String.format("%02d:%02d / %02d:%02d", curSec / 60, curSec % 60, totalSec / 60, totalSec % 60)
        }
        if (isLeft) txtDurationLeft.text = timeStr else txtDurationRight.text = timeStr
    }

    private fun isCueSlotUnlocked(slotIndex: Int): Boolean {
        if (slotIndex < 4) return true
        val prefs = getSharedPreferences("dj_mixer_cues_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("unlocked_slot_$slotIndex", false)
    }

    private fun unlockCueSlot(slotIndex: Int) {
        val prefs = getSharedPreferences("dj_mixer_cues_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("unlocked_slot_$slotIndex", true).apply()
    }

    private fun openCuesPanel(isLeft: Boolean) {
        val container = if (isLeft) containerLeft else containerRight
        val toolView = LayoutInflater.from(this).inflate(R.layout.activity_main_tool_cues_layout, container, false)
        val hotCues = if (isLeft) hotCuesA else hotCuesB

        val btnClear = toolView.findViewById<TextView>(R.id.clear)
        val cueIds = listOf(R.id.cues_1, R.id.cues_2, R.id.cues_3, R.id.cues_4, R.id.cues_5, R.id.cues_6)
        val cueBgs = listOf(
            R.drawable.cues_bg_1_selector,
            R.drawable.cues_bg_2_selector,
            R.drawable.cues_bg_3_selector,
            R.drawable.cues_bg_4_selector,
            R.drawable.cues_bg_5_selector,
            R.drawable.cues_bg_6_selector
        )
        val cueNames = listOf("A", "B", "C", "D", "E", "F")

        if (!isLeft) {
            btnClear?.setBackgroundResource(R.drawable.cues_clear_bg_selector_right)
            btnClear?.setTextColor(ContextCompat.getColorStateList(this, R.color.cues_clear_text_color_selector_right))
        }

        for (i in cueIds.indices) {
            toolView.findViewById<View>(cueIds[i])?.setBackgroundResource(cueBgs[i])
        }

        fun updateCuesView() {
            val curChannel = if (isLeft) channelA else channelB
            val curMp = if (isLeft) mediaPlayerA else mediaPlayerB
            val hasAnyCue = hotCues.any { it >= 0 }
            btnClear?.isEnabled = hasAnyCue
            if (!hasAnyCue) {
                if (isLeft) isCueDeleteModeLeft = false else isCueDeleteModeRight = false
            }
            val activeDelete = if (isLeft) isCueDeleteModeLeft else isCueDeleteModeRight
            btnClear?.isSelected = activeDelete
            btnClear?.text = if (activeDelete) getString(R.string.done) else getString(R.string.clear)

            if (isLeft) {
                callCuesLeft.isEnabled = hasAnyCue || tempCueA > 0
                callCuesLeft.isSelected = hasAnyCue
            } else {
                callCuesRight.isEnabled = hasAnyCue || tempCueB > 0
                callCuesRight.isSelected = hasAnyCue
            }

            for (i in cueIds.indices) {
                val item = toolView.findViewById<View>(cueIds[i]) ?: continue
                val txtName = item.findViewById<TextView>(R.id.name)
                val txtTime = item.findViewById<TextView>(R.id.time)
                val imgDelete = item.findViewById<ImageView>(R.id.delete)
                val lockView = item.findViewById<View>(R.id.lock)
                val cuePos = hotCues[i]

                txtName?.text = cueNames[i]

                val isLocked = !isCueSlotUnlocked(i)
                lockView?.visibility = if (isLocked) View.VISIBLE else View.GONE

                if (cuePos >= 0) {
                    item.isSelected = true
                    item.isEnabled = true
                    txtTime?.visibility = View.VISIBLE
                    imgDelete?.visibility = if (activeDelete) View.VISIBLE else View.GONE
                    txtName?.setTextColor(0xFF000000.toInt())
                    val sec = if (curChannel != 0) {
                        BASS.BASS_ChannelBytes2Seconds(curChannel, cuePos).toInt()
                    } else {
                        (cuePos / 1000).toInt()
                    }
                    txtTime?.text = String.format("%02d:%02d", sec / 60, sec % 60)
                } else {
                    item.isSelected = false
                    item.isEnabled = !activeDelete
                    txtTime?.visibility = View.GONE
                    imgDelete?.visibility = View.GONE
                    txtName?.setTextColor(if (activeDelete) 0x80FFFFFF.toInt() else 0xFFFFFFFF.toInt())
                }
            }
        }

        btnClear?.setOnClickListener {
            if (isLeft) isCueDeleteModeLeft = !isCueDeleteModeLeft else isCueDeleteModeRight = !isCueDeleteModeRight
            updateCuesView()
        }

        for (i in cueIds.indices) {
            val item = toolView.findViewById<View>(cueIds[i]) ?: continue
            item.setOnClickListener {
                if (!isCueSlotUnlocked(i)) {
                    AdsManager.showReward(
                        activity = this,
                        onEarnedReward = {
                            unlockCueSlot(i)
                            AdsManager.setRewardWatched()
                            updateCuesView()
                            Toast.makeText(this, getString(R.string.toast_unlocked_sound, "Cue ${cueNames[i]}"), Toast.LENGTH_SHORT).show()
                        },
                        onFailed = {
                            Toast.makeText(this, R.string.toast_watch_reward_unlock, Toast.LENGTH_SHORT).show()
                        }
                    )
                    return@setOnClickListener
                }
                val curChannel = if (isLeft) channelA else channelB
                val curMp = if (isLeft) mediaPlayerA else mediaPlayerB
                val hasSong = (curChannel != 0 || curMp != null)
                if (!hasSong) {
                    Toast.makeText(this, getString(R.string.toast_select_song_first), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val activeDelete = if (isLeft) isCueDeleteModeLeft else isCueDeleteModeRight
                if (activeDelete) {
                    hotCues[i] = -1L
                    updateCuesView()
                    updateWaveCues(isLeft)
                    Toast.makeText(this, getString(R.string.toast_cue_deleted, cueNames[i]), Toast.LENGTH_SHORT).show()
                } else {
                    if (hotCues[i] >= 0) {
                        seekDeckToByte(isLeft, hotCues[i])
                        val isPlaying = if (isLeft) isPlayingA else isPlayingB
                        if (!isPlaying) {
                            if (isLeft) toggleDeckA() else toggleDeckB()
                        }
                    } else {
                        val curPos = if (curChannel != 0) BASS.BASS_ChannelGetPosition(curChannel, BASS.BASS_POS_BYTE) else (curMp?.currentPosition?.toLong() ?: 0L)
                        hotCues[i] = curPos
                        updateCuesView()
                        updateWaveCues(isLeft)
                        Toast.makeText(this, getString(R.string.toast_cue_slot_set, cueNames[i]), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        updateCuesView()
        showToolView(container, toolView, isLeft)
    }

    private fun openFxPanel(isLeft: Boolean) {
        val container = if (isLeft) containerLeft else containerRight
        val toolView = LayoutInflater.from(this).inflate(R.layout.activity_main_tool_fx_layout, container, false)
        val rotateBar = toolView.findViewById<RotateStepBar>(R.id.rotate_step_bar_bass)

        val themeColor = ContextCompat.getColor(this, if (isLeft) R.color.theme_color_left else R.color.theme_color_right)
        val indicatorRes = if (isLeft) R.drawable.fx_rotate_indicator_left else R.drawable.fx_rotate_indicator_right
        rotateBar?.setCircleProgressColor(themeColor)
        rotateBar?.setIndicatorDrawable(ContextCompat.getDrawable(this, indicatorRes))
        rotateBar?.progress = if (isLeft) bassBoostProgressLeft else bassBoostProgressRight
        rotateBar?.setOnRotateListener(object : RotateStepBar.OnRotateListener {
            override fun onProgressChanged(progress: Int, fromUser: Boolean) {
                setBassBoostProgress(isLeft, progress)
                val pct = (progress / 1000f * 100).toInt()
                Log.d(tag, "Bass Boost Deck ${if (isLeft) "A" else "B"}: $pct%")
            }
        })

        showToolView(container, toolView, isLeft)
    }

    private fun showToolView(container: FrameLayout, toolView: View, isLeft: Boolean) {
        closeToolPanel(isLeft)
        if (isLeft) {
            turntableRootLeft.visibility = View.GONE
            activeToolViewLeft = toolView
        } else {
            turntableRootRight.visibility = View.GONE
            activeToolViewRight = toolView
        }
        container.addView(toolView)
    }

    private fun closeToolPanel(isLeft: Boolean) {
        val container = if (isLeft) containerLeft else containerRight
        val activeView = if (isLeft) activeToolViewLeft else activeToolViewRight
        activeView?.let {
            container.removeView(it)
            if (isLeft) {
                activeToolViewLeft = null
                turntableRootLeft.visibility = View.VISIBLE
            } else {
                activeToolViewRight = null
                turntableRootRight.visibility = View.VISIBLE
            }
        }
    }

    private fun toggleDeckA() {
        isPlayingA = !isPlayingA
        btnPlayLeft.isSelected = isPlayingA
        turntableLeft.isRotating = isPlayingA
        turntableLeft.setIndicatorState(if (isPlayingA) 1 else 0)

        if (channelA != 0) {
            if (isPlayingA) {
                BASS.BASS_ChannelPlay(channelA, false)
            } else {
                BASS.BASS_ChannelPause(channelA)
            }
        } else {
            try {
                if (isPlayingA) {
                    mediaPlayerA?.start()
                } else {
                    mediaPlayerA?.pause()
                }
            } catch (e: Exception) {}
        }
    }

    private fun toggleDeckB() {
        isPlayingB = !isPlayingB
        btnPlayRight.isSelected = isPlayingB
        turntableRight.isRotating = isPlayingB
        turntableRight.setIndicatorState(if (isPlayingB) 1 else 0)

        if (channelB != 0) {
            if (isPlayingB) {
                BASS.BASS_ChannelPlay(channelB, false)
            } else {
                BASS.BASS_ChannelPause(channelB)
            }
        } else {
            try {
                if (isPlayingB) {
                    mediaPlayerB?.start()
                } else {
                    mediaPlayerB?.pause()
                }
            } catch (e: Exception) {}
        }
    }

    private fun applyVolumes() {
        val finalVolA = if (isMutedA) 0f else (volumeA * crossfadeLeftGain).coerceIn(0f, 1f)
        val finalVolB = if (isMutedB) 0f else (volumeB * crossfadeRightGain).coerceIn(0f, 1f)

        if (channelA != 0) {
            BASS.BASS_ChannelSetAttribute(channelA, BASS.BASS_ATTRIB_VOL, finalVolA)
        } else {
            try {
                mediaPlayerA?.setVolume(finalVolA, finalVolA)
            } catch (e: Exception) {}
        }

        if (channelB != 0) {
            BASS.BASS_ChannelSetAttribute(channelB, BASS.BASS_ATTRIB_VOL, finalVolB)
        } else {
            try {
                mediaPlayerB?.setVolume(finalVolB, finalVolB)
            } catch (e: Exception) {}
        }
    }

    private fun animateCrossfader(targetProgress: Int) {
        val animator = ValueAnimator.ofInt(crossFadeSeekBar.progress, targetProgress)
        animator.duration = 350
        animator.addUpdateListener { anim ->
            crossFadeSeekBar.progress = anim.animatedValue as Int
            crossfadeLeftGain = crossFadeSeekBar.getLeftGain()
            crossfadeRightGain = crossFadeSeekBar.getRightGain()
            applyVolumes()
        }
        animator.start()
    }

    private fun handleScratchMove(deck: Int, deltaDegrees: Float, velocity: Float) {
        if (!isScratchSoundEnabled) return
        val sp = scratchSoundPool ?: return

        val speed = Math.abs(velocity)
        val absDelta = Math.abs(deltaDegrees)

        // Reschedule watchdog (40ms). If hand stops moving, watchdog immediately halts scratch audio!
        val watchdog = if (deck == 1) scratchStopRunnableA else scratchStopRunnableB
        scratchStopHandler.removeCallbacks(watchdog)
        scratchStopHandler.postDelayed(watchdog, 40L)

        // If speed or angle delta is negligible, hand is resting motionless on the vinyl
        if (speed < 12f || absDelta < 0.10f) {
            stopScratchStream(deck)
            return
        }

        val currentDir = if (deltaDegrees >= 0) 1 else -1
        val lastDir = if (deck == 1) lastScratchDirA else lastScratchDirB
        val activeStream = if (deck == 1) activeScratchStreamA else activeScratchStreamB
        val pairIndex = if (deck == 1) scratchPairA else scratchPairB

        // Dynamic pitch/rate following physical speed of record (33 RPM ~ 180-200 deg/sec = 1.0x rate)
        val rate = (0.55f + (speed / 180f) * 0.55f).coerceIn(0.50f, 2.00f)

        // Dynamic volume matching velocity and mixer gain
        val baseGain = if (deck == 1) volumeA * crossfadeLeftGain else volumeB * crossfadeRightGain
        val dynamicVol = (baseGain * (0.60f + (speed / 280f) * 0.40f)).coerceIn(0.12f, 1.0f)
        val leftVol = if (deck == 1) dynamicVol else dynamicVol * crossfadeRightGain
        val rightVol = if (deck == 2) dynamicVol else dynamicVol * crossfadeLeftGain

        if (activeStream != 0 && currentDir == lastDir) {
            // Continuing stroke in same direction: update rate & volume seamlessly with ZERO latency!
            sp.setRate(activeStream, rate)
            sp.setVolume(activeStream, leftVol, rightVol)
        } else {
            // Direction reversed (forward <-> backward) or starting a new stroke!
            if (activeStream != 0) {
                sp.stop(activeStream)
            }
            val soundArray = if (currentDir > 0) scratchFwdSoundIds else scratchRevSoundIds
            val soundId = soundArray[pairIndex % soundArray.size]
            if (soundId > 0) {
                // Play looping indefinitely while moving
                val streamId = sp.play(soundId, leftVol, rightVol, 1, -1, rate)
                if (deck == 1) {
                    activeScratchStreamA = streamId
                    lastScratchDirA = currentDir
                } else {
                    activeScratchStreamB = streamId
                    lastScratchDirB = currentDir
                }
            }
        }
    }

    private fun stopScratchStream(deck: Int) {
        val sp = scratchSoundPool ?: return
        if (deck == 1) {
            scratchStopHandler.removeCallbacks(scratchStopRunnableA)
            if (activeScratchStreamA != 0) {
                sp.stop(activeScratchStreamA)
                activeScratchStreamA = 0
                lastScratchDirA = 0
            }
        } else {
            scratchStopHandler.removeCallbacks(scratchStopRunnableB)
            if (activeScratchStreamB != 0) {
                sp.stop(activeScratchStreamB)
                activeScratchStreamB = 0
                lastScratchDirB = 0
            }
        }
    }

    private fun playSampleSound(assetPath: String, isLeft: Boolean) {
        val routeCross = if (isLeft) samplerRouteCrossfadeLeft else samplerRouteCrossfadeRight
        val crossGain = if (routeCross) (if (isLeft) crossfadeLeftGain else crossfadeRightGain) else 1.0f
        val baseVol = if (isLeft) samplerVolumeLeft else samplerVolumeRight
        val vol = (baseVol * crossGain).coerceIn(0f, 1f)

        var channel = samplerChannelMap[assetPath] ?: 0
        if (channel == 0) {
            val sampleFile = copyAssetToCache(assetPath)
            if (sampleFile != null) {
                channel = BASSEncrypt.BASS_StreamCreateFile(sampleFile, 0)
                if (channel != 0) {
                    samplerChannelMap[assetPath] = channel
                }
            }
        }

        if (channel != 0) {
            BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_VOL, vol)
            BASS.BASS_ChannelPlay(channel, true)
            Log.d(tag, "Playing sample $assetPath on channel $channel at vol $vol")
        } else {
            Log.e(tag, "Failed to play sample $assetPath, error=${BASS.BASS_ErrorGetCode()}")
        }
    }

    private fun nudgePitch(channel: Int, factor: Float) {
        if (channel == 0) return
        val currentFreq = BASS.FloatValue()
        BASS.BASS_ChannelGetAttribute(channel, BASS.BASS_ATTRIB_FREQ, currentFreq)
        val orig = if (currentFreq.value > 0) currentFreq.value else 44100f
        BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_FREQ, orig * factor)
        handler.postDelayed({
            if (channel != 0) {
                BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_FREQ, orig)
            }
        }, 200)
    }

    private fun nudgePitchContinuous(channel: Int, factor: Float, isLeft: Boolean) {
        if (channel == 0) return
        val baseFactor = if (isLeft) pitchFactorA else pitchFactorB
        BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_FREQ, 44100f * baseFactor * factor)
    }

    private fun restorePitch(channel: Int, isLeft: Boolean) {
        if (channel == 0) return
        val baseFactor = if (isLeft) pitchFactorA else pitchFactorB
        BASS.BASS_ChannelSetAttribute(channel, BASS.BASS_ATTRIB_FREQ, 44100f * baseFactor)
    }

    private fun extractCoverArt(filePath: String, isDeckA: Boolean): Drawable? {
        try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(filePath)
            val data = mmr.embeddedPicture
            mmr.release()
            if (data != null) {
                val isDefaultCyan = data.size == 241546
                if (!isDeckA && isDefaultCyan) {
                    val yellowBitmap = BitmapFactory.decodeResource(resources, R.drawable.rotate_cd_cover_right)
                    if (yellowBitmap != null) {
                        return createCircularDrawable(yellowBitmap)
                    }
                }
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                if (bitmap != null) {
                    if (!isDeckA && isDefaultCyanCover(bitmap)) {
                        val yellowBitmap = BitmapFactory.decodeResource(resources, R.drawable.rotate_cd_cover_right)
                        if (yellowBitmap != null) {
                            return createCircularDrawable(yellowBitmap)
                        }
                    }
                    return createCircularDrawable(bitmap)
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to extract cover art from $filePath", e)
        }
        return null
    }

    private fun isDefaultCyanCover(bitmap: Bitmap): Boolean {
        if (bitmap.width == 600 && bitmap.height == 600) {
            val p = bitmap.getPixel(100, 100)
            val r = Color.red(p)
            val g = Color.green(p)
            val b = Color.blue(p)
            return b > 180 && g > 160 && r < 100
        }
        return false
    }

    private fun createCircularDrawable(src: Bitmap): Drawable {
        val size = minOf(src.width, src.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.isFilterBitmap = true
        val shader = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        paint.shader = shader
        val r = size / 2.0f
        canvas.drawCircle(r, r, r, paint)
        return BitmapDrawable(resources, output)
    }

    private fun loadAudioFromFile(filePath: String, trackTitle: String?, isDeckA: Boolean) {
        try {
            val audioFile = File(filePath)
            val actualPath = if (audioFile.canRead()) {
                filePath
            } else {
                val cached = File(cacheDir, "track_${if (isDeckA) "A" else "B"}.${audioFile.extension.ifEmpty { "mp3" }}")
                try {
                    audioFile.inputStream().use { input ->
                        cached.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    cached.absolutePath
                } catch (e: Exception) {
                    filePath
                }
            }

            if (isDeckA) {
                if (channelA != 0) {
                    BASS.BASS_StreamFree(channelA)
                    channelA = 0
                    eqHandlesLeft.fill(0)
                    bassHandleLeft = 0
                }
                try {
                    mediaPlayerA?.stop()
                    mediaPlayerA?.release()
                    mediaPlayerA = null
                } catch (e: Exception) {}

                channelA = BASS.BASS_StreamCreateFile(actualPath, 0, 0, 0)
                if (channelA != 0) {
                    val lenBytes = BASS.BASS_ChannelGetLength(channelA, BASS.BASS_POS_BYTE)
                    val durSec = BASS.BASS_ChannelBytes2Seconds(channelA, lenBytes)
                    durationMsA = (durSec * 1000).toInt()
                } else {
                    mediaPlayerA = MediaPlayer().apply {
                        setDataSource(actualPath)
                        isLooping = false
                        prepare()
                    }
                    durationMsA = mediaPlayerA?.duration ?: 0
                }

                turntableLeft.progress = 0f
                turntableLeft.isDragEnable = true
                val cover = extractCoverArt(actualPath, isDeckA = true)
                turntableLeft.setAlbumDrawable(cover)
                turntableLeft.setLogoResource(R.drawable.rotate_cd_logo_left)
                turntableLeft.setIndicatorState(1)
                waveLeft.hasAudio = true
                waveLeft.progress = 0f
                waveViewLeft.hasAudio = true
                btnPlayLeft.isEnabled = true
                callCuesLeft.isEnabled = true
                keyLeft.visibility = View.VISIBLE
                keyLeft.text = "12B"
                tempCueA = 0L
                hotCuesA.fill(-1L)
                waveLeft.cuePosition = -1f
                waveLeft.hotCuePositions = null
                txtTitleLeft.text = trackTitle ?: File(filePath).nameWithoutExtension
                updatePitchUi(isLeft = true)
                turntableLeft.isRotating = isPlayingA
                if (isPlayingA) {
                    if (channelA != 0) {
                        BASS.BASS_ChannelPlay(channelA, false)
                    } else {
                        mediaPlayerA?.start()
                    }
                }
            } else {
                if (channelB != 0) {
                    BASS.BASS_StreamFree(channelB)
                    channelB = 0
                    eqHandlesRight.fill(0)
                    bassHandleRight = 0
                }
                try {
                    mediaPlayerB?.stop()
                    mediaPlayerB?.release()
                    mediaPlayerB = null
                } catch (e: Exception) {}

                channelB = BASS.BASS_StreamCreateFile(actualPath, 0, 0, 0)
                if (channelB != 0) {
                    val lenBytes = BASS.BASS_ChannelGetLength(channelB, BASS.BASS_POS_BYTE)
                    val durSec = BASS.BASS_ChannelBytes2Seconds(channelB, lenBytes)
                    durationMsB = (durSec * 1000).toInt()
                } else {
                    mediaPlayerB = MediaPlayer().apply {
                        setDataSource(actualPath)
                        isLooping = false
                        prepare()
                    }
                    durationMsB = mediaPlayerB?.duration ?: 0
                }

                turntableRight.progress = 0f
                turntableRight.isDragEnable = true
                val cover = extractCoverArt(actualPath, isDeckA = false)
                turntableRight.setAlbumDrawable(cover)
                turntableRight.setLogoResource(R.drawable.rotate_cd_logo_right)
                turntableRight.setIndicatorState(1)
                waveRight.hasAudio = true
                waveRight.progress = 0f
                waveViewRight.hasAudio = true
                btnPlayRight.isEnabled = true
                callCuesRight.isEnabled = true
                keyRight.visibility = View.VISIBLE
                keyRight.text = "12B"
                tempCueB = 0L
                hotCuesB.fill(-1L)
                waveRight.cuePosition = -1f
                waveRight.hotCuePositions = null
                txtTitleRight.text = trackTitle ?: File(filePath).nameWithoutExtension
                updatePitchUi(isLeft = false)
                turntableRight.isRotating = isPlayingB
                if (isPlayingB) {
                    if (channelB != 0) {
                        BASS.BASS_ChannelPlay(channelB, false)
                    } else {
                        mediaPlayerB?.start()
                    }
                }
            }
            applyVolumes()
            applyDeckEq(isDeckA)
            applyDeckBass(isDeckA)
            val titleToDisplay = trackTitle ?: File(filePath).nameWithoutExtension
            Toast.makeText(this, getString(R.string.toast_track_loaded, titleToDisplay), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(tag, "Failed to load audio file", e)
        }
    }

    private fun loadAudioFromUri(uri: Uri, isDeckA: Boolean) {
        try {
            val file = File(cacheDir, "imported_${if (isDeckA) "A" else "B"}.mp3")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            loadAudioFromFile(file.absolutePath, "Imported Audio", isDeckA)
        } catch (e: Exception) {
            Log.e(tag, "Failed to load audio from URI", e)
        }
    }

    private fun cycleTimeMode() {
        timeMode = (timeMode + 1) % 3
        val modeName = when (timeMode) {
            0 -> getString(R.string.time_mode_elapsed)
            1 -> getString(R.string.time_mode_remaining)
            else -> getString(R.string.time_mode_both)
        }
        Toast.makeText(this, getString(R.string.toast_time_mode, modeName), Toast.LENGTH_SHORT).show()
    }

    private fun showSaveRecordingDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_audio_item_save, null)
        val edtTitle = dialogView.findViewById<EditText>(R.id.edittext)
        val btnClear = dialogView.findViewById<View>(R.id.btn_clear_text)
        val btnDelete = dialogView.findViewById<TextView>(R.id.dialog_button_delete)
        val btnSave = dialogView.findViewById<TextView>(R.id.dialog_button_save)

        val recDir = File(getExternalFilesDir(null), "DJMixer/Recordings")
        recDir.mkdirs()
        val count = (recDir.listFiles()?.size ?: 0) + 1
        val defaultName = "My recording - $count"
        edtTitle.setText(defaultName)
        edtTitle.setSelection(defaultName.length)

        btnClear?.setOnClickListener {
            edtTitle.setText("")
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnDelete.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(this, getString(R.string.recording_discarded), Toast.LENGTH_SHORT).show()
        }

        btnSave.setOnClickListener {
            val inputName = edtTitle.text.toString().trim().ifEmpty { defaultName }
            val targetFile = File(recDir, "$inputName.${recFormat.lowercase(java.util.Locale.getDefault())}")

            try {
                targetFile.writeBytes(ByteArray(44100 * 4))
                Toast.makeText(this, getString(R.string.recording_saved, targetFile.name), Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.recording_save_error), Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        dialog.show()

        val dialogWidth = (390 * resources.displayMetrics.density).toInt()
        dialog.window?.setLayout(dialogWidth, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun showSettingsDialog() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_main_settings, null)
        dialog.setContentView(dialogView)

        val density = resources.displayMetrics.density
        val dialogBg = GradientDrawable().apply {
            colors = intArrayOf(Color.parseColor("#151821"), Color.parseColor("#1F2430"), Color.parseColor("#151821"))
            orientation = GradientDrawable.Orientation.LEFT_RIGHT
            val r = 24f * density
            cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        }
        dialogView.background = dialogBg

        // Gradient & Ripple styles for items
        val makeRipple = { c1: Int, c2: Int, rDp: Float ->
            val gd = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(c1, c2)).apply {
                cornerRadius = rDp * resources.displayMetrics.density
            }
            RippleDrawable(ColorStateList.valueOf(0x34000000), gd, null)
        }
        dialogView.findViewById<View>(R.id.ds_auto_mix)?.background = makeRipple(-16733441, -16748823, 16f)
        dialogView.findViewById<View>(R.id.ds_setting_layout)?.let {
            val gd = GradientDrawable().apply {
                cornerRadius = 8f * density
                setColor(0x34000000)
            }
            it.background = RippleDrawable(ColorStateList.valueOf(0x34000000), gd, null)
        }
        dialogView.findViewById<View>(R.id.ds_guide_tutorial)?.background = makeRipple(-14244097, -16747777, 8f)
        dialogView.findViewById<View>(R.id.ds_sampler_store)?.background = makeRipple(-5024513, -9943553, 8f)
        dialogView.findViewById<View>(R.id.ds_drumpad)?.background = makeRipple(-16861, -35800, 16f)
        dialogView.findViewById<View>(R.id.ds_beat_maker)?.background = makeRipple(-5024513, -9943553, 16f)
        dialogView.findViewById<View>(R.id.ds_ringtone_cutter)?.background = makeRipple(-28834, -49012, 16f)
        dialogView.findViewById<View>(R.id.ds_audio_mixer)?.background = makeRipple(-41792, -57202, 16f)
        dialogView.findViewById<View>(R.id.ds_audio_merge)?.background = makeRipple(-30208, -321266, 16f)
        dialogView.findViewById<View>(R.id.ds_remove_ads)?.background = makeRipple(-21464, -39106, 64f)

        dialogView.findViewById<View>(R.id.ds_cancel)?.setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<View>(R.id.ds_setting_layout)?.setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, ActivitySetting::class.java))
        }
        dialogView.findViewById<View>(R.id.ds_auto_mix)?.setOnClickListener {
            Toast.makeText(this, R.string.auto_mix, Toast.LENGTH_SHORT).show()
        }
        dialogView.findViewById<View>(R.id.ds_beat_maker)?.setOnClickListener {
            Toast.makeText(this, R.string.beat_maker, Toast.LENGTH_SHORT).show()
        }
        dialogView.findViewById<View>(R.id.ds_drumpad)?.setOnClickListener {
            Toast.makeText(this, R.string.drum_pad, Toast.LENGTH_SHORT).show()
        }
        dialogView.findViewById<View>(R.id.ds_ringtone_cutter)?.setOnClickListener {
            Toast.makeText(this, R.string.ringtone_cutter, Toast.LENGTH_SHORT).show()
        }
        dialogView.findViewById<View>(R.id.ds_audio_mixer)?.setOnClickListener {
            Toast.makeText(this, R.string.audio_mixer, Toast.LENGTH_SHORT).show()
        }
        dialogView.findViewById<View>(R.id.ds_audio_merge)?.setOnClickListener {
            Toast.makeText(this, R.string.main_tab_audiomerger, Toast.LENGTH_SHORT).show()
        }
        dialogView.findViewById<View>(R.id.ds_guide_tutorial)?.setOnClickListener {
            Toast.makeText(this, R.string.tutorial, Toast.LENGTH_SHORT).show()
        }
        dialogView.findViewById<View>(R.id.ds_sampler_store)?.setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, ActivitySoundSampleStore::class.java))
        }
        dialogView.findViewById<View>(R.id.ds_remove_ads)?.setOnClickListener {
            Toast.makeText(this, R.string.toast_pro_activated, Toast.LENGTH_SHORT).show()
        }

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                sheet.setBackgroundColor(Color.TRANSPARENT)
                val screenWidth = resources.displayMetrics.widthPixels
                val screenHeight = resources.displayMetrics.heightPixels
                val h = (screenHeight * 0.83f).toInt().coerceAtLeast((300f * density).toInt())
                sheet.layoutParams.height = h
                sheet.layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.maxWidth = screenWidth
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = h
                behavior.skipCollapsed = true
            }
        }

        dialog.window?.let { window ->
            window.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            )
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
            dialog.show()
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        } ?: dialog.show()
    }

    private fun showDjAudioSettingsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_dj_settings, null)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btn_close_settings)
        val rgPitch = dialogView.findViewById<android.widget.RadioGroup>(R.id.rg_pitch_range)
        val rgCurve = dialogView.findViewById<android.widget.RadioGroup>(R.id.rg_crossfader_curve)
        val swScratch = dialogView.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.sw_scratch_sound)
        val rgTime = dialogView.findViewById<android.widget.RadioGroup>(R.id.rg_time_mode)
        val rgRec = dialogView.findViewById<android.widget.RadioGroup>(R.id.rg_rec_format)

        when ((pitchSliderRange * 100).toInt()) {
            4 -> rgPitch.check(R.id.rb_pitch_4)
            8 -> rgPitch.check(R.id.rb_pitch_8)
            16 -> rgPitch.check(R.id.rb_pitch_16)
            32 -> rgPitch.check(R.id.rb_pitch_32)
            50 -> rgPitch.check(R.id.rb_pitch_50)
        }

        when (crossfaderCurve) {
            0 -> rgCurve.check(R.id.rb_curve_smooth)
            1 -> rgCurve.check(R.id.rb_curve_linear)
            2 -> rgCurve.check(R.id.rb_curve_cut)
        }

        swScratch.isChecked = isScratchSoundEnabled

        when (timeMode) {
            0 -> rgTime.check(R.id.rb_time_elapsed)
            1 -> rgTime.check(R.id.rb_time_remaining)
            2 -> rgTime.check(R.id.rb_time_total)
        }

        if (recFormat == "MP3") rgRec.check(R.id.rb_rec_mp3) else rgRec.check(R.id.rb_rec_wav)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnClose.setOnClickListener { dialog.dismiss() }

        rgPitch.setOnCheckedChangeListener { _, checkedId ->
            pitchSliderRange = when (checkedId) {
                R.id.rb_pitch_4 -> 0.04f
                R.id.rb_pitch_8 -> 0.08f
                R.id.rb_pitch_16 -> 0.16f
                R.id.rb_pitch_32 -> 0.32f
                R.id.rb_pitch_50 -> 0.50f
                else -> 0.08f
            }
            val percent = (pitchSliderRange * 100).toInt()
            Toast.makeText(this, getString(R.string.toast_pitch_range, percent), Toast.LENGTH_SHORT).show()
        }

        rgCurve.setOnCheckedChangeListener { _, checkedId ->
            crossfaderCurve = when (checkedId) {
                R.id.rb_curve_smooth -> 0
                R.id.rb_curve_linear -> 1
                R.id.rb_curve_cut -> 2
                else -> 0
            }
        }

        swScratch.setOnCheckedChangeListener { _, isChecked ->
            isScratchSoundEnabled = isChecked
        }

        rgTime.setOnCheckedChangeListener { _, checkedId ->
            timeMode = when (checkedId) {
                R.id.rb_time_elapsed -> 0
                R.id.rb_time_remaining -> 1
                R.id.rb_time_total -> 2
                else -> 0
            }
        }

        rgRec.setOnCheckedChangeListener { _, checkedId ->
            recFormat = if (checkedId == R.id.rb_rec_mp3) "MP3" else "WAV"
        }

        val btnChangeLanguage = dialogView.findViewById<View>(R.id.btn_change_language)
        val txtCurrentLanguage = dialogView.findViewById<TextView>(R.id.txt_current_language)

        val currentLangCode = SystemUtil.getPreLanguage(this)
        val currentLangNameRes = EnumSelectLanguage.entries.find { it.code.equals(currentLangCode, ignoreCase = true) }?.nameLanguage
        txtCurrentLanguage?.text = if (currentLangNameRes != null) getString(currentLangNameRes) else currentLangCode.uppercase()

        btnChangeLanguage?.setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, ActivityLanguageSetting::class.java))
        }

        dialog.window?.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        )
        dialog.show()
        dialog.window?.decorView?.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
        dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
    }

    private fun updateVuMeters() {
        if (isPlayingA && (mediaPlayerA != null || channelA != 0)) {
            val level = if (channelA != 0) BASS.BASS_ChannelGetLevel(channelA) else -1
            if (level != -1) {
                val leftLevel = (level and 0xFFFF) / 32768f
                val rightLevel = ((level shr 16) and 0xFFFF) / 32768f
                val avg = ((leftLevel + rightLevel) / 2f) * (if (isMutedA) 0f else volumeA) * crossfadeLeftGain
                vuLeft.setProgress(avg.coerceIn(0f, 1f))
            } else {
                val finalVolA = if (isMutedA) 0f else (volumeA * crossfadeLeftGain).coerceIn(0f, 1f)
                val bounce = (0.50f + 0.50f * Math.random().toFloat()) * finalVolA
                vuLeft.setProgress(bounce.coerceIn(0f, 1f))
            }
        } else {
            vuLeft.setProgress(0f)
        }

        if (isPlayingB && (mediaPlayerB != null || channelB != 0)) {
            val level = if (channelB != 0) BASS.BASS_ChannelGetLevel(channelB) else -1
            if (level != -1) {
                val leftLevel = (level and 0xFFFF) / 32768f
                val rightLevel = ((level shr 16) and 0xFFFF) / 32768f
                val avg = ((leftLevel + rightLevel) / 2f) * (if (isMutedB) 0f else volumeB) * crossfadeRightGain
                vuRight.setProgress(avg.coerceIn(0f, 1f))
            } else {
                val finalVolB = if (isMutedB) 0f else (volumeB * crossfadeRightGain).coerceIn(0f, 1f)
                val bounce = (0.50f + 0.50f * Math.random().toFloat()) * finalVolB
                vuRight.setProgress(bounce.coerceIn(0f, 1f))
            }
        } else {
            vuRight.setProgress(0f)
        }
    }

    private fun updateWaveAndNeedle() {
        // Deck A
        if (!turntableLeft.isScratching) {
            if (channelA != 0) {
                val posA = BASS.BASS_ChannelGetPosition(channelA, BASS.BASS_POS_BYTE)
                val lenA = BASS.BASS_ChannelGetLength(channelA, BASS.BASS_POS_BYTE)
                if (lenA > 0) {
                    val ratio = (posA.toFloat() / lenA).coerceIn(0f, 1f)
                    turntableLeft.progress = ratio
                    waveLeft.progress = ratio
                    btnResetLeft.isSelected = (ratio <= 0.005f)
                }
                val posSecA = BASS.BASS_ChannelBytes2Seconds(channelA, posA).toInt()
                val totalSecA = BASS.BASS_ChannelBytes2Seconds(channelA, lenA).toInt()
                val remSecA = (totalSecA - posSecA).coerceAtLeast(0)

                txtDurationLeft.text = when (timeMode) {
                    0 -> String.format("%02d:%02d", posSecA / 60, posSecA % 60)
                    1 -> String.format("-%02d:%02d", remSecA / 60, remSecA % 60)
                    else -> String.format("%02d:%02d / %02d:%02d", posSecA / 60, posSecA % 60, totalSecA / 60, totalSecA % 60)
                }
                val posMsA = (BASS.BASS_ChannelBytes2Seconds(channelA, posA) * 1000).toInt()
                waveViewLeft.playOffset = posMsA * 0.25f

                if (isPlayingA && !isLoopingA) {
                    val active = BASS.BASS_ChannelIsActive(channelA)
                    if (active == BASS.BASS_ACTIVE_STOPPED) {
                        isPlayingA = false
                        btnPlayLeft.isSelected = false
                        turntableLeft.isRotating = false
                        turntableLeft.setIndicatorState(0)
                    }
                }
            } else {
                val mpA = mediaPlayerA
                if (mpA != null && durationMsA > 0) {
                    val posMs = try { mpA.currentPosition } catch (e: Exception) { 0 }
                    val ratio = (posMs.toFloat() / durationMsA).coerceIn(0f, 1f)
                    turntableLeft.progress = ratio
                    waveLeft.progress = ratio
                    val posSecA = posMs / 1000
                    val totalSecA = durationMsA / 1000
                    val remSecA = (totalSecA - posSecA).coerceAtLeast(0)

                    txtDurationLeft.text = when (timeMode) {
                        0 -> String.format("%02d:%02d", posSecA / 60, posSecA % 60)
                        1 -> String.format("-%02d:%02d", remSecA / 60, remSecA % 60)
                        else -> String.format("%02d:%02d / %02d:%02d", posSecA / 60, posSecA % 60, totalSecA / 60, totalSecA % 60)
                    }
                    waveViewLeft.playOffset = posMs * 0.25f

                    if (isPlayingA && !isLoopingA && mpA.isPlaying.not() && posMs >= durationMsA - 500) {
                        isPlayingA = false
                        btnPlayLeft.isSelected = false
                        turntableLeft.isRotating = false
                        turntableLeft.setIndicatorState(0)
                    }
                }
            }
        }

        // Deck B
        if (!turntableRight.isScratching) {
            if (channelB != 0) {
                val posB = BASS.BASS_ChannelGetPosition(channelB, BASS.BASS_POS_BYTE)
                val lenB = BASS.BASS_ChannelGetLength(channelB, BASS.BASS_POS_BYTE)
                if (lenB > 0) {
                    val ratio = (posB.toFloat() / lenB).coerceIn(0f, 1f)
                    turntableRight.progress = ratio
                    waveRight.progress = ratio
                    btnResetRight.isSelected = (ratio <= 0.005f)
                }
                val posSecB = BASS.BASS_ChannelBytes2Seconds(channelB, posB).toInt()
                val totalSecB = BASS.BASS_ChannelBytes2Seconds(channelB, lenB).toInt()
                val remSecB = (totalSecB - posSecB).coerceAtLeast(0)

                txtDurationRight.text = when (timeMode) {
                    0 -> String.format("%02d:%02d", posSecB / 60, posSecB % 60)
                    1 -> String.format("-%02d:%02d", remSecB / 60, remSecB % 60)
                    else -> String.format("%02d:%02d / %02d:%02d", posSecB / 60, posSecB % 60, totalSecB / 60, totalSecB % 60)
                }
                val posMsB = (BASS.BASS_ChannelBytes2Seconds(channelB, posB) * 1000).toInt()
                waveViewRight.playOffset = posMsB * 0.25f

                if (isPlayingB && !isLoopingB) {
                    val active = BASS.BASS_ChannelIsActive(channelB)
                    if (active == BASS.BASS_ACTIVE_STOPPED) {
                        isPlayingB = false
                        btnPlayRight.isSelected = false
                        turntableRight.isRotating = false
                        turntableRight.setIndicatorState(0)
                    }
                }
            } else {
                val mpB = mediaPlayerB
                if (mpB != null && durationMsB > 0) {
                    val posMs = try { mpB.currentPosition } catch (e: Exception) { 0 }
                    val ratio = (posMs.toFloat() / durationMsB).coerceIn(0f, 1f)
                    turntableRight.progress = ratio
                    waveRight.progress = ratio
                    val posSecB = posMs / 1000
                    val totalSecB = durationMsB / 1000
                    val remSecB = (totalSecB - posSecB).coerceAtLeast(0)

                    txtDurationRight.text = when (timeMode) {
                        0 -> String.format("%02d:%02d", posSecB / 60, posSecB % 60)
                        1 -> String.format("-%02d:%02d", remSecB / 60, remSecB % 60)
                        else -> String.format("%02d:%02d / %02d:%02d", posSecB / 60, posSecB % 60, totalSecB / 60, totalSecB % 60)
                    }
                    waveViewRight.playOffset = posMs * 0.25f

                    if (isPlayingB && !isLoopingB && mpB.isPlaying.not() && posMs >= durationMsB - 500) {
                        isPlayingB = false
                        btnPlayRight.isSelected = false
                        turntableRight.isRotating = false
                        turntableRight.setIndicatorState(0)
                    }
                }
            }
        }
    }

    private fun checkLoopBoundaries() {
        if (isLoopingA && loopEndA > loopStartA) {
            if (channelA != 0) {
                val curBytes = BASS.BASS_ChannelGetPosition(channelA, BASS.BASS_POS_BYTE)
                if (curBytes >= loopEndA) {
                    BASS.BASS_ChannelSetPosition(channelA, loopStartA, BASS.BASS_POS_BYTE)
                }
            } else if (mediaPlayerA != null) {
                val curMs = mediaPlayerA?.currentPosition?.toLong() ?: 0L
                if (curMs >= loopEndA) {
                    mediaPlayerA?.seekTo(loopStartA.toInt())
                }
            }
        }
        if (isLoopingB && loopEndB > loopStartB) {
            if (channelB != 0) {
                val curBytes = BASS.BASS_ChannelGetPosition(channelB, BASS.BASS_POS_BYTE)
                if (curBytes >= loopEndB) {
                    BASS.BASS_ChannelSetPosition(channelB, loopStartB, BASS.BASS_POS_BYTE)
                }
            } else if (mediaPlayerB != null) {
                val curMs = mediaPlayerB?.currentPosition?.toLong() ?: 0L
                if (curMs >= loopEndB) {
                    mediaPlayerB?.seekTo(loopStartB.toInt())
                }
            }
        }
    }

    private fun updateRecordingTimer() {
        if (isRecording) {
            val elapsed = (SystemClock.elapsedRealtime() - recordStartTime) / 1000
            val m = elapsed / 60
            val s = elapsed % 60
            recordTime.text = String.format("%02d:%02d", m, s)
        }
    }

    private fun copyAssetToCache(assetName: String): String? {
        val outFile = File(cacheDir, assetName.replace('/', '_'))
        if (outFile.exists() && outFile.length() > 0) return outFile.absolutePath
        try {
            assets.open(assetName).use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
            return outFile.absolutePath
        } catch (e: Exception) {
            Log.e(tag, "Failed to copy asset $assetName", e)
            return null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(uiUpdateRunnable)
        try {
            mediaPlayerA?.stop()
            mediaPlayerA?.release()
            mediaPlayerA = null
        } catch (e: Exception) {}
        try {
            mediaPlayerB?.stop()
            mediaPlayerB?.release()
            mediaPlayerB = null
        } catch (e: Exception) {}
        if (channelA != 0) BASS.BASS_StreamFree(channelA)
        if (channelB != 0) BASS.BASS_StreamFree(channelB)
        for (ch in scratchChannelMap.values) {
            if (ch != 0) BASS.BASS_StreamFree(ch)
        }
        scratchChannelMap.clear()
        for (ch in samplerChannelMap.values) {
            if (ch != 0) BASS.BASS_StreamFree(ch)
        }
        samplerChannelMap.clear()
        scratchStopHandler.removeCallbacks(scratchStopRunnableA)
        scratchStopHandler.removeCallbacks(scratchStopRunnableB)
        try {
            scratchSoundPool?.release()
            scratchSoundPool = null
        } catch (e: Exception) {}
        BASS.BASS_Free()
        try {
            shakeListener?.let { sensorManager?.unregisterListener(it) }
        } catch (e: Exception) {}
    }

    private var sensorManager: android.hardware.SensorManager? = null
    private var accelerometer: android.hardware.Sensor? = null
    private var shakeListener: android.hardware.SensorEventListener? = null

    private fun initAdInspectorShakeDetector() {
        try {
            sensorManager = getSystemService(android.content.Context.SENSOR_SERVICE) as? android.hardware.SensorManager
            accelerometer = sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
            shakeListener = object : android.hardware.SensorEventListener {
                private var lastTime: Long = 0
                private var lastX = 0f
                private var lastY = 0f
                private var lastZ = 0f

                override fun onSensorChanged(event: android.hardware.SensorEvent?) {
                    if (event == null || !com.audioeffect.djmixer.music.mixer.core.ads.RemoteConfigs.enable_ad_inspector) return
                    val now = System.currentTimeMillis()
                    if (now - lastTime > 1500) {
                        val x = event.values[0]
                        val y = event.values[1]
                        val z = event.values[2]
                        val speed = kotlin.math.abs(x + y + z - lastX - lastY - lastZ)
                        if (speed > 18) {
                            lastTime = now
                            com.audioeffect.djmixer.music.mixer.core.ads.AdsManager.openAdInspector(this@MainActivity)
                        }
                        lastX = x
                        lastY = y
                        lastZ = z
                    }
                }

                override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
            }
            accelerometer?.let {
                sensorManager?.registerListener(shakeListener, it, android.hardware.SensorManager.SENSOR_DELAY_UI)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
