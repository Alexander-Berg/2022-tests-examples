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
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.module.GeneralMobileWebModule;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.general.consts.GeneralFeatures.CONTACTS_FEATURE;
import static ru.yandex.general.consts.Notifications.CHANGES_SAVED;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CONTACTS;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.element.Input.VALUE;
import static ru.yandex.general.mobile.page.ContactsPage.ADDING_ADDRESS;
import static ru.yandex.general.page.ContactsPage.ADD_ADDRESS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasAttribute;

@Epic(CONTACTS_FEATURE)
@Feature("Добавление адресов")
@DisplayName("Список адресов на странице контактов")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class ContactsAddressesAddTest {

    private static final String ADDRESS = "Павелецкая набережная, 2";
    private static final String ADDRESS_2 = "Астрадамский проезд, 4Ак1";
    private static final String DISTRICT = "Замоскворечье";
    private static final String METRO = "Парк Культуры";

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
        passportSteps.createAccountAndLogin();
        urlSteps.testing().path(MY).path(CONTACTS).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавление адреса")
    public void shouldSeeAddAddress() {
        basePageSteps.onContactsPage().addressesList().get(0).click();
        basePageSteps.onContactsPage().wrapper(ADDING_ADDRESS).setTextarea(ADDRESS);
        basePageSteps.onContactsPage().wrapper(ADDING_ADDRESS).suggestItem(ADDRESS).waitUntil(isDisplayed()).click();
        basePageSteps.onContactsPage().popupNotification(CHANGES_SAVED).waitUntil(isDisplayed());
        basePageSteps.refresh();
        basePageSteps.wait500MS();

        basePageSteps.onContactsPage().addressesList().should(hasSize(1))
                .should(hasItem(hasAttribute(VALUE, ADDRESS)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавление района")
    public void shouldSeeAddDistrict() {
        basePageSteps.onContactsPage().addressesList().get(0).click();
        basePageSteps.onContactsPage().wrapper(ADDING_ADDRESS).setTextarea(DISTRICT);
        basePageSteps.onContactsPage().wrapper(ADDING_ADDRESS).suggestItem(DISTRICT).waitUntil(isDisplayed()).click();
        basePageSteps.onContactsPage().popupNotification(CHANGES_SAVED).waitUntil(isDisplayed());
        basePageSteps.refresh();
        basePageSteps.wait500MS();

        basePageSteps.onContactsPage().addressesList().should(hasSize(1))
                .should(hasItem(hasAttribute(VALUE, DISTRICT)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавление метро")
    public void shouldSeeAddSubway() {
        basePageSteps.onContactsPage().addressesList().get(0).click();
        basePageSteps.onContactsPage().wrapper(ADDING_ADDRESS).setTextarea(METRO);
        basePageSteps.onContactsPage().wrapper(ADDING_ADDRESS).suggestItem(METRO).waitUntil(isDisplayed()).click();
        basePageSteps.onContactsPage().popupNotification(CHANGES_SAVED).waitUntil(isDisplayed());
        basePageSteps.refresh();
        basePageSteps.wait500MS();

        basePageSteps.onContactsPage().addressesList().should(hasSize(1))
                .should(hasItem(hasAttribute(VALUE, METRO)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавление 3 адресов разных типов - адрес/метро/район")
    public void shouldSeeAddAddressSubway() {
        basePageSteps.onContactsPage().addressesList().get(0).click();
        basePageSteps.onContactsPage().wrapper(ADDING_ADDRESS).setTextarea(ADDRESS);
        basePageSteps.onContactsPage().wrapper(ADDING_ADDRESS).suggestItem(ADDRESS).waitUntil(isDisplayed()).click();
        basePageSteps.onContactsPage().popupNotification(CHANGES_SAVED).waitUntil(isDisplayed());

        basePageSteps.onContactsPage().button(ADD_ADDRESS).click();
        basePageSteps.onContactsPage().wrapper(ADDING_ADDRESS).setTextarea(DISTRICT);
        basePageSteps.onContactsPage().wrapper(ADDING_ADDRESS).suggestItem(DISTRICT).waitUntil(isDisplayed()).click();
        basePageSteps.onContactsPage().popupNotification(CHANGES_SAVED).waitUntil(isDisplayed());

        basePageSteps.onContactsPage().button(ADD_ADDRESS).click();
        basePageSteps.onContactsPage().wrapper(ADDING_ADDRESS).setTextarea(METRO);
        basePageSteps.onContactsPage().wrapper(ADDING_ADDRESS).suggestItem(METRO).waitUntil(isDisplayed()).click();
        basePageSteps.onContactsPage().popupNotification(CHANGES_SAVED).waitUntil(isDisplayed());
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.refresh();
        basePageSteps.wait500MS();

        basePageSteps.onContactsPage().addressesList().should(hasSize(3))
                .should(hasItems(
                        hasAttribute(VALUE, ADDRESS),
                        hasAttribute(VALUE, METRO),
                        hasAttribute(VALUE, DISTRICT)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Смена адреса")
    public void shouldSeeChangeAddress() {
        basePageSteps.onContactsPage().addressesList().get(0).click();
        basePageSteps.onContactsPage().wrapper(ADDING_ADDRESS).setTextarea(ADDRESS);
        basePageSteps.onContactsPage().wrapper(ADDING_ADDRESS).suggestItem(ADDRESS).waitUntil(isDisplayed()).click();
        basePageSteps.onContactsPage().popupNotification(CHANGES_SAVED).waitUntil(isDisplayed());
        basePageSteps.refresh();
        basePageSteps.wait500MS();
        basePageSteps.onContactsPage().addressesList().get(0).click();
        basePageSteps.onContactsPage().wrapper(ADDING_ADDRESS).setTextarea(ADDRESS_2);
        basePageSteps.onContactsPage().wrapper(ADDING_ADDRESS).suggestItem(ADDRESS_2).waitUntil(isDisplayed()).click();
        basePageSteps.onContactsPage().popupNotification(CHANGES_SAVED).waitUntil(isDisplayed());
        basePageSteps.refresh();
        basePageSteps.wait500MS();

        basePageSteps.onContactsPage().addressesList().should(hasSize(1))
                .should(hasItem(hasAttribute(VALUE, ADDRESS_2)));
    }

}
