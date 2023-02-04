package ru.yandex.realty.page;

import io.qameta.allure.Step;
import io.qameta.atlas.webdriver.AtlasWebElement;
import io.qameta.atlas.webdriver.ElementsCollection;
import io.qameta.atlas.webdriver.WebPage;
import io.qameta.atlas.webdriver.extension.FindBy;
import io.qameta.atlas.webdriver.extension.Name;
import ru.yandex.realty.element.Button;
import ru.yandex.realty.element.profsearch.ProfOffer;
import ru.yandex.realty.element.saleads.WithApartmentFilters;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author kantemirov
 */
public interface ProfSearchPage extends WebPage, WithApartmentFilters, Button, BasePage {

    String RGID = "rgid";
    String MOSCOW_RGID = "587795";
    String DESCRIPTION = "Описание";
    String EXPORT = "Выгрузить";
    String INTO_FAV = "В избранное";

    @Name("Фильтры сортировки офферов")
    @FindBy("//label[contains(@class,'profsearch-offers-head__check-all')]")
    AtlasWebElement checkAll();

    @Name("Список офферов профпоиска")
    @FindBy("//div[@class='profsearch-offer']")
    ElementsCollection<ProfOffer> offerList();

    @Name("Список офферов")
    @FindBy("//div[@class='offer-grid-row']")
    ElementsCollection<ProfOffer> allOffers();

    @Name("Фильтры сортировки офферов")
    @FindBy("//div[contains(@class,'profsearch-offers-head__row')]")
    AtlasWebElement sortHeader();

    @Name("Групповые действия")
    @FindBy("//div[contains(@class,'profsearch-group-actions')]")
    Button groupAction();

    @Step("Формируем список ссылок на офферы")
    default List<String> offerLinks() {
        return allOffers().stream()
                .map(ProfOffer::convertToProdLink)
                .collect(Collectors.toList());
    }

    default ProfOffer offer(int i) {
        return offerList().should(hasSize(greaterThan(i))).get(i);
    }
}
