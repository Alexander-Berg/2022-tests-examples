package ru.yandex.realty.beans;

import lombok.Getter;

public class CodeResponse {

    @Getter
    private Response response;

    public class Response {
        @Getter
        private Boolean isDone;
        @Getter
        private String trackId;
    }

}