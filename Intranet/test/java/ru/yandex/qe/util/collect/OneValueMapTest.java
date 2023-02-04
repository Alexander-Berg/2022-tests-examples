package ru.yandex.qe.util.collect;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author entropia
 */
public class OneValueMapTest {
  @Test
  public void map_created_from_empty_array_is_empty() {
    assertTrue(OneValueMap.of("value").isEmpty());
  }

  @Test
  public void map_created_from_empty_iterable_is_empty() {
    assertTrue(OneValueMap.of("value", Collections.emptySet()).isEmpty());
  }

  @Test
  public void map_created_from_empty_array_has_size_zero() {
    assertEquals(0, OneValueMap.of("value").size());
  }

  @Test
  public void map_created_from_empty_iterable_has_size_zero() {
    assertEquals(0, OneValueMap.of("value", Collections.emptySet()).size());
  }

  @Test
  public void takes_a_snapshot_of_array() {
    final Object key = new Object();
    final Object[] keys = new Object[]{key};

    final String value = "value";
    final Map<Object, String> map = OneValueMap.of(value, keys);

    keys[0] = null;
    assertEquals(value, map.get(key));
  }

  @Test
  public void takes_a_snapshot_of_iterable() {
    final Object key = new Object();
    final Set<Object> keys = new HashSet<>();
    keys.add(key);

    final String value = "value";
    final Map<Object, String> map = OneValueMap.of(value, keys);

    keys.clear();
    assertEquals(value, map.get(key));
  }

  @Test
  public void maps_all_keys_to_the_same_value() {
    final Set<Object> keys = new HashSet<>();
    for (int i = 0; i < 10; i++) {
      keys.add(new Object());
    }

    final String value = "value";
    final Map<Object, String> map = OneValueMap.of(value, keys);

    for (Object key: keys) {
      assertEquals(value, map.get(key));
    }
  }

  @Test
  public void keys_are_in_order_with_duplicates_removed() {
    final List<String> keys = Arrays.asList("key1", "key2", "key1", "key3", "key4");
    final Map<String, Object> map = OneValueMap.of(new Object(), keys);

    assertEquals(map.keySet(), ImmutableSet.of("key1", "key2", "key3", "key4"));
  }

  @Test
  public void size_of_nonempty_map_equals_the_number_of_unique_keys() {
    final List<String> keys = Arrays.asList("key1", "key2", "key1", "key3", "key4");
    final Map<String, Object> map = OneValueMap.of(new Object(), keys);

    assertEquals(ImmutableSet.of("key1", "key2", "key3", "key4").size(), map.size());
  }

  @Test
  public void map_is_immutable_remove_fails_with_UOE() {
    assertThrows(UnsupportedOperationException.class, () -> {
        final Object key = new Object();
        final Map<?, ?> m = OneValueMap.of("value", key);
        m.remove(key);
    });
  }

  @Test
  public void map_is_immutable_clear_fails_with_UOE() {
    assertThrows(UnsupportedOperationException.class, () -> {
        final Object key = new Object();
        final Map<?, ?> m = OneValueMap.of("value", key);
        m.clear();
    });
  }

  @Test
  public void map_is_immutable_put_fails_with_UOE() {
    assertThrows(UnsupportedOperationException.class, () -> {
        final Map<Object, String> m = OneValueMap.of("value", new Object());
        m.put(new Object(), "value");
    });
  }

  @Test
  public void cannot_create_map_with_null_value() {
    assertThrows(NullPointerException.class, () -> {
        OneValueMap.of(null, new Object());
    });
  }

  @Test
  public void cannot_create_map_with_null_key_array() {
    assertThrows(NullPointerException.class, () -> {
        OneValueMap.of(new Object(), (Object[]) null);
    });
  }

  @Test
  public void cannot_create_map_with_null_iterable() {
    assertThrows(NullPointerException.class, () -> {
        OneValueMap.of(new Object(), (Iterable<?>) null);
    });
  }

  @Test
  public void cannot_create_map_with_array_containing_nulls() {
    assertThrows(NullPointerException.class, () -> {
        OneValueMap.of(new Object(), "abc", "def", null);
    });
  }

  @Test
  public void cannot_create_map_with_iterable_containing_nulls() {
    assertThrows(NullPointerException.class, () -> {
        OneValueMap.of(new Object(), Arrays.asList("abc", "def", null));
    });
  }

  @Test
  public void contains_key_returns_false_for_nonexistent_key() {
    final Map<Object, String> m = OneValueMap.of("value", new Object());
    assertFalse(m.containsKey(new Object()));
  }

  @Test
  public void contains_key_returns_true_for_existing_keys() {
    final List<String> keys = Arrays.asList("key1", "key2", "key3", "key4");
    final Map<String, Object> map = OneValueMap.of(new Object(), keys);

    for (String key: keys) {
      assertTrue(map.containsKey(key));
    }
  }

  @Test
  public void contains_value_returns_true_if_value_equals_specified_at_creation_time() {
    final Object value = new Object();
    final Map<String, Object> m = OneValueMap.of(value, "key");
    assertTrue(m.containsValue(value));
  }

  @Test
  public void contains_value_returns_false_for_all_other_values() {
    final Object value = new Object();
    final Map<String, Object> m = OneValueMap.of(value, "key");
    assertFalse(m.containsValue(new Object()));
  }

  @Test
  public void get_returns_same_value_for_all_specified_keys() {
    final List<String> keys = Arrays.asList("key1", "key2", "key3", "key4");
    final Object value = new Object();
    final Map<String, Object> map = OneValueMap.of(value, keys);

    for (String key: keys) {
      assertEquals(value, map.get(key));
    }
  }

  @Test
  public void get_returns_null_for_all_other_keys() {
    final List<String> keys = Arrays.asList("key1", "key2", "key3", "key4");
    final Object value = new Object();
    final Map<String, Object> map = OneValueMap.of(value, keys);

    assertEquals(null, map.get("key5"));
  }
}
