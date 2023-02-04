package ru.auto.tests.desktop.photoHD;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.GALLERY;
import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Epic(VIN)
@Feature(GALLERY)
@Story("HD фото")
@DisplayName("VIN отчет - галерея - HD фото")
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ReportTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String carfaxMock;

    @Parameterized.Parameter(1)
    public String url;

    @Parameterized.Parameters(name = "name = {index}: {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"desktop/CarfaxReportRawVinPaidHdPhoto", "4S2CK58D924333406"},
                {"desktop/CarfaxOfferCarsRawPaidHdPhoto", "1076842087-f1e84"}
        });
    }


    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub(carfaxMock)
        ).create();

        urlSteps.testing().path(HISTORY).path(url).path(SLASH).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Отображение бейджа HD в превью галереи")
    public void shouldSeeBadgeHd() {
        basePageSteps.onHistoryPage().vinReport().gallery().badge("HD").waitUntil(isDisplayed());
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение бейджей HD на превью в полной галерее")
    public void shouldSeeBadgeHdFullGallery() {
        basePageSteps.onHistoryPage().vinReport().gallery().waitUntil(isDisplayed()).click();

        basePageSteps.onHistoryPage().vinReport().fullScreenGallery().thumbList().forEach(thumb ->
                thumb.should(hasText("HD")));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(NATAGOLOVKINA)
    @DisplayName("Отображение бейджа HD в превью галереи в оффере")
    public void shouldSeeOfferBadgeHd() {
        basePageSteps.onHistoryPage().vinReport().offer().gallery().badge("HD").waitUntil(isDisplayed());
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение бейджей HD на превью в полной галерее в оффере")
    public void shouldSeeOfferBadgeHdFullGallery() {
        basePageSteps.onHistoryPage().vinReport().offer().gallery().waitUntil(isDisplayed()).click();

        basePageSteps.onHistoryPage().vinReport().offer().fullScreenGallery().thumbList().forEach(thumb ->
                thumb.should(hasText("HD")));
    }

}
