package ru.auto.tests.mobile.group;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GROUP;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Групповая карточка - нет предложений")
@Feature(AutoruFeatures.GROUP)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class GroupEmptyTest {

    private static final String PATH = "/kia/optima/21342050-21342121/";
    private static final String REGION_ID = "10";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("mobile/SearchCarsBreadcrumbsMarkModelGroupRid10",
                "mobile/SearchCarsGroupContextGroupRid10",
                "mobile/SearchCarsGroupContextListingRid10",
                "desktop/ReferenceCatalogCarsConfigurationsSubtree").post();

        urlSteps.testing().path(CARS).path(NEW).path(GROUP).path(PATH).addParam("geo_id", REGION_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение пустой групповой карточки")
    public void shouldSeeEmptyGroupSale() {
        basePageSteps.onGroupPage().content().should(hasText("Продажа новыхKiaOptimaСедан\nНовые Kia Optima IV " +
                "Рестайлинг\nот 1 169 400 ₽\nПоделиться\nО модели\nПараметры\nКомплектация\nДвигатель\nКоробка\nПривод" +
                "\nЦвет\nВ наличии\nВ выбранном регионе нет подходящих предложений\nПодпишитесь на новые предложения" +
                "\nKia Optima IV Рестайлинг, новые\nСохранить поиск"
        ));
    }
}
