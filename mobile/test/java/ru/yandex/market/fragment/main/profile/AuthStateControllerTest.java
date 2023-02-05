package ru.yandex.market.fragment.main.profile;

import android.os.Build;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import io.reactivex.Completable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import ru.yandex.market.clean.domain.usecase.auth.AuthenticationUseCase;
import ru.yandex.market.common.schedulers.MainScheduler;
import ru.yandex.market.domain.auth.usecase.CheckIsLoggedInUseCase;
import ru.yandex.market.util.AuthStateReceiver;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.P})
public class AuthStateControllerTest {

    @NonNull
    private final MainScheduler mainScheduler = new MainScheduler(Schedulers.trampoline());

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private AuthenticationUseCase authenticationUseCase;

    @Mock
    private CheckIsLoggedInUseCase checkIsLoggedInUseCase;

    private AuthStateController authStateController;

    @Before
    public void setUp() {
        authStateController = new AuthStateController(
                ApplicationProvider.getApplicationContext(),
                authenticationUseCase,
                checkIsLoggedInUseCase,
                mainScheduler
        );
    }

    @Test
    public void testLogout() {
        when(authenticationUseCase.logout()).thenReturn(Completable.complete());
        authStateController.logout();
        verify(authenticationUseCase).logout();
    }

    @Test
    public void testGetCurrentState() {
        doReturn(Single.just(false)).when(checkIsLoggedInUseCase).isLoggedIn();
        authStateController.getCurrentState()
                .test()
                .assertValue(AuthStateController.State.LOGGED_OUT);

        doReturn(Single.just(true)).when(checkIsLoggedInUseCase).isLoggedIn();
        authStateController.getCurrentState()
                .test()
                .assertValue(AuthStateController.State.LOGGED_IN);
    }

    @Test
    public void testAuthStateChange() {

        final TestObserver<AuthStateController.State> observer =
                authStateController.getAuthStateChangeObservable().test();

        AuthStateReceiver.sendLogin(ApplicationProvider.getApplicationContext());
        AuthStateReceiver.sendLogout(ApplicationProvider.getApplicationContext());
        AuthStateReceiver.sendLogin(ApplicationProvider.getApplicationContext());
        AuthStateReceiver.sendLogout(ApplicationProvider.getApplicationContext());
        AuthStateReceiver.sendLogin(ApplicationProvider.getApplicationContext());

        observer.assertValues(
                AuthStateController.State.LOGGED_IN,
                AuthStateController.State.LOGGED_OUT,
                AuthStateController.State.LOGGED_IN,
                AuthStateController.State.LOGGED_OUT,
                AuthStateController.State.LOGGED_IN
        )
                .assertNoErrors();
    }
}
