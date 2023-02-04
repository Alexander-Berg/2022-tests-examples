package ru.yandex.qe.bus.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
* @author nkey
* @since 19.02.14
*/
public class ApiJsonObject {
    @JsonProperty("x")
    private final Long x;
    @JsonProperty("y")
    private final String y;

    @JsonCreator
    public ApiJsonObject(@JsonProperty("x") Long x, @JsonProperty("y") String y) {
        this.x = x;
        this.y = y;
    }

    public Long getX() {
        return x;
    }

    public String getY() {
        return y;
    }
}
