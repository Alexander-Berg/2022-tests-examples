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
import ru.yandex.general.module.GeneralProxyWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.EventSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static ru.yandex.general.consts.Events.SNIPPET_SHOW;
import static ru.yandex.general.consts.GeneralFeatures.EVENTS_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.EVENT_SNIPPET_SHOW;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockSearch.listingCategoryResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;
import static ru.yandex.general.step.BasePageSteps.LIST;

@Epic(EVENTS_FEATURE)
@Feature(EVENT_SNIPPET_SHOW)
@DisplayName("Отправка события «snippetShow», проверка количества, с листинга категории, листинг списком")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralProxyWebModule.class)
public class ListListingSnippetShowCountSendEventTest {

    private static final String REGION_ID = "213";
    private static final int SNIPPETS_COUNT = 11;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private EventSteps eventSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        urlSteps.testing().path(ELEKTRONIKA);

        mockRule.graphqlStub(mockResponse().setSearch(listingCategoryResponse().addOffers(SNIPPETS_COUNT).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate().build()).withDefaults().create();

        basePageSteps.setCookie(CLASSIFIED_REGION_ID, REGION_ID);
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, LIST);

        basePageSteps.resize(1920, 1080);

        basePageSteps.waitSomething(2, TimeUnit.SECONDS);
        eventSteps.clearHar();
        urlSteps.open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Количество «snippetShow» с листинга категории, листинг списком")
    public void shouldSeeSnippetShowEventsCountFromHomePage() {
        basePageSteps.scrollingToElement(basePageSteps.onListingPage().footer());

        eventSteps.withEventType(SNIPPET_SHOW).withEventsCount(SNIPPETS_COUNT).shouldExist();

    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Индексы событий «snippetShow» уникальны и в верном диапазоне с листинга категории, листинг списком")
    public void shouldSeeSnippetShowIndexesFromHomePage() {
        basePageSteps.scrollingToElement(basePageSteps.onListingPage().footer());

        eventSteps.withEventType(SNIPPET_SHOW).snippetShowIndexesUniqAndInRange(SNIPPETS_COUNT).shouldExist();
    }

}
