package ru.auto.tests.mobile.crosslinks;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LISTING;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг - блок перелинковки")
@Feature(LISTING)
@Story("Блок перелинковки «Автомобили по параметрам»")
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class TagLinksByParamBlockUrlsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String startUrl;

    @Parameterized.Parameter(1)
    public String urlTitle;

    @Parameterized.Parameter(2)
    public String url;

    @Parameterized.Parameters(name = "name = {index}: {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"/moskva/cars/all/", "Компактные автомобили", "/tag/compact/"},
                {"/leningradskaya_oblast/cars/all/", "Семиместные семейные автомобили", "/tag/7seatsfamily/"},
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(startUrl).open();
        basePageSteps.onListingPage().footer().hover();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class})
    @DisplayName("Клик по ссылке")
    public void shouldClickUrl() {
        basePageSteps.onListingPage().tagsBlock().buttonContains("Показать все").waitUntil(isDisplayed())
                .hover().click();
        basePageSteps.onListingPage().tagsBlock().button(urlTitle).waitUntil(isDisplayed())
                .hover().click();
        urlSteps.path(url).shouldNotSeeDiff();
    }
}
