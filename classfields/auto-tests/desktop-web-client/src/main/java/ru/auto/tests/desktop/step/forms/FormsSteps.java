package ru.auto.tests.desktop.step.forms;

import io.qameta.allure.Step;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.passport.account.Account;
import ru.auto.tests.passport.adaptor.PassportApiAdaptor;
import ru.auto.tests.passport.manager.AccountManager;
import ru.auto.tests.vos2.step.VosUserSteps;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_ARRAY_ITEMS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.auto.tests.jsonunit.matcher.JsonPatchMatcher.jsonEquals;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@Getter
@Setter
public class FormsSteps extends BasePageSteps {

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private PassportApiAdaptor passportApiAdaptor;

    @Inject
    private AccountManager am;

    @Inject
    private PassportApiAdaptor adaptor;

    @Inject
    private VosUserSteps vosUserSteps;

    public List<String> salesIds = new ArrayList<>();

    public Field state = new Field("Какой транспорт вы продаете?", "radio");
    public Field category = new Field("Категория", "radio");
    public Field mark = new Field("Укажите марку", "radio");
    public Field markSearch = new Field("Укажите марку", "input", "Поиск марки", "");
    public Field model = new Field("Укажите модель", "radio");
    public Field generation = new Field("Поколение", "radio");
    public Field modelSearch = new Field("Укажите модель", "input", "Поиск модели", "");
    public Field type = new Field("Тип", "radio");
    public Field year = new Field("Год выпуска", "input", "Год", "2000");
    public Field run = new Field("Пробег, км", "input", "Пробег, км", "100");
    public Field volume = new Field("Объём двигателя в см³", "input", "Объём, см³", "500");
    public Field engine = new Field("Двигатель", "radio");
    public Field cylinders = new Field("Расположение цилиндров", "radio", "V-образное");
    public Field cylindersCount = new Field("Кол-во цилиндров", "radio", "1");
    public Field power = new Field("Мощность, л.с.", "input", "Мощность, л.с.", "500");
    public Field drive = new Field("Привод", "radio", "Кардан");
    public Field transmission = new Field("Коробка", "radio", "Вариатор");
    public Field modification = new Field("Модификация", "radio");
    public Field strokes = new Field("Число тактов", "radio", "2");
    public Field color = new Field("Укажите цвет", "color", "FAFBFB");
    public Field price = new Field("Цена, \u20BD", "input", "Цена, \u20BD", "500000");
    public Field nds = new Field("Цена, \u20BD", "checkbox", "Цена с НДС", "");
    public Field exchange = new Field("Цена, \u20BD", "checkbox", "Возможен обмен", "");
    public Field haggle = new Field("Цена, \u20BD", "checkbox", "Возможен торг", "");
    public Field user = new Field("Как к вам обращаться?", "input", "Как к вам обращаться?", "Тест");
    public Field email = new Field("Электронная почта (e-mail)", "input", "Электронная почта (e-mail)",
            "test@test.org");
    public Field place = new Field("Место осмотра", "input", "Место осмотра", "Покровка, 6");
    public Field phone = new Field("Номер телефона", "input", "Номер телефона", "");
    public Field pts = new Field("Паспорт транспортного средства", "radio", "ПТС",
            "Оригинал");
    public Field ownersCount = new Field("Паспорт транспортного средства", "radio", "1");
    public Field buyDateYear = new Field("Паспорт транспортного средства", "input", "Год",
            "2016");
    public Field buyDateMonth = new Field("Паспорт транспортного средства", "input",
            "Месяц (от 1 до 12)", "6");
    public Field warranty = new Field("Паспорт транспортного средства", "checkbox",
            "На гарантии", "");
    public Field vin = new Field("Паспорт транспортного средства", "input", "VIN",
            "JHMCU26809C211512");
    public Field sts = new Field("Паспорт транспортного средства", "input",
            "Свидетельство о регистрации (СТС)", "1234567890");
    public Field beaten = new Field("Паспорт транспортного средства", "checkbox", "Битый или не на ходу",
            "off");
    public Field customs = new Field("Паспорт транспортного средства", "checkbox", "Не растаможен",
            "off");
    public Field description = new Field("Укажите полное описание", "input", "description",
            "Мопед не мой, но с НДС");
    public Field photo = new Field("Добавить фотографии", "photo");
    public Field load = new Field("Загрузка, кг", "input", "Загрузка, кг", "1000");
    public Field bodyType = new Field("Тип кузова", "radio", "Автотопливозаправщик");
    public Field seatsCount = new Field("Количество мест", "input", "Количество мест", "10");
    public Field steeringWheel = new Field("Руль", "radio", "Левый");
    public Field owningTime = new Field("Срок владения", "radio", "Менее 6 месяцев");
    public Field complectation = new Field("Комплектация", "checkbox", "Электростартер", "");
    public Field evaluateReason = new Field("Для чего оцениваете свой автомобиль?", "radio");
    public Field multimedia = new Field("Мультимедиа", "checkbox", "Розетка 12V", "");
    public Field reviewTitle = new Field("Ваш отзыв", "input", "Заголовок",
            "Заголовок отзыва");
    public Field reviewText = new Field("Ваш отзыв", "input", "Текст отзыва", "");
    public Field reviewRatingExterior = new Field("Оцените модель вашего автомобиля", "rating",
            "Внешний вид", "5");
    public Field reviewRatingComfort = new Field("Оцените модель вашего автомобиля", "rating",
            "Комфорт", "5");
    public Field reviewRatingSafety = new Field("Оцените модель вашего автомобиля", "rating",
            "Безопасность", "5");
    public Field reviewRatingDrive = new Field("Оцените модель вашего автомобиля", "rating",
            "Ходовые качества", "5");
    public Field reviewRatingReliability = new Field("Оцените модель вашего автомобиля", "rating",
            "Надёжность", "5");
    public Field reviewPluses = new Field("Плюсы", "inputs_list", "плюс");
    public Field reviewMinuses = new Field("Минусы", "inputs_list", "минус");
    public Field reviewPhoto = new Field("Ваш отзыв", "review_photo");

