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
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.module.RealtyWebModuleWithReviewDelete;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.NewBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.core.IsNot.not;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.KOPITSA;
import static ru.yandex.realty.consts.RealtyFeatures.REVIEWS;
import static ru.yandex.realty.element.newbuildingsite.SiteReview.DELETE;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Удаление отзыва на новостройки")
@Feature(REVIEWS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModuleWithReviewDelete.class)
public class DeleteReviewTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private NewBuildingSteps newBuildingSteps;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private Account account;

    @Before
    public void openNewBuildingSitePage() {
        apiSteps.createVos2Account(account, OWNER);
        basePageSteps.setWindowSize(1400, 1800);
        urlSteps.testing().path("uhta").path(KUPIT).path(NOVOSTROJKA).path("optima").queryParam("id", "1847758").open();
        newBuildingSteps.deleteReview();
    }

    @Ignore
    @Test
    @Owner(KOPITSA)
    @DisplayName("Удаляем отзыв")
    public void shouldDeleteReview() {
        String text = getRandomString(10);
        basePageSteps.onNewBuildingSitePage().addReview(text);
        basePageSteps.onNewBuildingSitePage().reviewBlock().siteReview(FIRST).spanLink(DELETE).click();
        basePageSteps.onNewBuildingSitePage().reviewBlock().siteReview(FIRST).spanLink(DELETE)
                .should(not(exists()));
    }
}
