package ru.yandex.general.sellerProfile;

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
import ru.yandex.general.mock.MockResponse;
import ru.yandex.general.module.GeneralWebModule;
import ru.yandex.general.rules.MockRule;
import ru.yandex.general.step.BasePageSteps;
import ru.yandex.general.step.PassportSteps;
import ru.yandex.general.step.UrlSteps;

import static org.hamcrest.Matchers.not;
import static ru.yandex.general.consts.GeneralFeatures.PROFILE_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.MY;
import static ru.yandex.general.consts.Pages.OFFERS;
import static ru.yandex.general.consts.Pages.PROFILE;
import static ru.yandex.general.mock.MockPublicProfile.profileResponse;
import static ru.yandex.general.mock.MockResponse.mockResponse;
import static ru.yandex.general.page.PublicProfilePage.EDIT;
import static ru.yandex.general.page.PublicProfilePage.SUBSCRIBE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(PROFILE_FEATURE)
@Feature("Страница профиля для владельца")
@DisplayName("Страница профиля для владельца")
@RunWith(GuiceTestRunner.class)
@GuiceModules(GeneralWebModule.class)
public class SellerProfileOwnerTest {

    private static final String OWNER_PUBLIC_ID = "3qpdmk0crc5xn1cu9qvam5r01c";
    private static final String PUBLIC_ID = "j12j41h24j2h14j112";

    private MockResponse mockResponse = mockResponse()
            .setCurrentUserExample()
            .setCategoriesTemplate()
            .setRegionsTemplate();

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private PassportSteps passportSteps;

    @Before
    public void before() {
        basePageSteps.setMoscowCookie();
        passportSteps.commonAccountLogin();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Жмём на «Редактировать» - проверяем ссылку")
    public void shouldSeeUrlAfterEditClickOwner() {
        mockRule.graphqlStub(mockResponse
                .setPublicProfile(profileResponse().build())
                .build()).withDefaults().create();
        urlSteps.testing().path(PROFILE).path(OWNER_PUBLIC_ID).open();
        basePageSteps.onProfilePage().sidebar().button(EDIT).click();

        urlSteps.testing().path(MY).path(OFFERS).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет «Редактировать» для не владельца")
    public void shouldSeeNoEditForNotOwner() {
        mockRule.graphqlStub(mockResponse
                .setPublicProfile(profileResponse().build())
                .build()).withDefaults().create();
        urlSteps.testing().path(PROFILE).path(PUBLIC_ID).open();

        basePageSteps.onProfilePage().sidebar().button(EDIT).should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Нет «Подписаться» для владельца")
    public void shouldSeeNoSubscribeForOwner() {
        mockRule.graphqlStub(mockResponse
                .setPublicProfile(profileResponse().build())
                .build()).withDefaults().create();
        urlSteps.testing().path(PROFILE).path(OWNER_PUBLIC_ID).open();

        basePageSteps.onProfilePage().sidebar().button(SUBSCRIBE).should(not(isDisplayed()));
    }

}
