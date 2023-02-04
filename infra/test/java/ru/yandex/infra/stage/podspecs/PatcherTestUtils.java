package ru.yandex.infra.stage.podspecs;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import ru.yandex.infra.stage.dto.SecretSelector;
import ru.yandex.yp.client.pods.TBox;
import ru.yandex.yp.client.pods.TEnvVar;
import ru.yandex.yp.client.pods.TPodAgentSpec;
import ru.yandex.yp.client.pods.TWorkload;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class PatcherTestUtils {
    private PatcherTestUtils() {}

    public static <T> Map<String, T> groupById(List<T> objs, Function<T, String> idGetter) {
        // toMap() ensures absence of duplicate keys
        return objs.stream()
                .collect(Collectors.toMap(idGetter, Function.identity()));

    }

    public static <T> void testIds(List<T> objs, Set<String> expectedIds, Function<T, String> idGetter) {
        List<String> ids = objs.stream()
                .map(idGetter)
                .filter(expectedIds::contains)
                .collect(Collectors.toList());
        // Check size of list to sure there are no duplicate ids.
        assertThat(ids, hasSize(expectedIds.size()));
        assertThat(new HashSet<>(ids), equalTo(expectedIds));
    }

    public static boolean hasSecret(TWorkload workload, String secretEnvName, SecretSelector secretSelector) {
        return workload.getEnvList().stream()
                .anyMatch(e -> e.getValue().hasSecretEnv() &&
                        e.getName().equals(secretEnvName) &&
                        e.getValue().getSecretEnv().getId().equals(secretSelector.getKey()) &&
                        e.getValue().getSecretEnv().getAlias().equals(secretSelector.getAlias()));
    }

    public static boolean hasLiteralEnv(TWorkload workload, String literalEnvName, String value) {
        return containsLiteralEnv(workload.getEnvList(), literalEnvName, value);
    }

    public static boolean hasLiteralEnv(TBox box, String literalEnvName, String value) {
        return containsLiteralEnv(box.getEnvList(), literalEnvName, value);
    }

    private static boolean containsLiteralEnv(List<TEnvVar> envVars, String literalEnvName, String value) {
        return envVars.stream()
                .anyMatch(e -> e.getValue().hasLiteralEnv() &&
                        e.getName().equals(literalEnvName) &&
                        e.getValue().getLiteralEnv().getValue().equals(value));
    }

    public static Map<String, String> getAllLiteralVars(List<TEnvVar> vars) {
        return vars.stream()
                   .filter(e -> e.getValue().hasLiteralEnv())
                   .collect(Collectors.toMap(
                                TEnvVar::getName,
                                e -> e.getValue().getLiteralEnv().getValue()
                           )
                   );
    }

    public static Optional<TBox> getBoxById(TPodAgentSpec agentSpec, String boxId) {
        return agentSpec.getBoxesList().stream().filter(b -> b.getId().equals(boxId)).findFirst();
    }
}
