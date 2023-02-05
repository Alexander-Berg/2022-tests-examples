package ru.yandex.market.activity.web;

import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import ru.yandex.market.BaseTest;
import ru.yandex.market.activity.AuthDelegate;
import ru.yandex.market.activity.web.interceptors.LoginInterceptor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AuthInterceptorTest extends BaseTest {

    private LoginInterceptor interceptor;

    @Mock
    private AuthDelegate authDelegate;

    //@formatter:off
    @Test
    public void processTestPassportUri() {
        assertTrue(interceptor.proceed(Uri.parse("https://passport-rc.yandex.ru/passport?mode=login&retpath=https%3A%2F%2Ffull-touch.market-exp-touch.pepelac01ht.yandex.ru%2F%3Floggedin%3D1")));
    }

    @Test
    public void processProdPassportUri() {
        assertTrue(interceptor.proceed(Uri.parse("https://passport.yandex.ru/passport?mode=login&retpath=https%3A%2F%2Ffull-touch.market-exp-touch.pepelac01ht.yandex.ru%2F%3Floggedin%3D1")));
    }

    @Test
    public void processCorruptedPassportUri() {
        assertFalse(interceptor.proceed(Uri.parse("https://passport.yandex.ru?mode=login&retpath=https%3A%2F%2Ffull-touch.market-exp-touch.pepelac01ht.yandex.ru%2F%3Floggedin%3D1")));
        assertFalse(interceptor.proceed(Uri.parse("https://yandex.ru/passport?mode=login&retpath=https%3A%2F%2Ffull-touch.market-exp-touch.pepelac01ht.yandex.ru%2F%3Floggedin%3D1")));
        assertFalse(interceptor.proceed(Uri.parse("https://passport.com?mode=login&redir=\"vah.yandex.ru/passport\"")));
    }
    //@formatter:on

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        interceptor = new LoginInterceptor(authDelegate);
    }
}