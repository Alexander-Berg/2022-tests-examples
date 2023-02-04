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

import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.general.consts.GeneralFeatures.USER_MENU_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.mobile.element.Image.SRC;
import static ru.yandex.general.mock.MockCurrentUser.currentUserExample;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Epic(USER_MENU_FEATURE)
@Feature("Отображение данных юзера")
@DisplayName("Отображение аватара юзера")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralMobileWebModule.class)
public class UserMenuUserBlockAvatarTest {

    private static final String AVATAR_LINK = "https://avatars.mdst.yandex.net/get-yapic/1450/wSPcK5KpK2UKWEnj2Al8V1h88-1/islands";
    private static final String DUMMY_IMG_LINK = "https://avatars.mds.yandex.net/get-yapic/0/0-0/islands";

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
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Аватар юзера в попапе")
    public void shouldSeeUserAvatar() {
        mockRule.graphqlStub(mockResponse()
                .setCurrentUser(currentUserExample().build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();

        urlSteps.testing().open();
        basePageSteps.onBasePage().header().burger().click();

        basePageSteps.onListingPage().popup().userBlock().image().should(hasAttribute(SRC, containsString(AVATAR_LINK)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Заглушка аватара юзера в попапе")
    public void shouldSeeUserAvatarDummyImg() {
        mockRule.graphqlStub(mockResponse()
                .setCurrentUser(currentUserExample().removeAvatar().build())
                .setCategoriesTemplate()
                .setRegionsTemplate()
                .build()).withDefaults().create();

        urlSteps.testing().open();
        basePageSteps.onBasePage().header().burger().click();

        basePageSteps.onListingPage().popup().userBlock().image().should(hasAttribute(SRC, containsString(DUMMY_IMG_LINK)));
    }

}
