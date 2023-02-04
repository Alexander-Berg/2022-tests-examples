package ru.auto.tests.bem.catalogNew;

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
import ru.auto.tests.desktop.step.CatalogPageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CATALOG_NEW;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.SPECIFICATIONS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.page.CatalogNewPage.MARK;
import static ru.auto.tests.desktop.page.CatalogNewPage.MODEL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
@DisplayName("Каталог - карточка модели - контент ссылок")
@Epic(CATALOG_NEW)
@Feature("Блок конфигурации")
public class ConfiguraionBlockTest {

    private static final String MODIFICATION = "21745628/21745698";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public CatalogPageSteps catalogPageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase())
                .path(SPECIFICATIONS).open();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «х с пробегом»")
    public void shouldClickButton() {
        catalogPageSteps.onCatalogNewPage().specificationContentBlock().getBlock(0).buttonContains(" с пробегом")
                .waitUntil(isDisplayed()).click();

        urlSteps.testing().path(MOSKVA).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase())
                .path(MODIFICATION).path(USED).shouldNotSeeDiff();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по ссылке на «Все характеристики»")
    public void shouldClickLink() {
        catalogPageSteps.onCatalogNewPage().specificationContentBlock().getBlock(0).configurationLink()
                .waitUntil(isDisplayed()).click();

        urlSteps.testing().path(CATALOG).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase())
                .path(MODIFICATION).path(SPECIFICATIONS).shouldNotSeeDiff();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по тумбе с фото")
    public void shouldClickPhoto() {
        catalogPageSteps.onCatalogNewPage().specificationContentBlock().getBlock(0).photo()
                .waitUntil(isDisplayed()).click();

        urlSteps.testing().path(CATALOG).path(CARS).path(MARK.toLowerCase()).path(MODEL.toLowerCase())
                .path(MODIFICATION).path(SLASH).shouldNotSeeDiff();
    }

}
