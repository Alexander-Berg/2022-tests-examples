package ru.yandex.infra.sidecars_updater;

import java.util.List;
import java.util.Map;

import ru.yandex.infra.sidecars_updater.sandbox.SandboxResourceInfo;
import ru.yandex.infra.sidecars_updater.sidecars.Sidecar;

public class TestData {
    public static final Map<String, String> DEFAULT_LAYER_ATTRIBUTES = Map.of(
            "ttl", "inf",
            "released", "stable"
    );
    public static final int DEFAULT_LAYER_1_REVISION = 1000000000;
    public static final String DEFAULT_SKYNET_ID_URL = "skynet_id_1";
    public static final String DEFAULT_HTTP_PROXY_URL = "http_proxy_1";
    public static final String HTTP_URL_1 = "http_1";
    public static final String HTTP_URL_2 = "http_2";
    public static final List<String> DEFAULT_HTTP_URLS = List.of(HTTP_URL_1, HTTP_URL_2);
    public static final Sidecar.Type DEFAULT_LAYER_TYPE = Sidecar.Type.PORTO_LAYER_SEARCH_UBUNTU_XENIAL_APP;
    public static final SandboxResourceInfo DEFAULT_LAYER_INFO = new SandboxResourceInfo(
            DEFAULT_LAYER_1_REVISION,
            DEFAULT_LAYER_TYPE.toString(),
            DEFAULT_SKYNET_ID_URL,
            DEFAULT_HTTP_PROXY_URL,
            DEFAULT_HTTP_URLS
    );
}
