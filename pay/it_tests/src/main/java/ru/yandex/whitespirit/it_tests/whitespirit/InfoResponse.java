package ru.yandex.whitespirit.it_tests.whitespirit;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Value;

import java.util.List;

@Value
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InfoResponse {
    List<FirmInfo> firms;
    List<KKTInfo> kkts;
    String wsName;
    String environment;
    String version;

    @Value
    public static class KKTInfo {
        String sn;
    }

    @Value
    public static class FirmInfo {
        String inn;
        List<GroupInfo> groups;

        @Value
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        private static class GroupInfo {
            String group;
            int readyCashmachines;
        }
    }
}
