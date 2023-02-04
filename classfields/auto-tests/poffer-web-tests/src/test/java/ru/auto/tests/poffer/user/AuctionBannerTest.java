package ru.auto.tests.poffer.user;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.gson.JsonObject;
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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.poffer.BetaPofferSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.AUCTION_BANNER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BETA_POFFER;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.BETA;
import static ru.auto.tests.desktop.consts.Pages.C2B_AUCTION;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.element.poffer.beta.BetaAuctionBanner.ACCEPT;
import static ru.auto.tests.desktop.element.poffer.beta.BetaAuctionBanner.SIGN_UP_FOR_INSPECTION;
import static ru.auto.tests.desktop.mock.MockC2BApplicationInfo.c2bApplicationInfoExample;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserDraft.userDraftExample;
import static ru.auto.tests.desktop.mock.Paths.C2B_AUCTION_APPLICATION_CARS;
import static ru.auto.tests.desktop.mock.Paths.USER_DRAFT_CARS;
import static ru.auto.tests.desktop.mock.Paths.USER_DRAFT_CARS_ID_C2B_APPLICATION_INFO;
import static ru.auto.tests.desktop.step.CookieSteps.EXP_AUTORUFRONT_20574;
import static ru.auto.tests.desktop.utils.Utils.getRandomBetween;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@DisplayName("Тесты на баннер аукциона")
@Feature(BETA_POFFER)
@Story(AUCTION_BANNER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class AuctionBannerTest {

    private static final String DRAFT_ID = "4848705651719180864-7ac6416a";

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

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),

                stub().withGetDeepEquals(USER_DRAFT_CARS)
                        .withResponseBody(userDraftExample().getBody()),

                stub().withGetDeepEquals(format(USER_DRAFT_CARS_ID_C2B_APPLICATION_INFO, DRAFT_ID))
                        .withResponseBody(c2bApplicationInfoExample().setCanApply(true).getBody())
        ).create();

        cookieSteps.setExpFlags(EXP_AUTORUFRONT_20574);

        urlSteps.testing().path(CARS).path(USED).path(ADD).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Текст баннера аукциона")
    public void shouldSeeAuctionBannerText() {
        pofferSteps.onBetaPofferPage().auctionBanner().should(isDisplayed())
                .should(hasText("Авто.ру Выкуп\nПроведём осмотр, где вам удобно, и назовём финальную цену.\n" +
                        "Это бесплатно и не обязывает продавать автомобиль.\nУзнать подробности\n" +
                        "Предварительная оценка\n1 541 900 — 2 261 000 ₽\nСрок продажи\nдо 3 дней\n" +
                        "Специалист приедет к вам и проведет осмотр\nНазовем точную цену за авто после осмотра\n" +
                        "Организуем сделку с покупателем\nУслуга полностью бесплатна для вас\n" +
                        "Проводим осмотры в пределах 20 км от МКАД в будни с 9:00 до 21:00\nМенеджер позвонит на " +
                        "этот номер\nЗаписаться на осмотр\nПринимаю правила и даю согласие на обработку " +
                        "персональных данных\nОбъявление не будет опубликовано на Авто.ру, пока проходит Выкуп."));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Не отображается баннер аукциона")
    public void shouldNotSeeAuctionBanner() {
        mockRule.overwriteStub(2, stub().withGetDeepEquals(format(USER_DRAFT_CARS_ID_C2B_APPLICATION_INFO, DRAFT_ID))
                .withResponseBody(c2bApplicationInfoExample().setCanApply(false).getBody()));
        urlSteps.refresh();

        pofferSteps.onBetaPofferPage().auctionBanner().should(not(isDisplayed()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Кнопка «Записаться на осмотр» в банере задизейблена без согласия с правилами")
    public void shouldSeeDisabledSignUpButton() {
        pofferSteps.onBetaPofferPage().auctionBanner().checkboxChecked(ACCEPT).should(not(isDisplayed()));
        pofferSteps.onBetaPofferPage().auctionBanner().button(SIGN_UP_FOR_INSPECTION).should(not(isEnabled()));
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @Category({Regression.class, Testing.class})
    @DisplayName("Открывается страница заявки на осмотр при клике на «Записаться на осмотр» в банере")
    public void shouldGoToApplicationPage() {
        String applicationId = String.valueOf(getRandomBetween(1000, 5000));

        JsonObject responseBody = new JsonObject();
        responseBody.addProperty("application_id", applicationId);

        mockRule.setStubs(
                stub().withPostDeepEquals(format("%s/%s", C2B_AUCTION_APPLICATION_CARS, DRAFT_ID))
                        .withResponseBody(responseBody)
        ).update();

        pofferSteps.onBetaPofferPage().auctionBanner().checkboxContains(ACCEPT).click();
        pofferSteps.onBetaPofferPage().auctionBanner().checkboxChecked(ACCEPT).waitUntil(isDisplayed());

        pofferSteps.onBetaPofferPage().auctionBanner().button(SIGN_UP_FOR_INSPECTION).waitUntil(isEnabled()).click();

        urlSteps.testing().path(CARS).path(USED).path(ADD).path(C2B_AUCTION).path(applicationId).path(SLASH)
                .shouldNotSeeDiff();
    }

}
