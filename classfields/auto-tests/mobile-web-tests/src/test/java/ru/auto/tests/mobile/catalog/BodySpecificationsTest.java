package ru.auto.tests.mobile.catalog;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.SPECIFICATIONS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
@DisplayName("Каталог - карточка кузова - характеристики")
@Feature(AutoruFeatures.CATALOG)
public class BodySpecificationsTest {

    private static final String MARK = "Audi";
    private static final String MODEL = "Q7";
    private static final String GENERATION_ID = "21646875";
    private static final String BODY_ID = "21646934";
    private static final String OTHER_MODIFICATION_ID = "21646934__21734754";
    private static final String OTHER_MODIFICATION = "3.0 AT 340 л.c. ";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase())
                .path(GENERATION_ID).path(BODY_ID).path(SPECIFICATIONS).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Ссылка на листинг в описании модификации")
    @Category({Regression.class})
    public void shouldClickModificationListingUrl() {
        basePageSteps.onCatalogBodyPage().modificationListingUrl().waitUntil(isDisplayed()).click();
        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase())
                .path(GENERATION_ID).path(BODY_ID).path(USED).path("/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Просмотр другой модификации")
    public void shouldSeeOtherModification() {
        basePageSteps.onCatalogBodyPage().selector("Выбрать комплектацию").should(isDisplayed()).click();
        basePageSteps.onCatalogBodyPage().dropdown().item(OTHER_MODIFICATION).waitUntil(isDisplayed()).click();
        urlSteps.path(OTHER_MODIFICATION_ID).path("/").ignoreParam("cookiesync").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Ссылка в источнике данных")
    @Category({Regression.class})
    public void shouldClickProviderInfoUrl() {
        basePageSteps.onCatalogBodyPage().providerInfoUrl()
                .should(hasAttribute("href", "http://www.audi.ru/")).click();
        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }
}
