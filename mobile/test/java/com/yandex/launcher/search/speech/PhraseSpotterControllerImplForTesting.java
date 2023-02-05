package com.yandex.launcher.search.speech;

import android.content.Context;
import androidx.annotation.NonNull;
import com.yandex.launcher.TestProvider;
//import com.yandex.launcher.speech.PhraseSpotterControllerImpl;
//import com.yandex.launcher.speech.PhraseSpotterStarter;
//import com.yandex.launcher.speech.VoiceActivationDelegate;
import ru.yandex.speechkit.PhraseSpotter;

//TODO: PHONE-2835
public class PhraseSpotterControllerImplForTesting /*extends PhraseSpotterControllerImpl*/ {

    public static TestProvider<PhraseSpotter> mMockedSpotterProvider;
//    public static TestProvider<PhraseSpotterStarter> mMockedSpotterStarterProvider;

//    public PhraseSpotterControllerImplForTesting(@NonNull Context context, @NonNull VoiceActivationDelegate voiceActivationDelegate, @NonNull SpotterEnabledStateProvider spotterEnabledProvider) {
//        super(context, voiceActivationDelegate, spotterEnabledProvider);
//    }

//    @NonNull
//    @Override
//    public PhraseSpotter createPhraseSpotter() {
//        return mMockedSpotterProvider.get();
//    }

//    @NonNull
//    @Override
//    public PhraseSpotterStarter createPhraseSpotterStarter(@NonNull PhraseSpotter spotter) {
//        return mMockedSpotterStarterProvider.get();
//    }
}
