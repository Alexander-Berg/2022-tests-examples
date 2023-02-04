package ru.yandex.general.statistics;

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
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.STATISTICS;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.STATS;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.mock.MockUserStatistics.userStatistics;
import static ru.yandex.general.mobile.page.StatisticsPage.ACTIVE;
import static ru.yandex.general.mobile.page.StatisticsPage.BANNED;
import static ru.yandex.general.mobile.page.StatisticsPage.EARNED;
import static ru.yandex.general.mobile.page.StatisticsPage.EXPIRED;
import static ru.yandex.general.mobile.page.StatisticsPage.SOLD;
import static ru.yandex.general.utils.Utils.formatPrice;
import static ru.yandex.general.utils.Utils.getRandomIntInRange;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(STATISTICS)
@Feature("Каунтеры общей статистики юзера")
@DisplayName("Каунтеры общей статистики юзера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class StatisticsTotalCountersTest {

    private int counter;

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
        urlSteps.testing().path(MY).path(STATS);
        counter = getRandomIntInRange(10, 1000);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение каунтера общего заработка")
    public void shouldSeeTotalEarnedCounter() {
        counter = getRandomIntInRange(1000, 100000);
        mockRule.graphqlStub(mockResponse()
                .setUserStatistics(userStatistics().setTotalEarned(counter).build())
                .setCurrentUserExample()
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onStatisticsPage().statisticsCounter(EARNED).should(hasText(formatPrice(counter)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение каунтера активных офферов")
    public void shouldSeeTotalActiveCounter() {
        mockRule.graphqlStub(mockResponse()
                .setUserStatistics(userStatistics().setTotalActive(counter).build())
                .setCurrentUserExample()
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onStatisticsPage().statisticsCounter(ACTIVE).should(hasText(String.valueOf(counter)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение каунтера забаненных офферов")
    public void shouldSeeTotalBannedCounter() {
        mockRule.graphqlStub(mockResponse()
                .setUserStatistics(userStatistics().setTotalBanned(counter).build())
                .setCurrentUserExample()
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onStatisticsPage().statisticsCounter(BANNED).should(hasText(String.valueOf(counter)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение каунтера завершенных офферов")
    public void shouldSeeTotalExpiredCounter() {
        mockRule.graphqlStub(mockResponse()
                .setUserStatistics(userStatistics().setTotalExpired(counter).build())
                .setCurrentUserExample()
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onStatisticsPage().statisticsCounter(EXPIRED).should(hasText(String.valueOf(counter)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение каунтера проданных офферов")
    public void shouldSeeTotalSoldCounter() {
        mockRule.graphqlStub(mockResponse()
                .setUserStatistics(userStatistics().setTotalSold(counter).build())
                .setCurrentUserExample()
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onStatisticsPage().statisticsCounter(SOLD).should(hasText(String.valueOf(counter)));
    }

}