    public Boolean reg = true;

    public List<Field> fields = new ArrayList<>();

    public class Field {

        @Getter
        public String block, type, name, value;

        public Field(String block, String type, String name, String value) {
            this.block = block;
            this.type = type;
            this.name = name;
            this.value = value;
        }

        public Field(String block, String type, String value) {
            this.block = block;
            this.type = type;
            this.value = value;
        }

        public Field(String block, String type) {
            this.block = block;
            this.type = type;
        }

        public Field setBlock(String block) {
            this.block = block;
            return this;
        }

        public Field setType(String type) {
            this.type = type;
            return this;
        }

        public Field setName(String name) {
            this.name = name;
            return this;
        }

        public Field setValue(String value) {
            this.value = value;
            return this;
        }
    }

    public void createMotorcyclesForm() {
        fields.add(category.setValue("Мотоциклы"));
        fields.add(mark.setValue("ABM"));
        fields.add(model.setValue("Alpha 110"));
        fields.add(type.setBlock("Тип мотоцикла").setValue("Allround"));
        fields.add(year);
        fields.add(run);
        fields.add(volume);
        fields.add(engine.setValue("Дизель"));
        fields.add(cylinders);
        fields.add(cylindersCount);
        fields.add(power);
        fields.add(drive);
        fields.add(transmission.setValue("1 передача"));
        fields.add(strokes);
        fields.add(color);
        fields.add(price);
        fields.add(exchange);
        fields.add(haggle);
        fields.add(user);
        fields.add(email);
        fields.add(place);
        fields.add(phone);
        fields.add(ownersCount);
        fields.add(buyDateYear);
        fields.add(buyDateMonth);
        fields.add(warranty);
        fields.add(vin);
        fields.add(sts);
        fields.add(beaten);
        fields.add(customs);
        fields.add(complectation);
        fields.add(description);
        fields.add(photo);
    }

    public void createMotorcyclesDealerNewForm() {
        fields.add(state.setValue("Новый"));
        fields.add(category.setValue("Мотоциклы"));
        fields.add(mark.setValue("ABM"));
        fields.add(model.setValue("Alpha 110"));
        fields.add(type.setBlock("Тип мотоцикла").setValue("Allround"));
        fields.add(year);
        fields.add(volume);
        fields.add(engine.setValue("Дизель"));
        fields.add(cylinders);
        fields.add(cylindersCount);
        fields.add(power);
        fields.add(drive);
        fields.add(transmission.setValue("1 передача"));
        fields.add(strokes);
        fields.add(color);
        fields.add(price);
        fields.add(complectation);
        fields.add(description);
        fields.add(photo);
    }

