package com.yandex.mail.react;

import com.yandex.mail.BaseMailApplication;
import com.yandex.mail.fakeserver.FakeServer;
import com.yandex.mail.model.MessagesModel;
import com.yandex.mail.provider.Constants;
import com.yandex.mail.react.model.MessageBodyLoader;
import com.yandex.mail.react.selection.ReactMailSelection;
import com.yandex.mail.react.selection.SingleMessageSelection;
import com.yandex.mail.react.selection.ThreadSelection;
import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.tools.Accounts;
import com.yandex.mail.tools.User;
import com.yandex.mail.ui.utils.AvatarModel;
import com.yandex.mail.util.BaseIntegrationTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Mockito.mock;

@RunWith(IntegrationTestRunner.class)
public class SelectionProviderTest extends BaseIntegrationTest {

    private User user;

    @Before
    public void beforeEachTest() {
        initBase();

        FakeServer.getInstance().createAccountWrapper(Accounts.testLoginData);

        user = User.create(Accounts.testLoginData);
    }

    @Test
    public void shouldThrowExceptionIfBothThreadIdAndMessageIdAreNotSet() {
        long localUid = user.getUid();
        long localTid = Constants.NO_THREAD_ID;
        long localMid = Constants.NO_MESSAGE_ID;
        int initialLoadCount = 10;

        SelectionProvider selectionProvider = new SelectionProvider(
                mock(BaseMailApplication.class),
                mock(MessagesModel.class),
                mock(AvatarModel.class),
                mock(MessageBodyLoader.class)
        );

        try {
            selectionProvider.provide(localUid, localTid, localMid, initialLoadCount);
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (IllegalStateException expected) {
            assertThat(expected).hasMessage("Message id or thread id has to be set");
        }
    }

    @Test
    public void shouldCreateThreadSelection() {
        long localUid = user.getUid();
        long localTid = 15;
        long localMid = Constants.NO_MESSAGE_ID;
        int initialLoadCount = 10;

        SelectionProvider selectionProvider = new SelectionProvider(
                IntegrationTestRunner.app(),
                mock(MessagesModel.class),
                mock(AvatarModel.class),
                mock(MessageBodyLoader.class)
        );

        ReactMailSelection selection = selectionProvider
                .provide(localUid, localTid, localMid, initialLoadCount);

        assertThat(selection).isInstanceOf(ThreadSelection.class);
    }

    @Test
    public void shouldCreateSingleMessageSelection() {
        long localUid = user.getUid();
        long localTid = Constants.NO_THREAD_ID;
        long localMid = 8;
        int initialLoadCount = 10;

        SelectionProvider selectionProvider = new SelectionProvider(
                IntegrationTestRunner.app(),
                mock(MessagesModel.class),
                mock(AvatarModel.class),
                mock(MessageBodyLoader.class)
        );

        ReactMailSelection selection = selectionProvider
                .provide(localUid, localTid, localMid, initialLoadCount);

        assertThat(selection).isInstanceOf(SingleMessageSelection.class);
    }
}