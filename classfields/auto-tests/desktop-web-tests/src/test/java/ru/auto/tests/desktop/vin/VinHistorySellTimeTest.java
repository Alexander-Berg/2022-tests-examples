package ru.auto.tests.desktop.vin;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.core.StringRegularExpression.matchesRegex;
import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.element.history.ReportContent.AVERAGE_SELL_TIME;
import static ru.auto.tests.desktop.element.history.VinReport.BLOCK_SELL_TIME;
import static ru.auto.tests.desktop.mock.MockCarfaxReport.reportExample;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.CARFAX_OFFER_CARS_ID_RAW;
import static ru.auto.tests.desktop.utils.Utils.getRandomBetween;
import static ru.auto.tests.desktop.utils.Utils.getRandomOfferId;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Feature(VIN)
@Story("Блок «Среднее время продажи»")
@DisplayName("Блок «Среднее время продажи»")
@GuiceModules(DesktopTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class VinHistorySellTimeTest {

    private static final String SELL_TIME_BLOCK_TEMPLATE = "Время продажи\n~ %d (день|дня|дней)\nС опциями продвижения";

    private final String offerId = getRandomOfferId();
    private final int sellTimeDays = getRandomBetween(1, 25);

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),

                stub().withGetDeepEquals(format(CARFAX_OFFER_CARS_ID_RAW, offerId))
                        .withResponseBody(
                                reportExample()
                                        .setSellTimeDays(sellTimeDays).getBody())
        ).create();

        urlSteps.testing().path(HISTORY).path(offerId).path(SLASH).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Среднее время продажи в содержании отчёта")
    public void shouldSeeSellTimeInContents() {
        basePageSteps.onHistoryPage().vinReport().contents().block(AVERAGE_SELL_TIME).value().should(hasText(
                matchesRegex(format("%d (день|дня|дней)", sellTimeDays))));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображение блока с временем продажи")
    public void shouldSeeSellTimeBlock() {
        basePageSteps.onHistoryPage().vinReport().block(BLOCK_SELL_TIME).should(hasText(matchesRegex(format(
                SELL_TIME_BLOCK_TEMPLATE, sellTimeDays))));
    }

}
