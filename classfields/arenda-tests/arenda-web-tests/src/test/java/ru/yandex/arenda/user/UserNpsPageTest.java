package ru.yandex.arenda.user;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.arenda.module.ArendaWebModule;
import ru.yandex.arenda.steps.CompareSteps;
import ru.yandex.arenda.steps.LkSteps;
import ru.yandex.arenda.steps.PassportSteps;
import ru.yandex.arenda.steps.RetrofitApiSteps;
import ru.yandex.arenda.steps.UrlSteps;
import ru.yandex.qatools.ashot.Screenshot;

import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static ru.yandex.arenda.constants.UriPath.LK;
import static ru.yandex.arenda.pages.NpsPage.RATE_BUTTON;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Link("https://st.yandex-team.ru/VERTISTEST-2148")
@DisplayName("Тесты на форму подачи рекомендаций")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class UserNpsPageTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private Account account;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private LkSteps lkSteps;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        String uid = account.getId();
        retrofitApiSteps.createUser(uid);
        passportSteps.login(account);
        urlSteps.testing().path(LK).path("/nps/");
    }

    @Test
    @DisplayName("Видим скриншот страницы")
    public void shouldSeeNpsPageScreenshot() {
        compareSteps.resize(1920, 3000);
        urlSteps.open();
        Screenshot testing = compareSteps.takeScreenshot(lkSteps.onNpsPage().root());
        urlSteps.setProductionHost().open();
        Screenshot production = compareSteps.takeScreenshot(lkSteps.onNpsPage().root());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }

    @Test
    @DisplayName("Видим ошибку если не поставили оценку")
    public void shouldSeeErrorDescription() {
        urlSteps.open();
        lkSteps.onNpsPage().errorDescription().should(not(isDisplayed()));
        lkSteps.onNpsPage().button(RATE_BUTTON).click();
        lkSteps.onNpsPage().errorDescription().should(hasText("Пожалуйста, сначала поставьте оценку"));
    }

    @Test
    @DisplayName("Видим ошибку если попытаться оценить без квартиры")
    public void shouldSeeErrorToastRating() {
        urlSteps.open();
        lkSteps.onNpsPage().rating("10").click();
        lkSteps.onNpsPage().button(RATE_BUTTON).click();
        lkSteps.onNpsPage().errorToast();
        lkSteps.onNpsPage().toast()
                .waitUntil(hasText(containsString("Оценка доступна только для собственника или жильца")));
    }

    @Test
    @DisplayName("Видим успешную оценку если есть квартира")
    public void shouldSeeSuccessRating() {
        retrofitApiSteps.createConfirmedFlat(account.getId());
        urlSteps.open();
        final String randomRating = String.valueOf(nextInt(1, 11));
        lkSteps.onNpsPage().rating(randomRating).click();
        lkSteps.onNpsPage().textAreaId("COMMENT").sendKeys("отличный сервис тест");
        lkSteps.onNpsPage().button(RATE_BUTTON).click();
        String toastText = lkSteps.onNpsPage().toastSuccess().should(isDisplayed()).getText();
        assertThat(toastText).isEqualTo("Спасибо за оценку!\nНам это очень важно.");
    }

    @Test
    @DisplayName("Видим что страница с «only-content=1» открывается")
    public void shouldSeeOnlyContentPage() {
        urlSteps.queryParam("only-content", "1").open();
        lkSteps.onNpsPage().h2().should(hasText("Оцените готовность рекомендовать нас друзьям"));
    }
}
