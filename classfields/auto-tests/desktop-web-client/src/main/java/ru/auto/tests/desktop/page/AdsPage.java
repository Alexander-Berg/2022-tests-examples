package ru.auto.tests.desktop.page;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import org.hamcrest.Matcher;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.element.ads.AdBanner;
import ru.yandex.qatools.htmlelements.matchers.WebElementMatchers;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author kurau (Yuri Kalinin)
 */
public interface AdsPage extends BasePage {

    int WIDE_SCREEN = 1280;
    int NARROW_WINDOW = 1200;
    int REVIEWS_NARROW_WINDOW = 1219;
    int REVIEWS_WIDE_SCREEN = 1330;

    // https://wiki.yandex-team.ru/users/cofemiss/ad/reklamnye-mesta-glavnaja-bu-novye/#sxemartb-blokov2018
    String TOP = "top";
    String C1 = "c1";
    String C2 = "c2";
    String C3 = "c3";
    String R1 = "r1";
    String BUTTON = "button";
    String CB = "cb";
    String JOURNAL = "journal";
    String GALLERY = "gallery";

    //mobile banners
    String TOP_MOBILE = "top";
    String CAROUSEL = "carousel";
    String LISTING1 = "listing-1";
    String LISTING2 = "listing-2";
    String LISTING3 = "listing-3";
    String LISTING4 = "listing-4";
    String CARD_BUTTON = "card-credit-button";
    String GALLERY_MOBILE = "gallery";

    @Name("Список баннеров")
    //@FindBy("//yatag[@lang='ru' or contains(@class, 'ya_partner') or contains(@id, 'ya_partner')]")
    @FindBy("//*[contains(local-name(), 'vertisads')]")
    ElementsCollection<AdBanner> ads();

    @Name("Баннер «{{ value }}»")
    @FindBy("//*[contains(local-name(), 'vertisads-{{ value }}')]")
    AdBanner ad(@Param("value") String value);

    @Name("Баннер «{{ value }}» и элемент с shadowRoot")
    @FindBy("//*[local-name() = 'vertisads-{{ value }}']/*/div")
    VertisElement adWithShadowRoot(@Param("value") String value);

    @Name("Баннер «{{ value }} на второй странице»")
    @FindBy("(//*[contains(local-name(), 'vertisads-{{ value }}')])[2]")
    AdBanner adPage2(@Param("value") String value);

    @Name("Баннер CB»")
    @FindBy("(//*[local-name() = 'vertisads-cb'])[2]")
    AdBanner adCB();

    default ElementsCollection<AdBanner> visibleAds() {
        return ads().should(hasSize(greaterThan(0)))
                .filter(e -> WebElementMatchers.isDisplayed().matches(e));
    }

    @Step("Должно быть «{matcher}» рекламных баннеров на странице")
    default void shouldSeeAds(Matcher matcher) {
        visibleAds().waitUntil(String.format("На странице должно быть «%s» рекламных банеров", matcher), hasSize(matcher), 20);
    }

    @Step("Должно быть «{i}» рекламных баннеров на странице")
    default void shouldSeeAds(int i) {
        visibleAds().waitUntil(String.format("На странице должно быть «%d» рекламных банеров", i), hasSize(i), 20);
    }

    @Step("Кликаем на баннер «{bannerId}»")
    default void clickAtBanner(String bannerId) {
        if (bannerId.equals(C3)) {
            footer().hover();
        }

        ad(bannerId).waitUntil("Баннер не появился", WebElementMatchers.isDisplayed(), 10)
                .hover().click();
    }

}
