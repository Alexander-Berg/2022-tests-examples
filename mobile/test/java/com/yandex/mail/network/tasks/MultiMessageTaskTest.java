package com.yandex.mail.network.tasks;

import android.content.Context;

import com.yandex.mail.fakeserver.FakeServer;
import com.yandex.mail.network.json.response.StatusWrapper;
import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.tools.Accounts;
import com.yandex.mail.tools.User;
import com.yandex.mail.util.AccountNotInDBException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;

import androidx.annotation.NonNull;

import static com.yandex.mail.tools.MockServerTools.createOkStatusWrapper;
import static java.util.Collections.emptyList;

@RunWith(IntegrationTestRunner.class)
public class MultiMessageTaskTest {

    @SuppressWarnings("NullableProblems") // initialized in @Before
    @NonNull
    private User user;

    @Before
    public void setup() throws Exception {
        FakeServer.getInstance().createAccountWrapper(Accounts.testLoginData);

        user = User.create(Accounts.testLoginData);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowOnEmptyMidsList() throws AccountNotInDBException {
        new TestMultiMessageTask(IntegrationTestRunner.app(), emptyList(), user.getUid());
    }

    private static class TestMultiMessageTask extends MultiMessageTask {

        public TestMultiMessageTask(@NonNull Context context, @NonNull List<Long> localMids, long uid) throws AccountNotInDBException {
            super(context, localMids, uid);
        }

        @Override
        @NonNull
        public StatusWrapper performNetworkOperationRetrofit(@NonNull Context context) throws IOException {
            return createOkStatusWrapper();
        }

        @Override
        public byte getType() {
            return 0;
        }
    }
}
