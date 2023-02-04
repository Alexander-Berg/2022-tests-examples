package ru.auto.tests.desktop.garage;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.ALEKS_IVANOV;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.DREAMCAR;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.element.garage.CardForm.BODY_TYPE;
import static ru.auto.tests.desktop.element.garage.CardForm.CHOOSE_COLOR;
import static ru.auto.tests.desktop.element.garage.CardForm.CHOOSE_MARK;
import static ru.auto.tests.desktop.element.garage.CardForm.CHOOSE_MODEL;
import static ru.auto.tests.desktop.element.garage.CardForm.GENERATION;
import static ru.auto.tests.desktop.element.garage.CardForm.MARK;
import static ru.auto.tests.desktop.element.garage.CardForm.MODEL;
import static ru.auto.tests.desktop.element.garage.CardForm.MODIFICATION;
import static ru.auto.tests.desktop.element.garage.CardFormBlock.BLACK;
import static ru.auto.tests.desktop.element.garage.DreamCarBlock.CHOOSE_DREAM_CAR;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Добавление машины мечты в гараж")
@Epic(AutoruFeatures.GARAGE)
@Feature(AutoruFeatures.DREAM_CAR)
@Story("Добавление машины мечты в гараж")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class GarageAddDreamCarTest {

    private static final String AUDI = "Audi";
    private static final String A3 = "A3";
    private static final String SEDAN = "Седан";
    private static final String III = "2016 - 2020 III (8V) Рестайлинг";
    private static final String HP_228 = "2.0 AMT (228 л.с.) 2018 - 2020";
    private static final String DREAM_CAR_PATH = "/1146321503/";
    private static final String DISABLED = "disabled";
    private static final String TRUE = "true";
    private static final String GET_DREAM_CAR = "desktop/GarageUserCardDreamCarGetAudi_A3_23172670";

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

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/SearchCarsBreadcrumbs",
                "desktop/ReferenceCatalogCarsSuggest").post();

        urlSteps.testing().path(GARAGE).path(ADD).path(DREAMCAR).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Жмём на «Выбрать машину мечты»")
    public void shouldChooseDreamCarClick() {
        urlSteps.testing().path(GARAGE).path(ADD).open();
        basePageSteps.onGarageAddPage().dreamCarBlock().button(CHOOSE_DREAM_CAR).click();

        basePageSteps.onGarageCardPage().form().block(CHOOSE_MARK).shouldBeOpened();
        urlSteps.path(DREAMCAR).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем марку")
    public void shouldChooseDreamCarMark() {
        chooseMark();

        basePageSteps.onGarageCardPage().form().block(CHOOSE_MODEL).shouldBeOpened();
        basePageSteps.onGarageCardPage().form().block(CHOOSE_COLOR).should(not(isDisplayed()));
        basePageSteps.onGarageCardPage().form().block(format("%s%s", MARK, AUDI)).shouldBeMinimized();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем модель")
    public void shouldChooseDreamCarModel() {
        chooseMark();
        chooseModel();

        basePageSteps.onGarageCardPage().form().block(GENERATION).shouldBeOpened();
        basePageSteps.onGarageCardPage().form().block(CHOOSE_COLOR).shouldBeOpened();
        basePageSteps.onGarageCardPage().form().block(format("%s%s", MODEL, A3)).shouldBeMinimized();
        basePageSteps.onGarageCardPage().form().block(MODIFICATION).should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем поколение")
    public void shouldChooseDreamCarGeneration() {
        chooseMark();
        chooseModel();
        chooseGeneration();

        basePageSteps.onGarageCardPage().form().block(BODY_TYPE).shouldBeOpened();
        basePageSteps.onGarageCardPage().form().block(CHOOSE_COLOR).shouldBeOpened();
        basePageSteps.onGarageCardPage().form().block(format("%s%s", GENERATION, III)).shouldBeMinimized();
        basePageSteps.onGarageCardPage().form().block(MODIFICATION).shouldBeMinimized();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем тип кузова")
    public void shouldChooseDreamCarBodyType() {
        chooseMark();
        chooseModel();
        chooseGeneration();
        chooseBodyType();

        basePageSteps.onGarageCardPage().form().block(MODIFICATION).shouldBeOpened();
        basePageSteps.onGarageCardPage().form().block(CHOOSE_COLOR).shouldBeOpened();
        basePageSteps.onGarageCardPage().form().block(format("%s%s", BODY_TYPE, SEDAN)).shouldBeMinimized();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Выбираем модификацию")
    public void shouldChooseDreamCarModification() {
        chooseMark();
        chooseModel();
        chooseGeneration();
        chooseBodyType();
        chooseModification();

        basePageSteps.onGarageCardPage().form().block(CHOOSE_COLOR).shouldBeOpened();
        basePageSteps.onGarageCardPage().form().block(format("%s%s", MODIFICATION, HP_228)).shouldBeMinimized();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сохраняем с заполнением до модификации, без цвета")
    public void shouldSaveToModificationWithoutColor() {
        chooseMark();
        chooseModel();
        chooseGeneration();
        chooseBodyType();
        chooseModification();

        mockRule.with("desktop/GarageUserCardDreamCarToModificationWithoutColorPost", GET_DREAM_CAR).update();
        basePageSteps.onGarageCardPage().form().submitButton().click();

        urlSteps.testing().path(GARAGE).path(DREAM_CAR_PATH).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сохраняем с заполнением до модификации, с цветом")
    public void shouldSaveToModificationWithColor() {
        chooseMark();
        chooseModel();
        chooseGeneration();
        chooseBodyType();
        chooseModification();
        basePageSteps.onGarageCardPage().form().block(CHOOSE_COLOR).color(BLACK).click();

        mockRule.with("desktop/GarageUserCardDreamCarToModificationWithColorPost", GET_DREAM_CAR).update();
        basePageSteps.onGarageCardPage().form().submitButton().click();

        urlSteps.testing().path(GARAGE).path(DREAM_CAR_PATH).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сохраняем с заполнением до поколения, без цвета")
    public void shouldSaveToGenerationWithoutColor() {
        chooseMark();
        chooseModel();
        chooseGeneration();

        mockRule.with("desktop/GarageUserCardDreamCarToGenerationWithoutColorPost", GET_DREAM_CAR).update();
        basePageSteps.onGarageCardPage().form().submitButton().click();

        urlSteps.testing().path(GARAGE).path(DREAM_CAR_PATH).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Сохраняем с заполнением до поколения, с цветом")
    public void shouldSaveToGenerationWithColor() {
        chooseMark();
        chooseModel();
        chooseGeneration();
        basePageSteps.onGarageCardPage().form().block(CHOOSE_COLOR).color(BLACK).click();

        mockRule.with("desktop/GarageUserCardDreamCarToGenerationWithColorPost", GET_DREAM_CAR).update();
        basePageSteps.onGarageCardPage().form().submitButton().click();

        urlSteps.testing().path(GARAGE).path(DREAM_CAR_PATH).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Сохранить» задизейблена на этапе выбора марки")
    public void shouldSeeDisabledSubmitOnMarkStep() {
        basePageSteps.onGarageCardPage().form().submitButton().should(hasAttribute(DISABLED, TRUE));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Сохранить» задизейблена на этапе выбора модели")
    public void shouldSeeDisabledSubmitOnModelStep() {
        chooseMark();
        basePageSteps.onGarageCardPage().form().block(CHOOSE_MODEL).waitUntil(isDisplayed());

        basePageSteps.onGarageCardPage().form().submitButton().should(hasAttribute(DISABLED, TRUE));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(ALEKS_IVANOV)
    @DisplayName("Кнопка «Сохранить» задизейблена на этапе выбора поколения")
    public void shouldSeeDisabledSubmitOnGenerationStep() {
        chooseMark();
        chooseModel();
        basePageSteps.onGarageCardPage().form().block(GENERATION).waitUntil(isDisplayed());

        basePageSteps.onGarageCardPage().form().submitButton().should(hasAttribute(DISABLED, TRUE));
    }

    private void chooseMark() {
        mockRule.overwriteStub(1, "desktop/SearchCarsBreadcrumbsAudi");
        mockRule.overwriteStub(2, "desktop/ReferenceCatalogCarsSuggestAudi");
        basePageSteps.onGarageCardPage().form().block(CHOOSE_MARK).item(AUDI)
                .waitUntil(isDisplayed()).click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
    }

    private void chooseModel() {
        mockRule.overwriteStub(1, "desktop/SearchCarsBreadcrumbsAudi_A3");
        mockRule.overwriteStub(2, "desktop/ReferenceCatalogCarsSuggestAudiA3");
        basePageSteps.onGarageCardPage().form().block(CHOOSE_MODEL).radioButton(A3)
                .waitUntil(isDisplayed()).click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
    }

    private void chooseGeneration() {
        mockRule.overwriteStub(1, "desktop/SearchCarsBreadcrumbsAudi_A3_20785010");
        mockRule.overwriteStub(2, "desktop/ReferenceCatalogCarsSuggestAudiA3Gen20785010");
        basePageSteps.onGarageCardPage().form().block(GENERATION).radioButton(III)
                .waitUntil(isDisplayed()).click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
    }

    private void chooseBodyType() {
        mockRule.overwriteStub(1, "desktop/SearchCarsBreadcrumbsAudi_A3_20785010_2320785541");
        mockRule.overwriteStub(2, "desktop/ReferenceCatalogCarsSuggestAudiA3Gen20785010Sedan");
        basePageSteps.onGarageCardPage().form().block(BODY_TYPE).radioButton(SEDAN)
                .waitUntil(isDisplayed()).click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
    }

    private void chooseModification() {
        mockRule.overwriteStub(1, "desktop/SearchCarsBreadcrumbsAudi_A3_20785010_2320785541_23172670");
        mockRule.overwriteStub(2, "desktop/ReferenceCatalogCarsSuggestAudiA3Gen20785010Sedan23172670");
        basePageSteps.onGarageCardPage().form().block(MODIFICATION).radioButton(HP_228)
                .waitUntil(isDisplayed()).click();
        basePageSteps.waitSomething(1, TimeUnit.SECONDS);
    }

}
