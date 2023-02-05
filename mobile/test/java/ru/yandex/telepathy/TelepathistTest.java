package ru.yandex.telepathy;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import ru.yandex.telepathy.exception.EntryNotFoundException;
import ru.yandex.telepathy.interactor.RemoteConfigInteractor;
import ru.yandex.telepathy.interactor.TelepathistImpl;
import ru.yandex.telepathy.model.JsonPath;
import ru.yandex.telepathy.model.RemoteConfig;
import ru.yandex.telepathy.testutils.TestConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.telepathy.testutils.UtilsKt.jsonPath;

/**
 * This test is written on Java, because Telepathist should be compatible with Java.
 */
public class TelepathistTest {

    private Telepathist telepathist;

    private String defaultValue = "default";

    private String notExist = "notExist";

    private RemoteConfig remoteConfig;

    @Before
    public void runBeforeEachTest() {
        RemoteConfigInteractor interactor = Mockito.mock(RemoteConfigInteractor.class);
        Mockito.when(interactor.getCachedConfig()).thenReturn(remoteConfig = TestConfig.INSTANCE.nonEmpty());
        telepathist = new TelepathistImpl(interactor);
    }

    @Test
    public void getOrDefault_shouldReturnValue_whenExists() {
        String result = telepathist.get(TestConfig.obj1Key, TestConfig.value11).orDefault(defaultValue);
        assertThat(result).isEqualTo(TestConfig.value11);
    }

    @Test
    public void getOrDefault_shouldReturnDefault_whenNotExists() {
        String result = telepathist.get(TestConfig.obj1Key, notExist).orDefault(defaultValue);
        assertThat(result).isEqualTo(defaultValue);
    }

    @Test
    public void getOrNull_shouldReturnValue_whenExists() {
        String result = telepathist.get(TestConfig.obj1Key, TestConfig.value11).orNull();
        assertThat(result).isEqualTo(TestConfig.value11);
    }

    @Test
    public void getOrNull_shouldReturnNull_whenNotExists() {
        String result = telepathist.get(TestConfig.obj1Key, notExist).orNull();
        assertThat(result).isEqualTo(null);
    }

    @Test
    public void getOrException_shouldReturnValue_whenExists() throws EntryNotFoundException {
        String result = telepathist.get(TestConfig.obj1Key, TestConfig.value11).orException();
        assertThat(result).isEqualTo(TestConfig.value11);
    }

    @Test
    public void getOrNull_shouldThrow_whenNotExists() {
        assertThatThrownBy(() -> telepathist.get(TestConfig.obj1Key, notExist).orException())
                .isExactlyInstanceOf(EntryNotFoundException.class);
    }

    @Test
    public void getSubtree_shouldWork_withTopLevelObject() {
        ConfigSubtree subtree = telepathist.getSubtree(TestConfig.obj2Key);
        assertThat(subtree.get(TestConfig.value21).orDefault(defaultValue)).isEqualTo(TestConfig.value21);
        assertThat(subtree.get(TestConfig.value22).orDefault(defaultValue)).isEqualTo(TestConfig.value22);
    }

    @Test
    public void getSubtree_shouldWork_withNestedCalls() {
        ConfigSubtree subtree = telepathist.getSubtree(TestConfig.obj1Key).getSubtree(TestConfig.obj11Key);
        assertThat(subtree.get(TestConfig.value111).orDefault(defaultValue)).isEqualTo(TestConfig.value111);
    }

    @Test
    public void getSubtree_shouldNotThrowAnyException_whenNotExists() {
        ConfigSubtree subtree = telepathist.getSubtree(notExist);
        assertThat(subtree.get(TestConfig.value11).orDefault(defaultValue)).isEqualTo(defaultValue);
    }

    @Test
    public void getSubtree_shouldReturnEmptySubtree_whenCalledOnValue() {
        ConfigSubtree subtree = telepathist.getSubtree(TestConfig.obj1Key, TestConfig.value11);
        assertThat(subtree).isEqualToComparingFieldByFieldRecursively(
                new ConfigSubtree(TestConfig.INSTANCE.nonEmpty(), jsonPath(TestConfig.obj1Key, TestConfig.value11)));
    }

    @Test
    public void getOrNull_whenValueOverridden_shouldReturnOverride() {
        remoteConfig.override(jsonPath(TestConfig.obj1Key), TestConfig.override11);
        assertThat(telepathist.get(TestConfig.obj1Key).<String>orNull()).isEqualTo(TestConfig.override11);
    }

