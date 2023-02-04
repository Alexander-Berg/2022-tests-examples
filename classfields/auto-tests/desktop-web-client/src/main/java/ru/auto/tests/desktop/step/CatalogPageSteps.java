package ru.auto.tests.desktop.step;

import io.qameta.allure.Step;
import ru.auto.tests.desktop.element.catalog.MarkModelGenBlock;

import javax.inject.Inject;
import java.io.IOException;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;


public class CatalogPageSteps extends BasePageSteps {

    @Inject
    public SearcherUserSteps searcherSteps;

    public MarkModelGenBlock getMarkModelGenBlock() {
        return onCatalogPage().filter().markModelGenBlock();
    }

    @Step("Выбираем первую марку в списке")
    public String selectFirstMark() throws IOException {
        String mark = getMarkModelGenBlock().items()
                .waitUntil(hasSize(greaterThan(0))).get(0).getText();
        getMarkModelGenBlock().items().get(0).click();
        onCatalogPage().filter().markModelGenBlock().breadcrumbsItem("Выберите модель").waitUntil(isDisplayed());
        return searcherSteps.getMark(mark).getId();
    }

    @Step("Выбираем первую модель в списке")
    public String selectFirstModel() throws IOException {
        String mark = getMarkModelGenBlock().breadcrumbsItems().get(0).getText();
        String model = getMarkModelGenBlock().items().waitUntil(hasSize(greaterThan(0))).get(0).getText();
        getMarkModelGenBlock().items().get(0).click();
        onCatalogPage().filter().markModelGenBlock().breadcrumbsItem("Выбрать поколение").waitUntil(isDisplayed());
        return searcherSteps.getModel(mark.toUpperCase(), model).getId();
    }

    @Step("Выбираем первое поколение в списке")
    public void selectFirstGen() {
        getMarkModelGenBlock().generationsOrBodiesList().waitUntil(hasSize(greaterThan(0))).get(0).click();
        getMarkModelGenBlock().generationsOrBodiesList().waitUntil(not(isDisplayed()));
        onCatalogPage().filter().markModelGenBlock().breadcrumbsItem("Выбрать кузов").waitUntil(isDisplayed());
    }

    @Step("Выбираем кузов {1}")
    public void selectBody(String name) {
        getMarkModelGenBlock().body(name).waitUntil(isDisplayed()).click();
        getMarkModelGenBlock().generationsOrBodiesList().waitUntil(not(isDisplayed()));
    }

    @Step("Добавляем в сравнение")
    public void addToCompare() {
        onCatalogPage().compareAddButton().waitUntil(isDisplayed()).click();
        onCatalogPage().compareDeleteButton().waitUntil(isDisplayed());
    }

    @Step("Удаляем из сравнения")
    public void deleteFromCompare() {
        onCatalogPage().compareDeleteButton().waitUntil(isDisplayed()).click();
        onCatalogPage().compareAddButton().waitUntil(isDisplayed());
    }
}