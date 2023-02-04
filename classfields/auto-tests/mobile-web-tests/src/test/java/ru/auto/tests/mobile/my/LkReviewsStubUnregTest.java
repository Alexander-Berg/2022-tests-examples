package ru.auto.tests.mobile.my;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import static ru.auto.tests.desktop.consts.AutoruFeatures.LK;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.LOGIN;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AUTH;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Мои отзывы")
@Epic(LK)
@Feature(AutoruFeatures.REVIEWS)
@Story("Заглушка под незарегом")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class LkReviewsStubUnregTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        urlSteps.testing().path(MY).path(REVIEWS).open();
    }

    @Test
    @Owner(KRISKOLU)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение заглушки")
    public void shouldSeeStub() {
        basePageSteps.onLkReviewsPage().stub().should(hasText("Поделитесь опытом\n" +
                "Расскажите о владении вашим автомобилем, мотоциклом или другим видом транспорта. Помогите другим людям " +
                "принять правильное решение.\nДобавить отзыв\nВойти"));
    }

    @Test
    @Owner(KRISKOLU)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Добавить отзыв»")
    public void shouldClickAddReviewButton() {
        basePageSteps.onLkReviewsPage().stub().button("Добавить отзыв").click();
        urlSteps.desktopURI().path(CARS).path(REVIEWS).path(ADD).shouldNotSeeDiff();
    }

    @Test
    @Owner(KRISKOLU)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Войти»")
    public void shouldClickAuthButton() {
        String currentUrl = urlSteps.getCurrentUrl();
        basePageSteps.onLkReviewsPage().stub().button("Войти").click();
        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN).addParam("r", encode(currentUrl)).shouldNotSeeDiff();
    }
}
