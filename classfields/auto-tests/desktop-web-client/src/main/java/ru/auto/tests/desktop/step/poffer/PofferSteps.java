package ru.auto.tests.desktop.step.poffer;

import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.adaptor.PassportApiAdaptor;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.vos2.step.VosUserSteps;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@Getter
@Setter
public class PofferSteps extends BasePageSteps {

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private LoginSteps loginSteps;

    @Inject
    private AccountManager am;

    @Inject
    private PassportApiAdaptor adaptor;

    @Inject
    private VosUserSteps vosUserSteps;

    public List<String> salesIds = new ArrayList<>();

    @Step("Заполняем марку")
    public void fillMark(String mark) {
        waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onPofferPage().markBlock().mark(mark).click();
    }

    @Step("Заполняем модель")
    public void fillModel(String model) {
        basePageSteps.onPofferPage().modelBlock().model(model).waitUntil(isDisplayed()).click();
    }

    @Step("Заполняем год")
    public void fillYear(String year) {
        basePageSteps.onPofferPage().yearBlock().radioButton(year).waitUntil(isDisplayed()).click();
    }

    @Step("Заполняем кузов")
    public void fillBody(String body) {
        basePageSteps.onPofferPage().bodyBlock().radioButton(body).waitUntil(isDisplayed()).click();
    }

    @Step("Заполняем поколение")
    public void fillGeneration(String generation) {
        basePageSteps.onPofferPage().generationBlock().radioButton(generation).waitUntil(isDisplayed()).click();
    }

    @Step("Заполняем двигатель")
    public void fillEngine(String engine) {
        basePageSteps.onPofferPage().engineBlock().radioButton(engine).waitUntil(isDisplayed()).click();
    }

    @Step("Заполняем привод")
    public void fillDrive(String drive) {
        basePageSteps.onPofferPage().driveBlock().radioButton(drive).waitUntil(isDisplayed()).click();
    }

    @Step("Заполняем коробку передач")
    public void fillGearbox(String gearbox) {
        basePageSteps.onPofferPage().gearboxBlock().radioButton(gearbox).waitUntil(isDisplayed()).click();
    }

    @Step("Заполняем модификацию")
    public void fillModification(String modification) {
        basePageSteps.onPofferPage().modificationBlock().radioButton(modification).waitUntil(isDisplayed()).click();
    }

    @Step("Заполняем цвет")
    public void fillColor(String color) {
        basePageSteps.onPofferPage().color(color).waitUntil(isDisplayed()).click();
    }

    @Step("Заполняем пробег")
    public void fillRun(String run) {
        basePageSteps.onPofferPage().runBlock().input("run", run);
    }

    @Step("Заполняем цену")
    public void fillPrice(String price) {
        basePageSteps.onPofferPage().priceBlock().input("price", price);
    }

    @Step("Заполняем скидку за кредит")
    public void fillCreditDiscount(String discount) {
        basePageSteps.onPofferPage().priceBlock().input("credit_discount", discount);
    }

    @Step("Заполняем скидку за КАСКО")
    public void fillInsuranceDiscount(String discount) {
        basePageSteps.onPofferPage().priceBlock().input("insurance_discount", discount);
    }

    @Step("Заполняем скидку за трейд-ин")
    public void fillTradeInDiscount(String discount) {
        basePageSteps.onPofferPage().priceBlock().input("tradein_discount", discount);
    }

    @Step("Заполняем максимальную скидку")
    public void fillMaxDiscount(String discount) {
        basePageSteps.onPofferPage().priceBlock().input("max_discount", discount);
    }

    @Step("Заполняем наличие")
    public void fillAvailability(String availability) {
        basePageSteps.onPofferPage().availabilityBlock().radioButton(availability).click();
    }

    @Step("Заполняем пробег")
    public void fillExchange() {
        basePageSteps.onPofferPage().checkbox("Возможен обмен").click();
    }

    @Step("Заполняем ПТС")
    public void fillPts(String pts) {
        basePageSteps.onPofferPage().ptsBlock().radioButton(pts).click();
    }

