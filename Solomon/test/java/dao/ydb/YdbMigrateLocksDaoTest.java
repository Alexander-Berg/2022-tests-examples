package ru.yandex.solomon.alert.dao.ydb;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.yandex.ydb.scheme.SchemeOperationProtos;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import ru.yandex.devtools.test.annotations.YaIgnore;
import ru.yandex.solomon.alert.dao.SchemaAwareLocksDao;
import ru.yandex.solomon.kikimr.LocalKikimr;
import ru.yandex.solomon.kikimr.YdbHelper;
import ru.yandex.solomon.locks.LockDetail;
import ru.yandex.solomon.ut.ManualClock;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

/**
 * @author Vladimir Gordiychuk
 */
@YaIgnore
public class YdbMigrateLocksDaoTest {
    @ClassRule
    public static LocalKikimr kikimr = new LocalKikimr();

    @Rule
    public TestName testName = new TestName();
    private ManualClock clock;
    private String path;
    private SchemaAwareLocksDao min;
    private SchemaAwareLocksDao max;
    private YdbHelper ydb;

    @Before
    public void setUp() throws Exception {
        clock = new ManualClock();
        ydb = new YdbHelper(kikimr, this.getClass().getSimpleName() + "_" + testName.getMethodName());
        path = ydb.getRootPath();
        min = YdbLocksDaoFactory.create(path, ydb.getTableClient(), ydb.getSchemeClient(), YdbSchemaVersion.MIN, clock);
        max = YdbLocksDaoFactory.create(path, ydb.getTableClient(), ydb.getSchemeClient(), YdbSchemaVersion.MAX, clock);
    }

    @Test
    public void prevVersionCanAbsent() {
        max.createSchemaForTests().join();

        LockDetail acquired = max.acquireLock("test", "alice", clock.instant().plusSeconds(90)).join();
        assertThat(acquired.owner(), equalTo("alice"));
        assertThat(acquired.seqNo(), not(equalTo(0L)));

        LockDetail read = max.readLock("test").thenApply(Optional::get).join();
        assertThat(acquired, equalTo(read));

        boolean extend = max.extendLockTime("test", "alice", clock.instant().plusSeconds(100)).join();
        assertThat(extend, equalTo(true));

        LockDetail readExtended = max.readLock("test").thenApply(Optional::get).join();
        assertThat(readExtended.expiredAt(), greaterThan(acquired.expiredAt()));

        boolean release = max.releaseLock("test", "alice").join();
        assertThat(release, equalTo(true));

        Optional<LockDetail> readReleased = max.readLock("test").join();
        assertThat(readReleased.isPresent(), equalTo(false));

        LockDetail acquireReleased = max.acquireLock("test", "bob", clock.instant().plusSeconds(180)).join();
        assertThat(acquireReleased.owner(), equalTo("bob"));
        assertThat(acquireReleased.seqNo(), not(equalTo(acquired.seqNo())));
    }

    @Test
    public void failIfMigrateNotAvailableToOldVersion() {
        ydb.getSchemeClient().makeDirectories(path + "/Alerting/V1").join();
        Exception exception = (Exception) max.createSchemaForTests()
                .thenApply(ignore -> null)
                .exceptionally(e -> e)
                .join();

        assertThat("Schema migration not available because exist too old version", exception, notNullValue());
    }

    @Test
    public void failIfMigrateNotAvailableToNewVersion() {
        ydb.getSchemeClient().makeDirectories(path + "/Alerting/V100500").join();
        Exception exception = (Exception) max.createSchemaForTests()
                .thenApply(ignore -> null)
                .exceptionally(e -> e)
                .join();

        assertThat("Schema migration not available because exist too new version and backward migration not supported",
                exception, notNullValue());
    }

    @Test
    public void rmOldVersions() {
        min.createSchemaForTests().join();

        var scheme = ydb.getSchemeClient();

        scheme.makeDirectories(path + "/Alerting/V1").join().expect("success");
        max.createSchemaForTests().join();

        List<String> versions = scheme.listDirectory(path + "/Alerting")
                .thenApply(response -> response.expect("list dirs")
                    .getChildren()
                    .stream()
                    .map(SchemeOperationProtos.Entry::getName)
                    .collect(Collectors.toList()))
                .join();

        assertThat(versions, hasItem(YdbSchemaVersion.MAX.folderName()));
        assertThat(versions, hasItem(YdbSchemaVersion.MIN.folderName()));
        assertThat(versions, not(hasItem("V1")));
    }

