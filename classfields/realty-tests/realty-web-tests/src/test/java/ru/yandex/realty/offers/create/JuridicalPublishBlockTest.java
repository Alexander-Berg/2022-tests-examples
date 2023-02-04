package ru.yandex.realty.offers.create;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Testing;
import ru.yandex.realty.module.RealtyWebWithPhoneModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.CompareSteps;
import ru.yandex.realty.step.OfferAddSteps;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.consts.RealtyFeatures.OFFERS;
import static ru.yandex.realty.consts.RealtyStories.CREATE_OFFER;
import static ru.yandex.realty.consts.RealtyTags.JURICS;
import static ru.yandex.realty.page.OfferAddPage.NORMAL_PLACEMENT;

@Tag(JURICS)
@DisplayName("Форма добавления объявления.")
@Feature(OFFERS)
@Story(CREATE_OFFER)
@Issue("VERTISTEST-1396")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebWithPhoneModule.class)
public class JuridicalPublishBlockTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private Account account;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private OfferAddSteps offerAddSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void openManagementAddPage() {
        apiSteps.createRealty3JuridicalAccount(account);
        urlSteps.setSpbCookie();
        compareSteps.resize(1280, 10000);
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).open();
        urlSteps.shouldUrl(containsString("add"));
        offerAddSteps.fillRequiredFieldsForPublishBlock(OfferAddSteps.DEFAULT_LOCATION);
    }

    @Test
    @DisplayName("Скриншот блока публикации для юр. лица c фото")
    @Owner(KANTEMIROV)
    @Category({Regression.class, Testing.class})
    public void shouldSeeJuridicalWithPhotoPublishBlock() {
        offerAddSteps.onOfferAddPage().publishBlock().payButton().should(hasText(NORMAL_PLACEMENT));
        Screenshot testing = compareSteps.takeScreenshot(offerAddSteps.onOfferAddPage().publishBlock());

        urlSteps.setProductionHost().open();
        offerAddSteps.fillRequiredFieldsForPublishBlock(OfferAddSteps.DEFAULT_LOCATION);
        offerAddSteps.onOfferAddPage().publishBlock().payButton().should(hasText(NORMAL_PLACEMENT));
        Screenshot production = compareSteps.takeScreenshot(offerAddSteps.onOfferAddPage().publishBlock());
        compareSteps.screenshotsShouldBeTheSame(testing, production);
    }
}