    @Step("Заполняем количество владельцев")
    public void fillOwnersCount(String ownersCount) {
        basePageSteps.onPofferPage().ptsBlock().radioButton(ownersCount).click();
    }

    @Step("Заполняем дату приобретения")
    public void fillPurchaseDate(String year, String month) {
        basePageSteps.onPofferPage().ptsBlock().selectItem("Год", year);
        basePageSteps.onPofferPage().ptsBlock().selectItem("Месяц", month);
    }

    @Step("Заполняем госномер")
    public void fillPlateNumber(String plateNumber) {
        basePageSteps.onPofferPage().vinBlock().input("gos-number__series-number", plateNumber);
    }

    @Step("Заполняем регион госномера")
    public void fillPlateRegion(String plateRegion) {
        basePageSteps.onPofferPage().vinBlock().input("gos-number__region", plateRegion);
    }

    @Step("Заполняем VIN")
    public void fillVin(String vin) {
        basePageSteps.onPofferPage().vinBlock().input("vin", vin);
    }

    @Step("Заполняем СТС")
    public void fillSts(String sts) {
        basePageSteps.onPofferPage().vinBlock().input("Свидетельство о регистрации (СТС)", sts);
    }

    @Step("Заполняем комментарий продавца")
    public void fillComment(String comment) {
        basePageSteps.onPofferPage().input("Честно опишите достоинства и недостатки своего автомобиля.", comment);
    }

    @Step("Заполняем имя")
    public void fillName(String name) {
        basePageSteps.onPofferPage().input("Как к вам обращаться?", name);
    }

    @Step("Заполняем e-mail")
    public void fillEmail(String email) {
        basePageSteps.onPofferPage().input("Электронная почта (e-mail)", email);
    }

    @Step("Заполняем место продажи")
    public void fillPlace(String place) {
        basePageSteps.onPofferPage().contactsBlock().input("Место осмотра", place);
    }

    @Step("Загружаем фото")
    public void fillPhoto(String photo) {
        String imgPath = "//images.mds-proxy.test.avto.ru/get-autoru-vos/";
        basePageSteps.onPofferPage().photoBlock().photo()
                .sendKeys(new File(format("src/main/resources/images/%s.jpg", photo)).getAbsolutePath());
        basePageSteps.onPofferPage().photoEditor().button("Готово")
                .waitUntil("Фото не загрузилось", isDisplayed(), 10).click();
        basePageSteps.onPofferPage().photoBlock().getPhoto(0).waitUntil(isDisplayed());
        basePageSteps.onPofferPage().photoBlock().getPhoto(0).image().waitUntil("Фото не загрузилось",
                hasAttribute("src", startsWith(format("https:%s", imgPath))), 10);
    }

    @Step("Добавляем кастомный бейдж")
    public void addBadge(String badge) {
        onPofferPage().badges().input("Укажите свой текст (до 25 символов)", badge);
        onPofferPage().badges().submitButton().click();
        onPofferPage().badges().button(badge).waitUntil(isDisplayed());
    }

    @Step("Размещаем объявление")
    public void submitForm() {
        waitSomething(5, TimeUnit.SECONDS);
        basePageSteps.onPofferPage().submitButton().waitUntil(isEnabled()).click();
        waitSomething(5, TimeUnit.SECONDS);
    }

    @Step("Создаём пользователя и привязываем его к дилеру")
    public Account linkUserToDealer() {
        Account account = am.create();
        adaptor.addEmailToAccountForUserWithPhone(account.getId(), account.getLogin(), getRandomEmail());
        adaptor.linkUserToClient(account.getId(), "20101", "8");
        return account;
    }

    @Step("Отвязываем пользователя от дилера")
    public void unlinkUserFromDealer(String userId) {
        adaptor.unlinkUserFromClient(userId);
    }

    @Step("Скрываем мешающие элементы на форме")
    public void hideElements() {
        hideElement(onPofferPage().discountTimer());
        hideElement(onPofferPage().randomVas());
        hideElement(onPofferPage().bug());
        hideElement(onPofferPage().openAssistant());
    }

}
