
package com.jarvis.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private lateinit var status: TextView
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val requestCode = 42

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)

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
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
                speechRecognizer = null
                tts?.stop()
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

    override fun onInit(result: Int) {
        if (result == TextToSpeech.SUCCESS) {
            val languageResult = tts?.setLanguage(Locale.ITALY)

            ttsReady =
                languageResult != TextToSpeech.LANG_MISSING_DATA &&
                languageResult != TextToSpeech.LANG_NOT_SUPPORTED

            if (ttsReady) {
                speak("J.A.R.V.I.S. online. Sistema vocale pronto.")
            }
        }
    }

    private fun speak(message: String) {
        if (ttsReady) {
            tts?.speak(
                message,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "JARVIS"
            )
        }
    }

    private fun getResponse(question: String): String {
        val text = question.lowercase(Locale.ITALY)

        return when {
            "ciao" in text || "salve" in text ->
                "Salve. È un piacere sentirti."

            "come stai" in text ->
                "Tutti i sistemi sono operativi. Grazie per averlo chiesto."

            "chi sei" in text || "come ti chiami" in text ->
                "Sono J.A.R.V.I.S., il tuo assistente personale."

            "che ore sono" in text ->
                "Sono le " + SimpleDateFormat(
                    "HH:mm",
                    Locale.ITALY
                ).format(Date())

            "che giorno è" in text ->
                "Oggi è " + SimpleDateFormat(
                    "EEEE d MMMM yyyy",
                    Locale.ITALY
                ).format(Date())

            "grazie" in text ->
                "Sempre a tua disposizione."

            "come ti senti" in text ->
                "Tutti i sistemi funzionano correttamente."

            "arrivederci" in text || "a dopo" in text ->
                "A presto. Rimango a tua disposizione."

            else ->
                "Ho ricevuto il tuo comando: $question. Le mie capacità sono ancora in fase di sviluppo."
        }
    }

    private fun requestAudioAndStart() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO
        )

        if (Build.VERSION.SDK_INT >= 33) {
            permissions.add(
                Manifest.permission.POST_NOTIFICATIONS
            )
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

        startVoiceRecognition()
    }

    private fun startVoiceRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.text = "Riconoscimento vocale non disponibile"
            speak("Riconoscimento vocale non disponibile.")
            return
        }

        speechRecognizer?.destroy()

        speechRecognizer =
            SpeechRecognizer.createSpeechRecognizer(this)

        speechRecognizer?.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(params: Bundle?) {
                    status.text = "J.A.R.V.I.S.\nTi ascolto..."
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )

                    val spokenText = matches?.firstOrNull()

                    if (spokenText != null) {
                        val response = getResponse(spokenText)

                        status.text =
                            "Tu: $spokenText\n\nJ.A.R.V.I.S.: $response"

                        speak(response)
                    } else {
                        status.text = "Non ho capito."
                        speak("Mi dispiace, non ho capito.")
                    }
                }

                override fun onError(error: Int) {
                    status.text =
                        "Errore nel riconoscimento vocale: $error"
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            }
        )

        val intent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "it-IT"
            )
        }

        speechRecognizer?.startListening(intent)
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null

        tts?.stop()
        tts?.shutdown()
        tts = null

        super.onDestroy()
    }
}
