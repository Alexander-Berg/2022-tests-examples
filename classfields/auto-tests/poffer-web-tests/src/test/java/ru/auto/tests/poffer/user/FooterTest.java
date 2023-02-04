package ru.auto.tests.poffer.user;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.poffer.BetaPofferSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.BETA_POFFER;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.consts.Urls.YANDEX_SUPPORT_AUTORU_LEGAL_TERMS_OF_SERVICE;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserDraft.userDraftExample;
import static ru.auto.tests.desktop.mock.Paths.USER_DRAFT_CARS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Частник - подвал")
@Feature(BETA_POFFER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class FooterTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BetaPofferSteps pofferSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),

                stub().withGetDeepEquals(USER_DRAFT_CARS)
                        .withResponseBody(userDraftExample().getBody())
        ).create();

        urlSteps.testing().path(CARS).path(USED).path(ADD).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Должны видеть подвал")
    public void shouldSeeFooter() {
        pofferSteps.onBetaPofferPage().footer()
                .should(hasText("© 1996–2022 ООО «Яндекс.Вертикали»\nПользовательское соглашение"));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ссылка на Вконтакте")
    public void shouldClickVkUrl() {
        pofferSteps.onBetaPofferPage().footer().vkUrl().waitUntil(isDisplayed())
                .waitUntil(hasAttribute("href", "https://vk.com/autoru_news"))
                .click();

        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ссылка на Одноклассники")
    public void shouldClickOkUrl() {
        pofferSteps.onBetaPofferPage().footer().okUrl().waitUntil(isDisplayed())
                .waitUntil(hasAttribute("href", "https://ok.ru/group/52852027556005"))
                .click();

        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ссылка на YouTube")
    public void shouldClickYouTubeUrl() {
        pofferSteps.onBetaPofferPage().footer().youTubeUrl().waitUntil(isDisplayed())
                .waitUntil(hasAttribute("href", "https://www.youtube.com/user/AutoRuTv"))
                .click();

        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ссылка на Яндекс")
    public void shouldClickYandexUrl() {
        pofferSteps.onBetaPofferPage().footer().yandexUrl().waitUntil(isDisplayed())
                .waitUntil(hasAttribute("href", "https://www.yandex.ru/"))
                .click();

        urlSteps.fromUri("https://yandex.ru/").shouldNotSeeDiff();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Ссылка на Пользовательское соглашение")
    public void shouldClickTermOfServiceUrl() {
        pofferSteps.onBetaPofferPage().footer().button("Пользовательское соглашение").waitUntil(isDisplayed())
                .click();

        urlSteps.switchToNextTab();
        urlSteps.fromUri(YANDEX_SUPPORT_AUTORU_LEGAL_TERMS_OF_SERVICE).shouldNotSeeDiff();
    }
}
