package ru.auto.tests.desktop.mock.beans.userCompare;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
public class UserCompareResponse {

    @SerializedName("catalog_card_ids")
    List<String> catalogCardIds;
    String status;

    public UserCompareResponse setCatalogCardIds(String... catalogCardIds) {
        this.catalogCardIds = new ArrayList<>();
        this.catalogCardIds.addAll(Arrays.asList(catalogCardIds));
        return this;
    }

    public static UserCompareResponse userCompareResponse() {
        return new UserCompareResponse().setStatus("SUCCESS");
    }

}
