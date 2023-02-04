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
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CATALOG_NEW;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.SPECIFICATIONS;
import static ru.auto.tests.desktop.page.CatalogNewPage.MARK;
import static ru.auto.tests.desktop.page.CatalogNewPage.MODEL;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
@DisplayName("Каталог - карточка модели - контент ссылок")
@Epic(CATALOG_NEW)
@Feature("Блок конфигурации")
public class LinksContentTest {

    private static final String TRANSMISSION = "Трансмиссия";
    private static final String CONFIGURATION = "2 (Ф5) Рестайлинг 2019-2022";
    private static final String SIZE = "Размеры и вес";
    private static final String BLOCK_1_TEXT = "Лифтбек Sportback\nВсе характеристики\nМодификация Габариты, Д х Ш х В, мм Масса, кг\n35 TDI 2.0d AMT (163 л.с.) 4757 x 1843 x 1386 1525\n40 TDI 2.0d AMT (190 л.с.) 4WD 4757 x 1843 x 1386 1605\n40 TDI 2.0d AMT (190 л.с.) 4757 x 1843 x 1386 1520\n40 TDI 2.0d AMT (204 л.с.) 4WD 4757 x 1843 x 1386 1615\n40 TDI 2.0d AMT (204 л.с.) 4757 x 1843 x 1386 1605\n35 TFSI 2.0 MT (150 л.с.) 4757 x 1843 x 1386 1475\n35 TFSI 2.0 AMT (150 л.с.) 4757 x 1843 x 1386 1510\n40 g-tron 2.0 AMT (170 л.с.) 4757 x 1843 x 1386 1575\n40 TFSI 2.0 AMT (190 л.с.) 4757 x 1843 x 1386 1490\n40 TFSI 2.0 AMT (204 л.с.) 4757 x 1843 x 1386 1515\n40 TFSI 2.0 AMT (204 л.с.) 4WD 4757 x 1843 x 1386 1585\n45 TFSI 2.0 AMT (245 л.с.) 4WD 4757 x 1843 x 1386 1645\n45 TFSI 2.0 AMT (249 л.с.) 4WD 4757 x 1843 x 1386 1645\n45 TFSI 2.0 AMT (265 л.с.) 4WD 4757 x 1843 x 1386 1595\n45 TDI 3.0d AT (231 л.с.) 4WD 4757 x 1843 x 1386 1700\n50 TDI 3.0d AT (286 л.с.) 4WD 4757 x 1843 x 1386 1700\n78 новых\n14 с пробегом";
    private static final String BLOCK_2_TEXT = "Лифтбек Sportback\nВсе характеристики\nМодификация Коробка передач Количество передач\n35 TDI 2.0d AMT (163 л.с.) робот 7\n40 TDI 2.0d AMT (190 л.с.) 4WD робот 7\n40 TDI 2.0d AMT (190 л.с.) робот 7\n40 TDI 2.0d AMT (204 л.с.) 4WD робот 7\n40 TDI 2.0d AMT (204 л.с.) робот 7\n35 TFSI 2.0 MT (150 л.с.) механика 6\n35 TFSI 2.0 AMT (150 л.с.) робот 7\n40 g-tron 2.0 AMT (170 л.с.) робот 7\n40 TFSI 2.0 AMT (190 л.с.) робот 7\n40 TFSI 2.0 AMT (204 л.с.) робот 7\n40 TFSI 2.0 AMT (204 л.с.) 4WD робот 7\n45 TFSI 2.0 AMT (245 л.с.) 4WD робот 7\n45 TFSI 2.0 AMT (249 л.с.) 4WD робот 7\n45 TFSI 2.0 AMT (265 л.с.) 4WD робот 7\n45 TDI 3.0d AT (231 л.с.) 4WD автомат 8\n50 TDI 3.0d AT (286 л.с.) 4WD автомат 8\n78 новых\n14 с пробегом";


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
    @DisplayName("Отображение блока конфигурации")
    public void shouldSeeConfigurationsBlockContent() {
        catalogPageSteps.onCatalogNewPage().specificationContentBlock().getTitle(0).should(hasText(format(
                "%s %s %s %s", SIZE, MARK, MODEL, CONFIGURATION)));
        catalogPageSteps.onCatalogNewPage().specificationContentBlock().getBlock(0).should(hasText(BLOCK_1_TEXT));
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("12 блоков конфигурации на странице")
    public void shouldSeeConfigurationsBlocks() {
        catalogPageSteps.onCatalogNewPage().footer().hover();

        catalogPageSteps.onCatalogNewPage().specificationContentBlock().configurationBlockList().should(hasSize(12));
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Изменение блока конфигурации при клике на ссылку")
    public void shouldChangeH2() {
        catalogPageSteps.onCatalogNewPage().specificationLinksBlock().getLink(5).click();

        catalogPageSteps.onCatalogNewPage().specificationContentBlock().getTitle(0).should(hasText(format(
                "%s %s %s %s", TRANSMISSION, MARK, MODEL, CONFIGURATION)));
        catalogPageSteps.onCatalogNewPage().specificationContentBlock().getBlock(0).should(hasText(BLOCK_2_TEXT));

        catalogPageSteps.onCatalogNewPage().specificationLinksBlock().getLink(0).click();

        catalogPageSteps.onCatalogNewPage().specificationContentBlock().getTitle(0).should(hasText(format(
                "%s %s %s %s", SIZE, MARK, MODEL, CONFIGURATION)));
        catalogPageSteps.onCatalogNewPage().specificationContentBlock().getBlock(0).should(hasText(BLOCK_1_TEXT));

    }

}
