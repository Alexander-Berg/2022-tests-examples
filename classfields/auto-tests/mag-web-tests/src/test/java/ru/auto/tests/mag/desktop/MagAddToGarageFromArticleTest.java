package ru.auto.tests.mag.desktop;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAG;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ARTICLE;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_MAG;
import static ru.auto.tests.desktop.element.mag.AddToGarageBubble.ADD;
import static ru.auto.tests.desktop.element.mag.AddToGarageBubble.GO_TO_GARAGE;
import static ru.auto.tests.desktop.element.mag.AddToGarageBubble.LATER;
import static ru.auto.tests.desktop.mock.MockGarageCard.garageArticleCard;
import static ru.auto.tests.desktop.mock.MockGarageCards.garageCards;
import static ru.auto.tests.desktop.mock.MockGarageCards.getGarageCardsRequest;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.GARAGE_USER_CARD;
import static ru.auto.tests.desktop.mock.Paths.GARAGE_USER_CARDS;
import static ru.auto.tests.desktop.mock.Paths.GARAGE_USER_CARD_ARTICLE;
import static ru.auto.tests.desktop.mock.beans.error.Error.error;
import static ru.auto.tests.desktop.utils.Utils.getJsonObject;
import static ru.auto.tests.desktop.utils.Utils.getRandomId;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Добавление в гараж авто из статьи")
@Epic(MAG)
@Feature("Добавление в гараж авто из статьи")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class MagAddToGarageFromArticleTest {

    private static final String ARTICLE_PATH = "mercedesbenz-sklassa-kotoryy-polzhizni-prostoyal-pod-zaborom-o-chyom-ne-rasskazhet-prodavec";
    private static final String BUBBLE_TEXT = "Нравится Mercedes-Benz S-Класс?\nДобавь в Гараж мечты и узнай про неё больше!\nДобавить\nПозже";
    private static final String BUBBLE_ADDED_TEXT = "Автомобиль Mercedes-Benz S-Класс успешно добавлен\nПерейти в Гараж\nПозже";

    private static final String GARAGE_CARD_ID = getRandomId();

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void Before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/MagazineMercedesPost")
        );

        urlSteps.subdomain(SUBDOMAIN_MAG).path(ARTICLE).path(ARTICLE_PATH);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Отображается бабл, проскролив экран наполовину")
    public void shouldSeeBubble() {
        mockRule.setStubs(
                stub().withGetDeepEquals(format("%s/%s", GARAGE_USER_CARD_ARTICLE, ARTICLE_PATH))
                        .withResponseBody(
                                garageArticleCard().setStatusSuccess().getBody())
        ).create();

        urlSteps.open();

        basePageSteps.onMagPage().addToGarageIcon().waitUntil(isDisplayed());
        basePageSteps.scrollByHeightPercent(50);

        basePageSteps.onMagPage().addToGarageBubble().waitUntil(isDisplayed()).should(hasText(BUBBLE_TEXT));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается бабл, при недостаточном скроле экрана")
    public void shouldNotSeeBubbleWithoutScrollEnough() {
        mockRule.setStubs(
                stub().withGetDeepEquals(format("%s/%s", GARAGE_USER_CARD_ARTICLE, ARTICLE_PATH))
                        .withResponseBody(
                                garageArticleCard().setStatusSuccess().getBody())
        ).create();

        urlSteps.open();

        basePageSteps.onMagPage().addToGarageIcon().waitUntil(isDisplayed());
        basePageSteps.scrollByHeightPercent(45);

        basePageSteps.onMagPage().addToGarageBubble().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Не отображается иконка добавления в гараж и бабл, без карточки гаража в ответе ручки")
    public void shouldNotSeeBubbleWithoutCardInResponse() {
        mockRule.setStubs(
                stub().withGetDeepEquals(format("%s/%s", GARAGE_USER_CARD_ARTICLE, ARTICLE_PATH))
                        .withResponseBody(
                                getJsonObject(
                                        error().setError("ARTICLE_NOT_FOUND")
                                                .setStatus("ERROR")
                                                .setDetailedError(format("Bad request, article_id: %s", ARTICLE_PATH))))
        ).create();

        urlSteps.open();

        basePageSteps.onMagPage().addToGarageIcon().should(not(isDisplayed()));

        basePageSteps.scrollByHeightPercent(50);

        basePageSteps.onMagPage().addToGarageBubble().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Жмём «Позже» в бабле, бабл закрывается")
    public void shouldCloseBubbleByLater() {
        mockRule.setStubs(
                stub().withGetDeepEquals(format("%s/%s", GARAGE_USER_CARD_ARTICLE, ARTICLE_PATH))
                        .withResponseBody(
                                garageArticleCard().setStatusSuccess().getBody())
        ).create();

        urlSteps.open();

        basePageSteps.onMagPage().addToGarageIcon().waitUntil(isDisplayed()).click();
        basePageSteps.onMagPage().addToGarageBubble().button(LATER).waitUntil(isDisplayed()).click();

        basePageSteps.onMagPage().addToGarageIcon().should(isDisplayed());
        basePageSteps.onMagPage().addToGarageBubble().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Добавляем авто в гараж через бабл, проверяем текст бабла")
    public void shouldAddToGarageFromBubble() {
        mockRule.setStubs(
                stub().withGetDeepEquals(format("%s/%s", GARAGE_USER_CARD_ARTICLE, ARTICLE_PATH))
                        .withResponseBody(
                                garageArticleCard().setStatusSuccess().getBody()),

                stub().withPostDeepEquals(GARAGE_USER_CARD)
                        .withRequestBody(
                                garageArticleCard().setAddedManually(true).getBody())
                        .withResponseBody(
                                garageArticleCard().setStatusSuccess().getBody())
        ).create();

        urlSteps.open();

        basePageSteps.onMagPage().addToGarageIcon().waitUntil(isDisplayed()).click();
        basePageSteps.onMagPage().addToGarageBubble().button(ADD).waitUntil(isDisplayed()).click();

        basePageSteps.onMagPage().addToGarageBubble().should(hasText(BUBBLE_ADDED_TEXT));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переходим в гараж, через кнопку в бабле, после добавления авто")
    public void shouldGoToGarageFromBubble() {
        mockRule.setStubs(
                stub().withGetDeepEquals(format("%s/%s", GARAGE_USER_CARD_ARTICLE, ARTICLE_PATH))
                        .withResponseBody(
                                garageArticleCard().setStatusSuccess().getBody()),

                stub().withPostDeepEquals(GARAGE_USER_CARD)
                        .withRequestBody(
                                garageArticleCard().setAddedManually(true).getBody())
                        .withResponseBody(
                                garageArticleCard()
                                        .setId(GARAGE_CARD_ID)
                                        .setStatusSuccess().getBody())
        ).create();

        urlSteps.open();

        basePageSteps.onMagPage().addToGarageIcon().waitUntil(isDisplayed()).click();
        basePageSteps.onMagPage().addToGarageBubble().button(ADD).waitUntil(isDisplayed()).click();

        mockRule.setStubs(
                stub().withPostDeepEquals(GARAGE_USER_CARDS)
                        .withRequestBody(
                                getGarageCardsRequest())
                        .withResponseBody(
                                garageCards().setCards(
                                        garageArticleCard().setId(GARAGE_CARD_ID)).build()),

                stub().withGetDeepEquals(format("%s/%s", GARAGE_USER_CARD, GARAGE_CARD_ID))
                        .withResponseBody(
                                garageArticleCard().setId(GARAGE_CARD_ID).setStatusSuccess().getBody())
        ).update();

        basePageSteps.onMagPage().addToGarageBubble().button(GO_TO_GARAGE).waitUntil(isDisplayed()).click();

        basePageSteps.onGarageCardPage().h1().should(hasText("Mercedes-Benz S-Класс"));
        urlSteps.testing().path(GARAGE).path(GARAGE_CARD_ID).path(SLASH).shouldNotSeeDiff();
    }

}
