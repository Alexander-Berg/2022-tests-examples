package ru.yandex.general.offerCard;

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
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.yandex.general.beans.card.Statistics.statistics;
import static ru.yandex.general.consts.GeneralFeatures.OFFER_CARD_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.STATISTICS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CARD;
import static ru.yandex.general.mock.MockCard.BASIC_CARD;
import static ru.yandex.general.mock.MockCard.mockCard;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(OFFER_CARD_FEATURE)
@Feature(STATISTICS)
@DisplayName("Блок статистики с разной общей статистикой")
@RunWith(Parameterized.class)
@GuiceModules(GeneralMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class OfferCardTotalStatisticsTest {

    private static final String ID = "12345";
    private static final int DAYS_COUNT = 7;

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

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public int totalViews;

    @Parameterized.Parameter(2)
    public int totalContacts;

    @Parameterized.Parameter(3)
    public int totalFavoritesAdded;

    @Parameterized.Parameter(4)
    public int todayViews;

    @Parameterized.Parameter(5)
    public int todayContacts;

    @Parameterized.Parameter(6)
    public int todayFavoritesAdded;

    @Parameterized.Parameters(name = "{index} «{0}»")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"Общая + дневная статистика", 20, 12, 45, 51, 66, 85},
                {"Общая + пустая дневная статистика", 126, 99, 45, 0, 0, 0},
                {"Пустая общая + пустая дневная статистика", 0, 0, 0, 0, 0, 0}
        });
    }

    @Before
    public void before() {
        mockRule.graphqlStub(mockResponse()
                .setCard(mockCard(BASIC_CARD).setTotalStatistics(
                        statistics().setViewsCount(totalViews)
                                .setContactsCount(totalContacts)
                                .setFavoritesCount(totalFavoritesAdded))
                        .setTodayStatistics(
                                statistics().setViewsCount(todayViews)
                                        .setContactsCount(todayContacts)
                                        .setFavoritesCount(todayFavoritesAdded))
                        .setIsOwner(true).setStatisticsGraph(DAYS_COUNT).build())
                .setCurrentUserExample()
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.testing().path(CARD).path(ID).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Блок статистики с разной общей статистикой")
    public void shouldSeeOfferCardTotalStatistics() {
        basePageSteps.onOfferCardPage().statistics().viewsCount().should(
                hasText(formatStatistic(totalViews, todayViews)));
        basePageSteps.onOfferCardPage().statistics().contactsCount().should(
                hasText(formatStatistic(totalContacts, todayContacts)));
        basePageSteps.onOfferCardPage().statistics().favoritesAddedCount().should(
                hasText(formatStatistic(totalFavoritesAdded, todayFavoritesAdded)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Попап статистики с разной общей статистикой")
    public void shouldSeeOfferCardTotalStatisticsPopup() {
        basePageSteps.onOfferCardPage().statistics().bars().click();

        basePageSteps.onOfferCardPage().statisticsPopup().viewsCount().should(
                hasText(format("%sпросмотров объявления", formatStatisticPopup(totalViews, todayViews))));
        basePageSteps.onOfferCardPage().statisticsPopup().contactsCount().should(
                hasText(format("%sпоказов контактов", formatStatisticPopup(totalContacts, todayContacts))));
        basePageSteps.onOfferCardPage().statisticsPopup().favoritesAddedCount().should(
                hasText(format("%sдобавлений в избранное", formatStatisticPopup(totalFavoritesAdded, todayFavoritesAdded))));
    }

    private String formatStatistic(int total, int today) {
        String result;
        if (today == 0 && total == 0)
            result = "0";
        else if (today == 0)
            result = String.valueOf(total);
        else result = format("%d+%d", total, today);
        return result;
    }

    private String formatStatisticPopup(int total, int today) {
        String result;
        if (today == 0 && total == 0)
            result = "0";
        else if (today == 0)
            result = String.valueOf(total);
        else result = format("%d+%d ", total, today);
        return result;
    }

}
