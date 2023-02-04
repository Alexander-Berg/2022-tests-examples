package ru.auto.tests.desktop.sale;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static ru.auto.tests.desktop.TestData.USER_2_PROVIDER;
import static ru.auto.tests.desktop.consts.Owners.ALEXANDERREX;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления - контакты продавца")
@Feature(SALES)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class PhoneInContactsPopupTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private LoginSteps loginSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;


    @Before
    public void before() {
        loginSteps.loginAs(USER_2_PROVIDER.get());

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL).open();
    }

    @Test
    @Owner(ALEXANDERREX)
    @Category({Regression.class})
    @DisplayName("Отображение телефона продавца в попапе контактов")
    public void shouldSeePhoneOnContactsPopup() {
        basePageSteps.onListingPage().getSale(0).hover().click();
        basePageSteps.switchToNextTab();
        basePageSteps.onCardPage().contacts().showPhoneButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().popup().waitUntil(isDisplayed());

        basePageSteps.onCardPage().contactsPopup().getPhone(0).should(hasText(matchesPattern("\\+7 \\d{3} [\\d\\-]{7,10}")));

    }
}
