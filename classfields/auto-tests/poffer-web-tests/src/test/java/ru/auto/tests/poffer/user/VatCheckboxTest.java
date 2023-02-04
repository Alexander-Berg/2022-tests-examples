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
import ru.auto.tests.desktop.models.Offer;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.poffer.BetaPofferSteps;

import javax.inject.Inject;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.POFFER;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.element.poffer.beta.BetaPriceBlock.VAT;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.MockUserDraft.userDraftExample;
import static ru.auto.tests.desktop.mock.Paths.USER_DRAFT_CARS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Частник - чекбокс НДС")
@Feature(POFFER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class VatCheckboxTest {

    private static final String ADDITIONAL_DESCRIPTION = " НДС";

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
    private Offer offer;

    @Before
    public void before() throws IOException {
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
    @DisplayName("Показ и скрытие чекбокса «Учитывать НДС»")
    public void shouldShowAndHideVatCheckbox() {
        offer.setDescription(ADDITIONAL_DESCRIPTION);
        pofferSteps.enterDescription();

        pofferSteps.onBetaPofferPage().priceBlock().checkbox(VAT)
                .should(isDisplayed())
                .should(hasClass(containsString("Checkbox_checked")));

        pofferSteps.clearDescription();

        pofferSteps.onBetaPofferPage().priceBlock().checkbox(VAT).should(not(isDisplayed()));
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Описание НДС в попапе")
    public void shouldSeeDescriptionInPopup() {
        offer.setDescription(ADDITIONAL_DESCRIPTION);
        pofferSteps.enterDescription();
        pofferSteps.onBetaPofferPage().priceBlock().vatInfoIcon().hover();

        pofferSteps.onBetaPofferPage().popup().should(hasText("Выберите эту опцию, если будете готовы " +
                "предоставить покупателю-юрлицу счёт-фактуру для вычета НДС."));
    }
}