    @Test
    public void getOrDefault_whenValueOverridden_shouldReturnOverride() {
        remoteConfig.override(jsonPath(TestConfig.obj1Key), TestConfig.override11);
        assertThat(telepathist.get(TestConfig.obj1Key).orDefault(defaultValue)).isEqualTo(TestConfig.override11);
    }

    @Test
    public void getOrException_whenValueOverridden_shouldReturnOverride() throws EntryNotFoundException {
        remoteConfig.override(jsonPath(TestConfig.obj1Key), TestConfig.override11);
        assertThat(telepathist.get(TestConfig.obj1Key).<String>orException()).isEqualTo(TestConfig.override11);
    }

    @Test
    public void getOrNull_whenValueOverridden_shouldReturnOverride_ifNewKey() {
        remoteConfig.override(jsonPath(TestConfig.override11), TestConfig.override11);
        assertThat(telepathist.get(TestConfig.override11).<String>orNull()).isEqualTo(TestConfig.override11);
    }

    @Test
    public void getOrDefault_whenValueOverridden_shouldReturnOverride_ifNewKey() {
        remoteConfig.override(jsonPath(TestConfig.override11), TestConfig.override11);
        assertThat(telepathist.get(TestConfig.override11).orDefault(defaultValue)).isEqualTo(TestConfig.override11);
    }

    @Test
    public void getOrException_whenValueOverridden_shouldReturnOverride_ifNewKey() throws EntryNotFoundException {
        remoteConfig.override(jsonPath(TestConfig.override11), TestConfig.override11);
        assertThat(telepathist.get(TestConfig.override11).<String>orException()).isEqualTo(TestConfig.override11);
    }

    @Test
    public void getOrDefault_whenParentOverridden_shouldReturnOverride() {
        Map<String, Object> override = new HashMap<>();
        override.put(TestConfig.override11, TestConfig.override11);
        remoteConfig.override(jsonPath(TestConfig.obj1Key), override);
        assertThat(telepathist.get(TestConfig.obj1Key, TestConfig.override11).orDefault(defaultValue))
                .isEqualTo(TestConfig.override11);
        assertThat(telepathist.get(TestConfig.obj1Key, TestConfig.value11).orDefault(defaultValue))
                .isEqualTo(TestConfig.value11);
    }

    @Test
    public void getArray_whenPathIsCorrect_shouldWork() {
        remoteConfig.override(new JsonPath(), TestConfig.INSTANCE.withArrays());
        ConfigArray array = telepathist.getArray(TestConfig.array1Key);
        assertThat(array.get(-1).<String>orNull()).isNull();
        assertThat(array.get(0).<String>orNull()).isEqualTo(TestConfig.value11);
        assertThat(array.get(1).<String>orNull()).isEqualTo(TestConfig.value111);
        assertThat(array.get(2).<String>orNull()).isNull();
    }

    @Test
    public void getArray_shouldBeIterable() {
        remoteConfig.override(new JsonPath(), TestConfig.INSTANCE.withArrays());
        ConfigArray array = telepathist.getArray(TestConfig.array1Key);
        String[] expectedResult = new String[] {TestConfig.value11, TestConfig.value111};
        int i = 0;
        for (ConfigSubtree subtree : array) {
            assertThat(subtree.get().<String>orNull()).isEqualTo(expectedResult[i++]);
        }
    }

    @Test
    public void getArray_whenPathIsIncorrect_shouldReturnEmptyArray() {
        remoteConfig.override(new JsonPath(), TestConfig.INSTANCE.withArrays());
        ConfigArray array = telepathist.getArray(TestConfig.value11);
        assertThat(array.get(0).<String>orNull()).isEqualTo(null);
        assertThat(array.get(1).<String>orNull()).isEqualTo(null);
    }

    @Test
    public void getArray_whenPathIsIncorrect_shouldBeEmptyIterable() {
        remoteConfig.override(new JsonPath(), TestConfig.INSTANCE.withArrays());
        ConfigArray array = telepathist.getArray(TestConfig.value11);
        int i = 0;
        for (ConfigSubtree subtree : array) {
            i++;
        }
        assertThat(i).isZero();
    }

    @Test
    public void get_shouldReturnMap_ifCalledOnEmptyPath() {
        assertThat(telepathist.get().<Map<?, ?>>orNull()).isEqualTo(TestConfig.INSTANCE.getMap());
    }
}
