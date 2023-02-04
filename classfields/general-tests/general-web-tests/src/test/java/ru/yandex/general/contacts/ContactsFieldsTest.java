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
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static java.lang.String.format;
import static ru.yandex.general.consts.GeneralFeatures.CONTACTS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CONTACTS;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.mock.MockCurrentUser.currentUserExample;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.page.ContactsPage.NAME;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasText;

@Epic(CONTACTS_FEATURE)
@Feature("Проверка основных полей")
@DisplayName("Поля страницы контактов")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class ContactsFieldsTest {

    private static final String USER_NAME = "Илон Маск";
    private static final String USER_EMAIL = "ilon@mail.ru";
    private static final String USER_ID = "495817561";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private PassportSteps passportSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        passportSteps.commonAccountLogin();
        mockRule.graphqlStub(mockResponse()
                .setCurrentUser(currentUserExample()
                        .setUserName(USER_NAME)
                        .setUserEmail(USER_EMAIL)
                        .setUserId(USER_ID).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();
        urlSteps.testing().path(MY).path(CONTACTS).open();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Имя юзера в инпуте «Имя»")
    public void shouldSeeNameInNameInput() {
        basePageSteps.onContactsPage().field(NAME).input().should(hasAttribute("value", USER_NAME));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Id юзера")
    public void shouldSeeUserId() {
        basePageSteps.onContactsPage().userId().should(hasText(format("ID %s", USER_ID)));
    }

}
