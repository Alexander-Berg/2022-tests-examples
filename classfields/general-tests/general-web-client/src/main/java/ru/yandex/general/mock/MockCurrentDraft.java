package ru.yandex.general.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockCurrentDraft {

    public static final String TITLE_EMPTY_SCREEN = "mock/currentDraft/titleEmptyScreen.json";
    public static final String TITLE_SCREEN = "mock/currentDraft/title.json";
    public static final String MEDIA_SCREEN = "mock/currentDraft/mediaScreen.json";
    public static final String MEDIA_SCREEN_WITH_PHOTO = "mock/currentDraft/mediaScreenWithPhoto.json";
    public static final String CATEGORY_SCREEN = "mock/currentDraft/categoryScreen.json";
    public static final String CATEGORY_SELECTED_SCREEN = "mock/currentDraft/categorySelectedScreen.json";
    public static final String VIDEO_LINK_SCREEN = "mock/currentDraft/videoLinkScreen.json";
    public static final String VIDEO_WITHOUT_LINK_SCREEN = "mock/currentDraft/videoWithoutLinkScreen.json";
    public static final String DESCRIPTION_EMPTY_SCREEN = "mock/currentDraft/descriptionEmptyScreen.json";
    public static final String DESCRIPTION_SCREEN = "mock/currentDraft/descriptionScreen.json";
    public static final String CONDITION_EMPTY_SCREEN = "mock/currentDraft/conditionEmptyScreen.json";
    public static final String CONDITION_SCREEN = "mock/currentDraft/conditionScreen.json";
    public static final String ATTRIBUTES_EMPTY_SCREEN = "mock/currentDraft/attributesEmptyScreen.json";
    public static final String ATTRIBUTES_SCREEN = "mock/currentDraft/attributesScreen.json";
    public static final String PRICE_EMPTY_SCREEN = "mock/currentDraft/priceEmptyScreen.json";
    public static final String PRICE_SCREEN = "mock/currentDraft/priceScreen.json";
    public static final String PRICE_FREE_SCREEN = "mock/currentDraft/priceFreeScreen.json";
    public static final String CONTACTS_SCREEN = "mock/currentDraft/contactsScreen.json";
    public static final String PLACE_OF_DEAL_EMPTY_SCREEN = "mock/currentDraft/placeOfDealEmptyScreen.json";
    public static final String PLACE_OF_DEAL_SCREEN = "mock/currentDraft/placeOfDealScreen.json";
    public static final String FINAL_SCREEN = "mock/currentDraft/finalScreen.json";
    public static final String SUSPICIOUS_ACTIVITY_CONTACTS = "mock/currentDraft/suspiciousActivity.json";

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    private JsonObject currentDraft;

    private MockCurrentDraft(String pathToTemplate) {
        this.currentDraft = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockCurrentDraft mockCurrentDraft(String pathToCurrentDraft) {
        return new MockCurrentDraft(pathToCurrentDraft);
    }

    public String build() {
        return currentDraft.toString();
    }

}
