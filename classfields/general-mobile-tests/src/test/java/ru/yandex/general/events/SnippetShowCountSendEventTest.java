package ru.yandex.general.events;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.mock.MockResponse;
import ru.yandex.general.module.GeneralProxyMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.EventSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.Events.SNIPPET_SHOW;
import static ru.yandex.general.consts.GeneralFeatures.EVENTS_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.EVENT_SNIPPET_SHOW;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.SLASH;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_USER_HAS_SEEN_PROFILE;
import static ru.yandex.general.mobile.step.BasePageSteps.GRID;
import static ru.yandex.general.mobile.step.BasePageSteps.TRUE;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockHomepage.homepageResponse;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockSearch.listingCategoryResponse;
import static ru.yandex.general.step.BasePageSteps.CLASSIFIED_REGION_ID;

@Epic(EVENTS_FEATURE)
@Feature(EVENT_SNIPPET_SHOW)
@DisplayName("Отправка события «snippetShow», проверка количества")
@RunWith(Parameterized.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SnippetShowCountSendEventTest {

    private static final String REGION_ID = "213";
    private static final int SNIPPETS_COUNT = 10;
    private static final String CARD_ID = "1111111";

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

    @Parameterized.Parameter
    public String title;

    @Parameterized.Parameter(1)
    public String path;

    @Parameterized.Parameter(2)
    public MockResponse mockResponse;

    @Parameterized.Parameters(name = "{index}. {0}")
    public static Collection<Object[]> getTestParameters() {
        return asList(new Object[][]{
                {"Отправка события с главной", SLASH,
                        mockResponse().setHomepage(homepageResponse().addOffers(SNIPPETS_COUNT).build())
                },
                {"Отправка события с листинга категории", ELEKTRONIKA,
                        mockResponse().setSearch(listingCategoryResponse().addOffers(SNIPPETS_COUNT).build()),
                },
                {"Отправка события с листинга похожих офферов", CARD + CARD_ID + SLASH,
                        mockResponse().setCard(mockCard(BASIC_CARD).setId(CARD_ID)
                                .addSimilarOffers(SNIPPETS_COUNT).build())
                }
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(path);

        mockRule.graphqlStub(mockResponse
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate().build()).withDefaults().create();

        basePageSteps.setCookie(CLASSIFIED_REGION_ID, REGION_ID);
        basePageSteps.setCookie(CLASSIFIED_USER_HAS_SEEN_PROFILE, TRUE);
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, GRID);

        basePageSteps.waitSomething(2, TimeUnit.SECONDS);
        eventSteps.clearHar();
        urlSteps.open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Количество «snippetShow» с разных типов листингов")
    public void shouldSeeSnippetShowEventsCount() {
        basePageSteps.scrollingToElement(basePageSteps.onListingPage().footer());

        eventSteps.withEventType(SNIPPET_SHOW).withEventsCount(SNIPPETS_COUNT).shouldExist();

    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Индексы событий «snippetShow» уникальны и в верном диапазоне с разных типов листингов")
    public void shouldSeeSnippetShowIndexes() {
        basePageSteps.slowScrolling(basePageSteps.getMaxPageHeight());

        eventSteps.withEventType(SNIPPET_SHOW).snippetShowIndexesUniqAndInRange(SNIPPETS_COUNT).shouldExist();
    }

}
