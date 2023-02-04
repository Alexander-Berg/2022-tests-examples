package ru.auto.tests.desktop.mock;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.auto.tests.commons.util.Utils.getResourceAsString;

public class MockUserDraft {

    private static final String UNAUTH_USER_DRAFT_EXAMPLE = "mocksConfigurable/poffer/UnAuthUserDraftExample.json";
    private static final String USER_DRAFT_EXAMPLE = "mocksConfigurable/poffer/UserDraftExample.json";
    private static final String USER_DRAFT_PAID_EXAMPLE = "mocksConfigurable/poffer/UserDraftPaidExample.json";
    private static final String EMPTY_USER_DRAFT_EXAMPLE = "mocksConfigurable/poffer/EmptyUserDraftExample.json";
    private static final String USER_VIP_DRAFT_EXAMPLE = "mocksConfigurable/poffer/UserVipDraftExample.json";

    @Getter
    @Setter
    @Accessors(chain = true)
    JsonObject body;

    private MockUserDraft(String pathToTemplate) {
        this.body = new GsonBuilder().create().fromJson(getResourceAsString(pathToTemplate), JsonObject.class);
    }

    public static MockUserDraft userDraftExample() {
        return new MockUserDraft(USER_DRAFT_EXAMPLE);
    }

    public static MockUserDraft emptyUserDraftExample() {
        return new MockUserDraft(EMPTY_USER_DRAFT_EXAMPLE);
    }

    public static MockUserDraft unAuthUserDraftExample() {
        return new MockUserDraft(UNAUTH_USER_DRAFT_EXAMPLE);
    }

    public static MockUserDraft paidUserDraftExample() {
        return new MockUserDraft(USER_DRAFT_PAID_EXAMPLE);
    }

    public static MockUserDraft userVipDraftExample() {
        return new MockUserDraft(USER_VIP_DRAFT_EXAMPLE);
    }

}
