package ru.auto.tests.bem.catalogNew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
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
import static ru.auto.tests.desktop.consts.Pages.SPECIFICATIONS;
import static ru.auto.tests.desktop.page.CatalogNewPage.MARK;
import static ru.auto.tests.desktop.page.CatalogNewPage.MODEL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
@DisplayName("Каталог - карточка модели - сравнение")
@Epic(CATALOG_NEW)
@Feature("Блок конфигурации")
public class CompareTest {

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
    @DisplayName("Тултип по наведению на кнопку добавления в сравнение»")
    public void shouldSeeAddCompareTooltip() {
        seeTooltip("Добавить к сравнению");
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Добавление в сравнение/удаление из сравнения")
    public void shouldClickCompareButton() {
        clickCompareIcon();

        catalogPageSteps.onCatalogNewPage().notifier("Добавлено Перейти к сравнению");
        seeTooltip("Удалить из сравнения");

        clickCompareIcon();

        catalogPageSteps.onCatalogNewPage().notifier("Удалено");
        seeTooltip("Добавить к сравнению");
    }

    @Step("Клик по иконке сравнения")
    public void clickCompareIcon() {
        catalogPageSteps.onCatalogNewPage().specificationContentBlock().getBlock(0).row()
                .waitUntil(isDisplayed()).hover();
        catalogPageSteps.onCatalogNewPage().specificationContentBlock().getBlock(0).compareButton()
                .waitUntil(isDisplayed()).click();
    }

    @Step("Тултип по наведению на иконку сравнения")
    public void seeTooltip(String tooltipText) {
        catalogPageSteps.onCatalogNewPage().specificationContentBlock().getBlock(0).row().waitUntil(isDisplayed())
                .hover();
        catalogPageSteps.onCatalogNewPage().specificationContentBlock().getBlock(0).compareButton()
                .waitUntil(isDisplayed()).hover();
        catalogPageSteps.onCatalogNewPage().popup().should(isDisplayed()).should(hasText(tooltipText));
    }

}
