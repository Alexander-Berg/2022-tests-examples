package ru.yandex.general.events;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralProxyMobileWebModule;
import ru.yandex.general.step.EventSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static ru.yandex.general.consts.Events.SEARCH;
import static ru.yandex.general.consts.Events.SNIPPET_SHOW;
import static ru.yandex.general.consts.GeneralFeatures.EVENTS_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.EVENT_SNIPPET_SHOW;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.MOSKVA;

@Epic(EVENTS_FEATURE)
@Feature(EVENT_SNIPPET_SHOW)
@DisplayName("Отправка «snippetShow», queryId не меняется при возвращении с ЛК")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class SearchQueryIdNotChangeFromLkTest {

    private String queryId;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private EventSteps eventSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        basePageSteps.waitSomething(3, TimeUnit.SECONDS);
        eventSteps.clearHar();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("QueryId не меняется при возвращении на главную с ЛК")
    public void shouldSeeQueryIdByBackToHomepageFromLk() {
        urlSteps.testing().path(MOSKVA).open();
        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).shouldExist();
        queryId = eventSteps.getQueryId();

        basePageSteps.onListingPage().tabBar().myOffers().click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        eventSteps.clearHar();
        basePageSteps.back();
        basePageSteps.scrolling(1500, 30);

        eventSteps.withEventType(SNIPPET_SHOW).withEventsCount(greaterThan(5))
                .queryIdMatcher(equalTo(queryId)).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("QueryId не меняется при возвращении на листинг категории с ЛК")
    public void shouldSeeQueryIdByBackToCategoryListingFromLk() {
        urlSteps.testing().path(MOSKVA).path(ELEKTRONIKA).open();
        eventSteps.withEventType(SEARCH).withEventsCount(greaterThan(0)).shouldExist();
        queryId = eventSteps.getQueryId();

        basePageSteps.onListingPage().tabBar().myOffers().click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        eventSteps.clearHar();
        basePageSteps.back();
        basePageSteps.scrolling(1500, 30);

        eventSteps.withEventType(SNIPPET_SHOW).withEventsCount(greaterThan(5))
                .queryIdMatcher(equalTo(queryId)).shouldExist();
    }

}
