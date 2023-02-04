package ru.auto.tests.mobile.my;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ACTIVE;
import static ru.auto.tests.desktop.consts.Pages.CREDITS;
import static ru.auto.tests.desktop.consts.Pages.EDIT;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.mobile.element.lk.CreditsForm.ADDITIONALLY;
import static ru.auto.tests.desktop.mobile.element.lk.CreditsForm.ADDITIONAL_CONTACT;
import static ru.auto.tests.desktop.mobile.element.lk.CreditsForm.ADDRESS;
import static ru.auto.tests.desktop.mobile.element.lk.CreditsForm.PASSPORT_DATA;
import static ru.auto.tests.desktop.mobile.element.lk.CreditsForm.PLACE_OF_WORK;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Форма редактирования запроса на кредит")
@Feature(AutoruFeatures.LK)
@Story(AutoruFeatures.CREDITS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class CreditEditRequestTest {

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
                stub("desktop/SharkCreditApplicationActiveWithOffersNew"),
                stub("desktop-lk/SharkCreditProductList"),
                stub("desktop-lk/SharkCreditApplicationActiveWithOffersWithPersonProfiles"),
                stub("desktop/HistoryLastCars"),
                stub("desktop/UserFavoritesCars"),
                stub("desktop-lk/SharkCreditApplicationUpdate")
        ).create();

        urlSteps.testing().path(MY).path(CREDITS).path(EDIT).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(TIMONDL)
    @DisplayName("Должен заполнить форму редактирования заявки, фамилия не менялась")
    public void shouldFillCreditApplication() {
        fillToSurnameChangeField();

        basePageSteps.onLkCreditsPage().creditsForm().block(PASSPORT_DATA)
                .button("Не менялась").click();

        fillToFinalStep();

        mockRule.delete();
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/User"),
                stub("desktop-lk/SharkCreditApplicationUpdateFull"),
                stub("desktop/SharkBankList"),
                stub("desktop-lk/SharkCreditProductListByCreditApplication"),
                stub("desktop/SharkCreditProductListWithoutParams"),
                stub("desktop/SharkCreditProductListAll"),
                stub("desktop/SharkCreditApplicationListWithOffers"),
                stub("desktop-lk/SharkCreditApplicationAddProducts"),
                stub("desktop/SharkCreditApplicationActiveWithOffersWithPersonProfilesActive"),
                stub("mobile/SharkCreditApplicationUpdateWithClaims")
        ).create();

        basePageSteps.onLkCreditsPage().creditsForm().button("Отправить заявку").waitUntil(isDisplayed()).click();

        basePageSteps.onLkCreditsPage().creditsClaimsList().should(hasSize(4));
        urlSteps.testing().path(MY).path(CREDITS).path(ACTIVE).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Должен заполнить форму редактирования заявки, фамилия менялась")
    public void shouldFillCreditApplicationWithChangedSurname() {
        fillToSurnameChangeField();

        basePageSteps.onLkCreditsPage().creditsForm().block(PASSPORT_DATA)
                .button("Менялась").click();
        basePageSteps.onLkCreditsPage().creditsForm().block("У вас менялась фамилия?")
                .input("Предыдущая фамилия", "Петров");

        fillToFinalStep();

        mockRule.delete();
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/User"),
                stub("desktop-lk/SharkCreditApplicationUpdateFullWithOldName"),
                stub("desktop/SharkBankList"),
                stub("desktop-lk/SharkCreditProductListByCreditApplication"),
                stub("desktop/SharkCreditProductListWithoutParams"),
                stub("desktop/SharkCreditProductListAll"),
                stub("desktop/SharkCreditApplicationListWithOffers"),
                stub("desktop-lk/SharkCreditApplicationAddProducts"),
                stub("desktop/SharkCreditApplicationActiveWithOffersWithPersonProfilesActive"),
                stub("mobile/SharkCreditApplicationUpdateWithClaimsWithOldName")
        ).create();

        basePageSteps.onLkCreditsPage().creditsForm().button("Отправить заявку").waitUntil(isDisplayed()).click();
        basePageSteps.onLkCreditsPage().header().waitUntil(isDisplayed());

        basePageSteps.onLkCreditsPage().creditsClaimsList().should(hasSize(4));
        urlSteps.testing().path(MY).path(CREDITS).path(ACTIVE).shouldNotSeeDiff();
    }

    private void fillToSurnameChangeField() {
        basePageSteps.onLkCreditsPage().creditsForm().button("Больше 3").click();
        basePageSteps.onLkCreditsPage().creditsForm().block("Личные данные")
                .input("Придумайте кодовое слово", "слово");

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
    }

    private void fillToFinalStep() {
        basePageSteps.onLkCreditsPage().creditsForm().block(ADDRESS).input("Адрес регистрации", "Льва Толстого, 16");
        basePageSteps.onLkCreditsPage().creditsForm().geoSuggest().getItem(0).click();
        basePageSteps.onLkCreditsPage().creditsForm().block(ADDRESS)
                .button("По адресу регистрации").click();

        basePageSteps.onLkCreditsPage().creditsForm().block(PLACE_OF_WORK).button("В организации").click();
        basePageSteps.onLkCreditsPage().creditsForm().block(PLACE_OF_WORK).input("Название компании", "Яндекс");
        basePageSteps.onLkCreditsPage().creditsForm().geoSuggest().getItem(0).click();
        basePageSteps.onLkCreditsPage().creditsForm().block(PLACE_OF_WORK)
                .input("Рабочий номер телефона", WORK_PHONE);
        basePageSteps.onLkCreditsPage().creditsForm().block(PLACE_OF_WORK).button("Генеральный директор")
                .click();
        basePageSteps.onLkCreditsPage().creditsForm().block(PLACE_OF_WORK).button("7 лет и более").click();
        basePageSteps.onLkCreditsPage().creditsForm().block(PLACE_OF_WORK).input("Рабочий адрес", "Льва Толстого, 16");
        basePageSteps.onLkCreditsPage().creditsForm().geoSuggest().getItem(0).click();
        basePageSteps.onLkCreditsPage().creditsForm().block(PLACE_OF_WORK)
                .input("Ежемесячный доход", "1000000");
        basePageSteps.onLkCreditsPage().creditsForm().block(PLACE_OF_WORK).button("Справка 2-НДФЛ").click();

        basePageSteps.onLkCreditsPage().creditsForm().block(ADDITIONAL_CONTACT).button("Мой номер")
                .click();
        basePageSteps.onLkCreditsPage().creditsForm().block(ADDITIONAL_CONTACT)
                .input("Ваш дополнительный номер", ADDITIONAL_PHONE);

        basePageSteps.onLkCreditsPage().creditsForm().block(ADDITIONALLY).button("Высшее").click();
        basePageSteps.onLkCreditsPage().creditsForm().block(ADDITIONALLY).button("Холост").click();
        basePageSteps.onLkCreditsPage().creditsForm().block(ADDITIONALLY).button("Живу в своей квартире").click();
        basePageSteps.onLkCreditsPage().creditsForm().button("Выбрать").click();
        basePageSteps.onLkCreditsPage().creditsChooseCarsPopup().getCar(1).hover();
        basePageSteps.onLkCreditsPage().creditsChooseCarsPopup().getCar(1).button("Выбрать авто")
                .waitUntil(isDisplayed()).click();
    }

}
