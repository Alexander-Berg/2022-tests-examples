package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.element.offers.auth.ContactInfo;
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.auto.tests.commons.util.Utils.getRandomPhone;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.realty.consts.OfferAdd.DEAL_TYPE;
import static ru.yandex.realty.consts.OfferAdd.DEAL_TYPE_DIRECT;
import static ru.yandex.realty.consts.OfferAdd.FLAT;
import static ru.yandex.realty.consts.OfferAdd.SELL;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_SETTINGS;
import static ru.yandex.realty.consts.RealtyTags.JURICS;
import static ru.yandex.realty.element.management.SettingsContent.EMAIL_SECTION;
import static ru.yandex.realty.element.management.SettingsContent.SAVE_CHANGES;
import static ru.yandex.realty.element.management.SettingsContent.TITLE_NAME_SECTION;
import static ru.yandex.realty.element.management.SettingsContent.YOUR_PHONE_SECTION;
import static ru.yandex.realty.element.offers.auth.ContactInfo.ADD_MANAGER;
import static ru.yandex.realty.element.offers.auth.ContactInfo.EDIT_CONTACTS;
import static ru.yandex.realty.element.offers.auth.ContactInfo.HOW_ADDRESS_SECTION;
import static ru.yandex.realty.page.ManagementNewPage.EDIT;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.utils.UtilsWeb.PHONE_PATTERN_BRACKETS;
import static ru.yandex.realty.utils.UtilsWeb.makePhoneFormatted;

