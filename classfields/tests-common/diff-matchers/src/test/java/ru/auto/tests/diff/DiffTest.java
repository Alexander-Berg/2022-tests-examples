package ru.auto.tests.diff; /**
 * Created by vicdev on 26.10.17.
 */

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.junit.Test;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.diff.matcher.DiffJsonMatcher.hasNoDiff;

/**
 * Created by vicdev on 19.10.17.
 */
public class DiffTest {


    private static final String SIMPLE_JSON = "{ \"d\": 10000 }";
    private static final String SIMPLE_JSON_WITH_ADDED = "{ \"d\": 10000 , \"added\": 1 }";
    private static final String SIMPLE_JSON_WITH_ADDED_2 = "{ \"d\": 10000 , \"added\": 2 }";

    private static final String JSON_STR = "{ \"str\": \"123123213\" }";
    private static final String JSON_STR_MODIFIED = "{ \"str\": \"sda\" }";
    private static final String JSON_REGEXP_TEST = "{ \"d\": 10000, \"added\": \"aahttpAA\" }";
    private static final String JSON_REGEXP_TEST_RESULT = "{ \"d\": 10000, \"added\": \"aaAA\" }";

    private static final String JSON_COLOR = "{\n" +
            "  \"colors\": [\n" +
            "    {\n" +
            "      \"color\": \"black\",\n" +
            "      \"category\": \"hue\",\n" +
            "      \"type\": \"primary\",\n" +
            "      \"code\": {\n" +
            "        \"rgba\": [255,255,255,1],\n" +
            "        \"hex\": \"#000\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"color\": \"white\",\n" +
            "      \"category\": \"value\",\n" +
            "      \"code\": {\n" +
            "        \"rgba\": [0,0,0,1],\n" +
            "        \"hex\": \"#FFF\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"color\": \"red\",\n" +
            "      \"category\": \"hue\",\n" +
            "      \"type\": \"primary\",\n" +
            "      \"code\": {\n" +
            "        \"rgba\": [255,0,0,1],\n" +
            "        \"hex\": \"#FF0\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"color\": \"blue\",\n" +
            "      \"category\": \"hue\",\n" +
            "      \"type\": \"primary\",\n" +
            "      \"code\": {\n" +
            "        \"rgba\": [0,0,255,1],\n" +
            "        \"hex\": \"#00F\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"color\": \"yellow\",\n" +
            "      \"category\": \"hue\",\n" +
            "      \"type\": \"primary\",\n" +
            "      \"code\": {\n" +
            "        \"rgba\": [255,255,0,1],\n" +
            "        \"hex\": \"#FF0\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"color\": \"green\",\n" +
            "      \"category\": \"hue\",\n" +
            "      \"type\": \"secondary\",\n" +
            "      \"code\": {\n" +
            "        \"rgba\": [0,255,0,1],\n" +
            "        \"hex\": \"#0F0\"\n" +
            "      }\n" +
            "    },\n" +
            "  ]\n" +
            "}\n";

    private static final String JSON_COLOR_MODIFIED = "{\n" +
            "  \"colors\": [\n" +
            "    {\n" +
            "      \"color\": \"black\",\n" +
            "      \"category\": \"hue\",\n" +
            "      \"type\": \"primary\",\n" +
            "      \"code\": {\n" +
            "        \"rgba\": [255,255,255,1],\n" +
            "        \"hex\": \"#000\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"color\": \"white\",\n" +
            "      \"category\": \"value\",\n" +
            "      \"code\": {\n" +
            "        \"rgba\": [0,0,0,1],\n" +
            "        \"hex\": \"#FFF\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"color\": \"red\",\n" +
            "      \"category\": \"hue\",\n" +
            "      \"type\": \"primary\",\n" +
            "      \"code\": {\n" +
            "        \"rgba\": [255,0,0,1],\n" +
            "        \"hex\": \"#FF0\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"color\": \"blue\",\n" +
            "      \"category\": \"hue\",\n" +
            "      \"type\": \"primary\",\n" +
            "      \"code\": {\n" +
            "        \"rgba\": [0,0,255,1],\n" +
            "        \"hex\": \"#00F\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"color\": \"asdasdasdasd\",\n" +
            "      \"category\": \"123\",\n" +
            "      \"type\": \"primary\",\n" +
            "      \"code\": {\n" +
            "        \"rgba\": [255,255,0,1],\n" +
            "        \"hex\": \"#FF0\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"color\": \"green\",\n" +
            "      \"category\": \"hue\",\n" +
            "      \"type\": \"aaaaa\",\n" +
            "      \"code\": {\n" +
            "        \"rgba\": [0,255,0,1],\n" +
            "        \"hex\": \"#0F0\"\n" +
            "      }\n" +
            "    },\n" +
            "  ]\n" +
            "}\n";

    @Test
    public void shouldEmptyJsonEquals() {
        Gson gson = new GsonBuilder().create();
        JsonObject original = gson.fromJson(JSON_STR, JsonObject.class);
        JsonObject modified = gson.fromJson(JSON_STR_MODIFIED, JsonObject.class);
        assertThat(modified, hasNoDiff(original).ignore("$.str"));
    }

    @Test
    public void shouldJsonEqualsWithIgnoredType() {
        Gson gson = new GsonBuilder().create();
        JsonObject original = gson.fromJson(JSON_COLOR, JsonObject.class);
        JsonObject modified = gson.fromJson(JSON_COLOR, JsonObject.class);
        assertThat(modified, hasNoDiff(original).ignore("$.colors[*].type"));
    }

