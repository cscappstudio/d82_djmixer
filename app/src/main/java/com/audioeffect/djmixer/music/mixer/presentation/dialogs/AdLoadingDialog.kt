package com.audioeffect.djmixer.music.mixer.presentation.dialogs

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Window
import android.view.WindowManager
import com.audioeffect.djmixer.music.mixer.R

class AdLoadingDialog(private val activity: Activity) : Dialog(activity, R.style.Theme_Transparent_Dialog) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_ad_loading)
        setCancelable(false)
        setCanceledOnTouchOutside(false)

        window?.let { win ->
            win.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            win.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            win.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
        }
    }

    companion object {
        private const val TAG = "AdLoadingDialog"
        private var currentDialog: AdLoadingDialog? = null

        fun show(activity: Activity?): AdLoadingDialog? {
            if (activity == null || activity.isFinishing || activity.isDestroyed) {
                return null
            }

            dismiss()

            return try {
                val dialog = AdLoadingDialog(activity)
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    dialog.show()
                } else {
                    activity.runOnUiThread {
                        try {
                            if (!activity.isFinishing && !activity.isDestroyed) {
                                dialog.show()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error showing dialog on UI thread", e)
                        }
                    }
                }
                currentDialog = dialog
                dialog
            } catch (e: Exception) {
                Log.e(TAG, "Error creating AdLoadingDialog", e)
                null
            }
        }

        fun dismiss() {
            val dialogToDismiss = currentDialog ?: return
            currentDialog = null

            val dismissRunnable = Runnable {
                try {
                    if (dialogToDismiss.isShowing) {
                        dialogToDismiss.dismiss()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error dismissing dialog", e)
                }
            }

            if (Looper.myLooper() == Looper.getMainLooper()) {
                dismissRunnable.run()
            } else {
                Handler(Looper.getMainLooper()).post(dismissRunnable)
            }
        }

        fun isShowing(): Boolean {
            return currentDialog?.isShowing == true
        }
    }
}
