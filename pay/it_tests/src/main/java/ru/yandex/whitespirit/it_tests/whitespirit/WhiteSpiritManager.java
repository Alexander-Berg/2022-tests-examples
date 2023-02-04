package ru.yandex.whitespirit.it_tests.whitespirit;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import one.util.streamex.StreamEx;

import ru.yandex.whitespirit.it_tests.configuration.Config;
import ru.yandex.whitespirit.it_tests.configuration.KKT;
import ru.yandex.whitespirit.it_tests.templates.Template;
import ru.yandex.whitespirit.it_tests.templates.TemplatesManager;
import ru.yandex.whitespirit.it_tests.whitespirit.client.HudsuckerClient;
import ru.yandex.whitespirit.it_tests.whitespirit.client.WhiteSpiritClient;
import ru.yandex.whitespirit.it_tests.whitespirit.providers.LocalServiceProvider;
import ru.yandex.whitespirit.it_tests.whitespirit.providers.RealServiceProvider;
import ru.yandex.whitespirit.it_tests.whitespirit.providers.ServiceProvider;

import static java.util.Collections.emptyMap;
import static ru.yandex.whitespirit.it_tests.templates.Template.CLEAR_DEBUG_FN;
import static ru.yandex.whitespirit.it_tests.templates.Template.CONFIGURE_GROUP_REQUEST_BODY;
import static ru.yandex.whitespirit.it_tests.templates.Template.CONFIGURE_KKT_FFD12_REQUEST_BODY;
import static ru.yandex.whitespirit.it_tests.templates.Template.CONFIGURE_KKT_REQUEST_BODY;
import static ru.yandex.whitespirit.it_tests.templates.Template.REGISTER_KKT_REQUEST_BODY;
import static ru.yandex.whitespirit.it_tests.utils.Constants.FIRMS;
import static ru.yandex.whitespirit.it_tests.utils.Utils.checkResponseCode;
import static ru.yandex.whitespirit.it_tests.utils.Utils.generateRNM;
import static ru.yandex.whitespirit.it_tests.utils.Utils.getMySecret;

@Slf4j
@Getter
public class WhiteSpiritManager {
    private static final boolean DISABLE_MGM_TESTS = Boolean.parseBoolean(System.getenv("DISABLE_MGM_TESTS"));
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final TemplatesManager templatesManager;
    private final Map<String, KKT> KKTs;
    private final ServiceProvider serviceProvider;
    private final WhiteSpiritClient whiteSpiritClient;
    private final HudsuckerClient hudsuckerClient;

    private static final List<String> DEFAULT_WORK_MODE = List.of("internet_usage", "automatic");
    private static final List<String> BSO_WORK_MODE = StreamEx.of(DEFAULT_WORK_MODE)
            .append("BSO", "in_service_usage")
            .toImmutableList();

    public WhiteSpiritManager(Config config) {
        this.templatesManager = new TemplatesManager();

        this.KKTs = StreamEx.of(config.getKkts().entrySet())
                .filter(entry -> !DISABLE_MGM_TESTS || entry.getValue().isUseVirtualFn())
                .toMap(Map.Entry::getKey, Map.Entry::getValue);
        this.serviceProvider = makeProvider(config);

        Runtime.getRuntime().addShutdownHook(new Thread(this::onShutdown));
        serviceProvider.onStart();

        this.whiteSpiritClient = new WhiteSpiritClient(serviceProvider.getWhitespiritUrl());
        this.hudsuckerClient = new HudsuckerClient(serviceProvider.getHudsuckerUrl(),
                serviceProvider.getWhitespiritUrl());
        checkFoundKKTs();
        registerKKTs();
    }

    @SneakyThrows
    private void onShutdown() {
        log.info("Shutting down whitespirit, clearing data from all MGMs");
        KKTs.values().stream().filter(kkt -> !kkt.isRegistered() && !kkt.isUseVirtualFn())
                .forEach(kkt -> whiteSpiritClient.clearDebugFn(kkt.getKktSN(), getMySecret(kkt.getKktSN()),
                        templatesManager.processTemplate(CLEAR_DEBUG_FN, emptyMap())));

        serviceProvider.onShutdown();
    }

