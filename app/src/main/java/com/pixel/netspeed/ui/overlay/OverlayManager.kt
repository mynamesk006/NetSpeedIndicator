package com.pixel.netspeed.ui.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.pixel.netspeed.MainActivity
import com.pixel.netspeed.data.model.AppSettings
import com.pixel.netspeed.data.model.DisplayMode
import com.pixel.netspeed.data.model.NetworkSpeed
import com.pixel.netspeed.domain.SpeedCalculator
import kotlin.math.abs

class OverlayManager(
    private val context: Context,
    private val onPositionSaved: (x: Int, y: Int) -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var overlayView: View? = null
    private var params: WindowManager.LayoutParams? = null

    private var tvDownload: TextView? = null
    private var tvUpload: TextView? = null
    private var tvCombined: TextView? = null
    private var containerLayout: LinearLayout? = null

    private var currentSettings: AppSettings = AppSettings()
    private var isAttached = false

    fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            context.resources.getDimensionPixelSize(resourceId)
        } else {
            dpToPx(36)
        }
    }

    fun showOverlay(settings: AppSettings) {
        runOnMain {
            this.currentSettings = settings
            if (!canDrawOverlays()) {
                hideOverlayInternal()
                return@runOnMain
            }

            val minSafeY = getStatusBarHeight() + dpToPx(8)
            val initialY = if (settings.overlayY < minSafeY) minSafeY else settings.overlayY

            if (overlayView == null) {
                val view = LinearLayout(context).apply {
                    orientation = if (settings.isCompactOverlay) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
                    elevation = dpToPx(8).toFloat()
                }

                containerLayout = view

                val tvDown = TextView(context).apply {
                    id = View.generateViewId()
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, settings.overlayTextSizeSp.toFloat())
                    text = "↓ 0 B/s"
                }

                val tvUp = TextView(context).apply {
                    id = View.generateViewId()
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, settings.overlayTextSizeSp.toFloat())
                    text = "↑ 0 B/s"
                    if (settings.isCompactOverlay) {
                        setPadding(dpToPx(8), 0, 0, 0)
                    } else {
                        setPadding(0, dpToPx(2), 0, 0)
                    }
                }

                val tvComb = TextView(context).apply {
                    id = View.generateViewId()
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, settings.overlayTextSizeSp.toFloat())
                    text = "0 B/s"
                }

                tvDownload = tvDown
                tvUpload = tvUp
                tvCombined = tvComb

                view.addView(tvDown)
                view.addView(tvUp)
                view.addView(tvComb)

                val layoutParams = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    x = settings.overlayX
                    y = initialY
                }

                params = layoutParams
                overlayView = view

                applyAppearance(settings)

                // Touch dragging logic
                setupTouchListener(view)

                try {
                    windowManager.addView(view, layoutParams)
                    isAttached = true
                } catch (_: Exception) {
                    isAttached = false
                }
            } else {
                updateSettingsInternal(settings)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListener(view: View) {
        var initialX = 0
        var initialYPos = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = false

        view.setOnTouchListener { _, event ->
            val p = params ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = p.x
                    initialYPos = p.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 5 || abs(dy) > 5) {
                        isClick = false
                    }
                    p.x = initialX + dx
                    p.y = (initialYPos + dy).coerceAtLeast(getStatusBarHeight())
                    try {
                        windowManager.updateViewLayout(overlayView, p)
                    } catch (_: Exception) {}
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        val intent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        context.startActivity(intent)
                    } else {
                        onPositionSaved(p.x, p.y)
                    }
                    true
                }
                else -> false
            }
        }
    }

    fun updateSettings(settings: AppSettings) {
        runOnMain { updateSettingsInternal(settings) }
    }

    private fun updateSettingsInternal(settings: AppSettings) {
        this.currentSettings = settings
        if (!canDrawOverlays()) {
            hideOverlayInternal()
            return
        }

        val view = overlayView ?: run {
            showOverlay(settings)
            return
        }

        val minSafeY = getStatusBarHeight() + dpToPx(8)
        val safeY = if (settings.overlayY < minSafeY) minSafeY else settings.overlayY

        containerLayout?.orientation = if (settings.isCompactOverlay) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL

        tvDownload?.setTextSize(TypedValue.COMPLEX_UNIT_SP, settings.overlayTextSizeSp.toFloat())
        tvUpload?.setTextSize(TypedValue.COMPLEX_UNIT_SP, settings.overlayTextSizeSp.toFloat())
        tvCombined?.setTextSize(TypedValue.COMPLEX_UNIT_SP, settings.overlayTextSizeSp.toFloat())

        if (settings.isCompactOverlay) {
            tvUpload?.setPadding(dpToPx(8), 0, 0, 0)
        } else {
            tvUpload?.setPadding(0, dpToPx(2), 0, 0)
        }

        applyAppearance(settings)

        params?.let { p ->
            p.x = settings.overlayX
            p.y = safeY
            if (isAttached && view.isAttachedToWindow) {
                try {
                    windowManager.updateViewLayout(view, p)
                } catch (_: Exception) {}
            }
        }
    }

    fun updateSpeed(speed: NetworkSpeed) {
        runOnMain {
            if (overlayView == null || !isAttached) return@runOnMain

            if (currentSettings.hideWhenZeroSpeed &&
                speed.downloadBytesPerSec == 0L && speed.uploadBytesPerSec == 0L
            ) {
                containerLayout?.visibility = View.GONE
                return@runOnMain
            } else {
                containerLayout?.visibility = View.VISIBLE
            }

            val unit = currentSettings.speedUnit
            val mode = currentSettings.displayMode

            val downStr = "↓ ${SpeedCalculator.formatSpeed(speed.downloadBytesPerSec, unit)}"
            val upStr = "↑ ${SpeedCalculator.formatSpeed(speed.uploadBytesPerSec, unit)}"
            val combStr = "⇅ ${SpeedCalculator.formatSpeed(speed.combinedBytesPerSec, unit)}"

            when (mode) {
                DisplayMode.BOTH -> {
                    tvDownload?.visibility = View.VISIBLE
                    tvUpload?.visibility = View.VISIBLE
                    tvCombined?.visibility = View.GONE

                    tvDownload?.text = downStr
                    tvUpload?.text = upStr
                }
                DisplayMode.DOWNLOAD_ONLY -> {
                    tvDownload?.visibility = View.VISIBLE
                    tvUpload?.visibility = View.GONE
                    tvCombined?.visibility = View.GONE

                    tvDownload?.text = downStr
                }
                DisplayMode.UPLOAD_ONLY -> {
                    tvDownload?.visibility = View.GONE
                    tvUpload?.visibility = View.VISIBLE
                    tvCombined?.visibility = View.GONE

                    tvUpload?.text = upStr
                }
                DisplayMode.COMBINED -> {
                    tvDownload?.visibility = View.GONE
                    tvUpload?.visibility = View.GONE
                    tvCombined?.visibility = View.VISIBLE

                    tvCombined?.text = combStr
                }
            }
        }
    }

    fun hideOverlay() {
        runOnMain { hideOverlayInternal() }
    }

    private fun hideOverlayInternal() {
        overlayView?.let { view ->
            if (isAttached && view.isAttachedToWindow) {
                try {
                    windowManager.removeView(view)
                } catch (_: Exception) {}
            }
        }
        overlayView = null
        params = null
        containerLayout = null
        tvDownload = null
        tvUpload = null
        tvCombined = null
        isAttached = false
    }

    private fun applyAppearance(settings: AppSettings) {
        val bgColor = parseColorWithAlpha(settings.overlayBgColorHex, settings.overlayBgOpacity)
        val textColor = parseColorHex(settings.overlayTextColorHex)

        val shape = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(12).toFloat()
            setColor(bgColor)
            setStroke(dpToPx(1), Color.parseColor("#44FFFFFF"))
        }

        containerLayout?.background = shape
        tvDownload?.setTextColor(textColor)
        tvUpload?.setTextColor(textColor)
        tvCombined?.setTextColor(textColor)
    }

    private fun parseColorHex(colorHex: String): Int {
        return try {
            Color.parseColor(colorHex)
        } catch (_: Exception) {
            Color.WHITE
        }
    }

    private fun parseColorWithAlpha(colorHex: String, opacity: Float): Int {
        val baseColor = parseColorHex(colorHex)
        val alpha = (opacity.coerceIn(0.1f, 1.0f) * 255).toInt()
        return Color.argb(alpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    private inline fun runOnMain(crossinline action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post { action() }
        }
    }
}
