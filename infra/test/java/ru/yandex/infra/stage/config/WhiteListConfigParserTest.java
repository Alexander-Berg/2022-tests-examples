package ru.yandex.infra.stage.config;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import one.util.streamex.EntryStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.infra.stage.config.WhiteListConfigParser.WHITE_LIST_CONFIG_PATH;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

public class WhiteListConfigParserTest {

    private static final String NOT_WHITE_LIST_CONFIG_PATH = "not_" + WHITE_LIST_CONFIG_PATH;

    private static final Map<String, Map<String, Integer>> CORRECT_WHITE_LIST_CONFIG_MAP = Map.of(
            "stage_1.du_1", Map.of("a", 1),
            "stage_1.du_2", Map.of("a", 2, "b", 3),
            "stage_2.du_1", Map.of("b", 3, "c", 2, "d", 1)
    );

    private static final Config CORRECT_PARENT_CONFIG = ConfigFactory.parseMap(Map.of(
            NOT_WHITE_LIST_CONFIG_PATH, Map.of("key_1", "value_1", "key_2", "value_2"),
            WHITE_LIST_CONFIG_PATH, CORRECT_WHITE_LIST_CONFIG_MAP
            )
    );

    private static final Function<Config, Integer> DUMMY_VALUE_CONFIG_PARSER = config -> config.root().size();

    @Test
    public void parseConfigCorrectTest() {
        var expectedWhiteList = EntryStream.of(CORRECT_WHITE_LIST_CONFIG_MAP)
                .mapValues(ConfigFactory::parseMap)
                .mapValues(DUMMY_VALUE_CONFIG_PARSER)
                .toMap();

        var actualWhiteList = WhiteListConfigParser.parseConfig(
                ConfigFactory.parseMap(CORRECT_WHITE_LIST_CONFIG_MAP),
                DUMMY_VALUE_CONFIG_PARSER
        );

        assertThatEquals(actualWhiteList, expectedWhiteList);
    }

    private static Stream<Arguments> parseConfigIncorrectTestParameters() {
        String correctFullDeployUnitId = "correct_stage.correct_deploy_unit_id";
        Map<String, Integer> correctValue = Map.of("correctValue", 10);

        String notFullDeployUnitId = "not_full_du_id";
        String incorrectValue = "incorrectValue";

        return Stream.of(
                Arguments.of(Map.of(notFullDeployUnitId, correctValue)),
                Arguments.of(Map.of(correctFullDeployUnitId, incorrectValue))
        );
    }

    @ParameterizedTest
    @MethodSource("parseConfigIncorrectTestParameters")
    public void parseConfigIncorrectTest(Map<String, Map<String, Integer>> incorrectWhiteListConfigMap) {
        assertThrows(
                RuntimeException.class,
                () -> WhiteListConfigParser.parseConfig(
                        ConfigFactory.parseMap(incorrectWhiteListConfigMap),
                        DUMMY_VALUE_CONFIG_PARSER
                )
        );
    }

    @Test
    public void parseWhiteListFromCorrectTest() {
        var expectedWhiteList = WhiteListConfigParser.parseConfig(
                CORRECT_PARENT_CONFIG.getConfig(WHITE_LIST_CONFIG_PATH),
                DUMMY_VALUE_CONFIG_PARSER
        );

        var actualWhiteList = WhiteListConfigParser.parseWhiteListFrom(
                CORRECT_PARENT_CONFIG,
                DUMMY_VALUE_CONFIG_PARSER
        );

        assertThatEquals(actualWhiteList, expectedWhiteList);
    }

    @Test
    public void parseWhiteListFromMissingTest() {
        assertThrows(
                ConfigException.Missing.class,
                () -> WhiteListConfigParser.parseWhiteListFrom(
                        CORRECT_PARENT_CONFIG.withoutPath(WHITE_LIST_CONFIG_PATH),
                        DUMMY_VALUE_CONFIG_PARSER
                )
        );
    }
}
