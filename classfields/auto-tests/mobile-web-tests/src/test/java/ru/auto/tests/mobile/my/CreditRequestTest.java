package ru.auto.tests.mobile.my;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.mobile.step.LkCreditsFormSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CREDITS;
import static ru.auto.tests.desktop.consts.Pages.EDIT;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.WIZARD;
import static ru.auto.tests.desktop.mobile.element.lk.CreditsForm.ABOUT_COMPANY;
import static ru.auto.tests.desktop.mobile.element.lk.CreditsForm.NEXT;
import static ru.auto.tests.desktop.mobile.element.lk.CreditsForm.PASSPORT_DATA;
import static ru.auto.tests.desktop.mock.MockStub.stub;

@DisplayName("Кредитный брокер")
@Feature(AutoruFeatures.LK)
@Story(AutoruFeatures.CREDITS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class CreditRequestTest {

    private final static String WORK_PHONE = "+79111111112";
    private final static String ADDITIONAL_PHONE = "+79112221199";

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

    @Inject
    private LkCreditsFormSteps creditsFormSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/User"),
                stub("desktop/SuggestionsApiRSSuggestFio"),
                stub("desktop/SuggestionsApiRSSuggestAddressLeoTolstoy"),
                stub("desktop/SuggestionsApiRSSuggestAddressNovosibirsk"),
                stub("desktop/SuggestionsApiRSSuggestFms"),
                stub("desktop/SuggestionsApiRSSuggestParty"),
                stub("desktop/SharkCreditApplicationActiveNewWithPersonProfile"),
                stub("desktop/HistoryLastCars"),
                stub("desktop/UserFavoritesCars"),
                stub("mobile/SharkCreditApplicationUpdate")
        ).create();

        urlSteps.testing().path(MY).path(CREDITS).path(WIZARD).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @Issue("AUTORUFRONT-21486")
    @DisplayName("Заявка на кредит, фамилия не менялась")
    public void shouldFillCreditApplication() {
        fillToSurnameChangeScreen();

        basePageSteps.onLkCreditsPage().creditsForm().button("Не менялась").click();

        fillToFinalStep();

        urlSteps.testing().path(MY).path(CREDITS).path(EDIT).addParam("from", "wizard").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @Issue("AUTORUFRONT-21486")
    @DisplayName("Заявка на кредит, фамилия менялась")
    public void shouldFillCreditApplicationWithChangedSurname() {
        fillToSurnameChangeScreen();

        basePageSteps.onLkCreditsPage().creditsForm().button("Менялась").click();
        basePageSteps.onLkCreditsPage().creditsForm().block("Укажите предыдущую фамилию").input().sendKeys("Петров");
        basePageSteps.onLkCreditsPage().creditsForm().button(NEXT).click();

        fillToFinalStep();

        urlSteps.testing().path(MY).path(CREDITS).path(EDIT).addParam("from", "wizard").shouldNotSeeDiff();
    }

    private void fillToSurnameChangeScreen() {
        basePageSteps.onLkCreditsPage().creditsForm().block(PASSPORT_DATA)
                .input("Серия и номер паспорта", "1111111111");
        basePageSteps.onLkCreditsPage().creditsForm().block(PASSPORT_DATA)
                .input("Дата выдачи паспорта", "21.01.2004");
        basePageSteps.onLkCreditsPage().creditsForm().block(PASSPORT_DATA)
                .input("Код подразделения", "111111");
        basePageSteps.onLkCreditsPage().creditsForm().block(PASSPORT_DATA).input("Кем выдан", "Химки");
        basePageSteps.onLkCreditsPage().creditsForm().geoSuggest().getItem(0).click();
        basePageSteps.onLkCreditsPage().creditsForm().block(PASSPORT_DATA)
                .input("Дата рождения", "31.01.1983");
        basePageSteps.onLkCreditsPage().creditsForm().block(PASSPORT_DATA).input("Место рождения", "новосибирск");
        basePageSteps.onLkCreditsPage().creditsForm().geoSuggest().getItem(0).click();

        basePageSteps.onLkCreditsPage().creditsForm().button(NEXT).click();
    }

    private void fillToFinalStep() {
        creditsFormSteps.enterTextInSuggestInput("Адрес регистрации", "Льва Толстого, 16");
        basePageSteps.onLkCreditsPage().creditsForm().geoSuggest().getItem(0).click();

        basePageSteps.onLkCreditsPage().creditsForm().button(NEXT).click();

        basePageSteps.onLkCreditsPage().creditsForm().button("По адресу регистрации").click();
        basePageSteps.onLkCreditsPage().creditsForm().block("Тип занятости").button("В организации").click();
        basePageSteps.onLkCreditsPage().creditsForm().block(ABOUT_COMPANY).input("Название компании", "Яндекс");
        basePageSteps.onLkCreditsPage().creditsForm().geoSuggest().getItem(0).click();
        basePageSteps.onLkCreditsPage().creditsForm().block(ABOUT_COMPANY)
                .input("Рабочий телефон", WORK_PHONE);

        basePageSteps.onLkCreditsPage().creditsForm().button(NEXT).click();

        basePageSteps.onLkCreditsPage().creditsForm().button("Генеральный директор")
                .click();
        basePageSteps.onLkCreditsPage().creditsForm().button("7 лет и более").click();
        creditsFormSteps.enterTextInSuggestInput("Рабочий адрес", "Льва Толстого, 16");
        basePageSteps.onLkCreditsPage().creditsForm().geoSuggest().getItem(0).click();

        basePageSteps.onLkCreditsPage().creditsForm().button(NEXT).click();

        basePageSteps.onLkCreditsPage().creditsForm().suggestInput("Ежемесячный доход").sendKeys("1000000");

        basePageSteps.onLkCreditsPage().creditsForm().button(NEXT).click();

        basePageSteps.onLkCreditsPage().creditsForm().radioButton("Справка 2-НДФЛ").click();

        basePageSteps.onLkCreditsPage().creditsForm().radioButton("Мой номер").click();
        basePageSteps.onLkCreditsPage().creditsForm().suggestInput("Ваш дополнительный номер").sendKeys(ADDITIONAL_PHONE);

        basePageSteps.onLkCreditsPage().creditsForm().button(NEXT).click();

        basePageSteps.onLkCreditsPage().creditsForm().radioButton("Больше 3").click();

        basePageSteps.onLkCreditsPage().creditsForm().suggestInput("Придумайте кодовое\u00a0слово").sendKeys("слово");

        basePageSteps.onLkCreditsPage().creditsForm().button(NEXT).click();

        basePageSteps.onLkCreditsPage().creditsForm().radioButton("Высшее").click();

        basePageSteps.onLkCreditsPage().creditsForm().radioButton("Холост").click();

        basePageSteps.onLkCreditsPage().creditsForm().radioButton("Живу в своей квартире").click();

        basePageSteps.onLkCreditsPage().creditsForm().button(NEXT).click();

        basePageSteps.hideElement(basePageSteps.onLkCreditsPage().creditsForm().floatingControls());
        basePageSteps.onLkCreditsPage().creditsChooseCarsPopup().getCar(1).button("Выбрать авто").click();
    }

}
