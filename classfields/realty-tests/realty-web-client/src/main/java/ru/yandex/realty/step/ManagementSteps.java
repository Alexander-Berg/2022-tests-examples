package ru.yandex.realty.step;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.yandex.realty.adaptor.SearcherAdaptor;
import ru.yandex.realty.config.RealtyApiConfig;
import ru.yandex.realty.page.AuthManagementPage;
import ru.yandex.realty.page.EgrnListingPage;
import ru.yandex.realty.page.EgrnReportPage;
import ru.yandex.realty.page.ManagementNewPage;
import ru.yandex.realty.page.NotAuthManagementPage;
import ru.yandex.realty.page.OfferAddPage;
import ru.yandex.realty.page.TariffsPage;
import ru.yandex.realty.page.WalletPage;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.element.wallet.PromocodePaymentPopup.QUIT;
import static ru.yandex.realty.page.ManagementNewPage.ACTIVATE;
import static ru.yandex.realty.step.OfferAddSteps.getDefaultImagePath;

/**
 * Created by vicdev on 25.04.17.
 */
public class ManagementSteps extends CommonSteps {

    @Inject
    private RealtyApiConfig config;

    @Inject
    private WalletSteps walletSteps;

    @Inject
    private SearcherAdaptor searcherAdaptor;

    public NotAuthManagementPage onNotAuthManagementPage() {
        return on(NotAuthManagementPage.class);
    }

    public AuthManagementPage onAuthManagementPage() {
        return on(AuthManagementPage.class);
    }

    public ManagementNewPage onManagementNewPage() {
        return on(ManagementNewPage.class);
    }

    public OfferAddPage onOfferAddPage() {
        return on(OfferAddPage.class);
    }

    public WalletPage onWalletPage() {
        return on(WalletPage.class);
    }

    public EgrnReportPage onEgrnReportPage() {
        return on(EgrnReportPage.class);
    }

    public EgrnListingPage onEgrnListingPage() {
        return on(EgrnListingPage.class);
    }

    public TariffsPage onTariffsPage() {
        return on(TariffsPage.class);
    }

    @Step("После авторизации должны увидеть пустой аккаунт")
    public void shouldSeeEmptyAuthAccount() {
        onAuthManagementPage().newOfferButton().should(isDisplayed());
    }

    @Step("Должны увидеть алерты с предупреждениями {alerts}")
    public void shouldSeeAlerts(String... alerts) {
        shouldSeeAlerts(onNotAuthManagementPage().alerts(), alerts);
    }

    private void shouldSeeAlerts(ElementsCollection<AtlasWebElement> elements, String... alerts) {
        List<String> alertsText = elements.stream().map(e -> e.getText()).collect(Collectors.toList());
        assertThat(alertsText, containsInAnyOrder(alerts));
    }

    @Step("Раскрываем статистику офера")
    public void openOfferStatistic(int i) {
        onManagementNewPage().offer(i).offerInfo().statsOpener().click();
        onManagementNewPage().offer(i).stat(0).waitUntil(isDisplayed());
        waitSomething(5, TimeUnit.SECONDS);
    }

    @Step("Добавляем фото со страницы списка оферов")
    public void addPhoto() {
        if (!config.isLocalDebug()) {
            ((RemoteWebDriver) getDriver()).setFileDetector(new LocalFileDetector());
        }
        onManagementNewPage().ccOffer(FIRST).offerInfo().addPhotoButton().click();
        onManagementNewPage().ccOffer(FIRST).photoGallery().addPhotoInput().sendKeys(getDefaultImagePath());
        onManagementNewPage().ccOffer(FIRST).photoGallery().previews().should(hasSize(greaterThan(0)));
        onManagementNewPage().ccOffer(FIRST).photoGallery().previews()
                .forEach(p -> p.previewImg().waitUntil(isDisplayed()));
    }

    @Step("Активируем объявление оффера {offerId}. Ждем Активации")
    public void activatePay(String offerId) {
        onManagementNewPage().offerById(offerId).controlPanel().button(ACTIVATE).click();
        walletSteps.switchToCardForm();
        walletSteps.fillsCardFieldsWithDefaultData();
        walletSteps.switchToPaymentPopup();
        onWalletPage().cardsPopup().paymentButton().click();
        onWalletPage().cardsPopup().spinVisible().waitUntil(exists());
        searcherAdaptor.waitOffer(offerId);
        onWalletPage().cardsPopup().close().clickIf(isDisplayed());
        onWalletPage().promocodePopup().spanLink(QUIT).clickIf(isDisplayed());
    }

    @Step("Перемещаем зажатый «{element}» на «{target}» и отпускаем")
    public void moveElementTo(Supplier<AtlasWebElement> element, Supplier<AtlasWebElement> target) {
        Actions actions = new Actions(getDriver());
        WebElement movedElement = element.get();
        WebElement targetElement = target.get();
        actions.dragAndDrop(movedElement, targetElement).build().perform();
        Actions actions2 = new Actions(getDriver());
        actions2.release().build().perform();
    }
}
