package ru.yandex.market.web;

import android.webkit.CookieManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.emory.mathcs.backport.java.util.Collections;
import ru.yandex.market.BaseTest;
import ru.yandex.market.common.experiments.config.ExperimentConfig;
import ru.yandex.market.utils.CollectionUtils;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;


public class CookieManagerWrapperTest extends BaseTest {

    @Mock
    private CookieManager cookieManagerMock;

    private CookieManagerWrapper wrapper;

    @Test
    public void setExperimentCookie() {

        wrapper.setExperiments(CollectionUtils.toArrayList(ExperimentConfig.builder()
                .setAlias("full-touch")
                .setTestId("42417")
                .setBucketId("35428,0,79")
                .build()));

        String cookieValue = "mxp_touch=full-touch|42417|35428,0,79; HttpOnly";
        for(String domain:CookieManagerWrapper.COOKIE_DOMAINS) {
            verify(cookieManagerMock).setCookie(domain, cookieValue);
        }
    }

    @Test
    public void setNullExperimentCookie() {
        wrapper.setExperiments(Collections.emptyList());
        verifyNoMoreInteractions(cookieManagerMock);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        wrapper = new CookieManagerWrapper(cookieManagerMock);
    }
}