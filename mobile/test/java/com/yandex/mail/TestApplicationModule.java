package com.yandex.mail;

import com.google.gson.GsonBuilder;
import com.yandex.mail.account.MailProvider;
import com.yandex.mail.am.MockPassportApi;
import com.yandex.mail.auth.AuthToken;
import com.yandex.mail.di.AccountComponent;
import com.yandex.mail.di.AccountComponentProvider;
import com.yandex.mail.di.AccountModule;
import com.yandex.mail.di.ApplicationModule;
import com.yandex.mail.entity.AccountEntity;
import com.yandex.mail.entity.AccountType;
import com.yandex.mail.experiments.XmailSync;
import com.yandex.mail.metrica.YandexMailMetrica;
import com.yandex.mail.model.AccountModel;
import com.yandex.mail.model.DeveloperSettingsModel;
import com.yandex.mail.network.MailApi;
import com.yandex.mail.network.RetrofitMailApi;
import com.yandex.mail.network.RetrofitMailApiV2;
import com.yandex.mail.network.request.Requests;
import com.yandex.mail.network.response.BodyTypeAdapterFactory;
import com.yandex.mail.network.response.RetrofitComposeApi;
import com.yandex.mail.service.work.DataManagingExecutor;
import com.yandex.mail.service.work.TestDataManagingExecutor;
import com.yandex.mail.settings.AccountSettings;
import com.yandex.mail.util.AccountNotInDBException;
import com.yandex.mail.voice_control.MockSpeechKitFactory;
import com.yandex.mail.voice_control.SpeechKitFactory;
import com.yandex.passport.api.PassportApi;

import javax.inject.Named;
import javax.inject.Singleton;

import androidx.annotation.NonNull;
import dagger.Provides;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import okhttp3.HttpUrl;

import static org.mockito.Mockito.spy;

public class TestApplicationModule extends ApplicationModule {

    @NonNull
    @Override
    public PassportApi provideApi(@NonNull BaseMailApplication context, @NonNull DeveloperSettingsModel developerSettingsModel) {
        return spy(new MockPassportApi());
    }

    @Override
    @NonNull
    public AccountComponentProvider provideAccountComponentProvider(@NonNull BaseMailApplication application, @NonNull AccountModel accountModel) {
        return new AccountComponentProvider(application, accountModel) {

            @Override
            @NonNull
            protected AccountComponent createNewData(long uid) {
                final AccountEntity accountEntity = accountModel.getAccountByUidSingle(uid).blockingGet().orElse(null);
                if (accountEntity == null) {
                    throw new AccountNotInDBException(uid);
                }
                String typeString = accountEntity.getYandexAccountTypeString();
                AccountType type = AccountType.fromStringType(typeString);
                MailProvider mailProvider = MailProvider.unknownIfMailish(type == AccountType.MAILISH);
                return application.getApplicationComponent()
                        .plus(new AccountModule(uid, type, mailProvider, accountEntity.isYandexoid(), accountEntity.isPdd()) {

                    @Override
                    @NonNull
                    protected GsonBuilder initGson(@NonNull BodyTypeAdapterFactory bodyTypeAdapterFactory) {
                        return super.initGson(bodyTypeAdapterFactory)
                                .registerTypeAdapterFactory(new Requests.RequestsTypeAdapterFactory());
                    }

                    @Override
                    @NonNull
                    public MailApi provideMailApi(
                            @NonNull BaseMailApplication context,
                            @NonNull RetrofitMailApi retrofitMailApi,
                            @NonNull RetrofitMailApiV2 retrofitMailApiV2,
                            @NonNull RetrofitComposeApi composeApi,
                            @NonNull Single<AuthToken> tokenProvider,
                            @Named(API_HOST) @NonNull HttpUrl host,
                            @NonNull YandexMailMetrica metrica,
                            @NonNull AccountSettings accountSettings
                    ) {
                        return spy(super.provideMailApi(
                                context,
                                retrofitMailApi,
                                retrofitMailApiV2,
                                composeApi,
                                tokenProvider,
                                host,
                                metrica,
                                accountSettings
                        ));
                    }
                });
            }
        };
    }

    @Provides
    @Singleton
    @Named(COMMON_XMAIL_EXPERIMENT)
    @NonNull
    public XmailSync provideXmailSyncExperiment() {
        return XmailSync.DISABLED;
    }

    @Provides
    @Singleton
    @NonNull
    public SpeechKitFactory provideSpeechKitFactory() {
        return new MockSpeechKitFactory();
    }

    @NonNull
    @Override
    public DataManagingExecutor provideDataManagingExecutor(
            @NonNull BaseMailApplication context,
            @NonNull YandexMailMetrica metrica
    ) {
        return new TestDataManagingExecutor(context, metrica, Schedulers.trampoline());
    }
}