    private ServiceProvider makeProvider(Config config) {
        return config.isTestExistingWhiteSpirit()
                ? new RealServiceProvider(config.getWhiteSpiritUrl(), config.getHudsuckerUrl())
                : new LocalServiceProvider(
                infoResponse -> allKKTsFound(infoResponse, getKKTSns(KKTs.values())),
                KKTs,
                config.isUseLocalCompose(), DISABLE_MGM_TESTS);
    }

    private static boolean isFFD12(String version) {
        return version.startsWith("4");
    }

    private static Template getConfigureTemplate(String version) {
        if (isFFD12(version)) {
            return CONFIGURE_KKT_FFD12_REQUEST_BODY;
        }
        return CONFIGURE_KKT_REQUEST_BODY;
    }

    private static List<String> getWorkMode(KKT kkt) {
        return kkt.isBsoKkt() ? BSO_WORK_MODE : DEFAULT_WORK_MODE;
    }

    private String getConfigureBody(KKT kkt) {
        val inn = kkt.getInn();
        val kktSn = kkt.getKktSN();
        val firm = FIRMS.get(inn);
        return templatesManager.processTemplate(getConfigureTemplate(kkt.getVersion()),
                Map.of(
                        "rnm", generateRNM(inn, kktSn),
                        "firm", firm,
                        "work_mode", getWorkMode(kkt),
                        "mysecret", getMySecret(kktSn)
                ));
    }

    private String getConfigureGroupBody(KKT kkt) {
        val parameters = Map.<String, Object>of(
                "group", "\"" + kkt.getGroup() + "\"",
                "hidden", kkt.isHidden(),
                "logical_state", kkt.getLogicalState(),
                "mysecret", getMySecret(kkt.getKktSN())
        );
        return templatesManager.processTemplate(CONFIGURE_GROUP_REQUEST_BODY, parameters);
    }

    private void registerKKTs() {
        KKTs.values().forEach(kkt -> {
            if (kkt.isRegistered()) {
                return;
            }

            val configureBody = getConfigureBody(kkt);
            checkResponseCode(whiteSpiritClient.configure(kkt.getKktSN(), configureBody));

            val registerBody = templatesManager.processTemplate(REGISTER_KKT_REQUEST_BODY, emptyMap());
            checkResponseCode(whiteSpiritClient.register(kkt.getKktSN(), registerBody));

            val configureGroupBody = getConfigureGroupBody(kkt);
            checkResponseCode(whiteSpiritClient.configure(kkt.getKktSN(), configureGroupBody));
        });
    }

    private void checkFoundKKTs() {
        if (!allKKTsFound(whiteSpiritClient.info().body().asString(), getKKTSns(KKTs.values()))) {
            throw new IllegalStateException("Some of expected kkts were not found.");
        }
    }

    private static Set<String> getKKTSns(Collection<KKT> kkts) {
        return StreamEx.of(kkts)
                .map(KKT::getKktSN)
                .toImmutableSet();
    }


    @SneakyThrows
    public static boolean allKKTsFound(String infoResponseString, Set<String> expectedKKTs) {
        val infoResponse = deserializeWrappedInfoResponse(infoResponseString);
        val real = StreamEx.of(infoResponse.getKkts())
                .map(InfoResponse.KKTInfo::getSn)
                .toImmutableSet();
        if (expectedKKTs.equals(real)) {
            log.info("KKTs: Expected = {} | Real = {}", expectedKKTs, real);
        } else {
            log.warn("KKTs: Expected = {} | Real = {}", expectedKKTs, real);
        }
        return real.containsAll(expectedKKTs);
    }

    @SneakyThrows
    public static InfoResponse deserializeWrappedInfoResponse(String responseString) {
        val collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, InfoResponse.class);
        val wrappedResponse = objectMapper.<List<InfoResponse>>readValue(responseString, collectionType);
        return wrappedResponse.stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unexpected response: " + responseString));
    }

    public Set<String> getKktSerialNumbersByInn(String inn) {
        return StreamEx.of(KKTs.values())
                .filter(kkt -> kkt.getInn().equals(inn))
                .map(KKT::getKktSN)
                .toImmutableSet();
    }
}
