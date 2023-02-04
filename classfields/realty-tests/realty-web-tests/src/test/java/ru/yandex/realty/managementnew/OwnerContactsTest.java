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
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.UrlSteps;
import ru.yandex.realty.utils.AccountType;

import static org.hamcrest.CoreMatchers.not;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;
import static ru.yandex.realty.consts.OfferAdd.FLAT;
import static ru.yandex.realty.consts.OfferAdd.SELL;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_SETTINGS;
import static ru.yandex.realty.consts.RealtyTags.PHYSICS;
import static ru.yandex.realty.element.management.SettingsContent.EMAIL_SECTION;
import static ru.yandex.realty.element.management.SettingsContent.NAME_SECTION;
import static ru.yandex.realty.element.management.SettingsContent.SAVE_CHANGES;
import static ru.yandex.realty.element.management.SettingsContent.YOUR_PHONE_SECTION;
import static ru.yandex.realty.element.offers.auth.ContactInfo.EDIT_CONTACTS;
import static ru.yandex.realty.element.offers.auth.ContactInfo.HOW_ADDRESS_SECTION;
import static ru.yandex.realty.utils.UtilsWeb.PHONE_PATTERN_BRACKETS;
import static ru.yandex.realty.utils.UtilsWeb.makePhoneFormatted;

@Link("https://st.yandex-team.ru/VERTISTEST-1460")
@DisplayName("Физ лицо. Контакты.")
@Tag(PHYSICS)
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class OwnerContactsTest {

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
    private BasePageSteps basePageSteps;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Before
    public void before() {
        apiSteps.createVos2Account(account, AccountType.OWNER);
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        offerAddSteps.onOfferAddPage().dealType().selectButton(SELL);
        offerAddSteps.onOfferAddPage().offerType().selectButton(FLAT);
        basePageSteps.scrollToElement(managementSteps.onOfferAddPage().contactInfo());
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class})
    @DisplayName("Номер телефона физика заблокирован для редактирования ")
    public void shouldSeeEnabledPhoneInput() {
        managementSteps.onOfferAddPage().contactInfo().featureField(YOUR_PHONE_SECTION).input()
                .should(hasValue(makePhoneFormatted(account.getPhone().get(), PHONE_PATTERN_BRACKETS)));
        managementSteps.onOfferAddPage().contactInfo().featureField(YOUR_PHONE_SECTION).input().should(not(isEnabled()));
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class})
    @DisplayName("Кликнуть на «Редактировать конктакты»  -> редирект в настройки")
    public void shouldRedirectToContacts() {
        managementSteps.onOfferAddPage().contactInfo().link(EDIT_CONTACTS).click();
        urlSteps.testing().path(MANAGEMENT_NEW_SETTINGS).ignoreParam("redirect").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @Category({Regression.class})
    @DisplayName("Сохраняем новые контакты -> редирект обратно на форму добавления. Контакты сохранились")
    public void shouldRedirectToAddPage() {
        offerAddSteps.selectGeoLocation(OfferAddSteps.DEFAULT_LOCATION);
        managementSteps.onOfferAddPage().contactInfo().link(EDIT_CONTACTS).click();
        urlSteps.testing().path(MANAGEMENT_NEW_SETTINGS).ignoreParam("redirect").shouldNotDiffWithWebDriverUrl();
        String name = getRandomString();
        managementSteps.onManagementNewPage().settingsContent().section(NAME_SECTION).clearSign().click();
        managementSteps.onManagementNewPage().settingsContent().section(NAME_SECTION).input().sendKeys(name);
        String email = getRandomEmail();
        managementSteps.onManagementNewPage().settingsContent().section(EMAIL_SECTION).clearSign().click();
        managementSteps.onManagementNewPage().settingsContent().section(EMAIL_SECTION).input().sendKeys(email);
        managementSteps.onManagementNewPage().settingsContent().button(SAVE_CHANGES).click();
        urlSteps.ignoreParam("draftId").shouldNotDiffWithWebDriverUrl();
        managementSteps.onOfferAddPage().contactInfo().featureField(HOW_ADDRESS_SECTION).input().should(hasValue(name));
        managementSteps.onOfferAddPage().contactInfo().featureField(EMAIL_SECTION).input().should(hasValue(email));
    }
}
