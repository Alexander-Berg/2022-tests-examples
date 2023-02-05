package ru.yandex.yandexnavi.guidance_lib_test_app;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.yandex.mapkit.annotations.AnnotationLanguage;
import com.yandex.mapkit.annotations.LocalizedPhrase;
import com.yandex.mapkit.annotations.Speaker;

import java.util.Locale;

public class SpeakerImpl implements Speaker, TextToSpeech.OnInitListener {
    private final Context context;
    private final SayCallback callback;
    private TextToSpeech tts;
    private AnnotationLanguage language;
    private boolean ttsIsInitialized = false;
    private boolean ttsIsAvailable = false;

    private static Locale localeForLanguage(AnnotationLanguage language) {
        switch (language) {
            case RUSSIAN:
                return new Locale("ru", "RU");
            case ENGLISH:
                return new Locale("en", "US");
            case ITALIAN:
                return new Locale("it", "IT");
            case FRENCH:
                return new Locale("fr", "FR");
            case TURKISH:
                return new Locale("tr", "TR");
            case UKRAINIAN:
                return new Locale("uk", "UA");
            case HEBREW:
                return new Locale("he", "IL");
        }
        return null;
    }

    private void setTtsLanguage() {
        if (!ttsIsInitialized) return;

        int result = tts.setLanguage(localeForLanguage(language));
        ttsIsAvailable = (result != TextToSpeech.LANG_MISSING_DATA) &&
                (result != TextToSpeech.LANG_NOT_SUPPORTED);
        if (!ttsIsAvailable) {
            Toast.makeText(context, "Chosen language is not supported by TTS", Toast.LENGTH_LONG).show();
        }
    }

    public interface SayCallback {
        void onSay(String string);
    }

    public SpeakerImpl(Context context, AnnotationLanguage language, @Nullable SayCallback callback) {
        this.context = context;
        this.language = language;
        this.callback = callback;
        this.tts = new TextToSpeech(context, this);
    }

    public void setLanguage(AnnotationLanguage language) {
        this.language = language;
        setTtsLanguage();
    }

    //region LocalizedSpeaker
    @Override
    public void say(LocalizedPhrase phrase) {
        String result = phrase.getText();
        if (callback != null) {
            callback.onSay(result);
        }
        if (ttsIsAvailable) {
            tts.speak(result, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    @Override
    public double duration(LocalizedPhrase phrase) {
        // simplified Russian formula, see navikit/tts/TtsPlayerImpl.java
        return phrase.getText().length() * 0.06 + 0.6;
    }

    @Override
    public void reset() {
        if (ttsIsAvailable) {
            tts.stop();
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            ttsIsInitialized = true;
            setTtsLanguage();
        } else {
            String message = "Failed to initialize tts engine with error " + status;
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
    }
}
