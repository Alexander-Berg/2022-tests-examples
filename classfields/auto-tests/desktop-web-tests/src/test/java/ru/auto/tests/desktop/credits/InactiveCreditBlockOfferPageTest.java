package ru.auto.tests.desktop.credits;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Блок кредитов на карточке под зарегом")
@Feature(AutoruFeatures.CREDITS)
@Story(LISTING)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class InactiveCreditBlockOfferPageTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private static final String LAND_ROVER = "land_rover";
    private static final String DISCOVERY = "discovery";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                        "desktop/OfferCarsUsedUser",
                        "desktop/User",
                        "desktop/SharkBankList",
                        "desktop/SuggestionsApiRSSuggestFio",
                        "desktop/SharkCreditProductList",
                        "desktop/SharkCreditApplicationActiveWithOffersEmptyAndDraft",
                        "desktop/SharkCreditApplicationActiveWithOffersWithPersonProfiles",
                        "desktop/SharkCreditApplicationCreate",
                        "desktop/SharkCreditApplicationUpdate")
                .post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Issue("AUTORUFRONT-20180")
    @Ignore
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение визарда после короткой заявки")
    public void shouldFillCreditApplication() {
        basePageSteps.onCardPage().cardCreditBlock().button("Подтвердить").waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().creditCurtain().waitUntil(isDisplayed()).should(hasText("Далее\nЗаявка на подбор " +
                "кредита\nЛичные данные\nФИО\nПочта\nНомер телефона\n" +
                "Количество детей до 21 года\nНет\n1\n2\n3\nБольше 3\nПридумайте кодовое слово\nОбязательно " +
                "запомните слово. Оно понадобится для обслуживания в банке и по телефону. Использовать можно " +
                "только русские буквы."));

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(LAND_ROVER).path(DISCOVERY).path(SALE_ID)
                .shouldNotSeeDiff();
    }
}
