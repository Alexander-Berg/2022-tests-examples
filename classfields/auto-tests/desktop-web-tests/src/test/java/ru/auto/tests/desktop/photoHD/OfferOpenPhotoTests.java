package ru.auto.tests.desktop.photoHD;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.component.WithBadge.PHOTO_URL;
import static ru.auto.tests.desktop.consts.AutoruFeatures.GALLERY;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SALES;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;

@Epic(SALES)
@Feature(GALLERY)
@Story("HD фото")
@DisplayName("Объявление - галерея - HD фото")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class OfferOpenPhotoTests {

    private static final String SALE_ID = "1076842087-f1e84/";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthUser"),
                stub("desktop/OfferCarsUsedUserHd"),
                stub("desktop/CarfaxOfferCarsRawPaidHdPhoto")
        ).create();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().gallery().click();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Просмотр HD фото с купленным отчетом")
    public void shouldOpenHdPhotoFullGallery() {
        basePageSteps.onCardPage().fullScreenGallery().badgeOpenOrig().click();

        basePageSteps.switchToNextTab();
        urlSteps.fromUri(PHOTO_URL).shouldNotSeeDiff();
    }

}
