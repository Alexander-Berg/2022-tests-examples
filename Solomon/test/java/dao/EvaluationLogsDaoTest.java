package ru.yandex.solomon.alert.dao;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import ru.yandex.solomon.alert.dao.ProjectEvaluationLogsDao.FilterOpts;
import ru.yandex.solomon.alert.domain.AlertKey;
import ru.yandex.solomon.alert.rule.AlertEvalStateTestSupport;
import ru.yandex.solomon.alert.rule.EvaluationState;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.solomon.alert.rule.AlertEvalStateTestSupport.next;
import static ru.yandex.solomon.alert.rule.AlertEvalStateTestSupport.randomState;
import static ru.yandex.solomon.alert.util.CommonMatchers.reflectionEqualTo;

/**
 * @author Vladimir Gordiychuk
 */
public abstract class EvaluationLogsDaoTest {
    protected abstract EvaluationLogsDao getDao();

    private ProjectEvaluationLogsDao getDao(String projectId) {
        ProjectEvaluationLogsDao dao = getDao().forProject(projectId);
        dao.createSchemaForTests().join();
        return dao;
    }

    @Test
    public void findNothingByKey() {
        ProjectEvaluationLogsDao dao = getDao("junk");

        long ts = Instant.now().truncatedTo(ChronoUnit.MINUTES).toEpochMilli();
        assertEquals(Optional.empty(), dao.findOne(ts, "", "myRegularAlert").join());
        assertEquals(Optional.empty(), dao.findOne(ts, "myMultiAlert", "mySubAlert").join());
    }

    @Test
    public void saveAndFindOneRegularAlert() {
        ProjectEvaluationLogsDao dao = getDao("junk");

        AlertKey key = new AlertKey("junk", "", "myRegularAlert");
        EvaluationState state = randomState(key, 42);
        dao.save(state).join();

        Optional<EvaluationState> opt = dao.findOne(state.getLatestEvalTruncated().toEpochMilli(),
                key.getParentId(),
                key.getAlertId())
                .join();

        assertNotEquals(Optional.empty(), opt);
        assertThat(opt.get(), reflectionEqualTo(state));
    }

    @Test
    public void saveAndFindOneSubAlert() {
        ProjectEvaluationLogsDao dao = getDao("junk");

        AlertKey key = new AlertKey("junk", "myMultiAlert", "mySubAlert");
        EvaluationState state = randomState(key, 42);
        dao.save(state).join();

        Optional<EvaluationState> opt = dao.findOne(
                state.getLatestEvalTruncated().toEpochMilli(),
                key.getParentId(),
                key.getAlertId())
                .join();

        assertNotEquals(Optional.empty(), opt);
        assertThat(opt.get(), reflectionEqualTo(state));
    }

    @Test
    public void findMany() {
        ProjectEvaluationLogsDao dao = getDao("junk");

        AlertKey key = new AlertKey("junk", "myMultiAlert", "mySubAlert");

        List<EvaluationState> seq = genEvaluationSeq(key, 3000);
        dao.save(seq).join();
        assertFilterEqual(seq, new FilterOpts(), dao);
    }

    @Test
    public void findManyLimiting() {
        ProjectEvaluationLogsDao dao = getDao("junk");

        for (int index = 0; index < 10; index++) {
            AlertKey key = new AlertKey("junk", "", "myId" + index);
            List<EvaluationState> seq = genEvaluationSeq(key, 100);
            dao.save(seq).join();
        }

        AlertKey key = new AlertKey("junk", "", "myId");
        List<EvaluationState> seq = genEvaluationSeq(key, 100);
        dao.save(seq).join();

        FilterOpts filter = new FilterOpts()
                .setParentId(key.getParentId())
                .setAlertId(key.getAlertId())
                .setLimit(2);

        assertFilterEqual(seq.subList(0, 2), filter, dao);
    }

    @Test
    public void findManyFilterByFromTs() {
        ProjectEvaluationLogsDao dao = getDao("junk");
        AlertKey key = new AlertKey("junk", "", "myId");

        List<EvaluationState> seq = genEvaluationSeq(key, 3000);
        dao.save(seq).join();

        List<EvaluationState> expected = seq.subList(2990, 3000);

        FilterOpts filter = new FilterOpts()
                .setFromMillis(expected.get(0)
                        .getLatestEvalTruncated()
                        .toEpochMilli());

        assertFilterEqual(expected, filter, dao);
    }

    @Test
    public void findManyFilterByToTs() {
        ProjectEvaluationLogsDao dao = getDao("junk");
        AlertKey key = new AlertKey("junk", "", "myId");

        List<EvaluationState> seq = genEvaluationSeq(key, 3_000);
        dao.save(seq).join();

        List<EvaluationState> expected = seq.subList(0, 10);

        FilterOpts filter = new FilterOpts()
                .setToMillis(expected.get(9)
                        .getLatestEvalTruncated()
                        .toEpochMilli() + 1);

        assertFilterEqual(expected, filter, dao);
    }

