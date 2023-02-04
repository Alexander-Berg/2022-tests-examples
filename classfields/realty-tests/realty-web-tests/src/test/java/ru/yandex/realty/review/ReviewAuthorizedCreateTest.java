package ru.yandex.realty.review;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Dimension;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.module.RealtyWebModuleWithReviewDelete;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.NewBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsNot.not;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.KOPITSA;
import static ru.yandex.realty.consts.RealtyFeatures.REVIEWS;
import static ru.yandex.realty.element.newbuildingsite.AddEditReviewBlock.ADD_REVIEW;
import static ru.yandex.realty.element.newbuildingsite.ReviewBlock.SEND_ANONYMUS;
import static ru.yandex.realty.element.newbuildingsite.SiteReview.DELETE;
import static ru.yandex.realty.element.newbuildingsite.SiteReview.EDIT;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Тесты создания отзывов на новостройки для залогина")
@Feature(REVIEWS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModuleWithReviewDelete.class)
public class ReviewAuthorizedCreateTest {

    private final int MINIMAL_ACCEPTABLE_REVIEW_LENGTH = 10;

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
        basePageSteps.getDriver().manage().window().setSize(new Dimension(1400, 1800));
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).open();
        basePageSteps.onNewBuildingPage().snippetElements().waitUntil(hasSize(greaterThanOrEqualTo(1)));
        basePageSteps.onNewBuildingPage().snippetElements().get(1).click();
        basePageSteps.switchToTab(1);
        newBuildingSteps.deleteReview();
    }

    @Test
    @Owner(KOPITSA)
    @DisplayName("Создаем пустой отзыв")
    public void shouldNotCreateEmptyReview() {
        basePageSteps.onNewBuildingSitePage().reviewBlock().reviewArea().button(ADD_REVIEW).should(not(isEnabled()));
    }

    @Ignore
    @Test
    @Owner(KOPITSA)
    @DisplayName("Создаем отзыв анонимно")
    public void shouldCreateAnonymousReview() {
        String randomAcceptableString = getRandomString(MINIMAL_ACCEPTABLE_REVIEW_LENGTH);
        basePageSteps.onNewBuildingSitePage().reviewBlock().reviewArea().inputField().sendKeys(randomAcceptableString);
        basePageSteps.scrollToElement(basePageSteps.onNewBuildingSitePage().reviewBlock().reviewArea());
        basePageSteps.onNewBuildingSitePage().reviewBlock().reviewArea().selectCheckBox(SEND_ANONYMUS);
        basePageSteps.onNewBuildingSitePage().reviewBlock().reviewArea().button(ADD_REVIEW).click();

        shouldSeeReviewFields("Аноним", randomAcceptableString);
    }

    @Step("Проверяем отзыв")
    private void shouldSeeReviewFields(String name, String review) {
        basePageSteps.refreshUntil(() -> basePageSteps.onNewBuildingSitePage().reviewBlock().siteReviewList(),
                hasSize(greaterThanOrEqualTo(1)));
        basePageSteps.onNewBuildingSitePage().reviewBlock().siteReviewList().get(0).name().should(hasText(name));
        basePageSteps.onNewBuildingSitePage().reviewBlock().siteReview(FIRST).text().should(hasText(review));
        basePageSteps.onNewBuildingSitePage().reviewBlock().siteReview(FIRST).spanLink(DELETE).should(isEnabled());
        basePageSteps.onNewBuildingSitePage().reviewBlock().siteReview(FIRST).spanLink(EDIT).should(isEnabled());
    }
}
