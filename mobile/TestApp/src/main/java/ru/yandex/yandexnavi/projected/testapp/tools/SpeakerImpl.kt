import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.widget.Toast
import com.yandex.mapkit.annotations.AnnotationLanguage
import com.yandex.mapkit.annotations.LocalizedPhrase
import com.yandex.mapkit.annotations.Speaker
import java.util.*

class SpeakerImpl(
    private val context: Context,
    private var language: AnnotationLanguage,
    private val callback: SayCallback?
) : Speaker, OnInitListener {
    private val tts: TextToSpeech = TextToSpeech(context, this)
    private var ttsIsInitialized = false
    private var ttsIsAvailable = false

    private fun setTtsLanguage() {
        if (!ttsIsInitialized) return
        val result = tts.setLanguage(localeForLanguage(language))
        ttsIsAvailable = result != TextToSpeech.LANG_MISSING_DATA &&
            result != TextToSpeech.LANG_NOT_SUPPORTED
        if (!ttsIsAvailable) {
            Toast.makeText(context, "Chosen language is not supported by TTS", Toast.LENGTH_LONG).show()
        }
    }

    interface SayCallback {
        fun onSay(string: String?)
    }

    fun setLanguage(language: AnnotationLanguage) {
        this.language = language
        setTtsLanguage()
    }

    //region Speaker
    override fun say(phrase: LocalizedPhrase) {
        val result = phrase.text
        callback?.onSay(result)
        if (ttsIsAvailable) {
            tts.speak(result, TextToSpeech.QUEUE_FLUSH, null)
        }
    }

    override fun duration(phrase: LocalizedPhrase): Double {
        // simplified Russian formula, see navikit/tts/TtsPlayerImpl.java
        return phrase.text.length * 0.06 + 0.6
    }

    override fun reset() {
        if (ttsIsAvailable) {
            tts.stop()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ttsIsInitialized = true
            setTtsLanguage()
        } else {
            val message = "Failed to initialize tts engine with error $status"
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private fun localeForLanguage(language: AnnotationLanguage): Locale? {
            when (language) {
                AnnotationLanguage.RUSSIAN -> return Locale("ru", "RU")
                AnnotationLanguage.ENGLISH -> return Locale("en", "US")
                AnnotationLanguage.ITALIAN -> return Locale("it", "IT")
                AnnotationLanguage.FRENCH -> return Locale("fr", "FR")
                AnnotationLanguage.TURKISH -> return Locale("tr", "TR")
                AnnotationLanguage.UKRAINIAN -> return Locale("uk", "UA")
                AnnotationLanguage.HEBREW -> return Locale("he", "IL")
            }
            return null
        }
    }
}
