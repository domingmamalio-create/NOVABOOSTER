package com.drayvokz.booster

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

class FloatingPanel(private val ctx: Context) {

    private val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var root: LinearLayout? = null
    private var lp: WindowManager.LayoutParams? = null
    private var handler: Handler? = null
    private var running = false

    private var tvFps: TextView? = null
    private var tvRam: TextView? = null

    private fun dp(v: Int): Int =
        (v * ctx.resources.displayMetrics.density + 0.5f).toInt()

    fun isRunning() = running

    fun start() {
        if (running) return
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE

        val panel = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = GradientDrawable().apply {
                setColor(0xE61A1228.toInt())
                cornerRadius = dp(10).toFloat()
                setStroke(dp(1), 0xFFB482FF.toInt())
            }
        }

        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(ctx).apply {
            text = "◆ DRAYVOKZ"
            setTextColor(0xFFB482FF.toInt())
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, dp(8), 0)
        }
        val btnMin = TextView(ctx).apply {
            text = "–"
            setTextColor(0xFF78D4FF.toInt())
            textSize = 16f
            setPadding(dp(8), 0, dp(4), 0)
        }
        val btnClose = TextView(ctx).apply {
            text = "✕"
            setTextColor(0xFFFF6B8A.toInt())
            textSize = 14f
            setPadding(dp(4), 0, dp(4), 0)
        }
        header.addView(title)
        header.addView(btnMin)
        header.addView(btnClose)

        val body = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, 0)
        }
        tvFps = TextView(ctx).apply {
            text = "FPS  ~60"
            setTextColor(0xFF78FFB4.toInt())
            textSize = 13f
            typeface = Typeface.MONOSPACE
        }
        tvRam = TextView(ctx).apply {
            text = "RAM  --"
            setTextColor(0xFFFFD166.toInt())
            textSize = 11f
            typeface = Typeface.MONOSPACE
        }
        body.addView(tvFps)
        body.addView(tvRam)

        panel.addView(header)
        panel.addView(body)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(40)
            y = dp(200)
        }

        var dragging = false
        var downX = 0f; var downY = 0f
        var startX = 0; var startY = 0
        header.setOnTouchListener { _: View, e: MotionEvent ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dragging = false
                    downX = e.rawX; downY = e.rawY
                    startX = params.x; startY = params.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY
                    if (abs(dx) > dp(4) || abs(dy) > dp(4)) dragging = true
                    if (dragging) {
                        params.x = startX + dx.toInt()
                        params.y = startY + dy.toInt()
                        try { wm.updateViewLayout(panel, params) } catch (_: Exception) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP -> true
                else -> false
            }
        }

        var minimized = false
        btnMin.setOnClickListener {
            minimized = !minimized
            body.visibility = if (minimized) View.GONE else View.VISIBLE
            btnMin.text = if (minimized) "+" else "–"
        }
        btnClose.setOnClickListener { stop() }

        try {
            wm.addView(panel, params)
            root = panel
            lp = params
            running = true
            startTick()
        } catch (_: Exception) {
            running = false
        }
    }

    fun stop() {
        running = false
        try {
            handler?.removeCallbacksAndMessages(null)
            handler = null
            root?.let { wm.removeView(it) }
        } catch (_: Exception) {}
        root = null
        lp = null
    }

    @SuppressLint("SetTextI18n")
    private fun startTick() {
        handler = Handler(Looper.getMainLooper())
        handler?.post(object : Runnable {
            override fun run() {
                if (!running) return
                val (used, total) = DeviceOptimizer.ramInfo(ctx)
                tvRam?.text = "RAM  $used / $total MB"
                handler?.postDelayed(this, 1000)
            }
        })
    }
}
