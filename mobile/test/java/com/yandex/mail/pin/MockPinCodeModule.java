package com.yandex.mail.pin;

import com.yandex.mail.model.AccountModel;
import com.yandex.mail.settings.GeneralSettings;
import com.yandex.passport.api.PassportApi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dagger.Lazy;
import io.reactivex.Completable;

public class MockPinCodeModule extends PinCodeModule {

    @Nullable
    public static String pinCode;

    @Override
    @NonNull
    public PinCodeModel providePinCodeModel(
            @NonNull PassportApi passportApi,
            @NonNull AccountModel accountModel,
            @NonNull Lazy<GeneralSettings> generalSettings
    ) {
        return new PinCodeModelImpl(passportApi, accountModel, generalSettings) {

            @Override
            @NonNull
            public Completable addPin(@NonNull PinCode pin) {
                pinCode = pin.getPinCode();
                return super.addPin(pin);
            }
        };
    }
}
