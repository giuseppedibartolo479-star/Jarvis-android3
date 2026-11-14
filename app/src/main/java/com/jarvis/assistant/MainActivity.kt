
package com.jarvis.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {

    private lateinit var status: TextView
    private val requestCode = 42

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        status = TextView(this).apply {
            text = "J.A.R.V.I.S.\nSistema in standby"
            textSize = 26f
            gravity = Gravity.CENTER
            setTextColor(0xFF00BFFF.toInt())
            setPadding(20, 50, 20, 50)
        }

        val start = Button(this).apply {
            text = "ATTIVA JARVIS"
            setOnClickListener {
                requestAudioAndStart()
            }
        }

        val stop = Button(this).apply {
            text = "DISATTIVA JARVIS"
            setOnClickListener {
                stopService(
                    Intent(this@MainActivity, WakeWordService::class.java)
                )
                status.text = "J.A.R.V.I.S.\nSistema disattivato"
            }
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 24, 24, 24)
            setBackgroundColor(0xFF050B18.toInt())
            addView(status)
            addView(start)
            addView(stop)
        }

        setContentView(layout)
    }

    private fun requestAudioAndStart() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)

        if (Build.VERSION.SDK_INT >= 33) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) !=
                PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missing.toTypedArray(),
                requestCode
            )
        } else {
            startWakeService()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == this.requestCode &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startWakeService()
        } else {
            status.text = "Permesso microfono necessario"
        }
    }

    private fun startWakeService() {
        val intent = Intent(this, WakeWordService::class.java)
        ContextCompat.startForegroundService(this, intent)
        status.text = "J.A.R.V.I.S.\nAvvio ascolto..."
    }
}
