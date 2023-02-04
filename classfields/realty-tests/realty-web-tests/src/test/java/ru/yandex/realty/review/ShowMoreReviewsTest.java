package ru.yandex.realty.review;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Dimension;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.KOPITSA;
import static ru.yandex.realty.consts.RealtyFeatures.REVIEWS;

/**
 * Created by kopitsa on 08.08.17.
 */

@DisplayName("Кнопка «Показать еще отзывы на новостройку»")
@Feature(REVIEWS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ShowMoreReviewsTest {

    private static final String NEW_BUILDING_NAME = "apelsin";
    private static final String NEW_BUILDING_ID = "198143";
    private static final String SHOW_MORE = "Показать ещё";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void openNewBuildingSitePage() {
        basePageSteps.getDriver().manage().window().setSize(new Dimension(1400, 1800));
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).path(NEW_BUILDING_NAME)
                .queryParam("siteId", NEW_BUILDING_ID).queryParam("id", NEW_BUILDING_ID).open();
    }

    @Ignore
    @Test
    @Description("НУЖНА НОВСОТРОЙКА ГДЕ МНОГО ОТЗЫВОВ ИЛИ МОК НА РУЧКУ")
    @Owner(KOPITSA)
    @DisplayName("Видим ещё отзывы")
    public void shouldSeeMoreReviews() {
        basePageSteps.onNewBuildingSitePage().reviewBlock().button(SHOW_MORE).waitUntil(isDisplayed());
        basePageSteps.scrollToElement(basePageSteps.onNewBuildingSitePage().reviewBlock().button(SHOW_MORE));
        int reviews = basePageSteps.onNewBuildingSitePage().reviewBlock().siteReviewList().size();
        basePageSteps.onNewBuildingSitePage().reviewBlock().button(SHOW_MORE).click();
        basePageSteps.onNewBuildingSitePage().reviewBlock().siteReviewList()
                .waitUntil("Количество отзывов должно быть больше после нажатия кнопки", hasSize(greaterThan(reviews)));
    }
}
