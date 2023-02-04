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
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.EventSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.Events.SNIPPET_SHOW;
import static ru.yandex.general.consts.GeneralFeatures.EVENTS_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.EVENT_SNIPPET_SHOW;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.FAVORITES;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.mock.MockFavorites.favoritesResponse;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;

@Epic(EVENTS_FEATURE)
@Feature(EVENT_SNIPPET_SHOW)
@DisplayName("Отправка события «snippetShow» со страницы избранных офферов, проверка количества")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
public class SnippetShowFavoritesCountSendEventTest {

    private static final String REGION_ID = "213";
    private static final int SNIPPETS_COUNT = 21;

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

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        passportSteps.commonAccountLogin();

        mockRule.graphqlStub(mockResponse().setFavorites(favoritesResponse().addOffers(SNIPPETS_COUNT).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();

        basePageSteps.setCookie(CLASSIFIED_REGION_ID, REGION_ID);

        eventSteps.clearHar();
        urlSteps.testing().path(MY).path(FAVORITES).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Количество «snippetShow» со страницы избранных офферов")
    public void shouldSeeSnippetShowEventsCountFromFavorites() {
        basePageSteps.scrollingToElement(basePageSteps.onFavoritesPage().footer());

        eventSteps.withEventType(SNIPPET_SHOW).withEventsCount(SNIPPETS_COUNT).shouldExist();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Индексы событий «snippetShow» уникальны и в верном диапазоне на избранных офферах")
    public void shouldSeeSnippetShowIndexesFromFavorites() {
        basePageSteps.scrollingToElement(basePageSteps.onFavoritesPage().footer());

        eventSteps.withEventType(SNIPPET_SHOW).snippetShowIndexesUniqAndInRange(SNIPPETS_COUNT).shouldExist();
    }

}
