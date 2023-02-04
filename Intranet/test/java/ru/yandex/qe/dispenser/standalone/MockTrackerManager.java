package ru.yandex.qe.dispenser.standalone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.CollectionF;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.qe.dispenser.domain.tracker.TrackerManager;
import ru.yandex.startrek.client.error.EntityNotFoundException;
import ru.yandex.startrek.client.error.ErrorCollection;
import ru.yandex.startrek.client.model.CollectionUpdate;
import ru.yandex.startrek.client.model.CommentCreate;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueCreate;
import ru.yandex.startrek.client.model.IssueUpdate;
import ru.yandex.startrek.client.model.ScalarUpdate;
import ru.yandex.startrek.client.model.StatusRef;
import ru.yandex.startrek.client.model.UserRef;
import ru.yandex.startrek.client.utils.UriBuilder;

@ParametersAreNonnullByDefault
public class MockTrackerManager implements TrackerManager {

    private boolean isTrackerAvailable = true;
    private final Map<String, MockIssue> issues = new ConcurrentHashMap<>();

    @Override
    public String createIssues(final IssueCreate issueCreate) {
        if (!isTrackerAvailable) {
            throw new RuntimeException("Invalid content");
        }
        final Option<Object> unique = issueCreate.getValues().getO("unique");
        if (unique.isPresent()) {
            for (final String key : issues.keySet()) {
                final MockIssue issue = issues.get(key);
                if (unique.get().equals(issue.getValues().get("unique"))) {
                    return key;
                }
            }
        }
        final String key = UUID.randomUUID().toString();
        issues.put(key, new MockIssue(issueCreate));

        return key;
    }

    @Override
    public void executeTransition(final String issueKey, final String targetStatusKey, final IssueUpdate issueUpdate) {
        if (!isTrackerAvailable) {
            throw new RuntimeException("Tracker is unavailable");
        }
        final MockIssue mockIssue = issues.get(issueKey);
        mockIssue.update(issueUpdate);
        mockIssue.values.put("status", targetStatusKey);
    }

    @Override
    public void updateIssue(final String issueKey, final IssueUpdate issueUpdate) {
        if (!isTrackerAvailable) {
            throw new RuntimeException("Tracker is unavailable");
        }
        issues.get(issueKey).update(issueUpdate);
    }

    @Override
    @NotNull
    public Issue getIssue(final String issueKey) {
        if (!isTrackerAvailable) {
            throw new RuntimeException("Tracker is unavailable");
        }
        final MockIssue mockIssue = issues.get(issueKey);
        if (mockIssue == null) {
            throw new EntityNotFoundException(new ErrorCollection(Cf.map(),
                    Cf.list("Issue not found"), 404, Option.empty()));
        }
        final String summary = mockIssue.getValues().containsKey("summary")
                ? (String) mockIssue.getValues().get("summary") : "Summary";
        final MapF<String, Object> values = Cf.hashMap();
        mockIssue.getValues().forEach((key, value) -> {
            if (key.equals("assignee")) {
                values.put(key, Option.ofNullable(new MockUserRef((String) value)));
            } else if (key.equals("followers")) {
                if (value instanceof Object[]) {
                    values.put(key, Cf.list((Object[]) value).map(String::valueOf).map(MockUserRef::new));
                } else {
                    values.put(key, ((CollectionF<?>)value).map(String::valueOf).map(MockUserRef::new));
                }
            } else if (key.equals("status")) {
                values.put(key, new MockStatusRef((String) value));
            } else {
                values.put(key, value);
            }
        });
        return new Issue("id",
                UriBuilder.cons("https://st-api.yandex.ru/v2/issues/" + issueKey).build(),
                issueKey, summary, 1, values, null);
    }

    @Override
    public void createComment(final String issueKey, final CommentCreate comment) {
        if (!isTrackerAvailable) {
            throw new RuntimeException("Tracker is unavailable");
        }
        issues.get(issueKey)
                .update(IssueUpdate.builder()
                        .comment(comment)
                        .build());
    }

    @TestOnly
    public Integer getVersion(final String issueKey) {
        if (!isTrackerAvailable) {
            throw new RuntimeException("Tracker is unavailable");
        }
        return issues.containsKey(issueKey) ?
                issues.get(issueKey).getVersion()
                .get()
                : null;
    }

    @TestOnly
    public void setTrackerAvailable(final boolean trackerAvailable) {
        isTrackerAvailable = trackerAvailable;
    }

    @TestOnly
    public Map<String, Object> getIssueFields(final String key) {
        return issues.get(key).getValues();
    }

    @TestOnly
    public List<CommentCreate> getIssueComments(final String issueKey) {
        return issues.get(issueKey).getComments();
    }

    @TestOnly
    public List<CommentCreate> getIssuesComments() {
        return issues.values().stream().flatMap(i -> i.getComments().stream()).collect(Collectors.toList());
    }

    @TestOnly
    public List<Issue> getIssues() {
        return issues.keySet().stream().map(this::getIssue).collect(Collectors.toList());
    }

    @TestOnly
    public void clearIssues() {
        issues.clear();
    }

    public static class MockIssue {
        private final Map<String, Object> values;
        private final List<CommentCreate> comments;
        private final AtomicInteger version;

        private MockIssue(final IssueCreate issueCreate) {
            this.values = issueCreate.getValues();
            this.comments = new ArrayList<>();
            this.values.put("status", "open");
            this.values.put("createdBy", new MockUserRef((String) this.values.get("author")));
            this.version = new AtomicInteger();
        }

        public Map<String, Object> getValues() {
            return values;
        }

        public List<CommentCreate> getComments() {
            return comments;
        }

        public AtomicInteger getVersion() {
            return version;
        }

        public void update(final IssueUpdate issueUpdate) {
            version.getAndIncrement();
            comments.addAll(issueUpdate.getComment());
            issueUpdate.getValues().forEach((k, v) -> {
                if (v instanceof ScalarUpdate) {
                    final Option<?> set = ((ScalarUpdate<?>) v).getSet();
                    if (set.isPresent()) {
                        values.put(k, set.get());
                    }
                } else if (v instanceof CollectionUpdate) {
                    final ListF<?> set = ((CollectionUpdate<?>) v).getSet();
                    if (!set.isEmpty()) {
                        values.put(k, set);
                    } else {
                        final ListF<?> add = ((CollectionUpdate<?>) v).getAdd();
                        final ListF<?> remove = ((CollectionUpdate<?>) v).getRemove();
                        Object value = values.get(k);
                        if (value == null) {
                            value = new ArrayList<>();
                        } else if (value.getClass().isArray()) {
                            value = new ArrayList<>(Arrays.asList((Object[]) value));
                        }
                        ((Collection<Object>) value).addAll(add);
                        ((Collection<Object>) value).removeAll(remove);
                        values.put(k, ((Collection<Object>) value).toArray());
                    }
                }
            });
        }
    }

    public static class MockUserRef extends UserRef {

        public MockUserRef(String login) {
            super(login, null, null, null);
        }

    }

    public static class MockStatusRef extends StatusRef {
        public MockStatusRef(final String status) {
            super(status.hashCode(), null, status, status, null);
        }
    }

}
