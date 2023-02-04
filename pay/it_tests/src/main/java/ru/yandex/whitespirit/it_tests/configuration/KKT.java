package ru.yandex.whitespirit.it_tests.configuration;

import lombok.Value;

@Value
public class KKT {
    String kktSN;
    String fnSN;
    String inn;
    boolean registered;
    String group;
    boolean hidden;
    String logicalState;
    boolean isBsoKkt;
    String version;
    boolean useVirtualFn;
}
