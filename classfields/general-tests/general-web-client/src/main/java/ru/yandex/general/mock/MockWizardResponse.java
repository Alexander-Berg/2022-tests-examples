package ru.yandex.general.mock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.qameta.allure.Step;
import ru.yandex.general.beans.wizard.AutoItem;
import ru.yandex.general.beans.wizard.RealtyItem;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.commons.util.Utils.getResourceAsString;
import static ru.yandex.general.beans.wizard.Metro.metro;
import static ru.yandex.general.beans.wizard.Price.price;
import static ru.yandex.general.utils.Utils.getRandomIntInRange;

public class MockWizardResponse {

    public static final String REALTY_EXAMPLE = "mock/wizardRealtyExample.json";
    public static final String AUTO_EXAMPLE = "mock/wizardAutoExample.json";

    private JsonObject template;

    private MockWizardResponse(String pathToTemplate) {
        this.template = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockWizardResponse mockWizardResponse(String pathToMockWizardResponse) {
        return new MockWizardResponse(pathToMockWizardResponse);
    }

    public static MockWizardResponse mockRealtyExample() {
        return mockWizardResponse(REALTY_EXAMPLE);
    }

    public static MockWizardResponse mockAutoExample() {
        return mockWizardResponse(AUTO_EXAMPLE);
    }

    @Step("Добавляем «{count}» офферов авто к ответу")
    public MockWizardResponse addAutoItems(int count) {
        JsonArray thumbs = new JsonArray();
        for (int i = 0; i < count; i++) {
            thumbs.add(new Gson().toJsonTree(autoItem()).getAsJsonObject());
        }
        template.add("thumbs", thumbs);
        return this;
    }

    @Step("Добавляем «{count}» офферов недвижимости к ответу")
    public MockWizardResponse addRealtyItems(int count) {
        JsonArray offers = new JsonArray();
        for (int i = 0; i < count; i++) {
            offers.add(new Gson().toJsonTree(realtyItem()).getAsJsonObject());
        }
        template.add("offers", offers);
        return this;
    }

    private AutoItem autoItem() {
        return AutoItem.autoItem()
                .setImage("//avatars.mds.yandex.net/get-verba/1540742/2a0000016a96cccf7cf20b6720a0f937d22d/wizardv3mr")
                .setImageRetina("//avatars.mds.yandex.net/get-verba/1540742/2a0000016a96cccf7cf20b6720a0f937d22d/wizardv3mr")
                .setName("Mercedes-Benz")
                .setUrl(format("https://test.avto.ru/moskva/cars/mercedes/all/?from=wizard.common&utm_source=auto_" +
                        "wizard&utm_medium=desktop&utm_campaign=common&utm_content=listing&sort_offers=" +
                        "fresh_relevance_1-DESC%d", getRandomIntInRange(1, 10000)))
                .setText("2 930 объявлений");
    }

    private RealtyItem realtyItem() {
        return RealtyItem.realtyItem()
                .setImagePrefix("//avatars.mdst.yandex.net/get-realty/3019/add.1634736751904109789bed6")
                .setPrice(price().setOfferPrice("1000005").setCurrency("RUR"))
                .setMetro(metro().setDistance("13 мин.").setColor("ff8103").setName("м. Новые Черёмушки"))
                .setName("1-комнатная, 16 м²")
                .setDescription("2 часа назад")
                .setOfferImagesCount(4)
                .setText("улица Цюрупы, 20к1")
                .setType("offer")
                .setUrl(format("//realty.test.vertis.yandex.ru/search?type=SELL&category=APARTMENT&rgid=587795&" +
                        "nosplash=1&from=wizard.offers&pinnedOfferId=7970653194957850625%d&utm_source=wizard&" +
                        "utm_campaign=default&utm_medium=flat", getRandomIntInRange(1, 10000)))
                .setGallery(asList("//avatars.mdst.yandex.net/get-realty/3019/add.1634736751904109789bed6",
                        "//avatars.mdst.yandex.net/get-realty/3022/add.163473675196288a49797f7",
                        "//avatars.mdst.yandex.net/get-realty/3274/add.1634736752099e8b7a26eb0",
                        "//avatars.mdst.yandex.net/get-realty/3022/add.1634736752193527cfce849"));

    }

    public String build() {
        return template.toString();
    }

}
