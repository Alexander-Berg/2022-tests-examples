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

import static org.hamcrest.Matchers.equalTo;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.KOPITSA;
import static ru.yandex.realty.consts.RealtyFeatures.REVIEWS;
import static ru.yandex.realty.element.newbuildingsite.SiteReview.EDIT;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Редактирование отзыва на новостройки")
@Feature(REVIEWS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModuleWithReviewDelete.class)
public class EditReviewTest {

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
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).path("salarevo-park").queryParam("id", "375274")
                .open();
        newBuildingSteps.deleteReview();
    }

    @Ignore
    @Test
    @Owner(KOPITSA)
    @DisplayName("Редактируем отзыв")
    public void shouldEditReview() {
        basePageSteps.refreshUntil(() -> basePageSteps.onNewBuildingSitePage().reviewBlock().reviewArea().inputField(),
                isDisplayed());
        String text1 = getRandomString(10);
        basePageSteps.onNewBuildingSitePage().addReview(text1);
        basePageSteps.refreshUntil(() -> basePageSteps.onNewBuildingSitePage().reviewBlock().siteReview(FIRST).text().getText(),
                equalTo(text1));

        basePageSteps.scrollToElement(basePageSteps.onNewBuildingSitePage().reviewBlock().siteReview(FIRST));
        basePageSteps.onNewBuildingSitePage().reviewBlock().siteReview(FIRST).spanLink(EDIT).click();
        String text2 = getRandomString(10);
        basePageSteps.onNewBuildingSitePage().addReview(text2);
        basePageSteps.refresh();

        basePageSteps.onNewBuildingSitePage().reviewBlock().siteReview(FIRST).text().should(hasText(text1 + text2));
    }
}
