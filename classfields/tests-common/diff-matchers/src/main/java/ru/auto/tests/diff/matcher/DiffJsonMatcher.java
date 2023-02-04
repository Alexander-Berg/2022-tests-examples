package ru.auto.tests.diff.matcher;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.newArrayList;
import static io.qameta.allure.model.Status.FAILED;
import static io.qameta.allure.model.Status.PASSED;
import static org.apache.commons.lang3.StringEscapeUtils.escapeJson;
import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * Created by vicdev on 22.10.17.
 */

public class DiffJsonMatcher<T> extends TypeSafeMatcher<T> {

    private static final int CONTEXT_SIZE = 1000;
    private static final String ORIGINAL_NAME = "original";
    private static final String MODIFIED_NAME = "modified";
    private final T original;
    private List<Delta<String>> deltas;

    @Getter
    @Setter
    private String resultsDiff = EMPTY;

    @Getter
    @Setter
    private String originalDiff = EMPTY;

    @Getter
    @Setter
    private String modifiedDiff = EMPTY;

    @Getter
    @Setter
    private List<String> ignoredFields = newArrayList();

    @Getter
    @Setter
    private List<String> excludedFields = newArrayList();

    @Getter
    @Setter
    private List<String> regexpExcludedFields = newArrayList();

    private DiffJsonMatcher(T original) {
        this.original = original;
    }

    @Override
    protected boolean matchesSafely(T modified) {
        String originalJson = mappingObject(original);
        String modifiedJson = mappingObject(modified);

        List<String> originalInitList = prettyJson(originalJson);
        List<String> modifiedInitList = prettyJson(modifiedJson);

        if (!regexpExcludedFields.isEmpty()) {
            originalJson = excludeRegexpFields(originalJson);
            modifiedJson = excludeRegexpFields(modifiedJson);
        }

        if (!ignoredFields.isEmpty()) {
            originalJson = ignoreFields(originalJson);
            modifiedJson = ignoreFields(modifiedJson);
        }

        if (!excludedFields.isEmpty()) {
            originalJson = excludeFields(originalJson);
            modifiedJson = excludeFields(modifiedJson);
        }

        List<String> originalList = prettyJson(originalJson);
        List<String> modifiedList = prettyJson(modifiedJson);

        originalDiff = Joiner.on("\n").join(DiffUtils.generateUnifiedDiff(ORIGINAL_NAME, ORIGINAL_NAME, originalList,
                DiffUtils.diff(originalInitList, originalList), CONTEXT_SIZE));

        modifiedDiff = Joiner.on("\n").join(DiffUtils.generateUnifiedDiff(MODIFIED_NAME, MODIFIED_NAME, modifiedList,
                DiffUtils.diff(modifiedInitList, modifiedList), CONTEXT_SIZE));

        Patch<String> patch = DiffUtils.diff(originalList, modifiedList);
        resultsDiff = Joiner.on("\n").join(DiffUtils.generateUnifiedDiff(ORIGINAL_NAME, MODIFIED_NAME, originalList, patch, CONTEXT_SIZE));

        deltas = patch.getDeltas();

        Status status = FAILED;
        if (deltas.isEmpty()) {
            status = PASSED;
        }

        attachDiff(status);
        return deltas.isEmpty();
    }

    private String mappingObject(T o) {
        return new GsonBuilder().setPrettyPrinting().create().toJson(o);
    }

    private List<String> prettyJson(String json) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(json);
        return Splitter.on("\n").splitToList(gson.toJson(je));
    }

    private String excludeFields(String json) {
        DocumentContext context = JsonPath.parse(json);
        for (String field : excludedFields) {
            if (!field.isEmpty()) {
                context.delete(field);
            }
        }
        return context.jsonString();
    }

    private String ignoreFields(String json) {
        DocumentContext context = JsonPath.parse(json);
        for (String field : ignoredFields) {
            if (!field.isEmpty()) {
                context.set(field, "ignored");
            }
        }
        return context.jsonString();
    }

    private String excludeRegexpFields(String json) {
        for (String regexp : regexpExcludedFields) {
            json = StringUtils.removePattern(json, Pattern.compile(regexp).pattern());
        }
        return json;
    }

    public DiffJsonMatcher<T> exclude(String... paths) {
        excludedFields.addAll(newArrayList(paths));
        return this;
    }

    public DiffJsonMatcher<T> exclude(List<String> paths) {
        excludedFields.addAll(paths);
        return this;
    }

    public DiffJsonMatcher<T> excludeRegexp(String... regexp) {
        regexpExcludedFields.addAll(newArrayList(regexp));
        return this;
    }

    public DiffJsonMatcher<T> excludeRegexp(List<String> regexp) {
        regexpExcludedFields.addAll(regexp);
        return this;
    }

    public DiffJsonMatcher<T> ignore(String... paths) {
        ignoredFields.addAll(newArrayList(paths));
        return this;
    }

    public DiffJsonMatcher<T> ignore(List<String> paths) {
        ignoredFields.addAll(paths);
        return this;
    }

    @Factory
    @SuppressWarnings("unchecked")
    public static <T> DiffJsonMatcher<T> hasNoDiff(T original) {
        return new DiffJsonMatcher(original);
    }

    @Override
    protected void describeMismatchSafely(T o, Description mismatchDescription) {
        mismatchDescription.appendText("Deltas:\n").appendValueList("---->", "\n---->", "\n", deltas)
                .appendText("see unified diff in attachment");
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("there should not be differences");
    }

    private DiffJsonMatcher attachDiff(Status status) {
        AllureLifecycle lifecycle = Allure.getLifecycle();
        lifecycle.startStep(
                UUID.randomUUID().toString(),
                new StepResult().withName("HasNoDifference").withStatus(status)
        );
        resultsDiff = escapeJson(resultsDiff);
        originalDiff = escapeJson(originalDiff);
        modifiedDiff = escapeJson(modifiedDiff);

        final byte[] bytes = process("diff", this);
        lifecycle.addAttachment("Diff", "text/html", "html", bytes);

        lifecycle.stopStep();
        return this;
    }

    private static byte[] process(String templateName, Object object) {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_23);
        cfg.setClassForTemplateLoading(DiffJsonMatcher.class, "/templates");
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            Writer writer = new OutputStreamWriter(stream);
            Template template = cfg.getTemplate(String.format("%s.ftl", templateName));
            template.process(object, writer);
            return stream.toByteArray();
        } catch (IOException | TemplateException e) {
            throw new RuntimeException(e);
        }
    }
}
