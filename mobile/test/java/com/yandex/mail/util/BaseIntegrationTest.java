package com.yandex.mail.util;

import com.yandex.mail.BaseMailApplication;
import com.yandex.mail.LoginData;
import com.yandex.mail.TestMailApplication;
import com.yandex.mail.di.AccountComponent;
import com.yandex.mail.di.ApplicationComponent;
import com.yandex.mail.entity.FidWithCounters;
import com.yandex.mail.fakeserver.AccountWrapper;
import com.yandex.mail.fakeserver.FakeServer;
import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.tools.LocalHelper;
import com.yandex.mail.tools.ServerHelper;
import com.yandex.mail.tools.TestWorkerFactory;
import com.yandex.mail.tools.User;

import org.assertj.core.api.Condition;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;

import static android.os.Looper.getMainLooper;
import static com.yandex.mail.BaseMailApplication.getApplicationComponent;
import static org.mockito.Mockito.spy;
import static org.robolectric.Shadows.shadowOf;

public class BaseIntegrationTest extends ModelsHolder implements ServerHelper, LocalHelper {

    @SuppressWarnings("NullableProblems") // init()
    @NonNull
    public TestMailApplication app = spy(IntegrationTestRunner.app());

    @SuppressWarnings("NullableProblems") // init()
    @NonNull
    public AccountWrapper account;

    @SuppressWarnings("NullableProblems") // init()
    @NonNull
    public User user;

    @SuppressWarnings("NullableProblems") // init()
    @NonNull
    private Map<LoginData, AccountWrapper> accounts = new HashMap<>();

    @SuppressWarnings("NullableProblems") // init()
    @NonNull
    protected Map<LoginData, User> users = new HashMap<>();

    @SuppressWarnings("NullableProblems") // init()
    @NonNull
    protected ApplicationComponent applicationComponent;

    @SuppressWarnings("NullableProblems") // init()
    @NonNull
    protected AccountComponent accountComponent;

    @SuppressWarnings("NullableProblems") // init()
    @NonNull
    protected TestWorkerFactory workerFactory;

    protected void initBase() {
        applicationComponent = getApplicationComponent(IntegrationTestRunner.app());
        initApplicationModels(applicationComponent);
        workerFactory = TestMailApplication.workerFactory;
        shadowOf(getMainLooper()).idle();
    }

    protected void init(@NonNull LoginData loginData, boolean doInitialLoad, boolean enableTabs) {
        initBase();
        createServerAndUser(loginData, true, enableTabs);
        selectAccount(loginData, doInitialLoad, enableTabs);
        shadowOf(getMainLooper()).idle();
    }

    protected void init(@NonNull LoginData loginData, boolean doInitialLoad) {
        init(loginData, doInitialLoad, false);
    }

    public void init(@NonNull LoginData loginData) {
        init(loginData, true, false);
    }

    public void initMultiple(@NonNull LoginData selectedAccount, @NonNull LoginData... others) {
        init(selectedAccount, true, false);
        for (LoginData data : others) {
            createServerAndUser(data, false, false);
        }
    }

    protected void createServerAndUser(@NonNull LoginData loginData, boolean active) {
        createServerAndUser(loginData, active, false);
    }

    protected void createServerAndUser(@NonNull LoginData loginData, boolean active, boolean enabledTabs) {
        accounts.put(loginData, FakeServer.getInstance().createAccountWrapper(loginData, enabledTabs));
        if (!users.containsKey(loginData)) {
            users.put(loginData, active ? User.create(loginData) : User.createInactive(loginData));
        }
    }

    public void selectAccount(@NonNull LoginData data, boolean doInitialLoad) {
        selectAccount(data, doInitialLoad, true);
    }

    public void selectAccount(@NonNull LoginData data, boolean doInitialLoad, boolean enableTabs) {
        account = accounts.get(data);
        user = users.get(data);

        if (account == null || user == null) {
            throw new IllegalStateException("Account isn't created: " + data);
        }

        applicationComponent = getApplicationComponent(IntegrationTestRunner.app());
        applicationComponent.accountModel().selectAccount(data.uid);

        accountComponent = BaseMailApplication.getAccountComponent(IntegrationTestRunner.app(), user.getUid());

        initAccountModels(accountComponent, enableTabs);

        if (doInitialLoad) {
            user.initialLoad();
        }
    }

    public void initFromMailbox(@NonNull AccountWrapper accountWrapper, @NonNull User user) {
        this.account = accountWrapper;
        this.user = user;

        applicationComponent = getApplicationComponent(IntegrationTestRunner.app());
        accountComponent = BaseMailApplication.getAccountComponent(IntegrationTestRunner.app(), user.getUid());

        initModels(applicationComponent, accountComponent);
    }

    @NonNull
    protected FidWithCounters getTotalCounters(long fid) {
        return foldersModel.observeCounters()
                .map(longFidWithCountersMap -> longFidWithCountersMap.get(fid))
                .blockingFirst();
    }

    @NonNull
    protected static Condition<FidWithCounters> haveTotalCounters(int total, int unread) {
        return new Condition<FidWithCounters>() {

            @Override
            public boolean matches(@NonNull FidWithCounters totalCounters) {
                return (totalCounters.getTotal_counter() == total) && (totalCounters.getUnread_counter() == unread);
            }

            @Override
            public String toString() {
                return " have total counters :"
                       + "unread_counter=" + unread + ", "
                       + "total_counter=" + total
                        ;
            }
        };
    }

    @Override
    @NonNull
    public User get(@NonNull LocalHelper dummy) {
        return user;
    }

    @NonNull
    @Override
    public AccountWrapper get(@NonNull ServerHelper dummy) {
        return account;
    }

    protected final void runInTransaction(@NonNull Runnable block) {
        transacter.transaction(false, transactionWithoutReturn -> {
            block.run();
            return Unit.INSTANCE;
        });
    }

    protected void setAccountsEnabled(@NonNull Map<Long, Boolean> enabledMap) {
        final Collection<Long> ids = enabledMap.keySet();
        final Collection<Long> enabled = CollectionsKt.filter(ids, enabledMap::get);

        applicationComponent
                .accountDbModel()
                .setAccountsEnabled(enabled, ids);

        for (Long uid : ids) {
            @SuppressWarnings("ConstantConditions") //nonnull as ids is keyset of enabledMap
            final boolean isEnabled = enabledMap.get(uid);
            widgetsModel.showWidget(uid, isEnabled);
            if (!isEnabled) {
                accountModel.disableCalendarIfNeeded(uid);
            }
        }
    }
}
