package ru.auto.tests.poffer.user;

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
import ru.auto.tests.desktop.models.Offer;
import ru.auto.tests.desktop.module.DesktopDevToolsTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.SeleniumMockSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.poffer.BetaPofferSteps;

import javax.inject.Inject;

import static org.hamcrest.CoreMatchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.BETA_POFFER;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.matchers.RequestHasBodyMatcher.pofferHasJsonBody;
import static ru.auto.tests.desktop.matchers.RequestsMatcher.onlyOneRequest;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserDraft.userDraftExample;
import static ru.auto.tests.desktop.mock.Paths.USER_DRAFT_CARS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Частник - комментарий продавца")
@Feature(BETA_POFFER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopDevToolsTestsModule.class)
public class CommentTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BetaPofferSteps pofferSteps;

    @Inject
    private SeleniumMockSteps seleniumMockSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private Offer offer;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("poffer/ReferenceCatalogCarsParseOptions"),

                stub().withGetDeepEquals(USER_DRAFT_CARS)
                        .withResponseBody(userDraftExample().getBody())
        ).create();

        urlSteps.testing().path(CARS).path(USED).path(ADD).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Выбор вариантов описания из списка")
    public void shouldSeeCommentSuggest() {
        pofferSteps.onBetaPofferPage().descriptionBlock().descriptionTextArea().click();

        pofferSteps.onBetaPofferPage().descriptionBlock().suggestions().button("Комплект зимних шин в подарок").click();
        pofferSteps.onBetaPofferPage().descriptionBlock().suggestions().button("Пройдены все ТО").click();

        pofferSteps.onBetaPofferPage().descriptionBlock().descriptionTextArea()
                .should(hasValue("После реставрации. Разобрали полностью, отреставрировали и собрали обратно.  " +
                        "Комплект зимних шин в подарок. Пройдены все ТО."));

        pofferSteps.onBetaPofferPage().descriptionBlock().suggestions().button("Комплект зимних шин в подарок")
                .waitUntil(not(isDisplayed()));
        pofferSteps.onBetaPofferPage().descriptionBlock().suggestions().button("Пройдены все ТО")
                .waitUntil(not(isDisplayed()));

        seleniumMockSteps.assertWithWaiting(onlyOneRequest(
                "/-/ajax/poffer/saveDraftFormsToPublicApi/",
                pofferHasJsonBody("drafts/user_comment_suggest.json")
        ));
    }

    @Test
    @Ignore
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Парсинг опций в описании")
    public void shouldParseOptions() {
        offer.setDescription("Панорамная крыша, бортовой компьютер");
        pofferSteps.enterDescription();

        // TODO: Вернется Вася и Юля из отпуска - надо будет разобраться почему на моках этого блока нету
        pofferSteps.onBetaPofferPage().descriptionBlock().parsedOptions()
                .should(hasText("Мы нашли 2 опции в описании. Они " +
                        "будут автоматически указаны в разделе «Опции»"));

//        pofferSteps.onPofferPage().options().checkboxChecked("Панорамная крыша / лобовое стекло")
//                .waitUntil(isDisplayed());
//        pofferSteps.onPofferPage().options().checkboxChecked("Бортовой компьютер").waitUntil(isDisplayed());
//        pofferSteps.onPofferPage().options().radioButton("Выбранные (2)").waitUntil(isDisplayed());
//        pofferSteps.compareOffers(pofferProxySteps.getSaveDraftRequest(), "drafts/user_parsed_options.json");
    }
}
