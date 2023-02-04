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
import ru.yandex.general.mock.MockCurrentUser;
import ru.yandex.general.mock.MockResponse;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.general.consts.GeneralFeatures.CONTACTS_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CONTACTS;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.element.Input.VALUE;
import static ru.yandex.general.mock.MockCurrentUser.currentUserExample;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.utils.Utils.formatPhone;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasAttribute;

@Epic(CONTACTS_FEATURE)
@Feature("Проверка основных полей")
@DisplayName("Список телефонов на странице контактов")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class ContactsPhoneListTest {

    private static final String PHONE_1 = "+79119214431";
    private static final String PHONE_2 = "+79119214465";

    private MockCurrentUser currentUser = currentUserExample();
    private MockResponse mockResponse;

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
        mockResponse = mockResponse()
                .setCategoriesTemplate()
                .setRegionsTemplate();
        urlSteps.testing().path(MY).path(CONTACTS);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ни одного телефона не добавлено")
    public void shouldSeeNoPhone() {
        mockRule.graphqlStub(mockResponse.setCurrentUser(currentUser.addPhones().build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onContactsPage().phoneList()
                .should(hasSize(1))
                .should(hasItem(hasAttribute(VALUE, "")));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Один телефон добавлен")
    public void shouldSeeOnePhone() {
        mockRule.graphqlStub(mockResponse.setCurrentUser(currentUser.addPhones(PHONE_1).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onContactsPage().phoneList()
                .should(hasSize(1))
                .should(hasItem(hasAttribute(VALUE, formatPhone(PHONE_1))));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Два телефона добавлено")
    public void shouldSeeTwoPhones() {
        mockRule.graphqlStub(mockResponse.setCurrentUser(currentUser.addPhones(PHONE_1, PHONE_2).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onContactsPage().phoneList()
                .should(hasSize(2))
                .should(hasItems(
                        hasAttribute(VALUE, formatPhone(PHONE_1)),
                        hasAttribute(VALUE, formatPhone(PHONE_2))));
    }

}
