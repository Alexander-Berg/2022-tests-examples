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

import java.util.concurrent.TimeUnit;

import static ru.yandex.general.consts.GeneralFeatures.CONTACTS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CONTACTS;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.page.ContactsPage.NAME;
import static ru.yandex.general.page.ContactsPage.PROFILE_SETTINGS;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasText;

@Epic(CONTACTS_FEATURE)
@Feature("Проверка основных полей")
@DisplayName("Смена имени аккаунта")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class ContactsChangeNameTest {

    private static final String CHANGED_NAME = "Василий Петрович";

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
    @DisplayName("Смена имени аккаунта")
    public void shouldSeeChangedName() {
        basePageSteps.onContactsPage().field(NAME).input().clearInput().click();
        basePageSteps.onContactsPage().field(NAME).input().sendKeys(CHANGED_NAME);
        basePageSteps.onContactsPage().spanLink(PROFILE_SETTINGS).click();
        basePageSteps.waitSomething(2, TimeUnit.SECONDS);
        basePageSteps.refresh();

        basePageSteps.onContactsPage().lkSidebar().userName().should(hasText(CHANGED_NAME));
    }

}
