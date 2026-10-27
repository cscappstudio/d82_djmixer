package com.audioeffect.djmixer.music.mixer

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.audioeffect.djmixer.music.mixer.sampler.SamplePack
import com.audioeffect.djmixer.music.mixer.sampler.SamplerManager
import com.audioeffect.djmixer.music.mixer.core.utils.SystemUtil
import com.audioeffect.djmixer.music.mixer.core.ads.AdsManager

class ActivitySoundSampleStore : AppCompatActivity() {

    private lateinit var recyclerPacks: RecyclerView
    private lateinit var layoutEmpty: View
    private lateinit var btnSearch: ImageView
    private lateinit var btnFolder: ImageView
    private lateinit var btnMoreOptions: ImageView
    private lateinit var layoutSearchBar: LinearLayout
    private lateinit var edtSearch: EditText
    private lateinit var btnClearSearch: ImageView
    private lateinit var txtStoreTitle: TextView

    private lateinit var adapter: SamplePackAdapter
    private var allPacks: List<SamplePack> = emptyList()
    private var displayedPacks: MutableList<SamplePack> = mutableListOf()

    private var mediaPlayer: MediaPlayer? = null
    private var playingPackId: String? = null
    private var isFolderFilterActive: Boolean = false

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(SystemUtil.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sound_sample_store)

