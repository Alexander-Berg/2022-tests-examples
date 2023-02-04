package ru.yandex.realty.page;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.realty.element.Link;
import ru.yandex.realty.element.saleads.InputField;

public interface JournalPage extends BasePage, Link, InputField {

    @Name("Ссылка слева «{{ value }}»")
    @FindBy(".//div[contains(@class, 'PageAside__categories')]/a[contains(.,'{{ value }}')]")
    AtlasWebElement asideLink(@Param("value") String value);

     @Name("Хлебная крошка «{{ value }}»")
    @FindBy(".//a[contains(.,'{{ value }}') and contains(@class,'Breadcrumbs__crumb')]")
    AtlasWebElement breadcrumb(@Param("value") String value);

    @Name("Правая стрелка пейджера")
    @FindBy(".//label[contains(@class,'Pager2__next')]/button")
    AtlasWebElement pagerNext();

    @Name("Левая стрелка пейджера")
    @FindBy(".//label[contains(@class,'Pager2__prev')]/button")
    AtlasWebElement pagerPrev();

    @Name(" «{{ value }}»")
    @FindBy(".//span[contains(@class,'Pager2__radioGroup')]//button[contains(.,'{{ value }}')]")
    AtlasWebElement page(@Param("value") String value);

    @Name("Кнопка перехода предыдущая/следующая статья «{{ value }}»")
    @FindBy(".//div[contains(@class,'PostBeforeAfter__wrap')]/a[contains(., '{{ value }}')]")
    AtlasWebElement beforeAfterButton(@Param("value") String value);

    @Name("Тег «{{ value }}»")
    @FindBy(".//div[contains(@class,'PostFooter__tags')]/a[contains(.,'{{ value }}')]")
    AtlasWebElement tag(@Param("value") String value);

    @Name("Статус подписки")
    @FindBy(".//span[contains(@class,'PostSubscriptionForm__statusText')]")
    AtlasWebElement subscribeStatus();

    @Name("Блок картинок поста")
    @FindBy(".//div[contains(@class,'PostGalleryBlock__images') and not(contains(@class,'Wrap'))]")
    AtlasWebElement picturesBlock();

    @Name("Листать картинки влево")
    @FindBy(".//button[@data-test='button-left']")
    AtlasWebElement pictureSwipeButtonLeft();

    @Name("Листать картинки вправо")
    @FindBy(".//button[@data-test='button-right']")
    AtlasWebElement pictureSwipeButtonRight();

    @Name("Галерея")
    @FindBy(".//div[@class='FSGalleryLayout']")
    AtlasWebElement gallery();

    @Name("Закрыть галерею")
    @FindBy(".//i[contains(@class,'FSGalleryCloseIcon')]")
    AtlasWebElement closeGallery();

    @Name("Карточки новостроек")
    @FindBy(".//a[contains(@class,'PostRealtyNewBuildingBlockSiteCard__card')]")
    ElementsCollection<AtlasWebElement> newbuildingCards();

    @Name("Карточки офферов")
    @FindBy(".//a[contains(@class,'PostRealtyOffersBlockOfferCard__card')]")
    ElementsCollection<AtlasWebElement> offerCards();

    @Name("Поделяшка «{{ value }}»")
    @FindBy(".//*[contains(@class,'PostShare__button') and contains(.,'{{ value }}')]")
    AtlasWebElement shareService(@Param("value") String value);
}
