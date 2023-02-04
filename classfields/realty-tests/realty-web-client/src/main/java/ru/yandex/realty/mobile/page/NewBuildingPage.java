package ru.yandex.realty.mobile.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.mobile.element.Link;
import ru.yandex.realty.mobile.element.listing.TouchSite;
import ru.yandex.realty.mobile.element.newbuilding.DevInfo;
import ru.yandex.realty.mobile.element.newbuilding.DevModal;
import ru.yandex.realty.mobile.element.newbuilding.NewbuildingSubscriptionModal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public interface NewBuildingPage extends BasePage {

    @Name("Список новостроек")
    @FindBy("//li[contains(@class,'SerpListItem_type_offer')]")
    ElementsCollection<TouchSite> siteList();

    @Name("Баннер «ПИК»")
    @FindBy("//aside[@class = 'SearchResultsTopAd']")
    Link topAddBanner();

    @Name("Селектор сортировки")
    @FindBy("//button[contains(@class,'Select__button')]")
    AtlasWebElement sortSelect();

    @Name("Опция «{{ value }}»")
    @FindBy("//select[@name = 'serp_sort_select']//option[contains(., '{{ value }}')]")
    AtlasWebElement option(@Param("value") String value);

    @Name("Модалка подписок")
    @FindBy("//div[contains(@class, 'Modal_visible')]")
    NewbuildingSubscriptionModal subscriptionModal();

    @Name("Информация о застройщике")
    @FindBy("//div[@class = 'SerpDevInfo']")
    DevInfo devInfo();

    @Name("Модалка звонка застройщикам")
    @FindBy("//div[contains(@class, 'Modal_visible')]")
    DevModal devModal();

    default String getSiteId(int i) {
        String siteHref = site(i).link().getAttribute("href");
        Pattern pattern = Pattern.compile("\\/novostrojka\\/\\D+-(\\d+)\\/");
        Matcher matcher = pattern.matcher(siteHref);
        matcher.find();
        return String.format("site_%s", matcher.group(1));
    }

    default TouchSite site(int i) {
        return siteList().should(hasSize(greaterThan(i))).get(i);
    }
}
