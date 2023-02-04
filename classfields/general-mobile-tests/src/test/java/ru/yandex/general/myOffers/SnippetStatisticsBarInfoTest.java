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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.rules.MockRule;
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
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(MY_OFFERS_FEATURE)
@Feature(STATISTICS)
@DisplayName("Подробная инфа статистики по дню со сниппета в попапе")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
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
        passportSteps.commonAccountLogin();
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
        urlSteps.testing().path(MY).path(OFFERS).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Подробная инфа статистики по дню со сниппета в попапе")
    public void shouldSeeMyOfferSnippetStatisticsBarInfo() {
        basePageSteps.onMyOffersPage().snippetFirst().statistics().bars().click();
        basePageSteps.onMyOffersPage().statisticsPopup().bars().get(0).waitUntil(isDisplayed()).click();

        basePageSteps.onMyOffersPage().statisticsPopup().viewsCount().should(
                hasText("1 235просмотров объявления"));
        basePageSteps.onMyOffersPage().statisticsPopup().contactsCount().should(
                hasText(format("%sпоказов контактов", CONTACTS_COUNT)));
        basePageSteps.onMyOffersPage().statisticsPopup().favoritesAddedCount().should(
                hasText(format("%sдобавление в избранное", FAVORITES_COUNT)));
    }

}