    @Test
    public void shouldJsonEqualsWithExcludedType() {
        Gson gson = new GsonBuilder().create();
        JsonObject original = gson.fromJson(JSON_COLOR, JsonObject.class);
        JsonObject modified = gson.fromJson(JSON_COLOR, JsonObject.class);
        assertThat(modified, hasNoDiff(original).exclude("$.colors[*].type"));
    }


    @Test
    public void shouldJsonNotEquals() {
        Gson gson = new GsonBuilder().create();
        JsonObject original = gson.fromJson(JSON_COLOR, JsonObject.class);
        JsonObject modified = gson.fromJson(JSON_COLOR_MODIFIED, JsonObject.class);
        assertThat(modified, not(hasNoDiff(original)));
    }

    @Test
    public void shouldJsonNotEqualsWithIgnored() {
        Gson gson = new GsonBuilder().create();
        JsonObject original = gson.fromJson(JSON_COLOR, JsonObject.class);
        JsonObject modified = gson.fromJson(JSON_COLOR_MODIFIED, JsonObject.class);
        assertThat(modified, not(hasNoDiff(original).ignore("$.colors[*].type")));
    }

    @Test
    public void shouldJsonNotEqualsWithIgnores() {
        Gson gson = new GsonBuilder().create();
        JsonObject originalO = gson.fromJson(JSON_COLOR, JsonObject.class);
        JsonObject modifiedO = gson.fromJson(JSON_COLOR_MODIFIED, JsonObject.class);
        assertThat(modifiedO, not(hasNoDiff(originalO).ignore("$.colors[*].type", "$.colors[*].category")));
    }

    @Test
    public void shouldJsonNotEqualsWithIgnoredList() {
        Gson gson = new GsonBuilder().create();
        JsonObject originalO = gson.fromJson(JSON_COLOR, JsonObject.class);
        JsonObject modifiedO = gson.fromJson(JSON_COLOR_MODIFIED, JsonObject.class);
        assertThat(modifiedO, not(hasNoDiff(originalO).ignore(newArrayList("$.colors[*].type", "$.colors[*].category"))));
    }

    @Test
    public void shouldJsonEqualsIgnoredLong() {
        Gson gson = new GsonBuilder().create();
        JsonObject originalO = gson.fromJson(SIMPLE_JSON_WITH_ADDED, JsonObject.class);
        JsonObject modifiedO = gson.fromJson(SIMPLE_JSON_WITH_ADDED_2, JsonObject.class);
        assertThat(modifiedO, hasNoDiff(originalO).ignore("$.added"));
    }


    @Test
    public void shouldEqualsTwoObject() {
        TestObject object = new TestObject().withAdded("11").withConfirmed(true)
                .withEmail("email");
        assertThat(object, hasNoDiff(object));
    }

    @Test
    public void shouldNotEqualsTwoObject() {
        TestObject object = new TestObject().withAdded("11").withConfirmed(true)
                .withEmail("email");
        TestObject object2 = new TestObject().withAdded("1112313").withConfirmed(false)
                .withEmail("email11111");
        assertThat(object, not(hasNoDiff(object2)));
    }

    @Test
    public void shouldEqualExcludeIfAddedField() {
        Gson gson = new GsonBuilder().create();
        JsonObject original = gson.fromJson(SIMPLE_JSON, JsonObject.class);
        JsonObject modified = gson.fromJson(SIMPLE_JSON_WITH_ADDED, JsonObject.class);
        assertThat(modified, hasNoDiff(original).exclude(".added"));
    }

    @Test
    public void shouldExcludeEmpty() {
        Gson gson = new GsonBuilder().create();
        JsonObject original = gson.fromJson(SIMPLE_JSON, JsonObject.class);
        JsonObject modified = gson.fromJson(SIMPLE_JSON, JsonObject.class);
        assertThat(modified, hasNoDiff(original).exclude(EMPTY));
    }

    @Test
    public void shouldIgnoreEmpty() {
        Gson gson = new GsonBuilder().create();
        JsonObject original = gson.fromJson(SIMPLE_JSON, JsonObject.class);
        JsonObject modified = gson.fromJson(SIMPLE_JSON, JsonObject.class);
        assertThat(modified, hasNoDiff(original).ignore(EMPTY));
    }

    @Test
    public void shouldExcludeNotValidPath() {
        Gson gson = new GsonBuilder().create();
        JsonObject original = gson.fromJson(SIMPLE_JSON, JsonObject.class);
        JsonObject modified = gson.fromJson(SIMPLE_JSON, JsonObject.class);
        assertThat(modified, hasNoDiff(original).exclude(getRandomString()));
    }

    @Test
    public void shouldIgnoreNotValidPath() {
        Gson gson = new GsonBuilder().create();
        JsonObject original = gson.fromJson(SIMPLE_JSON, JsonObject.class);
        JsonObject modified = gson.fromJson(SIMPLE_JSON, JsonObject.class);
        assertThat(modified, hasNoDiff(original).ignore(getRandomString()));
    }

    @Test
    public void shouldExcludeString() {
        Gson gson = new GsonBuilder().create();
        JsonObject original = gson.fromJson(JSON_REGEXP_TEST, JsonObject.class);
        JsonObject modified = gson.fromJson(JSON_REGEXP_TEST_RESULT, JsonObject.class);
        assertThat(modified, hasNoDiff(original).excludeRegexp("http"));
    }

    @Test
    public void shouldExcludeRegexp() {
        Gson gson = new GsonBuilder().create();
        JsonObject original = gson.fromJson(JSON_REGEXP_TEST_RESULT, JsonObject.class);
        JsonObject modified = gson.fromJson(JSON_REGEXP_TEST_RESULT, JsonObject.class);
        assertThat(modified, hasNoDiff(original).excludeRegexp("\\\\u0026utm_content\\\\u003doffers_shuffle_disable"));
    }
}
