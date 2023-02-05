package com.yandex.mail.pin;

import com.yandex.mail.R;
import com.yandex.mail.fakeserver.FakeServer;
import com.yandex.mail.runners.IntegrationTestRunner;
import com.yandex.mail.tools.Accounts;
import com.yandex.mail.tools.User;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowLooper;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(IntegrationTestRunner.class)
public class AddOrChangePinFragmentTest {

    private User user;
    private ActivityController<AddOrChangePinActivity> controller;

    @Before
    public void setup() throws Exception {
        FakeServer.getInstance().createAccountWrapper(Accounts.testLoginData);

        user = User.create(Accounts.testLoginData);
        controller = Robolectric.buildActivity(AddOrChangePinActivity.class);
    }

    @Test
    public void testPinEnter() {
        controller.create().start().resume().visible();
        AddOrChangePinFragment fragment = (AddOrChangePinFragment) controller
                .get().getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        MockPinCodeModule.pinCode = "";

        assertThat(fragment.isSecondTry).isEqualTo(false);
        assertThat(fragment.firstTryPin).isNull();
        assertThat(fragment.viewBinding.pinView.getCurrentPinLength()).isEqualTo(-1);

        fragment.keyboardBinding.getRoot().findViewById(R.id.btn_one).performClick();
        fragment.keyboardBinding.getRoot().findViewById(R.id.btn_two).performClick();
        fragment.keyboardBinding.getRoot().findViewById(R.id.btn_nine).performClick();
        fragment.keyboardBinding.getRoot().findViewById(R.id.btn_backspace).performClick();
        fragment.keyboardBinding.getRoot().findViewById(R.id.btn_three).performClick();
        fragment.keyboardBinding.getRoot().findViewById(R.id.btn_four).performClick();

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertThat(fragment.isSecondTry).isEqualTo(true);
        assertThat(fragment.firstTryPin.getPinCode()).isEqualTo("1234");
        assertThat(fragment.viewBinding.pinView.getCurrentPinLength()).isEqualTo(-1);

        fragment.keyboardBinding.getRoot().findViewById(R.id.btn_one).performClick();
        fragment.keyboardBinding.getRoot().findViewById(R.id.btn_two).performClick();
        fragment.keyboardBinding.getRoot().findViewById(R.id.btn_three).performClick();
        fragment.keyboardBinding.getRoot().findViewById(R.id.btn_four).performClick();

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertThat(MockPinCodeModule.pinCode).isEqualTo("1234");
    }
}
