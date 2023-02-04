package ru.yandex.general.contacts;

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
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static ru.yandex.general.consts.GeneralFeatures.CONTACTS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CONTACTS;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.page.ContactsPage.ADDRESSES;
import static ru.yandex.general.page.ContactsPage.NAME;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(CONTACTS_FEATURE)
@Feature("Хинты при очищении полей")
@DisplayName("Хинты при очищении полей")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class ContactsEmptyFieldsHintsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        passportSteps.accountForOfferCreationLogin();
        urlSteps.testing().path(MY).path(CONTACTS).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Хинт «Вы не представились» при стирании имени и сбросе фокуса")
    public void shouldSeeClearNameHint() {
        basePageSteps.onContactsPage().field(NAME).input().clearInput().click();
        basePageSteps.onContactsPage().userId().click();

        basePageSteps.onContactsPage().field(NAME).inputHint("Вы не представились").should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Хинт «Поле нужно заполнить» при стирании адреса и сбросе фокуса")
    public void shouldSeeClearAddressHint() {
        basePageSteps.onContactsPage().addressesList().get(0).clearInput().click();
        basePageSteps.onContactsPage().userId().click();

        basePageSteps.onContactsPage().field(ADDRESSES).inputHint("Поле нужно заполнить").should(isDisplayed());
    }

}
