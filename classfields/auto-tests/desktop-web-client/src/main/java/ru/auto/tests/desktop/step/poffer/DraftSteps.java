package ru.auto.tests.desktop.step.poffer;

import io.qameta.allure.Step;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.yandex.qatools.htmlelements.matchers.WebElementMatchers;

import javax.inject.Inject;

/**
 * @author Artem Gribanov (avgribanov)
 * @date 05.04.19
 */
public class DraftSteps extends BasePageSteps {

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Step("Выбираем марку c индексом '{{mark}}'")
    public void selectMark(int mark) {
        basePageSteps.onPofferPage().blockMMM().getMark(mark).click();
    }

    @Step("Выбираем марку '{{mark}}'")
    public void selectMark(String mark) {
        basePageSteps.onPofferPage().blockMMM().mark(mark).click();
    }

    @Step("Выбираем модель c индексом '{{model}}'")
    public void selectModel(int model) {
        basePageSteps.onPofferPage().blockMMM().getModel(model).click();
    }

    @Step("Выбираем модель '{{model}}'")
    public void selectModel(String model) {
        basePageSteps.onPofferPage().blockMMM().model(model).click();
    }

    @Step("Выбираем год")
    public void selectYear(int year) {
        basePageSteps.onPofferPage().blockMMM().getItem("Год выпуска", year)
                .click();
    }

    @Step("Выбираем кузов")
    public void selectBody(int body) {
        basePageSteps.onPofferPage().blockMMM().getBody(body).click();
    }

    @Step("Выбираем поколение")
    public void selectGenegation(int generation) {
        basePageSteps.onPofferPage().blockMMM().getItem("Поколение", generation).click();
    }

    @Step("Выбираем двигатель")
    public void selectEngine(int engine) {
        basePageSteps.onPofferPage().blockMMM().getItem("Двигатель", engine).click();
    }

    @Step("Выбираем привод")
    public void createDraftDrives(int drive) {
        basePageSteps.onPofferPage().blockMMM().getItem("Привод", drive).click();
    }

    @Step("Выбираем коробку передач")
    public void selectGearboxes(int gearbox) {
        basePageSteps.onPofferPage().blockMMM().getItem("Коробка передач", gearbox).click();
    }

    @Step("Выбираем модификацию")
    public void selectModification(int i) throws InterruptedException {
        Thread.sleep(1000);
        basePageSteps.onPofferPage().blockMMM().getItem("Модификация", i)
                .waitUntil(WebElementMatchers.isDisplayed()).click();
    }

    @Step("Удаляем старый черновик с помощью очистки кук")
    public void deleteDraftCookie() {
        cookieSteps.deleteCookie("autoruuid");
        cookieSteps.deleteCookie("autoru_sid");
        basePageSteps.refresh();
    }

    @Step("Создаем черновик с заполенной модификацией")
    public void createDraftMMM() throws InterruptedException {
        selectMark("Audi");
        selectModel("A4");
        selectYear(0);
        selectBody(0);
        selectGenegation(0);
        selectEngine(0);
        createDraftDrives(0);
        selectGearboxes(0);
        selectModification(0);
    }
}
