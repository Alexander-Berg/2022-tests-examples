package com.yandex.mail.network;

import android.content.Context;

import com.yandex.mail.fakeserver.FakeServer;
import com.yandex.mail.network.json.response.StatusWrapper;
import com.yandex.mail.retrofit.RetrofitError;
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

import static com.yandex.mail.tools.MockServerTools.createOkStatusWrapper;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(IntegrationTestRunner.class)
public class ApiTaskTest {

    @SuppressWarnings("NullableProblems") // @Before
    @NonNull
    private User user;

    @Before
    public void beforeEachTest() {
        FakeServer.getInstance().createAccountWrapper(Accounts.testLoginData);
        user = User.create(Accounts.testLoginData);
    }

    @Test
    public void callsOnSuccess_onOkStatus() throws Exception {
        ApiTask task = new TestApiTask(RuntimeEnvironment.application, user.getUid()) {
            @Override
            @NonNull
            public StatusWrapper performNetworkOperationRetrofit(@NonNull Context context) throws IOException {
                return createOkStatusWrapper();
            }
        };
        final ApiTask spyTask = spy(task);
        spyTask.sendDataToServer(RuntimeEnvironment.application);
        verify(spyTask).onSuccess(RuntimeEnvironment.application);
    }

    @Test
    public void callsOnFailAndThrows_onRetrofitException() throws Exception {
        final RetrofitError thrownException = RetrofitError.unexpectedError(new RuntimeException());
        ApiTask task = new TestApiTask(RuntimeEnvironment.application, user.getUid()) {
            @Override
            public StatusWrapper performNetworkOperationRetrofit(@NonNull Context context) throws IOException {
                throw thrownException;
            }
        };
        final ApiTask spyTask = spy(task);
        try {
            spyTask.sendDataToServer(RuntimeEnvironment.application);
            failBecauseExceptionWasNotThrown(RetrofitError.class);
        } catch (RetrofitError expected) {
            verify(spyTask).onFail(RuntimeEnvironment.application, thrownException);
        }
    }

    private static abstract class TestApiTask extends ApiTask {
        protected TestApiTask(@NonNull Context context, long uid) throws AccountNotInDBException {
            super(context, uid);
        }

        @Override
        public byte getType() {
            throw new UnsupportedOperationException();
        }
    }
}
