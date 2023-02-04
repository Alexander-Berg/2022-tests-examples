package ru.yandex.general.beans.events;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class CategoryChosen {

    String draftId;
    String categoryId;
    String predictionId;
    CategorySelectedFromPrediction categorySelectedFromPrediction;

    public static CategoryChosen categoryChosen() {
        return new CategoryChosen();
    }

}