    @Test
    public void filterByTsRange() {
        ProjectEvaluationLogsDao dao = getDao("junk");
        AlertKey key = new AlertKey("junk", "", "myId");

        List<EvaluationState> seq = genEvaluationSeq(key, 3_000);
        dao.save(seq).join();

        List<EvaluationState> expected = seq.subList(1500, 1510);

        FilterOpts filter = new FilterOpts()
                .setFromMillis(expected.get(0).getLatestEvalTruncated().toEpochMilli())
                .setToMillis(expected.get(9).getLatestEvalTruncated().toEpochMilli() + 1);

        assertFilterEqual(expected, filter, dao);
    }

    @Test
    public void filterByAlertId() {
        ProjectEvaluationLogsDao dao = getDao("junk");

        for (int index = 0; index < 10; index++) {
            AlertKey key = new AlertKey("junk", "", "myId" + index);
            List<EvaluationState> seq = genEvaluationSeq(key, 100);
            dao.save(seq).join();
        }

        AlertKey key = new AlertKey("junk", "", "myId");
        List<EvaluationState> seq = genEvaluationSeq(key, 10);
        dao.save(seq).join();

        FilterOpts filter = new FilterOpts()
                .setParentId("")
                .setAlertId(key.getAlertId());

        assertFilterEqual(seq, filter, dao);
    }

    @Test
    public void filterBySubAlert() {
        ProjectEvaluationLogsDao dao = getDao("junk");

        for (int index = 0; index < 10; index++) {
            AlertKey key = new AlertKey("junk", "", "myId" + index);
            List<EvaluationState> seq = genEvaluationSeq(key, 100);
            dao.save(seq).join();
        }

        AlertKey key = new AlertKey("junk", "myMultiAlert", "myId");
        List<EvaluationState> seq = genEvaluationSeq(key, 10);
        dao.save(seq).join();

        FilterOpts filter = new FilterOpts()
                .setParentId(key.getParentId())
                .setAlertId(key.getAlertId());

        assertFilterEqual(seq, filter, dao);
    }

    @Test
    public void filterRangeAndAlert() {
        ProjectEvaluationLogsDao dao = getDao("junk");

        for (int index = 0; index < 10; index++) {
            AlertKey key = new AlertKey("junk", "", "myId" + index);
            List<EvaluationState> seq = genEvaluationSeq(key, 100);
            dao.save(seq).join();
        }

        AlertKey key = new AlertKey("junk", "", "myId");
        List<EvaluationState> seq = genEvaluationSeq(key, 10);
        dao.save(seq).join();

        List<EvaluationState> expected = seq.subList(4, 8);

        FilterOpts filter = new FilterOpts()
                .setFromMillis(expected.get(0).getLatestEvalTruncated().toEpochMilli())
                .setToMillis(expected.get(3).getLatestEvalTruncated().toEpochMilli() + 1)
                .setParentId("")
                .setAlertId(key.getAlertId());

        // truncated time base on eval interval
        assertFilterEqual(expected, filter, dao);
    }

    @Test
    public void filterRangeAndSubAlert() {
        ProjectEvaluationLogsDao dao = getDao("junk");

        for (int index = 0; index < 10; index++) {
            AlertKey key = new AlertKey("junk", "", "myId" + index);
            List<EvaluationState> seq = genEvaluationSeq(key, 100);
            dao.save(seq).join();
        }

        AlertKey key = new AlertKey("junk", "myMultiAlert", "myId");
        List<EvaluationState> seq = genEvaluationSeq(key, 10);
        dao.save(seq).join();

        List<EvaluationState> expected = seq.subList(4, 8);

        FilterOpts filter = new FilterOpts()
                .setFromMillis(expected.get(0).getLatestEvalTruncated().toEpochMilli())
                .setToMillis(expected.get(3).getLatestEvalTruncated().toEpochMilli() + 1)
                .setParentId(key.getParentId());

        assertFilterEqual(expected, filter, dao);
    }

    @Test
    public void delete() {
        ProjectEvaluationLogsDao dao = getDao("junk");
        AlertKey key = new AlertKey("junk", "myMultiAlert", "myId");
        List<EvaluationState> seq = genEvaluationSeq(key, 3000);
        dao.save(seq).join();

        List<EvaluationState> expected = seq.subList(2990, 3000);

        FilterOpts filter = new FilterOpts()
                .setToMillis(expected.get(9).getLatestEvalTruncated().toEpochMilli());
        dao.delete(filter).join();

    }

    private void assertFilterEqual(List<EvaluationState> expected, FilterOpts filter, ProjectEvaluationLogsDao dao) {
        List<EvaluationState> result = dao.findMany(filter).join();

        assertEquals(expected.size(), result.size());
        for (int index = 0; index < expected.size(); index++) {
            assertThat(result.get(index), reflectionEqualTo(expected.get(index)));
        }
    }

    private List<EvaluationState> genEvaluationSeq(AlertKey key, int size) {
        List<EvaluationState> result = new ArrayList<>(size);
        EvaluationState last = randomState(key, 42);
        for (int index = 0; index < size; index++) {
            result.add(last);
            last = next(last, AlertEvalStateTestSupport.randomEvalStatus());
        }
        return result;
    }
}
