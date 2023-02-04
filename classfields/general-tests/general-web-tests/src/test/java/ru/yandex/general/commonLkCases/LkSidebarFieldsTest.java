package ru.yandex.general.commonLkCases;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.general.mock.MockResponse;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static org.hamcrest.CoreMatchers.containsString;
import static ru.yandex.general.consts.GeneralFeatures.LK_SIDEBAR;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.CONTACTS;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.element.Image.SRC;
import static ru.yandex.general.mock.MockCurrentUser.currentUserExample;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WrapsElementMatchers.hasText;

@Epic(LK_SIDEBAR)
@DisplayName("Поля в сайдбаре ЛК")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class LkSidebarFieldsTest {

    private static final String USER_NAME = "Илон Маск";
    private static final String USER_EMAIL = "ilon@mail.ru";
    private static final String USER_ID = "495817561";
    private static final String AVATAR_URL = "https://avatars.mdst.yandex.net/get-yapic/1450/wSPcK5KpK2UKWEnj2Al8V1h88-1/";
    private static final String AVATAR_DUMMY_IMG_URL = "https://avatars.mds.yandex.net/get-yapic/0/0-0/islands-retina-50";

    private MockResponse mockResponse = mockResponse().setCategoriesTemplate()
                .setRegionsTemplate();
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
        urlSteps.testing().path(MY).path(CONTACTS);
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Имя юзера в сайдбаре")
    public void shouldSeeNameInSidebar() {
        mockRule.graphqlStub(mockResponse
                .setCurrentUser(currentUserExample().setUserName(USER_NAME).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onContactsPage().lkSidebar().userName().should(hasText(USER_NAME));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Email юзера в сайдбаре")
    public void shouldSeeEmailInSidebar() {
        mockRule.graphqlStub(mockResponse
                .setCurrentUser(currentUserExample().setUserEmail(USER_EMAIL).build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onContactsPage().lkSidebar().userEmail().should(hasText(USER_EMAIL));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Аватар юзера в сайдбаре")
    public void shouldSeeAvatarInSidebar() {
        mockRule.graphqlStub(mockResponse
                .setCurrentUser(currentUserExample().setAvatar().build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onContactsPage().lkSidebar().image().should(hasAttribute(SRC, containsString(AVATAR_URL)));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Заглушка аватара юзера в сайдбаре")
    public void shouldSeeAvatarDummyImgInSidebar() {
        mockRule.graphqlStub(mockResponse
                .setCurrentUser(currentUserExample().removeAvatar().build())
                .build()).withDefaults().create();
        urlSteps.open();

        basePageSteps.onContactsPage().lkSidebar().image().should(hasAttribute(SRC,
                containsString(AVATAR_DUMMY_IMG_URL)));
    }


}
