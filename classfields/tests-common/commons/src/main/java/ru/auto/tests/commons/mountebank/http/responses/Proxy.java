package ru.auto.tests.commons.mountebank.http.responses;

import java.util.HashMap;
import java.util.Map;

public class Proxy extends Response {

    public static final String PROXY = "proxy";
    public static final String MODE = "mode";
    public static final String TO = "to";

    private Map data;
    private ModeType mode;

    public Proxy() {
        data = new HashMap();
        this.put(PROXY, data);
    }

    public Proxy withMode(ModeType mode) {
        data.put(MODE, mode.getValue());
        return this;
    }

    public Proxy withTo(String url) {
        data.put(TO, url);
        return this;
    }
}