        initViews()
        initData()
        setupListeners()
    }

    private fun initViews() {
        recyclerPacks = findViewById(R.id.recycler_packs)
        layoutEmpty = findViewById(R.id.layout_empty)
        btnSearch = findViewById(R.id.btn_search)
        btnFolder = findViewById(R.id.btn_folder)
        btnMoreOptions = findViewById(R.id.btn_more_options)
        layoutSearchBar = findViewById(R.id.layout_search_bar)
        edtSearch = findViewById(R.id.edt_search)
        btnClearSearch = findViewById(R.id.btn_clear_search)
        txtStoreTitle = findViewById(R.id.txt_store_title)

        findViewById<View>(R.id.btn_back).setOnClickListener {
            stopPreview()
            finish()
        }

        recyclerPacks.layoutManager = GridLayoutManager(this, 3)
    }

    private fun initData() {
        allPacks = SamplerManager.loadAllPacks(this)
        displayedPacks.clear()
        displayedPacks.addAll(allPacks)

        adapter = SamplePackAdapter(
            packs = displayedPacks,
            onPackClick = { pack ->
                if (pack.isCustom) {
                    showCustomPackInfoDialog()
                } else if (!SamplerManager.isPackUnlocked(this, pack.id)) {
                    AdsManager.showReward(
                        activity = this,
                        onEarnedReward = {
                            SamplerManager.setPackUnlocked(this, pack.id)
                            AdsManager.setRewardWatched()
                            adapter.notifyDataSetChanged()
                            Toast.makeText(this, getString(R.string.toast_unlocked_sound, pack.title), Toast.LENGTH_SHORT).show()
                            showApplyPackDialog(pack)
                        },
                        onFailed = {
                            Toast.makeText(this, R.string.toast_watch_reward_unlock, Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    showApplyPackDialog(pack)
                }
            },
            onPreviewClick = { pack ->
                togglePreview(pack)
            }
        )
        recyclerPacks.adapter = adapter
    }

    private fun setupListeners() {
        // Toggle search bar
        btnSearch.setOnClickListener {
            if (layoutSearchBar.visibility == View.VISIBLE) {
                closeSearchBar()
            } else {
                openSearchBar()
            }
        }

        btnClearSearch.setOnClickListener {
            edtSearch.setText("")
        }

        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim().orEmpty()
                btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                filterPacks(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Folder button: Toggle "My Samples" filter (Active decks + Custom + Essential)
        btnFolder.setOnClickListener {
            isFolderFilterActive = !isFolderFilterActive
            btnFolder.imageTintList = android.content.res.ColorStateList.valueOf(
                if (isFolderFilterActive) Color.parseColor("#22D3EE") else Color.parseColor("#CBD5E1")
            )
            applyFilters()
        }

        // More options overflow menu
        btnMoreOptions.setOnClickListener { v ->
            showOverflowMenu(v)
        }
    }

    private fun openSearchBar() {
        layoutSearchBar.visibility = View.VISIBLE
        txtStoreTitle.visibility = View.GONE
        btnSearch.visibility = View.GONE
        edtSearch.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(edtSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun closeSearchBar() {
        layoutSearchBar.visibility = View.GONE
        txtStoreTitle.visibility = View.VISIBLE
        btnSearch.visibility = View.VISIBLE
        edtSearch.setText("")
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(edtSearch.windowToken, 0)
        applyFilters()
    }

    private fun filterPacks(query: String) {
        val currentDeckA = SamplerManager.getActivePackDeckA(this)
        val currentDeckB = SamplerManager.getActivePackDeckB(this)

        displayedPacks.clear()
        for (pack in allPacks) {
            val matchesFolder = if (isFolderFilterActive) {
                pack.isCustom || pack.isEssential || pack.id == currentDeckA || pack.id == currentDeckB
            } else {
                true
            }
            val matchesQuery = if (query.isNotEmpty()) {
                pack.title.contains(query, ignoreCase = true) || pack.subtitle.contains(query, ignoreCase = true)
            } else {
                true
            }

            if (matchesFolder && matchesQuery) {
                displayedPacks.add(pack)
            }
        }

        adapter.notifyDataSetChanged()
        layoutEmpty.visibility = if (displayedPacks.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun applyFilters() {
        filterPacks(edtSearch.text.toString().trim())
    }

    private fun showOverflowMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, getString(R.string.essential))
        popup.menu.add(0, 2, 1, getString(R.string.all_samples))
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    // Reset both decks to Essential
                    SamplerManager.setActivePackDeckA(this, SamplerManager.ID_ESSENTIAL)
                    SamplerManager.setActivePackDeckB(this, SamplerManager.ID_ESSENTIAL)
                    adapter.notifyDataSetChanged()
                    Toast.makeText(this, getString(R.string.pack_applied_both), Toast.LENGTH_SHORT).show()
                    true
                }
                2 -> {
                    isFolderFilterActive = false
                    btnFolder.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#CBD5E1"))
                    closeSearchBar()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun togglePreview(pack: SamplePack) {
        if (playingPackId == pack.id) {
            stopPreview()
            adapter.setPlayingPackId(null)
            return
        }

        stopPreview()

        try {
            val afd: AssetFileDescriptor = assets.openFd(pack.previewAssetPath)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                prepare()
                setOnCompletionListener {
                    playingPackId = null
                    adapter.setPlayingPackId(null)
                }
                start()
            }
            playingPackId = pack.id
            adapter.setPlayingPackId(pack.id)
        } catch (e: Exception) {
            e.printStackTrace()
            // If asset not directly openable via openFd (compressed), copy to cache and play
            try {
                val outFile = java.io.File(cacheDir, "preview_${pack.id}.mp3")
                if (!outFile.exists()) {
                    assets.open(pack.previewAssetPath).use { input ->
                        java.io.FileOutputStream(outFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(outFile.absolutePath)
                    prepare()
                    setOnCompletionListener {
                        playingPackId = null
                        adapter.setPlayingPackId(null)
                    }
                    start()
                }
                playingPackId = pack.id
                adapter.setPlayingPackId(pack.id)
            } catch (ex: Exception) {
                ex.printStackTrace()
                Toast.makeText(this, "Cannot play preview", Toast.LENGTH_SHORT).show()
                playingPackId = null
                adapter.setPlayingPackId(null)
            }
        }
    }

    private fun stopPreview() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // ignore
        }
        mediaPlayer = null
        playingPackId = null
    }

    private fun showApplyPackDialog(pack: SamplePack) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_apply_sampler_pack)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.52).toInt().coerceAtLeast(420),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Set Cover
        val imgCover = dialog.findViewById<ImageView>(R.id.dialog_pack_cover)
        loadCoverImage(pack.coverAssetPath, imgCover)

        dialog.findViewById<TextView>(R.id.dialog_pack_title).text = pack.title
        val subStr = "${pack.subtitle} • ${getString(R.string.sounds_count, pack.soundCount)}"
        dialog.findViewById<TextView>(R.id.dialog_pack_subtitle).text = subStr

        val applyWithReward = { onApply: () -> Unit ->
            if (!SamplerManager.isPackUnlocked(this, pack.id)) {
                AdsManager.showReward(
                    activity = this,
                    onEarnedReward = {
                        SamplerManager.setPackUnlocked(this, pack.id)
                        AdsManager.setRewardWatched()
                        adapter.notifyDataSetChanged()
                        Toast.makeText(this, getString(R.string.toast_unlocked_sound, pack.title), Toast.LENGTH_SHORT).show()
                        onApply()
                        dialog.dismiss()
                    },
                    onFailed = {
                        Toast.makeText(this, R.string.toast_watch_reward_unlock, Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                onApply()
                dialog.dismiss()
            }
        }

        dialog.findViewById<View>(R.id.btn_apply_deck_a).setOnClickListener {
            applyWithReward {
                SamplerManager.setActivePackDeckA(this, pack.id)
                adapter.notifyDataSetChanged()
                Toast.makeText(this, getString(R.string.pack_applied, "A"), Toast.LENGTH_SHORT).show()
            }
        }

        dialog.findViewById<View>(R.id.btn_apply_deck_b).setOnClickListener {
            applyWithReward {
                SamplerManager.setActivePackDeckB(this, pack.id)
                adapter.notifyDataSetChanged()
                Toast.makeText(this, getString(R.string.pack_applied, "B"), Toast.LENGTH_SHORT).show()
            }
        }

        dialog.findViewById<View>(R.id.btn_apply_both_decks).setOnClickListener {
            applyWithReward {
                SamplerManager.setActivePackDeckA(this, pack.id)
                SamplerManager.setActivePackDeckB(this, pack.id)
                adapter.notifyDataSetChanged()
                Toast.makeText(this, getString(R.string.pack_applied_both), Toast.LENGTH_SHORT).show()
            }
        }

        dialog.findViewById<View>(R.id.btn_cancel_dialog).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showCustomPackInfoDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_apply_sampler_pack)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.52).toInt().coerceAtLeast(420),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val imgCover = dialog.findViewById<ImageView>(R.id.dialog_pack_cover)
        loadCoverImage("sampler/icon_custom.webp", imgCover)

        dialog.findViewById<TextView>(R.id.dialog_pack_title).text = getString(R.string.custom_sound_sample)
        dialog.findViewById<TextView>(R.id.dialog_pack_subtitle).text = getString(R.string.import_sample)

        dialog.findViewById<View>(R.id.btn_apply_deck_a).setOnClickListener {
            SamplerManager.setActivePackDeckA(this, SamplerManager.ID_CUSTOM)
            adapter.notifyDataSetChanged()
            Toast.makeText(this, getString(R.string.pack_applied, "A"), Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.findViewById<View>(R.id.btn_apply_deck_b).setOnClickListener {
            SamplerManager.setActivePackDeckB(this, SamplerManager.ID_CUSTOM)
            adapter.notifyDataSetChanged()
            Toast.makeText(this, getString(R.string.pack_applied, "B"), Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.findViewById<View>(R.id.btn_apply_both_decks).setOnClickListener {
            SamplerManager.setActivePackDeckA(this, SamplerManager.ID_CUSTOM)
            SamplerManager.setActivePackDeckB(this, SamplerManager.ID_CUSTOM)
            adapter.notifyDataSetChanged()
            Toast.makeText(this, getString(R.string.pack_applied_both), Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.findViewById<View>(R.id.btn_cancel_dialog).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun loadCoverImage(path: String, imageView: ImageView) {
        try {
            assets.open(path).use { stream ->
                val bmp = BitmapFactory.decodeStream(stream)
                if (bmp != null) {
                    imageView.setImageBitmap(bmp)
                    return
                }
            }
        } catch (e: Exception) {
            // fallback
        }
        imageView.setImageResource(R.drawable.sample_off)
    }

    override fun onPause() {
        super.onPause()
        stopPreview()
        adapter.setPlayingPackId(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPreview()
    }

    // =========================================================================
    // Adapter
    // =========================================================================
    inner class SamplePackAdapter(
        private val packs: List<SamplePack>,
        private val onPackClick: (SamplePack) -> Unit,
        private val onPreviewClick: (SamplePack) -> Unit
    ) : RecyclerView.Adapter<SamplePackAdapter.PackViewHolder>() {

        private var currentPlayingId: String? = null

        @SuppressLint("NotifyDataSetChanged")
        fun setPlayingPackId(id: String?) {
            currentPlayingId = id
            notifyDataSetChanged()
        }

        inner class PackViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imgCover: ImageView = view.findViewById(R.id.img_cover)
            val tagAd: TextView? = view.findViewById(R.id.tag_ad_pack)
            val txtTitle: TextView = view.findViewById(R.id.txt_title)
            val txtSubtitle: TextView = view.findViewById(R.id.txt_subtitle)
            val badgeA: TextView = view.findViewById(R.id.badge_deck_a)
            val badgeB: TextView = view.findViewById(R.id.badge_deck_b)
            val btnAction: View = view.findViewById(R.id.btn_action)
            val imgAction: ImageView = view.findViewById(R.id.img_action)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_sample_pack, parent, false)
            return PackViewHolder(view)
        }

        override fun onBindViewHolder(holder: PackViewHolder, position: Int) {
            val pack = packs[position]
            val context = holder.itemView.context

            // Title & Subtitle
            holder.txtTitle.text = pack.title
            holder.txtSubtitle.text = pack.subtitle

            // Cover Image
            loadCoverImage(pack.coverAssetPath, holder.imgCover)

            // AD Badge
            val isUnlocked = SamplerManager.isPackUnlocked(context, pack.id)
            holder.tagAd?.visibility = if (!isUnlocked) View.VISIBLE else View.GONE

            // Deck Badges
            val activeDeckA = SamplerManager.getActivePackDeckA(context)
            val activeDeckB = SamplerManager.getActivePackDeckB(context)

            holder.badgeA.visibility = if (pack.id == activeDeckA) View.VISIBLE else View.GONE
            holder.badgeB.visibility = if (pack.id == activeDeckB) View.VISIBLE else View.GONE

            // Action / Preview Button
            if (pack.isCustom) {
                holder.btnAction.visibility = View.INVISIBLE
            } else {
                holder.btnAction.visibility = View.VISIBLE
                val isPlaying = (currentPlayingId == pack.id)
                if (isPlaying) {
                    holder.imgAction.setImageResource(R.drawable.vector_music_pause)
                    holder.imgAction.imageTintList = android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.theme_color_left)
                    )
                } else {
                    holder.imgAction.setImageResource(R.drawable.ic_play)
                    holder.imgAction.imageTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
                }

                holder.btnAction.setOnClickListener {
                    onPreviewClick(pack)
                }
            }

            // Click Card
            holder.itemView.setOnClickListener {
                onPackClick(pack)
            }
        }

        override fun getItemCount(): Int = packs.size
    }
}
