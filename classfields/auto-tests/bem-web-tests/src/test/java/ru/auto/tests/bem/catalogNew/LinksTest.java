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

import static java.lang.String.format;
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
@DisplayName("Каталог - карточка модели - ссылки с характеристиками")
@Epic(CATALOG_NEW)
@Feature("Ссылки")
public class LinksTest {

    private static final String TRANSMISSION = "Трансмиссия";
    private static final String SIZE = "Размеры и вес";

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
    @DisplayName("Отображение блока ссылок на характеристики")
    public void shouldSeeSpecLinksBlock() {
        catalogPageSteps.onCatalogNewPage().specificationLinksBlock().waitUntil(isDisplayed()).should(hasText("Размеры и " +
                "вес Audi A5\nРазмеры и вес\nОбъём багажника\nРазмер колес\nТип привода\nКлиренс\nТрансмиссия\n" +
                "Расход топлива\nОбъём топливного бака\nРазгон до 100\nХарактеристики двигателя"));
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("При заходе на страницу первая ссылка всегда активна")
    public void shouldSeeActiveLink() {
        catalogPageSteps.onCatalogNewPage().specificationLinksBlock().activeLink().waitUntil(isDisplayed())
                .should(hasText("Размеры и вес"));
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Изменение заголовка при клике на ссылку")
    public void shouldChangeH2() {
        catalogPageSteps.onCatalogNewPage().specificationLinksBlock().getLink(5).click();

        catalogPageSteps.onCatalogNewPage().specificationLinksBlock().h2().should(hasText(format(
                "%s %s %s", TRANSMISSION, MARK, MODEL)));

        catalogPageSteps.onCatalogNewPage().specificationLinksBlock().getLink(0).click();

        catalogPageSteps.onCatalogNewPage().specificationLinksBlock().h2().should(hasText(format(
                "%s %s %s", SIZE, MARK, MODEL)));

    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по неактивной ссылке")
    public void shouldClickLink() {
        catalogPageSteps.onCatalogNewPage().specificationLinksBlock().getLink(5).click();

        urlSteps.path("/transmissiya/").shouldNotSeeDiff();
        catalogPageSteps.onCatalogNewPage().specificationLinksBlock().activeLink().should(isDisplayed())
                .should(hasText(TRANSMISSION));

        urlSteps.refresh();

        catalogPageSteps.onCatalogNewPage().specificationLinksBlock().activeLink().should(isDisplayed())
                .should(hasText(TRANSMISSION));

    }

}
