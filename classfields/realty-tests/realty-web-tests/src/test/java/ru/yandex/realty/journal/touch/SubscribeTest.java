package ru.yandex.realty.journal.touch;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebWithProxyModule;
import ru.yandex.realty.step.GoalsSteps;
import ru.yandex.realty.step.ProxySteps;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.format;
import static net.lightbody.bmp.proxy.CaptureType.getAllContentCaptureTypes;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.JOURNAL;
import static ru.yandex.realty.consts.Pages.POST_PAGE;
import static ru.yandex.realty.consts.RealtyFeatures.JOURNAL_FEATURE;
import static ru.yandex.realty.step.GoalsSteps.RESPONSE_STATUS_OK;

@Link("https://st.yandex-team.ru/VERTISTEST-1621")
@Feature(JOURNAL_FEATURE)
@DisplayName("Журнал. Тач")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithProxyModule.class)
public class SubscribeTest {

    private static final String REQUEST_PATTERN = "{\"email\":\"%s\"}";
    private static final String SUBSCRIBE_REQUEST_URL = "/journal/gate/maillist/subscribe/";
    private static final String BAD_REQUEST_RESPONSE = "{\"error\":{\"code\":\"BAD_REQUEST\",\"message\":\"Bad " +
            "request (maillist.subscribe.email Format validation failed (value is not a valid e-mail address))\"}}";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ProxySteps proxy;

    @Inject
    private GoalsSteps goalsSteps;

    @Before
    public void before() {
        proxy.getProxyServerManager().getServer().setHarCaptureTypes(getAllContentCaptureTypes());
        urlSteps.testing().path(JOURNAL).path(POST_PAGE).path("/prosrochka-v-novostroyke-chto-delat/").open();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Успешная подписка")
    public void shouldSeeSuccessSubscribe() {
        String email = getRandomEmail();
        basePageSteps.onJournalPage().input("Ваша почта").sendKeys(email);
        proxy.clearHarUntilThereAreNoHarEntries();
        basePageSteps.onJournalPage().button("Подписаться").click();
        basePageSteps.onJournalPage().subscribeStatus().should(hasText("Вы подписались"));
        String requestParams = format(REQUEST_PATTERN, email);
        goalsSteps.urlMatcher(StringContains.containsString(SUBSCRIBE_REQUEST_URL))
                .withParamsAndResponse(requestParams, RESPONSE_STATUS_OK).shouldExist();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Неуспешная подписка")
    public void shouldSeeNotSuccessSubscribe() {
        String email = "ололо@yandex.ru";
        basePageSteps.onJournalPage().input("Ваша почта").sendKeys(email);
        proxy.clearHarUntilThereAreNoHarEntries();
        basePageSteps.onJournalPage().button("Подписаться").click();
        basePageSteps.onJournalPage().subscribeStatus().should(hasText("Увы, что-то пошло не так, попробуйте позже"));
        String requestParams = format(REQUEST_PATTERN, email);
        goalsSteps.urlMatcher(StringContains.containsString(SUBSCRIBE_REQUEST_URL))
                .withParamsAndResponse(requestParams, BAD_REQUEST_RESPONSE).shouldExist();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Ссылка пользовательского соглашения")
    public void shouldSeeTermsOfUse() {
        basePageSteps.onJournalPage().link("Пользовательского соглашения").click();
        basePageSteps.waitUntilSeeTabsCount(2);
        basePageSteps.switchToNextTab();
        urlSteps.fromUri("https://yandex.ru/legal/realty_termsofuse/").shouldNotDiffWithWebDriverUrl();
    }
}
