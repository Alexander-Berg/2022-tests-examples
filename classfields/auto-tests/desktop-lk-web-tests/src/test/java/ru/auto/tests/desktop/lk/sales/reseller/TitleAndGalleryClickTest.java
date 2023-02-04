package ru.auto.tests.desktop.lk.sales.reseller;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.RESELLER;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Клик по заголовку и галерее")
@Epic(AutoruFeatures.LK)
@Feature(AutoruFeatures.MY_OFFERS_RESELLER)
@GuiceModules(DesktopTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class TitleAndGalleryClickTest {

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

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String categoryOfferCard;

    @Parameterized.Parameter(2)
    public String salesMock;

    @Parameterized.Parameter(3)
    public String markPath;

    @Parameterized.Parameter(4)
    public String modelPath;

    @Parameterized.Parameter(5)
    public String offerIdPath;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, CARS, "desktop-lk/UserOffersCarsActive", "/vaz/", "/2121/", "/1076842087-f1e84/"},
                {MOTO, MOTORCYCLE, "desktop-lk/UserOffersMotoActive", "/ducati/", "/monster_s4/", "/1076842087-f1e84/"},
                {TRUCKS, LCV, "desktop-lk/UserOffersTrucksActive", "/hyundai/", "/porter/", "/1076842087-f1e84/"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop-lk/UserFavoriteReseller",
                salesMock).post();

        basePageSteps.setWideWindowSize();

        urlSteps.testing().path(MY).path(RESELLER).path(category).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Кликаем в тайтл оффера")
    public void shouldClickInOfferTitle() {
        basePageSteps.onLkResellerSalesPage().getSale(0).mainInfoColumn().vehicleName().should(isDisplayed()).click();
        urlSteps.testing().path(categoryOfferCard).path(USED).path(SALE)
                .path(markPath).path(modelPath).path(offerIdPath).shouldNotSeeDiff();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Кликаем в картинку оффера")
    public void shouldClickInOfferImage() {
        basePageSteps.onLkResellerSalesPage().getSale(0).photo().should(isDisplayed()).click();
        urlSteps.testing().path(categoryOfferCard).path(USED).path(SALE)
                .path(markPath).path(modelPath).path(offerIdPath).shouldNotSeeDiff();
    }
}
