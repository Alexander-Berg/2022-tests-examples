package ru.auto.tests.realtyapi.responses;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class FavoritesInner {
    @SerializedName("actual")
    private List<String> actualField;
    private List<String> outdated;
    private List<String> relevant;
}
