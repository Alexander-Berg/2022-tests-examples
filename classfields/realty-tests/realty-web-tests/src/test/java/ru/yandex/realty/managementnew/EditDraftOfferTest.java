package ru.yandex.realty.managementnew;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
import ru.auto.test.api.realty.draft.create.userid.Price;
import ru.auto.tests.passport.account.Account;
import ru.yandex.realty.adaptor.Vos2Adaptor;
import ru.yandex.realty.consts.Pages;
import ru.yandex.realty.consts.RealtyFeatures;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.ApiSteps;
import ru.yandex.realty.step.ManagementSteps;
import ru.yandex.realty.step.OfferBuildingSteps;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL;
import static ru.auto.test.api.realty.OfferType.APARTMENT_SELL_WITHOUT_PHOTO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.MANAGEMENT_NEW_ADD;
import static ru.yandex.realty.page.ManagementNewPage.REDACT;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.OfferBuildingSteps.getDefaultDraftOffer;
import static ru.yandex.realty.utils.AccountType.OWNER;

@DisplayName("Редактирование черновика со страницы личного кабинета")
@Link("https://st.yandex-team.ru/VERTISTEST-1490")
@Feature(RealtyFeatures.MANAGEMENT_NEW)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class EditDraftOfferTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private ApiSteps apiSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private Account account;

    @Inject
    private ManagementSteps managementSteps;

    @Inject
    private OfferBuildingSteps offerBuildingSteps;

    @Inject
    private Vos2Adaptor vos2Adaptor;

    @Before
    public void openManagementPage() {
        apiSteps.createVos2Account(account, OWNER);
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Черновик без цены -> в инпуте «0»")
    public void shouldSeeDraftZeroPrice() {
        offerBuildingSteps.createSpecDraft(account, asList(
                getDefaultDraftOffer(APARTMENT_SELL).withPrice(new Price())));

        urlSteps.testing().path(Pages.MANAGEMENT_NEW).open();
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().input().should(hasValue("0"));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Черновик видим переход в редактирование при клике на фото")
    public void shouldSeePassToEditOfferForm() {
        offerBuildingSteps.createDefaultDraft(account);
        String offerId = vos2Adaptor.getUserOffers(account.getId()).getOffers().get(0).getId();
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).open();
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().addPhotoButton().waitUntil(isDisplayed())
                .click();
        urlSteps.testing().path(MANAGEMENT_NEW_ADD).queryParam("draftId", offerId).path("/")
                .queryParam("scroll", "photo").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Видим «добавить фото» для черновика без фото")
    public void shouldSeeAddPhotoMessage() {
        offerBuildingSteps.createSpecDraft(account, asList(getDefaultDraftOffer(APARTMENT_SELL_WITHOUT_PHOTO)));
        urlSteps.testing().path(Pages.MANAGEMENT_NEW).open();
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().addPhotoButton()
                .should(hasText(containsString("Добавить фото")));
        managementSteps.onManagementNewPage().offer(FIRST).controlPanel().link(REDACT).click();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Нельзя менять цену на «0»")
    public void shouldNotChangeDraftToZeroPrice() {
        long initialPrice = 100L;
        Price price = getDefaultDraftOffer(APARTMENT_SELL).getPrice().withValue(initialPrice);
        offerBuildingSteps.createSpecDraft(account, asList(
                getDefaultDraftOffer(APARTMENT_SELL).withPrice(price)));

        urlSteps.testing().path(Pages.MANAGEMENT_NEW).open();
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().input()
                .waitUntil(hasValue(valueOf(initialPrice)));
        managementSteps.clearInputByBackSpace(() ->
                managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().input());
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().input().sendKeys("0");
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().input().waitUntil(hasValue("0"));
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().input().sendKeys(Keys.ENTER);
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().input()
                .should(hasValue(valueOf(initialPrice)));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Меняем цену черновика")
    public void shouldChangeDraftPrice() {
        long initialPrice = 550L;
        long secondPrice = 11000L;
        String initialPriceByMeter = "10 ₽ за м²";
        String secondPriceByMeter = "200 ₽ за м²";
        Price price = getDefaultDraftOffer(APARTMENT_SELL).getPrice().withValue(initialPrice);
        offerBuildingSteps.createSpecDraft(account, asList(
                getDefaultDraftOffer(APARTMENT_SELL).withPrice(price)));

        urlSteps.testing().path(Pages.MANAGEMENT_NEW).open();
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().byMeter()
                .waitUntil(hasText(initialPriceByMeter));
        managementSteps.clearInputByBackSpace(() ->
                managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().input());
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().input()
                .sendKeys(valueOf(secondPrice) + Keys.ENTER);
        managementSteps.onManagementNewPage().offer(FIRST).offerInfo().price().byMeter()
                .waitUntil(hasText(secondPriceByMeter));
        apiSteps.waitOfferPrice(account.getId(), secondPrice);
    }
}
