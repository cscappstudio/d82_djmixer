package com.audioeffect.djmixer.music.mixer

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.audioeffect.djmixer.music.mixer.core.ads.AdsManager
import com.audioeffect.djmixer.music.mixer.core.api.JamendoApiClient
import com.audioeffect.djmixer.music.mixer.core.api.JamendoTrack
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActivityAudioLibrary : AppCompatActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.audioeffect.djmixer.music.mixer.core.utils.SystemUtil.wrapContext(newBase))
    }

    companion object {
        const val EXTRA_DECK = "EXTRA_DECK"
        const val EXTRA_AUDIO_PATH = "EXTRA_AUDIO_PATH"
        const val EXTRA_AUDIO_TITLE = "EXTRA_AUDIO_TITLE"
        const val EXTRA_AUDIO_ARTIST = "EXTRA_AUDIO_ARTIST"
        const val EXTRA_AUDIO_DURATION = "EXTRA_AUDIO_DURATION"

        const val SOURCE_ONLINE = 0
        const val SOURCE_DEVICE = 1

        val COLOR_TAB_SELECTED = Color.parseColor("#39C9E1")
        val COLOR_TAB_UNSELECTED = Color.parseColor("#FFFFFF")
    }

    data class AudioTrack(
        val id: Long,
        val title: String,
        val artist: String,
        val path: String,
        val duration: Long,
        val folder: String,
        val isRecording: Boolean = false,
        val isOnline: Boolean = false,
        val onlineId: String = "",
        val imageUrl: String = ""
    )

    data class OnlineGenre(val titleRes: Int, val tag: String)

    private val onlineGenres = listOf(
        OnlineGenre(R.string.genre_hiphop, "hiphop"),
        OnlineGenre(R.string.genre_edm, "electronic"),
        OnlineGenre(R.string.genre_trap, "trap"),
        OnlineGenre(R.string.genre_future_bass, "bass"),
        OnlineGenre(R.string.genre_house, "house"),
        OnlineGenre(R.string.genre_lofi, "lofi"),
        OnlineGenre(R.string.genre_dance, "dance"),
        OnlineGenre(R.string.genre_remix, "remix")
    )

    data class GenreDataState(
        val tracks: MutableList<AudioTrack> = mutableListOf(),
        var offset: Int = 0,
        var hasMore: Boolean = true,
        var isLoaded: Boolean = false,
        var isLoading: Boolean = false
    )

    private val genreDataMap = mutableMapOf<String, GenreDataState>()
    private var currentOnlineFetchId = 0L

    private var currentSearchOffset = 0
    private var isSearchLoadingMore = false
    private var hasMoreSearchTracks = true

    private val PAGE_SIZE = 30
    private var selectedOnlineGenreIndex = 0

    private var targetDeck: Int = 0 // 0 = Deck A, 1 = Deck B
    private var currentSourceType: Int = SOURCE_DEVICE
    private var currentDeviceTab: Int = 0 // 0 = Tracks, 1 = Folders, 2 = Recordings
    private var hasLoadedLocalTracks = false
    private var hasWatchedReward = false

    private val allLocalTracks = mutableListOf<AudioTrack>()
    private val folderMap = mutableMapOf<String, MutableList<AudioTrack>>()
    private val recordingTracks = mutableListOf<AudioTrack>()
    private val displayedTracks = mutableListOf<AudioTrack>()

    private lateinit var btnSourceSelector: View
    private lateinit var imgSourceIcon: ImageView
    private lateinit var txtSourceTitle: TextView
    private lateinit var txtDeckBadge: TextView
    private lateinit var edtSearch: EditText
    private lateinit var layoutDeviceTabs: View
    private lateinit var tabTracks: TextView
    private lateinit var tabFolders: TextView
    private lateinit var tabRecordings: TextView
    private lateinit var indicatorTabTracks: View
    private lateinit var indicatorTabFolders: View
    private lateinit var indicatorTabRecordings: View
    private lateinit var layoutOnlineTabs: View
    private lateinit var layoutOnlineTabsContainer: LinearLayout
    private lateinit var recyclerAudio: RecyclerView
    private lateinit var layoutLoading: View
    private lateinit var layoutEmpty: View
    private lateinit var txtEmpty: TextView
    private lateinit var txtJamendoAttribution: View
    private lateinit var layoutPermission: View
    private lateinit var btnAllowAccess: View
    private lateinit var adapter: AudioTrackAdapter

    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    private val requestAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onAudioPermissionGranted()
        } else {
            val perm = getRequiredAudioPermission()
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, perm)) {
                showPermissionSettingsDialog()
            } else {
                Toast.makeText(this, getString(R.string.permission_toast_required), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_library)

        targetDeck = intent.getIntExtra(EXTRA_DECK, 0)

        initViews()

        if (hasAudioPermission()) {
            hasLoadedLocalTracks = true
            loadLocalTracks()
            loadRecordings()
        }

        // Default to Device mode as requested
        applySourceMode(SOURCE_DEVICE)
    }

    override fun onResume() {
        super.onResume()
        if (currentSourceType == SOURCE_DEVICE) {
            if (hasAudioPermission()) {
                if (!hasLoadedLocalTracks || layoutPermission.visibility == View.VISIBLE) {
                    onAudioPermissionGranted()
                }
            } else {
                layoutPermission.visibility = View.VISIBLE
                recyclerAudio.visibility = View.GONE
                layoutEmpty.visibility = View.GONE
            }
        }
    }

    private fun initViews() {
        val flTopBanner = findViewById<FrameLayout>(R.id.fl_top_banner)
        AdsManager.showNativeBanner(this, flTopBanner)

        findViewById<View>(R.id.btn_back).setOnClickListener {
            finish()
        }

        btnSourceSelector = findViewById(R.id.btn_source_selector)
        imgSourceIcon = findViewById(R.id.img_source_icon)
        txtSourceTitle = findViewById(R.id.txt_source_title)

        btnSourceSelector.setOnClickListener {
            showSourcePopupMenu(btnSourceSelector)
        }

        txtDeckBadge = findViewById(R.id.txt_deck_badge)
        txtDeckBadge.text = getString(R.string.deck_badge, if (targetDeck == 0) "A" else "B")
        val deckColor = ContextCompat.getColor(
            this,
            if (targetDeck == 0) R.color.theme_color_left else R.color.theme_color_right
        )
        txtDeckBadge.setTextColor(deckColor)

        findViewById<View>(R.id.btn_music_guide).setOnClickListener {
            startActivity(Intent(this, ActivityMusicGuide::class.java))
        }

        edtSearch = findViewById(R.id.edt_search)
        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString().orEmpty()
                if (currentSourceType == SOURCE_ONLINE) {
                    searchRunnable?.let { searchHandler.removeCallbacks(it) }
                    searchRunnable = Runnable {
                        displayCurrentGenreTracks()
                    }
                    searchHandler.postDelayed(searchRunnable!!, 350)
                } else {
                    filterDeviceTracks(query)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        layoutDeviceTabs = findViewById(R.id.layout_device_tabs)
        tabTracks = findViewById(R.id.tab_tracks)
        tabFolders = findViewById(R.id.tab_folders)
        tabRecordings = findViewById(R.id.tab_recordings)
        indicatorTabTracks = findViewById(R.id.indicator_tab_tracks)
        indicatorTabFolders = findViewById(R.id.indicator_tab_folders)
        indicatorTabRecordings = findViewById(R.id.indicator_tab_recordings)

        findViewById<View>(R.id.tab_container_tracks).setOnClickListener { switchDeviceTab(0) }
        findViewById<View>(R.id.tab_container_folders).setOnClickListener { switchDeviceTab(1) }
        findViewById<View>(R.id.tab_container_recordings).setOnClickListener { switchDeviceTab(2) }

        layoutOnlineTabs = findViewById(R.id.layout_online_tabs)
        layoutOnlineTabsContainer = findViewById(R.id.layout_online_tabs_container)
        setupOnlineGenreTabs()

        tabTracks.setOnClickListener { switchDeviceTab(0) }
        tabFolders.setOnClickListener { switchDeviceTab(1) }
        tabRecordings.setOnClickListener { switchDeviceTab(2) }

        layoutLoading = findViewById(R.id.layout_loading)
        layoutEmpty = findViewById(R.id.layout_empty)
        txtEmpty = findViewById(R.id.txt_empty)
        txtJamendoAttribution = findViewById(R.id.txt_jamendo_attribution)

        layoutPermission = findViewById(R.id.layout_permission)
        btnAllowAccess = findViewById(R.id.btn_allow_access)
        btnAllowAccess.setOnClickListener {
            requestAudioPermission()
        }

        layoutEmpty.setOnClickListener {
            if (currentSourceType == SOURCE_ONLINE) {
                val q = edtSearch.text?.toString()?.trim().orEmpty()
                val activeTag = onlineGenres.getOrNull(selectedOnlineGenreIndex)?.tag ?: "hiphop"
                if (q.isEmpty()) {
                    genreDataMap[activeTag]?.isLoaded = false
                }
                displayCurrentGenreTracks()
            }
        }

        recyclerAudio = findViewById(R.id.recycler_audio)
        recyclerAudio.layoutManager = LinearLayoutManager(this)
        adapter = AudioTrackAdapter(displayedTracks) { track, position ->
            handleTrackSelection(track, position)
        }
        recyclerAudio.adapter = adapter
        recyclerAudio.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy <= 0) return
                if (currentSourceType != SOURCE_ONLINE) return

                val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val visibleCount = lm.childCount
                val totalCount = lm.itemCount
                val firstVisible = lm.findFirstVisibleItemPosition()

                if ((firstVisible + visibleCount) >= totalCount - 5 && totalCount > 0) {
                    loadMoreOnlineTracks()
                }
            }
        })
    }

    private fun setupOnlineGenreTabs() {
        layoutOnlineTabsContainer.removeAllViews()
        val density = resources.displayMetrics.density
        val horizontalPad = (14 * density).toInt()
        val indicatorHeight = (2.5f * density).toInt().coerceAtLeast(6)

        onlineGenres.forEachIndexed { index, genre ->
            val tabContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                setPadding(horizontalPad, 0, horizontalPad, 0)
                val outValue = TypedValue()
                theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                setBackgroundResource(outValue.resourceId)
                isClickable = true
                isFocusable = true

                val txtView = TextView(context).apply {
                    tag = "tab_title"
                    text = getString(genre.titleRes)
                    textSize = 13.5f
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        0,
                        1f
                    )
                }
                addView(txtView)

                val indicator = View(context).apply {
                    tag = "tab_indicator"
                    setBackgroundColor(COLOR_TAB_SELECTED)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        indicatorHeight
                    )
                }
                addView(indicator)

                setOnClickListener {
                    selectOnlineGenre(index)
                }
            }
            layoutOnlineTabsContainer.addView(tabContainer)
        }
        updateOnlineGenreTabsUi()
    }

    private fun selectOnlineGenre(index: Int) {
        if (selectedOnlineGenreIndex == index && displayedTracks.isNotEmpty()) return
        selectedOnlineGenreIndex = index
        updateOnlineGenreTabsUi()

        val child = layoutOnlineTabsContainer.getChildAt(index)
        val hsv = layoutOnlineTabs as? HorizontalScrollView
        if (child != null && hsv != null) {
            val scrollX = child.left - (hsv.width - child.width) / 2
            hsv.smoothScrollTo(scrollX.coerceAtLeast(0), 0)
        }

        displayCurrentGenreTracks()
    }

    private fun updateOnlineGenreTabsUi() {
        for (i in 0 until layoutOnlineTabsContainer.childCount) {
            val tabContainer = layoutOnlineTabsContainer.getChildAt(i) as? LinearLayout ?: continue
            val txtView = tabContainer.findViewWithTag<TextView>("tab_title")
            val indicator = tabContainer.findViewWithTag<View>("tab_indicator")
            val isSelected = (i == selectedOnlineGenreIndex)

            txtView?.setTextColor(if (isSelected) COLOR_TAB_SELECTED else COLOR_TAB_UNSELECTED)
            indicator?.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun showSourcePopupMenu(anchor: View) {
        val popupView = LayoutInflater.from(this).inflate(R.layout.layout_popup_music_source, null)
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 16f
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        val itemDevice = popupView.findViewById<View>(R.id.item_source_device)
        val itemOnline = popupView.findViewById<View>(R.id.item_source_online)
        val checkDevice = popupView.findViewById<View>(R.id.ic_check_device)
        val checkOnline = popupView.findViewById<View>(R.id.ic_check_online)

        checkDevice.visibility = if (currentSourceType == SOURCE_DEVICE) View.VISIBLE else View.GONE
        checkOnline.visibility = if (currentSourceType == SOURCE_ONLINE) View.VISIBLE else View.GONE

        itemDevice.setOnClickListener {
            popupWindow.dismiss()
            if (currentSourceType != SOURCE_DEVICE) {
                applySourceMode(SOURCE_DEVICE)
            }
        }

        itemOnline.setOnClickListener {
            popupWindow.dismiss()
            if (currentSourceType != SOURCE_ONLINE) {
                applySourceMode(SOURCE_ONLINE)
            }
        }

        popupWindow.showAsDropDown(anchor, 0, 12)
    }

    private fun applySourceMode(source: Int) {
        currentSourceType = source
        if (source == SOURCE_ONLINE) {
            imgSourceIcon.setImageResource(R.drawable.ic_headphone_white)
            txtSourceTitle.text = getString(R.string.source_online)
            layoutDeviceTabs.visibility = View.GONE
            layoutOnlineTabs.visibility = View.VISIBLE
            txtJamendoAttribution.visibility = View.VISIBLE
            edtSearch.hint = getString(R.string.search_online_tracks)
            layoutPermission.visibility = View.GONE
            recyclerAudio.visibility = View.VISIBLE
            updateOnlineGenreTabsUi()
            displayCurrentGenreTracks()
        } else {
            imgSourceIcon.setImageResource(R.drawable.ic_device_phone_white)
            txtSourceTitle.text = getString(R.string.source_device)
            layoutDeviceTabs.visibility = View.VISIBLE
            layoutOnlineTabs.visibility = View.GONE
            txtJamendoAttribution.visibility = View.GONE
            edtSearch.hint = getString(R.string.search_local_tracks)
            layoutLoading.visibility = View.GONE

            if (!hasAudioPermission()) {
                layoutPermission.visibility = View.VISIBLE
                recyclerAudio.visibility = View.GONE
                layoutEmpty.visibility = View.GONE
            } else {
                layoutPermission.visibility = View.GONE
                recyclerAudio.visibility = View.VISIBLE
                if (!hasLoadedLocalTracks) {
                    hasLoadedLocalTracks = true
                    loadLocalTracks()
                    loadRecordings()
                }
                switchDeviceTab(currentDeviceTab)
            }
        }
    }

    private fun switchDeviceTab(tab: Int) {
        currentDeviceTab = tab

        tabTracks.setTextColor(if (tab == 0) COLOR_TAB_SELECTED else COLOR_TAB_UNSELECTED)
        indicatorTabTracks.visibility = if (tab == 0) View.VISIBLE else View.INVISIBLE

        tabFolders.setTextColor(if (tab == 1) COLOR_TAB_SELECTED else COLOR_TAB_UNSELECTED)
        indicatorTabFolders.visibility = if (tab == 1) View.VISIBLE else View.INVISIBLE

        tabRecordings.setTextColor(if (tab == 2) COLOR_TAB_SELECTED else COLOR_TAB_UNSELECTED)
        indicatorTabRecordings.visibility = if (tab == 2) View.VISIBLE else View.INVISIBLE

        if (!hasAudioPermission()) {
            layoutPermission.visibility = View.VISIBLE
            recyclerAudio.visibility = View.GONE
            layoutEmpty.visibility = View.GONE
            return
        }

        layoutPermission.visibility = View.GONE
        recyclerAudio.visibility = View.VISIBLE
        filterDeviceTracks(edtSearch.text.toString())
    }

    private fun filterDeviceTracks(query: String) {
        if (!hasAudioPermission()) {
            layoutPermission.visibility = View.VISIBLE
            recyclerAudio.visibility = View.GONE
            layoutEmpty.visibility = View.GONE
            return
        }

        val q = query.trim().lowercase(Locale.getDefault())
        displayedTracks.clear()

        val sourceList = when (currentDeviceTab) {
            0 -> allLocalTracks
            1 -> allLocalTracks.sortedBy { it.folder }
            else -> recordingTracks
        }

        if (q.isEmpty()) {
            displayedTracks.addAll(sourceList)
        } else {
            displayedTracks.addAll(sourceList.filter {
                it.title.lowercase(Locale.getDefault()).contains(q) ||
                it.artist.lowercase(Locale.getDefault()).contains(q) ||
                it.folder.lowercase(Locale.getDefault()).contains(q)
            })
        }

        adapter.notifyDataSetChanged()
        layoutEmpty.visibility = if (displayedTracks.isEmpty()) View.VISIBLE else View.GONE
        txtEmpty.text = when (currentDeviceTab) {
            0 -> getString(R.string.empty_device_tracks)
            1 -> getString(R.string.empty_folders)
            else -> getString(R.string.empty_recordings)
        }
    }

    private fun getRequiredAudioPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    private fun hasAudioPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                return true
            }
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestAudioPermission() {
        val perm = getRequiredAudioPermission()
        requestAudioPermissionLauncher.launch(perm)
    }

    private fun onAudioPermissionGranted() {
        hasLoadedLocalTracks = true
        loadLocalTracks()
        loadRecordings()
        if (currentSourceType == SOURCE_DEVICE) {
            layoutPermission.visibility = View.GONE
            recyclerAudio.visibility = View.VISIBLE
            filterDeviceTracks(edtSearch.text.toString())
        }
    }

    private fun showPermissionSettingsDialog() {
        try {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.permission_required_title)
                .setMessage(R.string.permission_required_desc)
                .setPositiveButton(R.string.open_settings) { _, _ ->
                    try {
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.fromParts("package", packageName, null)
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun displayCurrentGenreTracks() {
        val q = edtSearch.text?.toString()?.trim().orEmpty()
        val activeTag = onlineGenres.getOrNull(selectedOnlineGenreIndex)?.tag ?: "hiphop"

        if (q.isNotEmpty()) {
            fetchOnlineSearchResults(q, activeTag)
            return
        }

        val state = genreDataMap.getOrPut(activeTag) { GenreDataState() }
        if (state.isLoaded) {
            // Instant load from cache! Zero delay, zero network, zero flicker!
            layoutLoading.visibility = View.GONE
            displayedTracks.clear()
            displayedTracks.addAll(state.tracks)
            adapter.notifyDataSetChanged()
            recyclerAudio.scrollToPosition(0)
            layoutEmpty.visibility = if (displayedTracks.isEmpty()) View.VISIBLE else View.GONE
            txtEmpty.text = getString(R.string.no_online_tracks_found)
            return
        }

        // Cache miss: First time opening this genre tab (lazy loading)
        // Clear immediately so previous tab's list doesn't flicker on screen!
        displayedTracks.clear()
        adapter.notifyDataSetChanged()
        layoutEmpty.visibility = View.GONE
        layoutLoading.visibility = View.VISIBLE

        val fetchId = ++currentOnlineFetchId
        state.isLoading = true

        JamendoApiClient.fetchTracks(
            query = null,
            genreTag = activeTag,
            offset = 0,
            limit = PAGE_SIZE
        ) { result ->
            if (isFinishing || isDestroyed) return@fetchTracks
            state.isLoading = false

            result.onSuccess { tracks ->
                state.isLoaded = true
                state.tracks.clear()
                state.hasMore = (tracks.size >= PAGE_SIZE)
                state.offset = tracks.size

                val newTracks = tracks.map { jt ->
                    AudioTrack(
                        id = jt.id.toLongOrNull() ?: System.currentTimeMillis(),
                        title = jt.name,
                        artist = jt.artistName,
                        path = jt.audioUrl,
                        duration = jt.durationSeconds * 1000L,
                        folder = if (jt.albumName.isNotEmpty()) jt.albumName else "Jamendo Music",
                        isRecording = false,
                        isOnline = true,
                        onlineId = jt.id,
                        imageUrl = jt.imageUrl
                    )
                }
                state.tracks.addAll(newTracks)

                if (fetchId == currentOnlineFetchId && currentSourceType == SOURCE_ONLINE && edtSearch.text.isNullOrEmpty()) {
                    val currentTag = onlineGenres.getOrNull(selectedOnlineGenreIndex)?.tag
                    if (currentTag == activeTag) {
                        displayedTracks.clear()
                        displayedTracks.addAll(state.tracks)
                        adapter.notifyDataSetChanged()
                        layoutLoading.visibility = View.GONE
                        layoutEmpty.visibility = if (displayedTracks.isEmpty()) View.VISIBLE else View.GONE
                        txtEmpty.text = getString(R.string.no_online_tracks_found)
                    }
                }
            }.onFailure {
                if (fetchId == currentOnlineFetchId && currentSourceType == SOURCE_ONLINE && edtSearch.text.isNullOrEmpty()) {
                    val currentTag = onlineGenres.getOrNull(selectedOnlineGenreIndex)?.tag
                    if (currentTag == activeTag) {
                        layoutLoading.visibility = View.GONE
                        layoutEmpty.visibility = View.VISIBLE
                        txtEmpty.text = getString(R.string.failed_load_online_retry)
                    }
                }
            }
        }
    }

    private fun fetchOnlineSearchResults(query: String, activeTag: String) {
        val q = query.trim()
        currentSearchOffset = 0
        hasMoreSearchTracks = true
        isSearchLoadingMore = false

        displayedTracks.clear()
        adapter.notifyDataSetChanged()
        layoutLoading.visibility = View.VISIBLE
        layoutEmpty.visibility = View.GONE

        val fetchId = ++currentOnlineFetchId

        JamendoApiClient.fetchTracks(
            query = q,
            genreTag = activeTag,
            offset = 0,
            limit = PAGE_SIZE
        ) { result ->
            if (isFinishing || isDestroyed) return@fetchTracks
            if (fetchId != currentOnlineFetchId || currentSourceType != SOURCE_ONLINE) return@fetchTracks

            layoutLoading.visibility = View.GONE

            result.onSuccess { tracks ->
                val newTracks = tracks.map { jt ->
                    AudioTrack(
                        id = jt.id.toLongOrNull() ?: System.currentTimeMillis(),
                        title = jt.name,
                        artist = jt.artistName,
                        path = jt.audioUrl,
                        duration = jt.durationSeconds * 1000L,
                        folder = if (jt.albumName.isNotEmpty()) jt.albumName else "Jamendo Music",
                        isRecording = false,
                        isOnline = true,
                        onlineId = jt.id,
                        imageUrl = jt.imageUrl
                    )
                }
                hasMoreSearchTracks = (tracks.size >= PAGE_SIZE)
                currentSearchOffset = tracks.size

                displayedTracks.clear()
                displayedTracks.addAll(newTracks)
                adapter.notifyDataSetChanged()
                layoutEmpty.visibility = if (displayedTracks.isEmpty()) View.VISIBLE else View.GONE
                txtEmpty.text = getString(R.string.no_tracks_matching, q)
            }.onFailure {
                displayedTracks.clear()
                adapter.notifyDataSetChanged()
                layoutEmpty.visibility = View.VISIBLE
                txtEmpty.text = getString(R.string.search_failed_retry)
            }
        }
    }

    private fun loadMoreOnlineTracks() {
        if (currentSourceType != SOURCE_ONLINE) return
        val q = edtSearch.text?.toString()?.trim().orEmpty()
        val activeTag = onlineGenres.getOrNull(selectedOnlineGenreIndex)?.tag ?: "hiphop"

        if (q.isNotEmpty()) {
            if (isSearchLoadingMore || !hasMoreSearchTracks) return
            isSearchLoadingMore = true

            JamendoApiClient.fetchTracks(
                query = q,
                genreTag = activeTag,
                offset = currentSearchOffset,
                limit = PAGE_SIZE
            ) { result ->
                if (isFinishing || isDestroyed) return@fetchTracks
                isSearchLoadingMore = false

                result.onSuccess { tracks ->
                    if (tracks.isEmpty()) {
                        hasMoreSearchTracks = false
                        return@onSuccess
                    }
                    if (tracks.size < PAGE_SIZE) {
                        hasMoreSearchTracks = false
                    }
                    val startPos = displayedTracks.size
                    val newTracks = tracks.map { jt ->
                        AudioTrack(
                            id = jt.id.toLongOrNull() ?: System.currentTimeMillis(),
                            title = jt.name,
                            artist = jt.artistName,
                            path = jt.audioUrl,
                            duration = jt.durationSeconds * 1000L,
                            folder = if (jt.albumName.isNotEmpty()) jt.albumName else "Jamendo Music",
                            isRecording = false,
                            isOnline = true,
                            onlineId = jt.id,
                            imageUrl = jt.imageUrl
                        )
                    }
                    currentSearchOffset += tracks.size
                    if (currentSourceType == SOURCE_ONLINE && edtSearch.text?.toString()?.trim() == q) {
                        displayedTracks.addAll(newTracks)
                        adapter.notifyItemRangeInserted(startPos, newTracks.size)
                    }
                }
            }
            return
        }

        val state = genreDataMap[activeTag] ?: return
        if (state.isLoading || !state.hasMore) return
        state.isLoading = true

        JamendoApiClient.fetchTracks(
            query = null,
            genreTag = activeTag,
            offset = state.offset,
            limit = PAGE_SIZE
        ) { result ->
            if (isFinishing || isDestroyed) return@fetchTracks
            state.isLoading = false

            result.onSuccess { tracks ->
                if (tracks.isEmpty()) {
                    state.hasMore = false
                    return@onSuccess
                }
                if (tracks.size < PAGE_SIZE) {
                    state.hasMore = false
                }

                val startPos = state.tracks.size
                val newTracks = tracks.map { jt ->
                    AudioTrack(
                        id = jt.id.toLongOrNull() ?: System.currentTimeMillis(),
                        title = jt.name,
                        artist = jt.artistName,
                        path = jt.audioUrl,
                        duration = jt.durationSeconds * 1000L,
                        folder = if (jt.albumName.isNotEmpty()) jt.albumName else "Jamendo Music",
                        isRecording = false,
                        isOnline = true,
                        onlineId = jt.id,
                        imageUrl = jt.imageUrl
                    )
                }
                state.offset += tracks.size
                state.tracks.addAll(newTracks)

                val currentTag = onlineGenres.getOrNull(selectedOnlineGenreIndex)?.tag
                if (currentSourceType == SOURCE_ONLINE && currentTag == activeTag && edtSearch.text.isNullOrEmpty()) {
                    displayedTracks.addAll(newTracks)
                    adapter.notifyItemRangeInserted(startPos, newTracks.size)
                }
            }
        }
    }

    private fun handleTrackSelection(track: AudioTrack, position: Int) {
        if (track.isOnline) {
            val isUnlocked = isTrackUnlocked(track.onlineId)
            if (!isUnlocked) {
                // Must unlock via reward video first!
                handleDownloadClick(track, position)
                return
            }

            val cacheDir = File(cacheDir, "jamendo_cache")
            val cachedFile = File(cacheDir, "jamendo_${track.onlineId}.mp3")
            if (cachedFile.exists() && cachedFile.length() > 50000) {
                // Already cached, load directly
                selectAndReturnTrack(track.copy(path = cachedFile.absolutePath))
            } else {
                // Download and cache before passing to BASS engine
                downloadTrackAndReturn(track)
            }
        } else {
            selectAndReturnTrack(track)
        }
    }

    private fun downloadTrackAndReturn(track: AudioTrack) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_downloading_track, null)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val txtTitle = dialogView.findViewById<TextView>(R.id.txt_download_title)
        val txtStatus = dialogView.findViewById<TextView>(R.id.txt_download_status)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progress_bar)

        txtTitle.text = track.title
        val deckName = if (targetDeck == 0) "A" else "B"
        txtStatus.text = getString(R.string.loading_track_deck, deckName)
        dialog.show()

        val jamendoTrack = JamendoTrack(
            id = track.onlineId,
            name = track.title,
            artistName = track.artist,
            albumName = track.folder,
            durationSeconds = track.duration / 1000L,
            audioUrl = track.path,
            imageUrl = track.imageUrl
        )

        JamendoApiClient.downloadTrack(
            context = this,
            track = jamendoTrack,
            onProgress = { percent ->
                if (dialog.isShowing) {
                    txtStatus.text = getString(R.string.downloading_audio_percent, percent)
                }
            },
            onComplete = { file ->
                if (!isFinishing && !isDestroyed) {
                    try {
                        dialog.dismiss()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    if (file != null && file.exists()) {
                        val saved = track.copy(path = file.absolutePath)
                        saveDownloadedTrackInfo(saved)
                        selectAndReturnTrack(saved)
                    } else {
                        Toast.makeText(
                            this@ActivityAudioLibrary,
                            R.string.toast_network_error,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }

    private fun selectAndReturnTrack(track: AudioTrack) {
        // If user has watched a reward ad, OR if this track was unlocked via reward, DO NOT show interstitial ad!
        if (hasWatchedReward || (track.isOnline && isTrackUnlocked(track.onlineId))) {
            returnTrackToMixer(track)
            return
        }

        AdsManager.showInterAll(this) {
            returnTrackToMixer(track)
        }
    }

    private fun returnTrackToMixer(track: AudioTrack) {
        val resultIntent = Intent().apply {
            putExtra(EXTRA_DECK, targetDeck)
            putExtra(EXTRA_AUDIO_PATH, track.path)
            putExtra(EXTRA_AUDIO_TITLE, track.title)
            putExtra(EXTRA_AUDIO_ARTIST, track.artist)
            putExtra(EXTRA_AUDIO_DURATION, track.duration)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun loadLocalTracks() {
        if (!hasAudioPermission()) return
        hasLoadedLocalTracks = true
        allLocalTracks.clear()
        folderMap.clear()
        val pathSet = mutableSetOf<String>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION
        )

        try {
            val cursor: Cursor? = contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val dataCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val durCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val title = it.getString(titleCol) ?: "Unknown"
                    val artist = it.getString(artistCol) ?: "<Unknown Artist>"
                    val path = it.getString(dataCol) ?: ""
                    val duration = it.getLong(durCol)
                    val file = File(path)
                    val folder = file.parentFile?.name ?: "Music"

                    if (file.exists() && duration > 2000 && !pathSet.contains(path)) {
                        val track = AudioTrack(id, title, artist, path, duration, folder)
                        allLocalTracks.add(track)
                        pathSet.add(path)
                        folderMap.getOrPut(folder) { mutableListOf() }.add(track)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Direct directory scanning fallback for /sdcard/Download, /sdcard/Music
        val searchDirs = listOf(
            File("/sdcard/Download"),
            File("/sdcard/Music"),
            File("/sdcard/DCIM"),
            File(android.os.Environment.getExternalStorageDirectory(), "Download"),
            File(android.os.Environment.getExternalStorageDirectory(), "Music")
        )
        val audioExts = setOf("mp3", "wav", "m4a", "aac", "flac", "ogg")
        var scanId = 50000L

        for (dir in searchDirs) {
            if (dir.exists() && dir.isDirectory) {
                dir.walkTopDown().maxDepth(2).forEach { f ->
                    if (f.isFile && audioExts.contains(f.extension.lowercase(Locale.getDefault())) && !pathSet.contains(f.absolutePath)) {
                        val name = f.nameWithoutExtension
                        val folder = f.parentFile?.name ?: "Downloads"
                        val dur = 180000L
                        val track = AudioTrack(scanId++, name, "Local Audio", f.absolutePath, dur, folder)
                        allLocalTracks.add(track)
                        pathSet.add(f.absolutePath)
                        folderMap.getOrPut(folder) { mutableListOf() }.add(track)
                    }
                }
            }
        }

        // Also include downloaded tracks from Online mode so users can find them under Device mode
        val downloadedTracks = getDownloadedTracks()
        for (dt in downloadedTracks) {
            if (!pathSet.contains(dt.path)) {
                allLocalTracks.add(dt)
                pathSet.add(dt.path)
                folderMap.getOrPut("Downloaded") { mutableListOf() }.add(dt)
            }
        }

        // Add built-in samples if device has no tracks
        if (allLocalTracks.isEmpty()) {
            val sampleA = copyAssetToCache("beat/group/A1.wav")
            val sampleB = copyAssetToCache("beat/group/A2.wav")
            sampleA?.let {
                allLocalTracks.add(AudioTrack(1, "DJ Demo Beat A", "DJ Mixer Built-in", it, 32000L, "Demo Beats"))
            }
            sampleB?.let {
                allLocalTracks.add(AudioTrack(2, "DJ Demo Beat B", "DJ Mixer Built-in", it, 32000L, "Demo Beats"))
            }
        }
    }

    private fun loadRecordings() {
        recordingTracks.clear()
        val recDir = File(getExternalFilesDir(null), "DJMixer/Recordings")
        if (recDir.exists()) {
            val files = recDir.listFiles { f -> f.extension.equals("wav", true) || f.extension.equals("mp3", true) }
            files?.sortedByDescending { it.lastModified() }?.forEachIndexed { index, file ->
                val duration = file.length() / (44100 * 2 * 2) * 1000L
                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(file.lastModified()))
                recordingTracks.add(
                    AudioTrack(
                        id = 1000L + index,
                        title = file.nameWithoutExtension,
                        artist = "Recording • $dateStr",
                        path = file.absolutePath,
                        duration = duration,
                        folder = "Recordings",
                        isRecording = true
                    )
                )
            }
        }
    }

    private fun copyAssetToCache(assetPath: String): String? {
        return try {
            val file = File(cacheDir, assetPath.replace('/', '_'))
            if (!file.exists()) {
                file.parentFile?.mkdirs()
                assets.open(assetPath).use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun isTrackUnlocked(onlineId: String): Boolean {
        if (onlineId.isEmpty()) return true
        val prefs = getSharedPreferences("dj_mixer_unlocked_tracks", android.content.Context.MODE_PRIVATE)
        val set = prefs.getStringSet("unlocked_ids", emptySet()) ?: emptySet()
        return set.contains(onlineId)
    }

    private fun markTrackUnlocked(onlineId: String) {
        if (onlineId.isEmpty()) return
        val prefs = getSharedPreferences("dj_mixer_unlocked_tracks", android.content.Context.MODE_PRIVATE)
        val currentSet = prefs.getStringSet("unlocked_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.add(onlineId)
        prefs.edit().putStringSet("unlocked_ids", currentSet).apply()
    }

    private fun handleDownloadClick(track: AudioTrack, position: Int) {
        val isUnlocked = isTrackUnlocked(track.onlineId)
        if (!isUnlocked) {
            // Must watch reward ad to unlock
            AdsManager.showReward(
                activity = this,
                onEarnedReward = {
                    hasWatchedReward = true
                    AdsManager.setRewardWatched()
                    markTrackUnlocked(track.onlineId)
                    adapter.notifyItemChanged(position)
                    Toast.makeText(this, getString(R.string.toast_unlocked_sound, track.title), Toast.LENGTH_SHORT).show()
                    downloadOnlineTrack(track)
                },
                onFailed = {
                    Toast.makeText(this, R.string.toast_watch_reward_unlock, Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            // Already unlocked, download or notify
            downloadOnlineTrack(track)
        }
    }

    private fun downloadOnlineTrack(track: AudioTrack) {
        val cacheDir = File(cacheDir, "jamendo_cache")
        val cachedFile = File(cacheDir, "jamendo_${track.onlineId}.mp3")
        if (cachedFile.exists() && cachedFile.length() > 50000) {
            Toast.makeText(this, getString(R.string.toast_downloaded_ready, track.title), Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_downloading_track, null)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val txtTitle = dialogView.findViewById<TextView>(R.id.txt_download_title)
        val txtStatus = dialogView.findViewById<TextView>(R.id.txt_download_status)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progress_bar)

        txtTitle.text = track.title
        txtStatus.text = getString(R.string.downloading_audio_jamendo)
        progressBar.isIndeterminate = false
        progressBar.progress = 0

        dialog.show()

        val jTrack = JamendoTrack(
            id = track.onlineId,
            name = track.title,
            artistName = track.artist,
            albumName = track.folder,
            durationSeconds = track.duration / 1000,
            audioUrl = track.path,
            imageUrl = track.imageUrl
        )

        JamendoApiClient.downloadTrack(this, jTrack, onProgress = { progress ->
            progressBar.progress = progress
            txtStatus.text = getString(R.string.downloading_audio_percent, progress)
        }) { file ->
            if (isFinishing || isDestroyed) return@downloadTrack
            dialog.dismiss()

            if (file != null && file.exists()) {
                saveDownloadedTrackInfo(track.copy(path = file.absolutePath))
                Toast.makeText(this, getString(R.string.toast_download_complete, track.title), Toast.LENGTH_SHORT).show()
                loadLocalTracks()
            } else {
                Toast.makeText(this, R.string.toast_download_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveDownloadedTrackInfo(track: AudioTrack) {
        try {
            val prefs = getSharedPreferences("dj_mixer_downloaded_tracks", android.content.Context.MODE_PRIVATE)
            val jsonStr = prefs.getString("tracks_json", "[]") ?: "[]"
            val arr = JSONArray(jsonStr)

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.optString("onlineId") == track.onlineId) {
                    return
                }
            }

            val newObj = JSONObject().apply {
                put("id", track.id)
                put("title", track.title)
                put("artist", track.artist)
                put("path", track.path)
                put("duration", track.duration)
                put("folder", track.folder)
                put("onlineId", track.onlineId)
                put("imageUrl", track.imageUrl)
            }
            arr.put(newObj)
            prefs.edit().putString("tracks_json", arr.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getDownloadedTracks(): List<AudioTrack> {
        val list = mutableListOf<AudioTrack>()
        try {
            val prefs = getSharedPreferences("dj_mixer_downloaded_tracks", android.content.Context.MODE_PRIVATE)
            val jsonStr = prefs.getString("tracks_json", "[]") ?: "[]"
            val arr = JSONArray(jsonStr)
            val cacheDir = File(cacheDir, "jamendo_cache")

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val onlineId = obj.optString("onlineId")
                val path = obj.optString("path")
                val localFile = if (path.isNotEmpty()) File(path) else File(cacheDir, "jamendo_${onlineId}.mp3")

                if (localFile.exists() && localFile.length() > 10000) {
                    list.add(
                        AudioTrack(
                            id = obj.optLong("id", System.currentTimeMillis()),
                            title = obj.optString("title", "Jamendo Track"),
                            artist = obj.optString("artist", "Jamendo Artist"),
                            path = localFile.absolutePath,
                            duration = obj.optLong("duration", 180000L),
                            folder = obj.optString("folder", "Downloaded"),
                            isRecording = false,
                            isOnline = true,
                            onlineId = onlineId,
                            imageUrl = obj.optString("imageUrl", "")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    inner class AudioTrackAdapter(
        private val items: List<AudioTrack>,
        private val onItemClick: (AudioTrack, Int) -> Unit
    ) : RecyclerView.Adapter<AudioTrackAdapter.ViewHolder>() {

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val imgThumb: ImageView = v.findViewById(R.id.img_thumb)
            val txtTitle: TextView = v.findViewById(R.id.txt_title)
            val txtSubtitle: TextView = v.findViewById(R.id.txt_subtitle)
            val btnDownload: View = v.findViewById(R.id.btn_download)
            val tagAd: View = v.findViewById(R.id.tag_ad)
            val btnAction: TextView = v.findViewById(R.id.btn_action)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_audio_track, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val track = items[position]
            holder.txtTitle.text = track.title

            val mins = (track.duration / 1000) / 60
            val secs = (track.duration / 1000) % 60
            val durStr = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

            val tagLabel = if (track.isOnline) "Jamendo CC" else track.folder
            holder.txtSubtitle.text = "${track.artist}  •  $durStr  •  $tagLabel"
            val deckName = if (targetDeck == 0) "A" else "B"
            holder.btnAction.text = holder.itemView.context.getString(R.string.btn_load_deck, deckName)
            val deckColor = ContextCompat.getColor(
                holder.itemView.context,
                if (targetDeck == 0) R.color.theme_color_left else R.color.theme_color_right
            )
            holder.btnAction.setTextColor(deckColor)

            if (track.isOnline) {
                holder.btnDownload.visibility = View.VISIBLE
                val isUnlocked = isTrackUnlocked(track.onlineId)
                holder.tagAd.visibility = if (isUnlocked) View.GONE else View.VISIBLE
                holder.btnDownload.setOnClickListener {
                    handleDownloadClick(track, holder.bindingAdapterPosition)
                }
            } else {
                holder.btnDownload.visibility = View.GONE
            }

            if (track.imageUrl.isNotEmpty()) {
                holder.imgThumb.setPadding(0, 0, 0, 0)
                Glide.with(holder.itemView.context)
                    .load(track.imageUrl)
                    .placeholder(R.drawable.vector_default_music)
                    .error(R.drawable.vector_default_music)
                    .transform(CenterCrop(), RoundedCorners(16))
                    .into(holder.imgThumb)
            } else {
                val pad = (holder.itemView.resources.displayMetrics.density * 6).toInt()
                holder.imgThumb.setPadding(pad, pad, pad, pad)
                holder.imgThumb.setImageResource(R.drawable.vector_default_music)
            }

            holder.itemView.setOnClickListener { onItemClick(track, holder.bindingAdapterPosition) }
            holder.btnAction.setOnClickListener { onItemClick(track, holder.bindingAdapterPosition) }
        }

        override fun getItemCount(): Int = items.size
    }
}
