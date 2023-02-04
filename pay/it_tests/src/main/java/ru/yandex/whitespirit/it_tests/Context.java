package ru.yandex.whitespirit.it_tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.val;
import ru.yandex.whitespirit.it_tests.configuration.Config;
import ru.yandex.whitespirit.it_tests.whitespirit.WhiteSpiritManager;
import ru.yandex.whitespirit.it_tests.templates.TemplatesManager;

import static ru.yandex.whitespirit.it_tests.utils.Utils.getResourceAsStream;

@UtilityClass
public class Context {
    private final Config config = loadConfig();

    @Getter private final WhiteSpiritManager whiteSpiritManager = new WhiteSpiritManager(config);
    @Getter private final TemplatesManager templatesManager = whiteSpiritManager.getTemplatesManager();

    @SneakyThrows
    private static Config loadConfig() {
        val mapper = new ObjectMapper(new YAMLFactory());
        val configFile = getResourceAsStream("config.yaml");
        return mapper.readValue(configFile, Config.class);
    }
}