    public void createMotorcyclesDealerUsedForm() {
        fields.add(state.setValue("С пробегом"));
        fields.add(category.setValue("Мотоциклы"));
        fields.add(mark.setValue("ABM"));
        fields.add(model.setValue("Alpha 110"));
        fields.add(type.setBlock("Тип мотоцикла").setValue("Allround"));
        fields.add(year);
        fields.add(run);
        fields.add(volume);
        fields.add(engine.setValue("Дизель"));
        fields.add(cylinders);
        fields.add(cylindersCount);
        fields.add(power);
        fields.add(drive);
        fields.add(transmission.setValue("1 передача"));
        fields.add(strokes);
        fields.add(color);
        fields.add(price);
        fields.add(ownersCount);
        fields.add(buyDateYear);
        fields.add(buyDateMonth);
        fields.add(warranty);
        fields.add(complectation);
        fields.add(description);
        fields.add(photo);
    }

    public void createScootersForm() {
        fields.add(category.setValue("Скутеры"));
        fields.add(mark.setValue("Honda"));
        fields.add(model.setValue("Giorno"));
        fields.add(year);
        fields.add(run);
        fields.add(strokes);
        fields.add(volume);
        fields.add(engine.setValue("Инжектор"));
        fields.add(power);
        fields.add(transmission);
        fields.add(color);
        fields.add(price);
        fields.add(user);
        fields.add(email);
        fields.add(place);
        fields.add(phone);
        fields.add(ownersCount);
        fields.add(buyDateYear);
        fields.add(buyDateMonth);
        fields.add(description);
        fields.add(photo);
    }

    public void createAtvForm() {
        fields.add(category.setValue("Мотовездеходы"));
        fields.add(mark.setValue("Yamaha"));
        fields.add(model.setValue("Grizzly 550"));
        fields.add(type.setBlock("Тип вездехода").setValue("Амфибия"));
        fields.add(year);
        fields.add(run);
        fields.add(volume);
        fields.add(engine.setValue("Инжектор"));
        fields.add(cylindersCount);
        fields.add(power);
        fields.add(drive.setValue("Полный"));
        fields.add(transmission);
        fields.add(cylinders);
        fields.add(strokes);
        fields.add(color);
        fields.add(price);
        fields.add(user);
        fields.add(email);
        fields.add(place);
        fields.add(phone);
        fields.add(ownersCount);
        fields.add(buyDateYear);
        fields.add(buyDateMonth);
        fields.add(description);
        fields.add(photo);
    }

    public void createSnowmobilesForm() {
        fields.add(category.setValue("Снегоходы"));
        fields.add(mark.setValue("Stels"));
        fields.add(model.setValue("Viking 600"));
        fields.add(type.setBlock("Тип снегохода").setValue("Спортивный кроссовый"));
        fields.add(year);
        fields.add(run);
        fields.add(volume);
        fields.add(engine.setValue("Инжектор"));
        fields.add(cylindersCount);
        fields.add(power);
        fields.add(strokes);
        fields.add(cylinders);
        fields.add(color);
        fields.add(price);
        fields.add(user);
        fields.add(email);
        fields.add(place);
        fields.add(phone);
        fields.add(ownersCount);
        fields.add(buyDateYear);
        fields.add(buyDateMonth);
        fields.add(description);
        fields.add(photo);
    }

    public void createLcvForm() {
        fields.add(category.setValue("Лёгкие коммерческие"));
        fields.add(mark.setValue("ГАЗ"));
        fields.add(model.setValue("ГАЗель (2705)"));
        fields.add(load);
        fields.add(year);
        fields.add(run);
        fields.add(bodyType);
        fields.add(drive.setValue("Полный"));
        fields.add(engine.setValue("Дизель"));
        fields.add(transmission.setValue("Автомат"));
        fields.add(seatsCount);
        fields.add(volume);
        fields.add(power);
        fields.add(steeringWheel);
        fields.add(color);
        fields.add(price);
        fields.add(user);
        fields.add(email);
        fields.add(place);
        fields.add(phone);
        fields.add(ownersCount);
        fields.add(buyDateYear);
        fields.add(buyDateMonth);
        fields.add(warranty);
        fields.add(complectation.setName("Антиблокировочная система (ABS)"));
        fields.add(description);
        fields.add(photo);
    }

