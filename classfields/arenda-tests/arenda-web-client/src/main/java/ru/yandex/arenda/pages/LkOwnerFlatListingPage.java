package ru.yandex.arenda.pages;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import io.qameta.atlas.webdriver.extension.Param;
import ru.yandex.arenda.element.lk.ownerlk.ToDoRow;
import ru.yandex.arenda.element.lk.ownerlk.InsuranceModal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;

public interface LkOwnerFlatListingPage extends BasePage {

    @Name("Кнопки навигации по квартирам собственника")
    @FindBy("//div[contains(@class,'NavigationFlatIcon__container')]")
    ElementsCollection<AtlasWebElement> navFlatsButton();

    default AtlasWebElement firstNavFlatsButton() {
        navFlatsButton().waitUntil(hasSize(greaterThan(0))).get(0).waitUntil(hasClass(containsString("_isActive")));
        return navFlatsButton().get(0);
    }

    @Name("Ссылка на страницу с фотографиями")
    @FindBy("//span[contains(@class,'NavigationDesktopServiceButton__serviceButton') and contains(.,'О квартире')]")
    AtlasWebElement flatPhotoLink();


    @Name("Тудушка «{{ value }}»")
    @FindBy("//div[contains(@class,'TodoNotificationItem__container') and contains(.,'{{ value }}')]")
    ToDoRow toDoRow(@Param("value") String value);

    @Name("Настройки ЖКХ")
    @FindBy("//div[contains(@class,'NotificationCard__container') and contains(.,'Настройте раздел коммуналки')]//a")
    AtlasWebElement adjustJkhButton();

    @Name("Модалка со страховкой")
    @FindBy("//div[contains(@class,'OwnerFlatInsuredModal__modal')]")
    InsuranceModal insuranceModal();
}
