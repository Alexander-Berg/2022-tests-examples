package ru.auto.tests.desktop.beans;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class LogEntity {

    private Message message;

    @Data
    public static class Message {
        private String method;
        private Params params;

        @Data
        public static class Params {
            private Response response;

            @Data
            public static class Response {
                private String url;
                private Headers headers;

                @Data
                public static class Headers {
                    @SerializedName(value = "x-autoru-app-id", alternate = {"X-Autoru-App-Id"})
                    private String xAutoruAppId;
                }
            }
        }
    }
}
