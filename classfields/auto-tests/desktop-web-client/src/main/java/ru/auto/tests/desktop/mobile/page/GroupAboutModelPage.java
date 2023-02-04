package ru.auto.tests.desktop.mobile.page;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.auto.tests.commons.extension.element.VertisElement;
import ru.auto.tests.desktop.mobile.component.WithGallery;
import ru.auto.tests.desktop.mobile.component.WithMag;
import ru.auto.tests.desktop.mobile.component.WithReviews;
import ru.auto.tests.desktop.mobile.component.WithVideos;
import ru.auto.tests.desktop.mobile.element.group.Related;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public interface GroupAboutModelPage extends BasePage, WithReviews, WithVideos, WithGallery, WithMag {

    @Name("Список цветов")
    @FindBy(".//div[contains(@class, 'CardGroupImageGalleryMobile__colorContainer')]")
    ElementsCollection<VertisElement> colorsList();

    @Step("Получаем цвет с индексом {i}")
    default VertisElement getColor(int i) {
        return colorsList().should(hasSize(greaterThan(i))).get(i);
    }

    @Name("Похожие")
    @FindBy("//div[contains(@class, 'RelatedGroups')]")
    Related related();
}