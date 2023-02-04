package ru.auto.tests.desktop.dealersdistributor;

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
import org.openqa.selenium.Keys;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DEALERS_DISTRIBUTOR;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@DisplayName("Подбор дилера - фильтр по марке, модели, поколению")
@Feature(DEALERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class FilterTest {

    private static final String MARK = "LADA (ВАЗ)";
    private static final String MARK_CODE = "vaz";
    private static final String MODEL = "Vesta";
    private static final String GENERATION = "I";
    private static final String GENERATION_CODE = "20417749";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(DEALERS_DISTRIBUTOR).path(CARS).path(NEW).open();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class})
    @DisplayName("Выбор марки и модели")
    public void shouldSelectMarkAndModel() {
        basePageSteps.onDealerDisturbutorPage().filter().selectItem("Марка", MARK);
        basePageSteps.onDealerDisturbutorPage().filter().selectItem("Модель", MODEL);
        basePageSteps.onDealerDisturbutorPage().filter().select(MARK).waitUntil(isDisplayed());
        basePageSteps.onDealerDisturbutorPage().filter().select(MODEL).waitUntil(isDisplayed());
        basePageSteps.onDealerDisturbutorPage().filter().resultsButton().waitUntil(isDisplayed()).click();

        urlSteps.testing().path(MOSKVA).path(DEALERS_DISTRIBUTOR).path(CARS).path(NEW)
                .path(MARK_CODE).path(MODEL.toLowerCase()).path("/").shouldNotSeeDiff();

        basePageSteps.onDealerDisturbutorPage().dealerList().waitUntil(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class})
    @DisplayName("Выбор поколения")
    public void shouldSelectGeneration() {
        basePageSteps.onDealerDisturbutorPage().filter().selectItem("Марка", MARK);
        basePageSteps.onDealerDisturbutorPage().filter().select("Модель").waitUntil(isEnabled());
        basePageSteps.onDealerDisturbutorPage().filter().selectItem("Модель", MODEL);
        basePageSteps.onDealerDisturbutorPage().filter().select("Поколение").selectButton()
                .waitUntil(isEnabled()).click();
        basePageSteps.onDealerDisturbutorPage().filter().generationsPopup().generationItem(GENERATION)
                .waitUntil(isDisplayed()).click();
        basePageSteps.onDealerDisturbutorPage().body().sendKeys(Keys.ESCAPE);
        basePageSteps.onDealerDisturbutorPage().filter().select(GENERATION).waitUntil(isDisplayed());
        basePageSteps.onDealerDisturbutorPage().filter().resultsButton().waitUntil(isDisplayed()).click();

        urlSteps.testing().path(MOSKVA).path(DEALERS_DISTRIBUTOR).path(CARS).path(NEW).path(MARK_CODE)
                .path(MODEL.toLowerCase()).path(GENERATION_CODE).path("/").shouldNotSeeDiff();

        basePageSteps.onDealerDisturbutorPage().dealerList().waitUntil(hasSize(greaterThan(0)));
    }
}