    public void createLcvDealerNewForm() {
        fields.add(state.setValue("Новый"));
        fields.add(category.setValue("Лёгкие коммерческие"));
        fields.add(mark.setValue("ГАЗ"));
        fields.add(model.setValue("ГАЗель (2705)"));
        fields.add(load);
        fields.add(year);
        fields.add(bodyType);
        fields.add(drive.setValue("Полный"));
        fields.add(engine.setValue("Дизель"));
        fields.add(transmission.setValue("Автомат"));
        fields.add(seatsCount);
        fields.add(volume);
        fields.add(power);
        fields.add(steeringWheel);
        fields.add(color);
        fields.add(price);
        fields.add(complectation.setName("Антиблокировочная система (ABS)"));
        fields.add(description);
        fields.add(photo);
    }

    public void createLcvDealerUsedForm() {
        fields.add(state.setValue("С пробегом"));
        fields.add(category.setValue("Лёгкие коммерческие"));
        fields.add(mark.setValue("ГАЗ"));
        fields.add(model.setValue("ГАЗель (2705)"));
        fields.add(load);
        fields.add(year);
        fields.add(run);
        fields.add(bodyType);
        fields.add(drive.setValue("Полный"));
        fields.add(engine.setValue("Дизель"));
        fields.add(transmission.setValue("Автомат"));
        fields.add(seatsCount);
        fields.add(volume);
        fields.add(power);
        fields.add(steeringWheel);
        fields.add(color);
        fields.add(price);
        fields.add(nds);
        fields.add(ownersCount);
        fields.add(complectation.setName("Антиблокировочная система (ABS)"));
        fields.add(description);
        fields.add(photo);
    }

    public void createReviewsCarsForm() {
        fields.add(mark.setValue("Audi"));
        fields.add(model.setValue("A4"));
        fields.add(year.setBlock("Год").setType("radio").setValue("2000"));
        fields.add(generation.setValue("1999 - 2001 I (B5) Рестайлинг"));
        fields.add(bodyType.setValue("Седан"));
        fields.add(engine.setValue("Бензин"));
        fields.add(drive.setValue("Передний"));
        fields.add(transmission.setBlock("Коробка передач").setValue("Автомат"));
        fields.add(modification.setValue("1.6 AT (101\u00a0л.с.) 1999 - 2000"));
        fields.add(owningTime);
        fields.add(reviewTitle);
        fields.add(reviewText.setValue(StringUtils.repeat("*", 500)));
        fields.add(reviewPhoto);
        fields.add(reviewRatingExterior);
        fields.add(reviewRatingComfort);
        fields.add(reviewRatingSafety);
        fields.add(reviewRatingDrive);
        fields.add(reviewRatingReliability);
        fields.add(reviewPluses);
        fields.add(reviewMinuses);
    }

    public void createReviewsMotorcyclesForm() {
        fields.add(category.setValue("Мотоциклы"));
        fields.add(mark.setValue("ABM"));
        fields.add(model.setValue("Alpha 110"));
        fields.add(year);
        fields.add(owningTime);
        fields.add(reviewTitle);
        fields.add(reviewText.setValue(StringUtils.repeat("*", 500)));
        fields.add(reviewPhoto);
        fields.add(reviewRatingExterior.setBlock("Оцените модель вашего мототранспорта"));
        fields.add(reviewRatingComfort.setBlock("Оцените модель вашего мототранспорта"));
        fields.add(reviewRatingSafety.setBlock("Оцените модель вашего мототранспорта"));
        fields.add(reviewRatingDrive.setBlock("Оцените модель вашего мототранспорта"));
        fields.add(reviewRatingReliability.setBlock("Оцените модель вашего мототранспорта"));
        fields.add(reviewPluses);
        fields.add(reviewMinuses);
    }

