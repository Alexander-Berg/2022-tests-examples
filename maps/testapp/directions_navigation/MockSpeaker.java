package com.yandex.maps.testapp.directions_navigation;

import com.yandex.mapkit.annotations.AnnotationLanguage;
import com.yandex.mapkit.annotations.LocalizedPhrase;
import com.yandex.mapkit.annotations.Speaker;
import com.yandex.maps.testapp.Utils;

import android.speech.tts.TextToSpeech;
import android.content.Context;

import java.util.Locale;

public class MockSpeaker implements Speaker, TextToSpeech.OnInitListener {
    private final Context context;
    private final SayCallback callback;
    private TextToSpeech tts;
    private AnnotationLanguage language;
    private boolean ttsIsInitialized = false;
    private boolean ttsIsAvailable = false;

    private static Locale localeForLanguage(AnnotationLanguage language) {
        switch (language) {
            case RUSSIAN   : return new Locale("ru", "RU");
            case ENGLISH   : return new Locale("en", "US");
            case ITALIAN   : return new Locale("it", "IT");
            case FRENCH    : return new Locale("fr", "FR");
            case TURKISH   : return new Locale("tr", "TR");
            case UKRAINIAN : return new Locale("uk", "UA");
            case HEBREW    : return new Locale("he", "IL");
            case SERBIAN   : return new Locale("sr-Latn", "RS");
            case LATVIAN   : return new Locale("lv", "LV");
            case FINNISH   : return new Locale("fi", "FI");
            case ROMANIAN  : return new Locale("ro", "RO");
            case KYRGYZ    : return new Locale("ky", "KG");
            case KAZAKH    : return new Locale("kk", "KZ");
            case LITHUANIAN: return new Locale("lt", "LT");
            case ESTONIAN  : return new Locale("et", "EE");
            case GEORGIAN  : return new Locale("ka", "GE");
            case UZBEK     : return new Locale("uz", "UZ");
            case ARMENIAN  : return new Locale("hy", "AM");
        }
        return null;
    }

    private void setTtsLanguage() {
        if (!ttsIsInitialized)
            return;
        int result = tts.setLanguage(localeForLanguage(language));
        ttsIsAvailable =
            (result != TextToSpeech.LANG_MISSING_DATA) &&
            (result != TextToSpeech.LANG_NOT_SUPPORTED);
        if (!ttsIsAvailable) {
            Utils.showMessage(context, "Chosen language is not supported by TTS");
        }
    }

    public interface SayCallback {
        void onSay(String string);
    }

    public MockSpeaker(Context context, AnnotationLanguage language, SayCallback callback) {
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
            callback.onSay((phrase.getPhraseFlags().getHasToponyms() ? "has toponym: " : "") + (phrase.getPhraseFlags().getHasCustomAnnotations() ? "has custom: " : "") + result);
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
    //endregion

    //region TextToSpeech.OnInitListener
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            ttsIsInitialized = true;
            setTtsLanguage();
        }
    }
    //endregion
};