    @Test
    public void duplicateCallToPrevVersion() {
        min.createSchemaForTests().join();
        max.createSchemaForTests().join();

        LockDetail acquired = max.acquireLock("test", "alice", clock.instant().plusSeconds(90)).join();
        assertThat(acquired.owner(), equalTo("alice"));

        LockDetail readNew = max.readLock("test").thenApply(Optional::get).join();
        LockDetail readOld = min.readLock("test").thenApply(Optional::get).join();

        assertThat(readNew, equalTo(acquired));
        assertThat(readOld, equalTo(acquired));
        assertThat(readOld, equalTo(readNew));

        boolean extend = max.extendLockTime("test", "alice", clock.instant().plusSeconds(100)).join();
        assertThat(extend, equalTo(true));

        LockDetail readExtendedNew = max.readLock("test").thenApply(Optional::get).join();
        LockDetail readExtendedOld = max.readLock("test").thenApply(Optional::get).join();
        assertThat(readExtendedNew, equalTo(readExtendedOld));
        assertThat(readExtendedOld.expiredAt(), greaterThan(acquired.expiredAt()));

        boolean release = max.releaseLock("test", "alice").join();
        assertThat(release, equalTo(true));

        Optional<LockDetail> readReleasedNew = max.readLock("test").join();
        Optional<LockDetail> readReleasedOld = min.readLock("test").join();
        assertThat(readReleasedNew.isPresent(), equalTo(false));
        assertThat(readReleasedOld.isPresent(), equalTo(false));

        LockDetail acquireTwo = max.acquireLock("test", "bob", clock.instant().plusSeconds(180)).join();
        assertThat(acquireTwo.owner(), equalTo("bob"));
        assertThat(acquireTwo.seqNo(), not(equalTo(acquired.seqNo())));
    }

    @Test
    public void changedInOldVisibleForNew() {
        min.createSchemaForTests().join();
        max.createSchemaForTests().join();

        LockDetail acquired = min.acquireLock("test", "alice", clock.instant().plusSeconds(30)).join();
        assertThat(acquired.owner(), equalTo("alice"));

        LockDetail read = max.readLock("test").thenApply(Optional::get).join();
        assertThat(read, equalTo(acquired));

        boolean extend = min.extendLockTime("test", "alice", clock.instant().plusSeconds(100)).join();
        assertThat(extend, equalTo(true));

        LockDetail readExtended = max.readLock("test").thenApply(Optional::get).join();
        assertThat(readExtended.expiredAt(), greaterThan(acquired.expiredAt()));

        boolean release = min.releaseLock("test", "alice").join();
        assertThat(release, equalTo(true));

        Optional<LockDetail> readReleased = max.readLock("test").join();
        assertThat(readReleased.isPresent(), equalTo(false));
    }

    @Test
    public void acquiredOnlyWhenAcquiredBothVersion() {
        min.createSchemaForTests().join();
        max.createSchemaForTests().join();

        LockDetail acquireOld = min.acquireLock("test", "alice", clock.instant().plusSeconds(30)).join();
        LockDetail acquiredNew = max.acquireLock("test", "bob", clock.instant().plusSeconds(30)).join();
        assertThat(acquireOld.owner(), equalTo("alice"));
        assertThat(acquireOld, equalTo(acquiredNew));

        LockDetail readOld = min.readLock("test").thenApply(Optional::get).join();
        LockDetail readNew = max.readLock("test").thenApply(Optional::get).join();
        assertThat(readOld, equalTo(readNew));
    }

    @Test
    public void extendOnlyWhenExtendedBothVersion() {
        assumeThat(YdbSchemaVersion.MIN, not(equalTo(YdbSchemaVersion.MAX)));

        min.createSchemaForTests().join();
        max.createSchemaForTests().join();

        LockDetail acquireOld = min.acquireLock("test", "alice", clock.instant().plusSeconds(60)).join();
        assertThat(acquireOld.owner(), equalTo("alice"));

        // node restarted with new version as a result now write into both tables
        boolean extend = max.extendLockTime("test", "alice", clock.instant().plusSeconds(100)).join();
        assertThat(extend, equalTo(false));

        Optional<LockDetail> read = max.readLock("test").join();
        assertThat(read.isPresent(), equalTo(false));
    }
}