    public void createReviewsLcvForm() {
        fields.add(category.setValue("Лёгкие коммерческие"));
        fields.add(mark.setValue("BAW"));
        fields.add(model.setValue("Tonik"));
        fields.add(year);
        fields.add(owningTime);
        fields.add(reviewTitle);
        fields.add(reviewText.setValue(StringUtils.repeat("*", 500)));
        fields.add(reviewPhoto);
        fields.add(reviewRatingExterior.setBlock("Оцените модель вашего коммерческого транспорта"));
        fields.add(reviewRatingComfort.setBlock("Оцените модель вашего коммерческого транспорта"));
        fields.add(reviewRatingSafety.setBlock("Оцените модель вашего коммерческого транспорта"));
        fields.add(reviewRatingDrive.setBlock("Оцените модель вашего коммерческого транспорта"));
        fields.add(reviewRatingReliability.setBlock("Оцените модель вашего коммерческого транспорта"));
        fields.add(reviewPluses);
        fields.add(reviewMinuses);
    }

    public void createEvaluationCarsForm() {
        fields.add(mark.setValue("Hyundai"));
        fields.add(model.setValue("Solaris"));
        fields.add(year.setBlock("Год").setType("radio").setValue("2020"));
        fields.add(bodyType.setValue("Седан"));
        fields.add(generation.setValue("2020 - н.в. II Рестайлинг"));
        fields.add(engine.setValue("Бензин"));
        fields.add(drive.setValue("Передний"));
        fields.add(transmission.setBlock("Коробка передач").setValue("Автомат"));
        fields.add(modification.setValue("1.4 AT (100\u00a0л.с.) 2020 - н.в."));
        //fields.add(complectation.setType("radio").setValue("Active (22 опции)"));
        fields.add(color);
        fields.add(run.setValue("100"));
        fields.add(ownersCount.setBlock("Владельцев по ПТС"));
        fields.add(buyDateYear.setBlock("Год покупки ТС").setType("radio").setValue("2020"));
        fields.add(evaluateReason.setValue("Хочу продать и купить новый"));
    }

    @Step("Заполняем форму до блока «{block}» включительно")
    public void fillForm(String block) throws IOException {
        for (Field field : fields) {
            if (reg && field.block.equals("Номер телефона")) {
                continue;
            }

            unfoldBlock(field.block);

            switch (field.type) {
                case "input":
                    basePageSteps.onFormsPage().unfoldedBlock(field.block).input(field.name, field.value);
                    if (field.name.equals("Номер телефона")) {
                        confirmPhone();
                    } else if (field.name.equals("Место осмотра")) {
                        waitForSuggest(field.block, field.name, field.value);
                        basePageSteps.onFormsPage().unfoldedBlock(field.block).geoSuggest().getItem(0).click();
                    }
                    break;

                case "radio":
                    if (field.block.equals("Укажите марку")) {
                        basePageSteps.onFormsPage().unfoldedBlock(field.block).button("Все марки").click();
                    } else if (field.block.equals("Укажите модель")
                            && basePageSteps.isElementExist(basePageSteps.onFormsPage().unfoldedBlock(field.block)
                            .button("Все модели"))) {
                        basePageSteps.onFormsPage().unfoldedBlock(field.block).button("Все модели").click();
                    }
                    basePageSteps.onFormsPage().unfoldedBlock(field.block).radioButton(field.value).click();
                    if (field.block.equals("Категория")) {
                        basePageSteps.hideElement(basePageSteps.onFormsPage().discountTimer());
                    }
                    break;

                case "checkbox":
                    if (field.value.equals("off")) break;
                    basePageSteps.onFormsPage().unfoldedBlock(field.block).checkbox(field.name).click();
                    break;

                case "color":
                    basePageSteps.onFormsPage().unfoldedBlock(field.block).color(field.value).click();
                    break;

                case "photo":
                    addPhoto();
                    break;

                case "rating":
                    basePageSteps.onFormsPage().unfoldedBlock(field.block).rating(field.name).star(field.value).click();
                    break;

                case "inputs_list":
                    basePageSteps.onFormsPage().unfoldedBlock(field.block).inputsList()
                            .forEach(input -> input.sendKeys(field.value));
                    break;

                case "review_photo":
                    addReviewPhoto();
                    break;
            }

            waitSomething(1, TimeUnit.SECONDS);

            if (field.block.equals(block)) return;
        }
    }

    @Step("Раскрываем блок на форме")
    public void unfoldBlock(String block) {
        if (!basePageSteps.isElementExist(basePageSteps.onFormsPage().unfoldedBlock(block))) {
            basePageSteps.onFormsPage().foldedBlock(block).click();
        }

        waitSomething(1, TimeUnit.SECONDS);
    }

