package ru.yandex.solomon.alert.dao.ydb.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.grpc.Status;
import org.junit.Test;

import ru.yandex.solomon.alert.api.converters.NotificationConverter;
import ru.yandex.solomon.alert.cluster.broker.alert.activity.search.AlertPersistStateSupport;
import ru.yandex.solomon.alert.notification.channel.telegram.ChatIdResolverStub;
import ru.yandex.solomon.alert.protobuf.TPersistAlertState;
import ru.yandex.solomon.ut.ManualClock;

import static org.junit.Assert.assertEquals;

/**
 * @author Vladimir Gordiychuk
 */
public abstract class AlertStatesFilesDaoTest {

    private AlertPersistStateSupport support = new AlertPersistStateSupport(new NotificationConverter(new ChatIdResolverStub()));
    private ManualClock clock = new ManualClock();

    protected abstract AlertStatesFilesDao getDao();

    @Test
    public void saveOneReadOne() {
        getDao().createSchemaForTests().join();
        var expected = randomState();
        upload("solomon", "myFile", List.of(expected));
        var result = download("solomon", "myFile");
        assertEquals(List.of(expected), result);
    }

    @Test
    public void saveManyReadMany() {
        getDao().createSchemaForTests().join();
        var source = IntStream.range(0, 10)
            .mapToObj(ignore -> randomState())
            .collect(Collectors.toList());

        upload("junk", "test", source);
        var result = download("junk", "test");
        assertEquals(source, result);
    }

    @Test
    public void save20MegabyteState() {
        getDao().createSchemaForTests().join();
        final int expectedSize = 20 << 20;
        List<TPersistAlertState> source = new ArrayList<>();
        int size = 0;
        while (size < expectedSize) {
            TPersistAlertState state = randomState();
            size += state.getSerializedSize();
            source.add(state);
        }

        upload("junk", "state", source);
        var result = download("junk", "state");
        assertEquals(source, result);
    }

    @Test
    public void updateState() {
        getDao().createSchemaForTests().join();
        TPersistAlertState v1 = randomState();
        upload("solomon", "v1", List.of(v1));

        TPersistAlertState v2 = randomState();
        upload("solomon", "v2", List.of(v2));

        {
            var result = download("solomon", "v1");
            assertEquals(List.of(v1), result);
        }
        {
            var result = download("solomon", "v2");
            assertEquals(List.of(v2), result);
        }
        {
            getDao().deleteFile("solomon", "v1").join();
            assertEquals(List.of(), download("solomon", "v1"));
            assertEquals(List.of(v2), download("solomon", "v2"));
        }
    }

    @Test
    public void deleteProject() {
        getDao().createSchemaForTests().join();
        TPersistAlertState alice = randomState();
        upload("alice", "v1", List.of(alice));

        TPersistAlertState bob = randomState();
        upload("bob", "v1", List.of(bob));

        {
            var result = download("alice", "v1");
            assertEquals(List.of(alice), result);
        }
        {
            var result = download("bob", "v1");
            assertEquals(List.of(bob), result);
        }

        getDao().deleteProject("bob").join();
        assertEquals(List.of(alice), download("alice", "v1"));
        assertEquals(List.of(), download("bob", "v1"));
    }

    private void upload(String projectId, String fileName, List<TPersistAlertState> data) {
        upload(getDao(), projectId, fileName, data);
    }

    protected void upload(AlertStatesFilesDao dao, String projectId, String fileName, List<TPersistAlertState> data) {
        dao.upload(projectId, fileName, clock.millis(), data).join();
    }

    private List<TPersistAlertState> download(String projectId, String fileName) {
        return download(getDao(), projectId, fileName);
    }

    protected List<TPersistAlertState> download(AlertStatesFilesDao dao, String projectId, String fileName) {
        try {
            List<TPersistAlertState> result = new ArrayList<>();
            dao.download(projectId, fileName, result::add).join();
            return result;
        } catch (CompletionException e) {
            var cause = e.getCause();
            var status = Status.fromThrowable(cause);
            assertEquals(status.toString(), Status.Code.NOT_FOUND, status.getCode());
            return List.of();
        }
    }

    protected TPersistAlertState randomState() {
        return support.randomState();
    }
}
