package com.yandex.mail.provider;

import com.yandex.mail.fakeserver.AccountWrapper;
import com.yandex.mail.fakeserver.FakeServer;
import com.yandex.mail.network.response.AbookSuggestJson;
import com.yandex.mail.network.response.AbookSuggestJson.SuggestContact;
import com.yandex.mail.provider.suggestion.AbookQueryTask;
import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.tools.Accounts;
import com.yandex.mail.tools.User;
import com.yandex.mail.util.AccountNotInDBException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;

import androidx.annotation.NonNull;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(IntegrationTestRunner.class)
public class AbookQueryTaskTest {

    @SuppressWarnings("NullableProblems")
    @NonNull
    private User user;

    @SuppressWarnings("NullableProblems")
    @NonNull
    private AccountWrapper account;

    @Before
    public void beforeEachMethod() {
        account = FakeServer.getInstance().createAccountWrapper(Accounts.testLoginData);
        user = User.create(Accounts.testLoginData);
    }

    @Test
    public void should_querySuggestions() throws AccountNotInDBException, IOException {
        account.addContacts(
                account.newContact("melnikov@ya.ru", "Sergey", "Melnikov").build(),
                account.newContact("tralalal@mail.ru", "Alalal", "Tralalal").build(),
                account.newContact("whatever@ya.ru", "Sergey", "Whatever").build()
        );

        AbookQueryTask task = new AbookQueryTask(
                RuntimeEnvironment.application,
                user.getUid(),
                "Sergey"
        );

        task.sendDataToServer(RuntimeEnvironment.application);

        AbookSuggestJson result = task.getResult();
        //noinspection ConstantConditions
        SuggestContact[] suggestions = result.getSuggestedContactsOrThrow();
        assertThat(suggestions).hasSize(2);
    }

}