    @Step("Подтверждаем телефон")
    private void confirmPhone() {
        basePageSteps.onFormsPage().unfoldedBlock("Номер телефона").button("Подтвердить").click();
        waitSomething(3, TimeUnit.SECONDS);
        basePageSteps.onFormsPage().unfoldedBlock("Номер телефона")
                .input("Код из смс", passportApiAdaptor.getLastSmsCode(phone.value));
        waitSomething(1, TimeUnit.SECONDS);
    }

    @Step("Загружаем фото")
    private void addPhoto() {
        String imgPath = "//images.mds-proxy.test.avto.ru/get-autoru-vos/";
        basePageSteps.onFormsPage().photo().sendKeys(new File("src/main/resources/offers/photo.jpg")
                .getAbsolutePath());
        basePageSteps.onFormsPage().getPhoto(0).waitUntil(isDisplayed());
        basePageSteps.onFormsPage().photoExample().waitUntil("Фото не загрузилось", hasAttribute("src",
                startsWith(format("https:%s", imgPath))), 10);
        basePageSteps.onFormsPage().getPhoto(0).waitUntil("Фото не загрузилось", hasAttribute("style",
                containsString(imgPath)), 10);
    }

    @Step("Загружаем фото в отзыв")
    public void addReviewPhoto() {
        String imgPath = "//avatars.mdst.yandex.net/get-autoru-reviews/";
        basePageSteps.onFormsPage().reviewPhotoInput().sendKeys(new File("src/main/resources/offers/photo.jpg")
                .getAbsolutePath());
        basePageSteps.onFormsPage().reviewPhoto().waitUntil("Фото не загрузилось", hasAttribute("src",
                startsWith(format("https:%s", imgPath))), 10);
    }

    @Step("Ждём появления саджеста")
    public void waitForSuggest(String block, String input, String text) {
        if (isElementExist(basePageSteps.onFormsPage().unfoldedBlock(block).geoSuggest()) &&
                basePageSteps.onFormsPage().unfoldedBlock(block).geoSuggest().itemsList().size() > 0) return;
        await().ignoreExceptions().atMost(60, TimeUnit.SECONDS).pollInterval(10, TimeUnit.SECONDS)
                .until(() -> {
                    onFormsPage().unfoldedBlock(block).input(input, text);
                    TimeUnit.SECONDS.sleep(3);
                    return basePageSteps.onFormsPage().unfoldedBlock(block).geoSuggest().itemsList().size() > 0;
                });
    }

    @Step("Выбираем адрес в саджесте")
    public void selectAddressFromSuggest(String address) {
        String block = place.block;
        basePageSteps.onFormsPage().unfoldedBlock(block).geoSuggest().waitUntil(isDisplayed());
        basePageSteps.onFormsPage().unfoldedBlock(block).geoSuggest().region(address).waitUntil(isDisplayed()).click();
        basePageSteps.onFormsPage().unfoldedBlock(block).geoSuggest().waitUntil(not(isDisplayed()));
    }

    @Step("Размещаем объявление")
    public void submitForm() {
        waitSomething(5, TimeUnit.SECONDS);
        basePageSteps.onFormsPage().submitButton().waitUntil(isEnabled()).click();
    }

    @Step("Создаём пользователя и привязываем его к дилеру")
    public Account linkUserToDealer() {
        Account account = am.create();
        adaptor.addEmailToAccountForUserWithPhone(account.getId(), account.getLogin(), getRandomEmail());
        adaptor.linkUserToClient(account.getId(), "20101", "8");
        return account;
    }

    @Step("Удаляем объявления")
    public void deleteOffers(String category) {
        salesIds.forEach(saleId -> {
            try {
                vosUserSteps.deleteOffer(category, saleId);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public String formatPhone(String phone) {
        return phone.replaceFirst("(\\d{1})(\\d{3})(\\d{3})(\\d{2})(\\d{2})", "+$1 $2 $3-$4-$5");
    }

    @Step("Сравниваем созданный оффер с ожидаемым")
    public void compareOffers(String actualOffer, String expectedOfferPath) {
        assertThat("Не создался нужный оффер", actualOffer,
                jsonEquals(getResourceAsString(expectedOfferPath))
                        .when(IGNORING_EXTRA_ARRAY_ITEMS).when(IGNORING_ARRAY_ORDER));
    }
}