@Link("https://st.yandex-team.ru/VERTISTEST-1460")
@Tag(JURICS)
@DisplayName("Агентство. Контакты.")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class JuridicalAgencyContactsTest {

    public static final int SECOND_INPUT = 1;
    public static final int THIRD_INPUT = 2;
    private String phone;
    private String formattedPhone;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private Account account;

    @Inject
    private ManagementSteps managementSteps;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        apiSteps.createRealty3JuridicalAccount(account);
        urlSteps.setSpbCookie();
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.onOfferAddPage().dealType().selectButton(SELL);
        offerAddSteps.onOfferAddPage().offerType().selectButton(FLAT);
        offerAddSteps.addPhoto(OfferAddSteps.DEFAULT_PHOTO_COUNT_FOR_DWELLING);
        phone = getRandomPhone();
        formattedPhone = makePhoneFormatted(phone, PHONE_PATTERN_BRACKETS);
        basePageSteps.scrollToElement(managementSteps.onOfferAddPage().contactInfo());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Номер телефона агенства не заблокирован для редактирования -> вводим и видим «value» инпута")
    public void shouldSeeEnabledPhoneInput() {
        managementSteps.onOfferAddPage().contactInfo().featureField(YOUR_PHONE_SECTION).input().click();
        managementSteps.clearInputByBackSpace(
                () -> managementSteps.onOfferAddPage().contactInfo().featureField(YOUR_PHONE_SECTION).input());
        managementSteps.onOfferAddPage().contactInfo().featureField(ContactInfo.YOUR_PHONE).input()
                .sendKeys(phone);
        managementSteps.onOfferAddPage().contactInfo().featureField(ContactInfo.YOUR_PHONE).input()
                .should(hasValue(formattedPhone));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Меняем номер телефона агенства на форме подачи и создаем оффер проверяем настройки, номер остался прежний")
    public void shouldSeeSavedPhone() {
        offerAddSteps.onOfferAddPage().featureField(DEAL_TYPE).selectButton(DEAL_TYPE_DIRECT);
        offerAddSteps.fillWithoutPhone(OfferAddSteps.DEFAULT_LOCATION);
        managementSteps.onOfferAddPage().contactInfo().featureField(YOUR_PHONE_SECTION).input().clear();
        managementSteps.onOfferAddPage().contactInfo().featureField(YOUR_PHONE_SECTION).input().sendKeys(phone);
        offerAddSteps.onOfferAddPage().publishBlock().payButton().click();
        offerAddSteps.waitPublish();
        urlSteps.testing().path(MANAGEMENT_NEW_SETTINGS).open();
        String agencyPhone = makePhoneFormatted(account.getPhone().get(), PHONE_PATTERN_BRACKETS);
        managementSteps.onManagementNewPage().settingsContent().section(YOUR_PHONE_SECTION).input()
                .should(hasValue(agencyPhone));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Добавить менеджера в оффер агенства данные отображаются в оффере и на форме подачи.")
    public void shouldSeeNewManager() {
        offerAddSteps.onOfferAddPage().featureField(DEAL_TYPE).selectButton(DEAL_TYPE_DIRECT);
        offerAddSteps.fillWithoutPhone(OfferAddSteps.DEFAULT_LOCATION);
        managementSteps.onOfferAddPage().contactInfo().button(ADD_MANAGER).click();

        String name = getRandomString();
        managementSteps.onOfferAddPage().contactInfo().featureField(YOUR_PHONE_SECTION)
                .inputInItem(SECOND_INPUT, name);
        managementSteps.onOfferAddPage().contactInfo().featureField(YOUR_PHONE_SECTION).input(THIRD_INPUT)
                .sendKeys(phone.substring(1));
        offerAddSteps.onOfferAddPage().publishBlock().payButton().click();
        offerAddSteps.waitPublish();
        managementSteps.onManagementNewPage().agencyOffer(FIRST).link(EDIT).click();
        managementSteps.switchToNextTab();
        managementSteps.onOfferAddPage().contactInfo().featureField(YOUR_PHONE_SECTION).input(SECOND_INPUT)
                .should(hasValue(name));
        managementSteps.onOfferAddPage().contactInfo().featureField(YOUR_PHONE_SECTION).input(THIRD_INPUT)
                .should(hasValue(formattedPhone));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кликнуть на «Редактировать конктакты»  -> редирект в настройки")
    public void shouldRedirectToContacts() {
        managementSteps.onOfferAddPage().contactInfo().link(EDIT_CONTACTS).click();
        urlSteps.testing().path(MANAGEMENT_NEW_SETTINGS).ignoreParam("redirect").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Сохраняем новые контакты -> редирект обратно на форму добавления. Контакты сохранились")
    public void shouldRedirectToAddPage() {
        offerAddSteps.fillWithoutPhone(OfferAddSteps.DEFAULT_LOCATION);
        managementSteps.onOfferAddPage().contactInfo().link(EDIT_CONTACTS).click();
        urlSteps.testing().path(MANAGEMENT_NEW_SETTINGS).ignoreParam("redirect").shouldNotDiffWithWebDriverUrl();
        String name = getRandomString();
        managementSteps.onManagementNewPage().settingsContent().section(TITLE_NAME_SECTION).clearSign().click();
        managementSteps.onManagementNewPage().settingsContent().section(TITLE_NAME_SECTION).input().sendKeys(name);
        String email = getRandomEmail();
        managementSteps.onManagementNewPage().settingsContent().section(EMAIL_SECTION).clearSign().click();
        managementSteps.onManagementNewPage().settingsContent().section(EMAIL_SECTION).input().sendKeys(email);
        managementSteps.onManagementNewPage().settingsContent().section(YOUR_PHONE_SECTION).clearSign().click();
        managementSteps.onManagementNewPage().settingsContent().section(YOUR_PHONE_SECTION).input().sendKeys(phone);
        managementSteps.onManagementNewPage().settingsContent().button(SAVE_CHANGES).click();
        urlSteps.ignoreParam("draftId").shouldNotDiffWithWebDriverUrl();
        managementSteps.onOfferAddPage().contactInfo().featureField(HOW_ADDRESS_SECTION).input().should(hasValue(name));
        managementSteps.onOfferAddPage().contactInfo().featureField(EMAIL_SECTION).input().should(hasValue(email));
        managementSteps.onOfferAddPage().contactInfo().featureField(YOUR_PHONE_SECTION).input()
                .should(hasValue(formattedPhone));
    }
}
