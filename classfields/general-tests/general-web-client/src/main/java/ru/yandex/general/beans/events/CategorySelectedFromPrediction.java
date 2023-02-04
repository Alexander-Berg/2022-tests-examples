package ru.yandex.general.beans.events;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class CategorySelectedFromPrediction {

    int categoryRank;
    String categoryId;

    public static CategorySelectedFromPrediction categorySelectedFromPrediction() {
        return new CategorySelectedFromPrediction();
    }

}
