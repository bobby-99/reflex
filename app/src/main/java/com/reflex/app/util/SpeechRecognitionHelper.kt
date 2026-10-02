package com.reflex.app.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class SpeechRecognitionHelper(
    private val context: Context,
    private val onPartialResult: (String) -> Unit,
    private val onFinalResult: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun isAvailable(): Boolean {
        val available = SpeechRecognizer.isRecognitionAvailable(context)
        Log.d("ReflexSpeech", "SpeechRecognizer isRecognitionAvailable: $available")
        return available
    }

    fun startListening() {
        mainHandler.post {
            if (!isAvailable()) {
                Log.e("ReflexSpeech", "Speech recognition unavailable on device")
                onError("Speech recognition unavailable on this device")
                return@post
            }

            stopListeningInternal()
            createRecognizerAndStart(preferOnDevice = true)
        }
    }

    private fun createRecognizerAndStart(preferOnDevice: Boolean) {
        mainHandler.post {
            try {
                Log.d("ReflexSpeech", "Creating SpeechRecognizer (preferOnDevice=$preferOnDevice)...")
                val useOnDevice = preferOnDevice &&
                        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
                        SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

                speechRecognizer = if (useOnDevice) {
                    Log.d("ReflexSpeech", "Using createOnDeviceSpeechRecognizer")
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                } else {
                    Log.d("ReflexSpeech", "Using default createSpeechRecognizer")
                    SpeechRecognizer.createSpeechRecognizer(context)
                }

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d("ReflexSpeech", "onReadyForSpeech")
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d("ReflexSpeech", "onBeginningOfSpeech")
                    }

                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        Log.d("ReflexSpeech", "onEndOfSpeech")
                    }

                    override fun onError(error: Int) {
                        Log.w("ReflexSpeech", "SpeechRecognizer onError code: $error (useOnDevice=$useOnDevice)")
                        mainHandler.post {
                            if (useOnDevice && (error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_SERVER || error == SpeechRecognizer.ERROR_AUDIO || error == 12)) {
                                Log.i("ReflexSpeech", "Falling back from on-device to standard SpeechRecognizer...")
                                stopListeningInternal()
                                createRecognizerAndStart(preferOnDevice = false)
                                return@post
                            }

                            val msg = when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                                SpeechRecognizer.ERROR_CLIENT -> "Client error"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Audio permission required"
                                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network error"
                                else -> "Recognition error ($error)"
                            }
                            onError(msg)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        Log.d("ReflexSpeech", "onResults matches: $matches")
                        mainHandler.post {
                            if (!matches.isNullOrEmpty()) {
                                onFinalResult(matches[0])
                            }
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        Log.d("ReflexSpeech", "onPartialResults matches: $matches")
                        mainHandler.post {
                            if (!matches.isNullOrEmpty()) {
                                onPartialResult(matches[0])
                            }
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                Log.d("ReflexSpeech", "startListening intent launched")
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e("ReflexSpeech", "Exception in createRecognizerAndStart", e)
                if (preferOnDevice) {
                    Log.i("ReflexSpeech", "On-device exception, falling back to default...")
                    createRecognizerAndStart(preferOnDevice = false)
                } else {
                    onError(e.message ?: "Failed to start speech recognizer")
                }
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            stopListeningInternal()
        }
    }

    private fun stopListeningInternal() {
        try {
            Log.d("ReflexSpeech", "stopListening called")
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w("ReflexSpeech", "Error in stopListening teardown", e)
        } finally {
            speechRecognizer = null
        }
    }
}
