package ru.yandex.general.beans.ajaxRequests.updateDraft;

import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class UpdateDraft {

    String draftId;
    Form form;
    boolean useNewForm;

    public static UpdateDraft updateDraft() {
        return new UpdateDraft();
    }

    @Override
    public String toString() {
        return new GsonBuilder().create().toJson(this);
    }

}
