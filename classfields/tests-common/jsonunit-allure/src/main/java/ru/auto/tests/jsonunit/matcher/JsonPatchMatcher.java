package ru.auto.tests.jsonunit.matcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.attachment.DefaultAttachmentProcessor;
import io.qameta.allure.attachment.FreemarkerAttachmentRenderer;
import io.qameta.allure.jsonunit.AllureConfigurableJsonMatcher;
import io.qameta.allure.jsonunit.DiffAttachment;
import io.qameta.allure.jsonunit.JsonPatchListener;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import net.javacrumbs.jsonunit.core.Configuration;
import net.javacrumbs.jsonunit.core.Option;
import net.javacrumbs.jsonunit.core.internal.Diff;
import net.javacrumbs.jsonunit.core.internal.Options;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_ARRAY_ITEMS;

@SuppressWarnings("unused")
public final class JsonPatchMatcher<T> implements AllureConfigurableJsonMatcher<T> {

    private static AllureLifecycle lifecycle;

    private static final int MAX_WIDTH = 700;
    private static final String EMPTY_PATH = "";
    private static final String FULL_JSON = "fullJson";
    private Configuration configuration = Configuration.empty();
    private final Object expected;
    private String differences;

    private JsonPatchMatcher(final Object expected) {
        this.expected = expected;
    }

    public static <T> AllureConfigurableJsonMatcher<T> jsonEquals(final Object expected) {
        return new JsonPatchMatcher<T>(expected)
                //ignoring extra items by default
                .when(Option.IGNORING_EXTRA_FIELDS, IGNORING_EXTRA_ARRAY_ITEMS);
    }

    @Override
    public AllureConfigurableJsonMatcher<T> withTolerance(final BigDecimal tolerance) {
        configuration = configuration.withTolerance(tolerance);
        return this;
    }

    @Override
    public AllureConfigurableJsonMatcher<T> withTolerance(final double tolerance) {
        configuration = configuration.withTolerance(tolerance);
        return this;
    }

    @Override
    public AllureConfigurableJsonMatcher<T> when(final Option first, final Option... next) {
        configuration = configuration.when(first, next);
        return this;
    }

    @Override
    public AllureConfigurableJsonMatcher<T> withOptions(final Options options) {
        configuration = configuration.withOptions(options);
        return this;
    }

    @Override
    public AllureConfigurableJsonMatcher<T> withMatcher(final String matcherName, final Matcher matcher) {
        configuration = configuration.withMatcher(matcherName, matcher);
        return this;
    }

    @Override
    public AllureConfigurableJsonMatcher<T> whenIgnoringPaths(final String... paths) {
        configuration = configuration.whenIgnoringPaths(paths);
        return this;
    }

    @Override
    public boolean matches(final Object actual) {
        final String uuid = UUID.randomUUID().toString();
        final StepResult result = new StepResult()
                .setName("Has no difference").setStatus(Status.PASSED);
        getLifecycle().startStep(uuid, result);
        final Diff diff;
        try {
            final JsonPatchListener listener = new JsonPatchListener();
            diff = Diff.create(expected, actual, FULL_JSON, EMPTY_PATH,
                    configuration.withDifferenceListener(listener));
            if (!diff.similar()) {
                getLifecycle().updateStep(uuid, s -> s.setStatus(Status.FAILED));
                differences = diff.differences();
                attachTextDifference(differences);
                render(listener);
            }
        } catch (Throwable e) {
            getLifecycle().updateStep(uuid, s -> s
                    .setStatus(getStatus(e).orElse(Status.BROKEN))
                    .setStatusDetails(getStatusDetails(e).orElse(null)));
            throw e;
        } finally {
            getLifecycle().stopStep(uuid);
        }
        return diff.similar();
    }

    private void attachTextDifference(String differences) {
        ExceptionAttachment exceptionAttachment = new ExceptionAttachment(differences);
        new DefaultAttachmentProcessor().addAttachment(exceptionAttachment,
                new FreemarkerAttachmentRenderer("exception.ftl"));
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText("has no difference");
    }

    @Override
    public void describeMismatch(final Object item, final Description description) {
        description.appendText(StringUtils.abbreviate(differences, MAX_WIDTH));
    }

    @SuppressWarnings("deprecation")
    @Override
    public void _dont_implement_Matcher___instead_extend_BaseMatcher_() {
        //do nothing
    }

    private void render(final JsonPatchListener listener) {
        final ObjectMapper mapper = new ObjectMapper();
        final String patch = listener.getJsonPatch();
        try {
            final String actual = mapper.writeValueAsString(listener.getContext().getActualSource());
            final String expected = mapper.writeValueAsString(listener.getContext().getExpectedSource());
            final DiffAttachment attachment = new DiffAttachment(actual, expected, patch);
            new DefaultAttachmentProcessor().addAttachment(attachment,
                    new FreemarkerAttachmentRenderer("diff.ftl"));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not process actual/expected json", e);
        }
    }

    private static AllureLifecycle getLifecycle() {
        if (Objects.isNull(lifecycle)) {
            lifecycle = Allure.getLifecycle();
        }
        return lifecycle;
    }
}
