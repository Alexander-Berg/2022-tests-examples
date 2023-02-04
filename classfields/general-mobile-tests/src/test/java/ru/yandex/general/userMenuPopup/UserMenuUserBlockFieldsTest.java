package ru.yandex.general.userMenuPopup;

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
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static java.lang.String.format;
import static ru.yandex.general.consts.GeneralFeatures.USER_MENU_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.mock.MockCurrentUser.currentUserExample;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(USER_MENU_FEATURE)
@Feature("Отображение данных юзера")
@DisplayName("Отображение данных юзера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class UserMenuUserBlockFieldsTest {

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
        mockRule.graphqlStub(mockResponse()
                .setCurrentUser(currentUserExample()
                        .setUserName(USER_NAME)
                        .setUserEmail(USER_EMAIL)
                        .setUserId(USER_ID).build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();

        passportSteps.commonAccountLogin();
        basePageSteps.onBasePage().header().burger().click();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Имя юзера в попапе")
    public void shouldSeeUserName() {
        basePageSteps.onListingPage().popup().userBlock().name().should(hasText(USER_NAME));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Email юзера в попапе")
    public void shouldSeeUserEmail() {
        basePageSteps.onListingPage().popup().userBlock().email().should(hasText(USER_EMAIL));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("ID юзера в попапе")
    public void shouldSeeUserId() {
        basePageSteps.onListingPage().popup().userBlock().id().should(hasText(format("ID %s", USER_ID)));
    }

}
