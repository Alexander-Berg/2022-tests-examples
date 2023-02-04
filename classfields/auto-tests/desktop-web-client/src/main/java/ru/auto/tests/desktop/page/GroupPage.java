package ru.auto.tests.desktop.page;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.component.WithBillingModalPopup;
import ru.auto.tests.desktop.component.WithBreadcrumbs;
import ru.auto.tests.desktop.component.WithCheckbox;
import ru.auto.tests.desktop.component.WithContactsPopup;
import ru.auto.tests.desktop.component.WithFullScreenGallery;
import ru.auto.tests.desktop.component.WithGallery;
import ru.auto.tests.desktop.component.WithPager;
import ru.auto.tests.desktop.component.WithReviews;
import ru.auto.tests.desktop.component.WithSelect;
import ru.auto.tests.desktop.component.WithShare;
import ru.auto.tests.desktop.component.WithVideos;
import ru.auto.tests.desktop.element.group.Complectations;
import ru.auto.tests.desktop.element.group.GroupColorsPopup;
import ru.auto.tests.desktop.element.group.GroupComplectationsPopup;
import ru.auto.tests.desktop.element.group.GroupOffer;
import ru.auto.tests.desktop.element.group.GroupOffers;
import ru.auto.tests.desktop.element.group.GroupTabs;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface GroupPage extends BasePage,
        WithBreadcrumbs,
        WithBillingModalPopup,
        WithShare,
        WithContactsPopup,
        WithSelect,
        WithCheckbox,
        WithGallery,
        WithFullScreenGallery,
        WithVideos,
        WithReviews,
        WithPager {

    @Name("Содержимое страницы")
    @FindBy("//div[@class = 'PageCardGroup']")
    VertisElement pageContent();

    @Name("Заголовок группы")
    @FindBy("//div[@class = 'CardGroupHeaderDesktop'] | " +
            "//div[contains(@class, 'CardGroupHeaderDesktop__title')] | " +
            "//div[contains(@class, 'CardHead-module__groupHead')] | " +
            "//div[contains(@class, 'CardNewHead')]")
    VertisElement groupHeader();

    @Name("Вкладки")
    @FindBy("//div[contains(@class, 'CardGroupTabs')]")
    GroupTabs groupTabs();

    @Name("Блок со списком предолжений")
    @FindBy("//div[@name = 'CardGroupOffers']")
    GroupOffers groupOffers();

    @Name("Список предложений")
    @FindBy(".//div[contains(@class, 'CardGroupListingItem ')]")
    ElementsCollection<GroupOffer> groupOffersList();

    @Name("Кнопка «Показать ещё N предложений»")
    @FindBy("//div[contains(@class, 'CardGroupOffersList__moreButton')]")
    VertisElement showMoreOffersButton();

    @Name("Вкладка «{{ text }}»")
    @FindBy("//div[contains(@class, 'TabsItem ') and . = '{{ text }}']")
    VertisElement tab(@Param("text") String Text);

    @Name("Вкладка «Характеристики»")
    @FindBy("//div[@class = 'CardTechInfo']")
    VertisElement specifications();

    @Name("Поп-ап выбора цвета")
    @FindBy("//div[contains(@class, 'CardGroupFilterColor__selectMenu')]")
    GroupColorsPopup groupColorsPopup();

    @Name("Поп-ап комплектаций")
    @FindBy("//div[contains(@class, 'CardGroupFilterComplectation__selectPopup')]")
    GroupComplectationsPopup groupComplectationsPopup();

    @Name("Комплектация «{{ text }}»")
    @FindBy("//div[contains(@class, 'CardGroupComplectationSelectorItem') and contains(., '{{ text }}')]")
    VertisElement complectation(@Param("text") String Text);

    @Name("Выбранная комплектация")
    @FindBy("//div[contains(@class, 'CardGroupComplectationSelectorItem_selected')]//span[contains(@class, 'name')] | " +
            "//div[contains(@class, 'CardGroupComplectationSelectorItem_selected')]//div[contains(@class, 'title')]")
    VertisElement selectedComplectation();

    @Name("Содержимое вкладки «Комплектации»")
    @FindBy("//div[@class = 'CardGroupComplectations']")
    Complectations complectations();

    @Name("Кнопка «Подписаться на новые»")
    @FindBy("//div[contains(@class, 'CardGroupOffersFilters__subscription')]")
    VertisElement subscribeButton();

    @Step("Получаем предложение с индексом {i}")
    default GroupOffer getOffer(int i) {
        return groupOffersList().should(hasSize(greaterThan(i))).get(i);
    }
}
