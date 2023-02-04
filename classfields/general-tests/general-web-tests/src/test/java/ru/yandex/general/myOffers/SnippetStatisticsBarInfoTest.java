package ru.yandex.general.myOffers;

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
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.yandex.general.beans.card.Statistics.statistics;
import static ru.yandex.general.beans.card.StatisticsGraphItem.statisticsGraphItem;
import static ru.yandex.general.consts.GeneralFeatures.MY_OFFERS_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.STATISTICS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.mock.MockCabinetListing.cabinetListingResponse;
import static ru.yandex.general.mock.MockCabinetSnippet.BASIC_SNIPPET;
import static ru.yandex.general.mock.MockCabinetSnippet.mockSnippet;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.utils.Utils.formatDate;
import static ru.yandex.general.utils.Utils.getCurrentDate;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(MY_OFFERS_FEATURE)
@Feature(STATISTICS)
@DisplayName("Попап подробной инфы по дню, статистика на сниппете моего оффера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class SnippetStatisticsBarInfoTest {

    private static final int VIEWS_COUNT = 1235;
    private static final int CONTACTS_COUNT = 9;
    private static final int FAVORITES_COUNT = 121;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        mockRule.graphqlStub(mockResponse()
                .setCabinetListing(cabinetListingResponse().offers(asList(
                        mockSnippet(BASIC_SNIPPET).setStatisticsGraph(asList(
                                statisticsGraphItem().setDate(formatDate(getCurrentDate())).setHighlighted(false)
                                        .setStatistics(statistics()
                                                .setViewsCount(VIEWS_COUNT)
                                                .setFavoritesCount(FAVORITES_COUNT)
                                                .setContactsCount(CONTACTS_COUNT)))))).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        passportSteps.commonAccountLogin();
        urlSteps.testing().path(MY).path(OFFERS).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Попап подробной инфы по дню, статистика на сниппете моего оффера")
    public void shouldSeeMyOfferSnippetStatisticsBarInfo() {
        basePageSteps.onMyOffersPage().snippetFirst().statistics().bars().get(0).hover();

        basePageSteps.onMyOffersPage().snippetFirst().statistics().barInfo().viewsCount().should(
                hasText("1 235 просмотров объявления"));
        basePageSteps.onMyOffersPage().snippetFirst().statistics().barInfo().contactsCount().should(
                hasText(format("%s показов контактов", CONTACTS_COUNT)));
        basePageSteps.onMyOffersPage().snippetFirst().statistics().barInfo().favoritesAddedCount().should(
                hasText(format("%s добавление в избранное", FAVORITES_COUNT)));
    }

}
