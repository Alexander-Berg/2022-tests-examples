package ru.yandex.realty.review;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.KOPITSA;
import static ru.yandex.realty.consts.RealtyFeatures.REVIEWS;
import static ru.yandex.realty.element.newbuildingsite.ReviewBlock.FOR_DEVELOPER;

@DisplayName("Кнопка «Для застройщика» в блоке отзывов на новостройки")
@Feature(REVIEWS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class DeveloperReviewTest {

    private static final String DEVELOPER_RIGHTS_APPROVAL_TEAM_MAIL = "adv@realty.yandex.ru";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void openNewBuildingSitePage() {
        basePageSteps.setWindowSize(1400, 1800);
        urlSteps.testing().path("uhta").path(KUPIT).path(NOVOSTROJKA).path("optima").queryParam("id", "1847758").open();
    }

    @Ignore("Нет ссылки «Для застройщика?»")
    @Test
    @Category({Regression.class, Testing.class})
    @Owner(KOPITSA)
    @DisplayName("Видим попап для застройщика")
    public void shouldSeeDeveloperPopup() {
        basePageSteps.refreshUntil(() -> basePageSteps.onNewBuildingSitePage().reviewBlock().spanLink(FOR_DEVELOPER),
                isDisplayed());
        basePageSteps.onNewBuildingSitePage().reviewBlock().spanLink(FOR_DEVELOPER).click();
        basePageSteps.onNewBuildingSitePage().toDeveloperPopup().should(isDisplayed());
        basePageSteps.onNewBuildingSitePage().toDeveloperPopup().link()
                .should(hasText(DEVELOPER_RIGHTS_APPROVAL_TEAM_MAIL));
    }
}
