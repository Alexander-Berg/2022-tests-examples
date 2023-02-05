package ru.yandex.autotests.mobile.disk.android.core.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.yandex.disk.rest.OkHttpClientFactory;
import okhttp3.OkHttpClient;
import ru.yandex.autotests.mobile.disk.android.core.accounts.Account;
import ru.yandex.autotests.mobile.disk.android.core.api.*;
import ru.yandex.autotests.mobile.disk.android.core.auth.OauthService;

import static ru.yandex.autotests.mobile.disk.data.AccountConstants.TEST_USER;

public class TestUserMobDiskApiProvider implements Provider<DiskApi> {

    @Inject
    private Environment environment;

    @Inject
    @Named(TEST_USER)
    private Account account;

    @Override
    public DiskApi get() {
        return new DiskApi(buildRestApiClient(), account);
    }

    public RestApiClient buildRestApiClient() {
        OkHttpClient.Builder builder = makeClient();
        return new RestApiClient(
                new Credentials(account.getLogin(),
                        OauthService.oauthToken(account, environment.getOauthApp())),
                builder,
                environment.getBackendUrl()
        );
    }

    private static OkHttpClient.Builder makeClient() {
        return ClientUpdater.updateClient(OkHttpClientFactory.makeClient());
    }
}
