package ru.auto.tests.bem.catalog;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.SPECIFICATIONS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
@DisplayName("Каталог - карточка кузова - характеристики")
@Feature(AutoruFeatures.CATALOG)
public class BodySpecificationsTest {

    private static final String MARK = "hyundai";
    private static final String MODEL = "sonata";
    private static final String GENERATION = "21104772";
    private static final String BODY = "21104826";
    private static final String MODIFICATION = "21104909";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    public ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK).path(MODEL).path(GENERATION).path(BODY)
                .path(SPECIFICATIONS).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Ссылка на листинг в описании модификации")
    @Category({Regression.class})
    public void shouldClickModificationListingUrl() {
        basePageSteps.onCatalogPage().modificationListingUrl()
                .should(hasAttribute("href", urlSteps.testing().path(MOSKVA).path(CARS).path(MARK).path(MODEL)
                        .path(GENERATION).path(BODY).path(MODIFICATION).path(USED).toString()))
                .sendKeys(Keys.chord(Keys.CONTROL, Keys.RETURN));
        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Просмотр другой модификации")
    public void shouldSeeOtherModification() {
        basePageSteps.onCatalogPage().getModification(1).click();
        basePageSteps.onCatalogPage().modificationTitle()
                .should(hasText(format("Модификация %s", basePageSteps.onCatalogPage().getModification(1)
                        .getText())));
    }
}