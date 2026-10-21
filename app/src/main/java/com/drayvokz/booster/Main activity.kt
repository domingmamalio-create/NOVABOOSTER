package com.drayvokz.booster

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var panel: FloatingPanel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        panel = FloatingPanel(this)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val tvInfo = findViewById<TextView>(R.id.tvInfo)

        findViewById<Button>(R.id.btnKillBg).setOnClickListener {
            tvStatus.text = "killing background…"
            thread {
                val killed = DeviceOptimizer.killBackground(this)
                val (used, total) = DeviceOptimizer.ramInfo(this)
                runOnUiThread {
                    tvStatus.text = "killed $killed bg · RAM $used/$total MB"
                }
            }
        }

        findViewById<Button>(R.id.btnCleanCache).setOnClickListener {
            tvStatus.text = "cleaning…"
            thread {
                val freed = DeviceOptimizer.clearCache(this) / 1048576
                runOnUiThread { tvStatus.text = "cache freed: $freed MB" }
            }
        }

        findViewById<Button>(R.id.btnTogglePanel).setOnClickListener {
            val btn = it as Button
            if (panel.isRunning()) {
                panel.stop()
                stopService(Intent(this, BoosterService::class.java))
                tvStatus.text = "panel stopped"
                btn.text = getString(R.string.start_panel)
            } else {
                if (!Settings.canDrawOverlays(this)) {
                    try {
                        startActivity(Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        ))
                    } catch (_: Exception) {}
                    Toast.makeText(this, "grant overlay permission", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                panel.start()
                startForegroundService(Intent(this, BoosterService::class.java))
                tvStatus.text = "panel active"
                btn.text = getString(R.string.stop_panel)
            }
        }

        val (used, total) = DeviceOptimizer.ramInfo(this)
        tvInfo.text = buildString {
            appendLine("RAM  : $used / $total MB")
            appendLine("ARCH : ${Build.SUPPORTED_ABIS.firstOrNull() ?: "?"}")
            append("ANDROID: ${Build.VERSION.RELEASE}")
        }
    }

    override fun onDestroy() {
        panel.stop()
        super.onDestroy()
    }
}
