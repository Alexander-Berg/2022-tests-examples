package ru.yandex.solomon.alert.dao.ydb;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import ru.yandex.devtools.test.annotations.YaIgnore;
import ru.yandex.solomon.alert.dao.EntitiesDao;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.AlertTestSupport;
import ru.yandex.solomon.kikimr.LocalKikimr;
import ru.yandex.solomon.kikimr.YdbHelper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.emptyIterable;
import static org.junit.Assert.assertThat;
import static ru.yandex.solomon.idempotency.IdempotentOperation.NO_OPERATION;

/**
 * @author Vladimir Gordiychuk
 */
@YaIgnore
public class YdbAlertsDaoVersionTest {
    @ClassRule
    public static LocalKikimr kikimr = new LocalKikimr();

    @Rule
    public TestName testName = new TestName();
    private EntitiesDao<Alert> min;
    private EntitiesDao<Alert> max;

    @Before
    public void setUp() throws Exception {
        var mapper = new ObjectMapper();
        var ydb = new YdbHelper(kikimr, this.getClass().getSimpleName() + "_" + testName.getMethodName());
        var root = ydb.getRootPath();
        min = YdbAlertsDaoFactory.create(root, ydb.getTableClient(), ydb.getSchemeClient(), YdbSchemaVersion.MIN, mapper);
        max = YdbAlertsDaoFactory.create(root, ydb.getTableClient(), ydb.getSchemeClient(), YdbSchemaVersion.MAX, mapper);

        min.createSchemaForTests().join();
        max.createSchemaForTests().join();
    }

    @Test
    public void dataMigratedAfterSchemaInit() {
        final String projectId = "junk";
        min.createSchema(projectId).join();

        List<Alert> source = IntStream.range(0, 1000)
                .mapToObj(index -> randomAlert(projectId)
                        .toBuilder()
                        .setName(String.valueOf(index))
                        .build())
                .sorted(Comparator.comparing(Alert::getName))
                .collect(Collectors.toList());

        insertAlerts(source, min);

        max.createSchema("junk").join();

        List<String> result = max.findAll(projectId).join()
                .stream()
                .sorted(Comparator.comparing(Alert::getName))
                .map(Alert::getId)
                .collect(Collectors.toList());

        List<String> expected = source.stream()
                .map(Alert::getId)
                .collect(Collectors.toList());

        assertThat(result, Matchers.equalTo(expected));
    }

    @Test
    public void changesForNewSchemaAlwaysDuplicatedToMin() {
        final String projectId = "junk";
        min.createSchema(projectId).join();
        max.createSchema(projectId).join();

        Alert v1 = randomAlert(projectId)
                .toBuilder()
                .setVersion(10)
                .build();

        max.insert(v1, NO_OPERATION).join();
        assertThat(fetchOne(min, projectId).getVersion(), equalTo(v1.getVersion()));
        assertThat(fetchOne(max, projectId).getVersion(), equalTo(v1.getVersion()));

        Alert v2 = v1.toBuilder()
                .setVersion(11)
                .setName("changed name")
                .build();

        max.update(v2, NO_OPERATION).join();
        assertThat(fetchOne(min, projectId).getVersion(), equalTo(v2.getVersion()));
        assertThat(fetchOne(max, projectId).getVersion(), equalTo(v2.getVersion()));

        max.deleteById(projectId, v2.getId(), NO_OPERATION).join();
        assertThat(min.findAll(projectId).join(), emptyIterable());
        assertThat(max.findAll(projectId).join(), emptyIterable());
    }

    @Test
    public void repeatMigrateIfRequiredUpdates() {
        final String projectId = "junk";
        min.createSchema(projectId).join();
        max.createSchema(projectId).join();

        Alert v1 = randomAlert(projectId)
                .toBuilder()
                .setVersion(10)
                .build();

        max.insert(v1, NO_OPERATION).join();
        assertThat(fetchOne(min, projectId).getVersion(), equalTo(v1.getVersion()));
        assertThat(fetchOne(max, projectId).getVersion(), equalTo(v1.getVersion()));

        Alert v2 = v1.toBuilder()
                .setName("name one")
                .setVersion(v1.getVersion() + 1)
                .build();

        min.update(v2, NO_OPERATION).join();
        assertThat(fetchOne(min, projectId).getVersion(), equalTo(v2.getVersion()));

        Alert v3 = v2.toBuilder()
                .setName("name two")
                .setVersion(v2.getVersion() + 1)
                .build();

        min.update(v3, NO_OPERATION).join();
        assertThat(fetchOne(min, projectId).getVersion(), equalTo(v3.getVersion()));

        max.createSchema(projectId).join();
        assertThat(fetchOne(min, projectId).getVersion(), equalTo(v3.getVersion()));
        assertThat(fetchOne(max, projectId).getVersion(), equalTo(v3.getVersion()));
    }

    @Test
    public void repeatMigrateIfRequiredDeletes() {
        final String projectId = "junk";
        min.createSchema(projectId).join();
        max.createSchema(projectId).join();

        Alert one = randomAlert(projectId);
        Alert two = randomAlert(projectId);
        Alert tree = randomAlert(projectId);

        max.insert(one, NO_OPERATION).join();
        max.insert(two, NO_OPERATION).join();
        max.insert(tree, NO_OPERATION).join();

        min.deleteById(projectId, one.getId(), NO_OPERATION).join();
        min.deleteById(projectId, two.getId(), NO_OPERATION).join();
        min.deleteById(projectId, tree.getId(), NO_OPERATION).join();

        max.createSchema(projectId).join();
        assertThat(min.findAll(projectId).join(), emptyIterable());
        assertThat(max.findAll(projectId).join(), emptyIterable());
    }

    @Test
    public void repeatMigrateIfRequiredInserts() {
        final String projectId = "junk";
        min.createSchema(projectId).join();
        max.createSchema(projectId).join();

        Alert alert = randomAlert(projectId);
        min.insert(alert, NO_OPERATION).join();

        max.createSchema(projectId).join();
        assertThat(fetchOne(min, projectId).getVersion(), equalTo(alert.getVersion()));
        assertThat(fetchOne(max, projectId).getVersion(), equalTo(alert.getVersion()));
    }

    private Alert randomAlert(String projectId) {
        return AlertTestSupport.randomAlert()
            .toBuilder()
            .setProjectId(projectId)
            .build();
    }

    private Alert fetchOne(EntitiesDao<Alert> dao, String projectId) {
        return Iterables.getOnlyElement(dao.findAll(projectId).join());
    }

    private void insertAlerts(List<Alert> alerts, EntitiesDao<Alert> dao) {
        for (Alert alert : alerts) {
            dao.insert(alert, NO_OPERATION).join();
        }
    }
}
