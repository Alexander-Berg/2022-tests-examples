package ru.yandex.general.ads;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.general.consts.BaseConstants;
import ru.yandex.general.module.GeneralProxyMobileWebModule;
import ru.yandex.general.mobile.step.BasePageSteps;
import ru.yandex.general.step.GoalsSteps;
import ru.yandex.general.step.UrlSteps;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static ru.yandex.general.consts.BaseConstants.ClientType.TOUCH;
import static ru.yandex.general.consts.BaseConstants.ListingType.GRID;
import static ru.yandex.general.consts.BaseConstants.ListingType.LIST;
import static ru.yandex.general.consts.GeneralFeatures.ADS_FEATURE;
import static ru.yandex.general.consts.GeneralFeatures.LISTING_FEATURE;
import static ru.yandex.general.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.general.consts.Pages.ELEKTRONIKA;
import static ru.yandex.general.consts.Pages.ROSSIYA;
import static ru.yandex.general.mobile.step.BasePageSteps.CLASSIFIED_LISTING_DISPLAY_TYPE;

@Epic(LISTING_FEATURE)
@Feature(ADS_FEATURE)
@RunWith(Parameterized.class)
@GuiceModules(GeneralProxyMobileWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ListingAdsBannerRotationTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private GoalsSteps goalsSteps;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public BaseConstants.ListingType listingType;

    @Parameterized.Parameters(name = "Ротация баннеров на «{0}»")
    public static Collection<Object[]> links() {
        return asList(new Object[][]{
                {"Листинг сеткой", GRID},
                {"Листинг списком", LIST}
        });
    }

    @Before
    public void before() {
        basePageSteps.setCookie(CLASSIFIED_LISTING_DISPLAY_TYPE, listingType.getType());
        urlSteps.testing().path(ROSSIYA).path(ELEKTRONIKA).open();
        basePageSteps.waitSomething(500, TimeUnit.MILLISECONDS);
        goalsSteps.clearHar();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Ротация рекламных баннеров на листинге по категории")
    public void shouldSeeBannerRotationCategoryListing() {
        basePageSteps.slowScrolling(18000);

        goalsSteps.shouldSeeAdBannerRotation(TOUCH, listingType);
    }

}
