package ru.yandex.arenda.pages;

import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.arenda.element.lk.admin.ManagerFlatFilters;
import ru.yandex.arenda.element.lk.admin.ManagerFlatItem;
import ru.yandex.arenda.element.lk.admin.ManagerUserItem;
import ru.yandex.arenda.element.lk.admin.ManagerUsersFilters;
import ru.yandex.qatools.htmlelements.matchers.WebElementMatchers;

import static org.hamcrest.Matchers.not;
import static ru.yandex.arenda.steps.MainSteps.FIRST;

public interface AdminListingPage extends BasePage {

    @Name("Админские фильтры квартир")
    @FindBy(".//div[contains(@class,'ManagerSearchFlatsFilters__wrapper')]")
    ManagerFlatFilters managerFlatFilters();

    @Name("Админские фильтры пользователей")
    @FindBy(".//div[contains(@class,'ManagerSearchUsersFilters__wrapper')]")
    ManagerUsersFilters managerUserFilters();

    @Name("Список квартир")
    @FindBy(".//div[contains(@class,'ManagerSearchFlatsItem__container')]")
    ElementsCollection<ManagerFlatItem> managerFlatsItem();

    @Name("Список юзеров")
    @FindBy(".//*[contains(@class,'ManagerSearchUsersItem__listItem')]")
    ElementsCollection<ManagerUserItem> managerUsersItem();

    @Name("Пустой листинг квартир")
    @FindBy(".//div[contains(@class,'ManagerSearchFlatsList__notFound')]")
    AtlasWebElement flatNotFoundListing();


    @Name("Пустой листинг квартир")
    @FindBy(".//div[contains(@class,'ManagerSearchUsersList__notFound')]")
    AtlasWebElement userNotFoundListing();

    default ManagerFlatItem managerFlatsItemFirst() {
        managerFlatsItem().get(FIRST).skeletonItem().waitUntil(not(WebElementMatchers.isDisplayed()));
        return managerFlatsItem().get(FIRST);
    }
}
