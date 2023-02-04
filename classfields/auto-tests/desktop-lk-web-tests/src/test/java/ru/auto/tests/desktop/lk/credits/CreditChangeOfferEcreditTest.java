package ru.auto.tests.desktop.lk.credits;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.isEmptyString;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CREDITS;
import static ru.auto.tests.desktop.consts.Pages.EDIT;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.element.lk.CreditsForm.ADDRESS;
import static ru.auto.tests.desktop.element.lk.CreditsForm.DRIVING_LICENSE;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Форма редактирования запроса на кредит")
@Feature(AutoruFeatures.CREDITS)
@Story("Редактирование")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class CreditChangeOfferEcreditTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/User"),
                stub("desktop/HistoryLastCars"),
                stub("desktop/UserFavoritesCarsForEcredit"),
                stub("desktop/SuggestionsApiRSSuggestFio"),
                stub("desktop/SuggestionsApiRSSuggestAddressLeoTolstoy"),
                stub("desktop/SuggestionsApiRSSuggestAddressNovosibirsk"),
                stub("desktop/SuggestionsApiRSSuggestFms"),
                stub("desktop/SuggestionsApiRSSuggestParty"),
                stub("desktop/SharkCreditApplicationActiveWithOffersFull"),
                stub("desktop-lk/SharkCreditProductList"),
                stub("desktop-lk/SharkCreditApplicationActiveWithOffersWithPersonProfilesFull"),
                stub("desktop-lk/SharkCreditApplicationUpdate")
        ).create();

        urlSteps.testing().path(MY).path(CREDITS).path(EDIT).open();

        basePageSteps.onLkCreditsPage().creditsForm().button("Изменить автомобиль").click();
        basePageSteps.onLkCreditsPage().creditsChooseCarsPopup().getCar(0).hover();
        basePageSteps.onLkCreditsPage().creditsChooseCarsPopup().getCar(0).button("Выбрать")
                .waitUntil(isDisplayed()).click();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Должны видеть пустое поле «Дата регистрации» в блоке «Адрес»")
    public void shouldSeeEmptyDateRegistrationFieldInAddress() {
        basePageSteps.onLkCreditsPage().creditsForm().block(ADDRESS).waitUntil(hasText("Адрес\nЗаполнено 2 из 3"));

        basePageSteps.onLkCreditsPage().creditsForm().block(ADDRESS).click();

        basePageSteps.onLkCreditsPage().creditsForm().block(ADDRESS).input("Дата регистрации")
                .should(isDisplayed())
                .should(hasValue(isEmptyString()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Должны видеть пустые поля в блоке «Водительское удостоверение»")
    public void shouldSeeEmptyDrivingLicenseFields() {
        basePageSteps.onLkCreditsPage().creditsForm().block(DRIVING_LICENSE).waitUntil(hasText("Водительское " +
                "удостоверение\nНомер водительского удостоверения\nВ формате 12 34 567890 или " +
                "12 AA 567890\nДата выдачи"));

        basePageSteps.onLkCreditsPage().creditsForm().block(DRIVING_LICENSE).input("Номер водительского удостоверения")
                .should(isDisplayed())
                .should(hasValue(isEmptyString()));
        basePageSteps.onLkCreditsPage().creditsForm().block(DRIVING_LICENSE).input("Дата выдачи")
                .should(isDisplayed())
                .should(hasValue(isEmptyString()));
    }

}
