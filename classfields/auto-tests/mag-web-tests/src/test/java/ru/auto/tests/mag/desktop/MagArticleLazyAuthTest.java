package ru.auto.tests.mag.desktop;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAG;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.ARTICLE;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_MAG;
import static ru.auto.tests.desktop.mobile.page.MagPage.TEST_ARTICLE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Журнал - статья - комментарии")
@Feature(MAG)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class MagArticleLazyAuthTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void Before() {
        mockRule.newMock().with("desktop/AuthLoginOrRegister",
                "desktop/UserConfirm",
                "desktop/PostsArticle12132",
                "desktop/CommentsArticle12132").post();

        urlSteps.subdomain(SUBDOMAIN_MAG).path(ARTICLE).path(TEST_ARTICLE)
                .addParam("withDraftModel", "true").open();
    }

    @Test
    @Ignore
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Ленивая авторизация в блоке комментариев")
    public void shouldLazyAuthForComment() {
        basePageSteps.onMagPage().comments().commentAuthBlock().waitUntil(isDisplayed());
        basePageSteps.onMagPage().comments().commentAuthBlock().input().click();
        basePageSteps.onMagPage().comments().commentAuthBlock().input("Номер телефона", "9111111111");
        basePageSteps.onMagPage().comments().commentAuthBlock().button("Подтвердить").click();
        basePageSteps.onMagPage().comments().commentAuthBlock().input("Код из SMS", "1234");
        waitSomething(3, TimeUnit.SECONDS);
        basePageSteps.onMagPage().comments().ownerAvatar().waitUntil(isDisplayed());
    }
}