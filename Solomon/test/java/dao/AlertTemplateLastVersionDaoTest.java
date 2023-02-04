package ru.yandex.solomon.alert.dao;

import java.util.Optional;
import java.util.concurrent.CompletionException;

import org.junit.Test;

import ru.yandex.solomon.alert.template.domain.AlertTemplateLastVersion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexey Trushkin
 */
public abstract class AlertTemplateLastVersionDaoTest {

    private static final String ALERT_ID = "alert_id";
    private static final String ALERT_VERSION_ID = "alert_version_id";
    private static final String SERVICE_PROVIDER = "service provider id";
    private static final String SERVICE_PROVIDER2 = "service provider id2";
    private static final String SERVICE_PROVIDER3 = "service provider id3";
    private static final String NAME = "name";
    private static final String NAME2 = "name2";
    private static final String TASK_ID = "task";
    private static final String TASK_ID2 = "task2";

    protected abstract AlertTemplateLastVersionDao getDao();

    @Test
    public void create() {
        assertTrue(createSync(new AlertTemplateLastVersion(ALERT_ID, ALERT_VERSION_ID, SERVICE_PROVIDER, NAME, 0, TASK_ID)));
    }

    @Test(expected = CompletionException.class)
    public void update_failed() {
        assertTrue(createSync(new AlertTemplateLastVersion(ALERT_ID, ALERT_VERSION_ID, SERVICE_PROVIDER, NAME, 0, TASK_ID)));
        updateTaskSync(ALERT_ID, 1, TASK_ID2);
    }

    @Test
    public void updateTaskId() {
        assertTrue(createSync(new AlertTemplateLastVersion(ALERT_ID, ALERT_VERSION_ID, SERVICE_PROVIDER, NAME, 0, TASK_ID)));
        updateTaskSync(ALERT_ID, 0, TASK_ID2);

        var versionOptional = findSync(ALERT_ID);
        assertTrue(versionOptional.isPresent());
        assertEquals(new AlertTemplateLastVersion(ALERT_ID, ALERT_VERSION_ID, SERVICE_PROVIDER, NAME, 1, TASK_ID2), versionOptional.get());
    }

    @Test
    public void find() {
        assertTrue(createSync(new AlertTemplateLastVersion(ALERT_ID, ALERT_VERSION_ID, SERVICE_PROVIDER, NAME, 0, TASK_ID)));
        assertTrue(createSync(new AlertTemplateLastVersion(ALERT_ID + 2, ALERT_VERSION_ID, SERVICE_PROVIDER2, NAME2, 0, TASK_ID)));

        assertTrue(findSync("someId").isEmpty());

        var versionOptional = findSync(ALERT_ID);
        assertTrue(versionOptional.isPresent());
        assertEquals(new AlertTemplateLastVersion(ALERT_ID, ALERT_VERSION_ID, SERVICE_PROVIDER, NAME, 0, TASK_ID), versionOptional.get());
    }

    @Test
    public void find_criteria() {
        assertTrue(createSync(new AlertTemplateLastVersion(ALERT_ID, ALERT_VERSION_ID, SERVICE_PROVIDER, NAME, 0, TASK_ID)));
        assertTrue(createSync(new AlertTemplateLastVersion(ALERT_ID + 2, ALERT_VERSION_ID, SERVICE_PROVIDER2, NAME2, 0, TASK_ID)));
        assertTrue(createSync(new AlertTemplateLastVersion(ALERT_ID + 3, ALERT_VERSION_ID, SERVICE_PROVIDER3, NAME2 + "some", 0, TASK_ID)));

        var result = getDao().find("", "", 100, "").join();
        assertEquals(3, result.getItems().size());

        result = getDao().find(SERVICE_PROVIDER, "", 100, "").join();
        assertEquals(1, result.getItems().size());
        assertEquals(new AlertTemplateLastVersion(ALERT_ID, ALERT_VERSION_ID, SERVICE_PROVIDER, NAME, 0, TASK_ID), result.getItems().get(0));

        result = getDao().find("", NAME2, 100, "").join();
        assertEquals(2, result.getItems().size());
        assertTrue(result.getItems().contains(new AlertTemplateLastVersion(ALERT_ID + 2, ALERT_VERSION_ID, SERVICE_PROVIDER2, NAME2, 0, TASK_ID)));
        assertTrue(result.getItems().contains(new AlertTemplateLastVersion(ALERT_ID + 3, ALERT_VERSION_ID, SERVICE_PROVIDER3, NAME2 + "some", 0, TASK_ID)));

        result = getDao().find(SERVICE_PROVIDER3, NAME2, 100, "").join();
        assertEquals(1, result.getItems().size());
        assertTrue(result.getItems().contains(new AlertTemplateLastVersion(ALERT_ID + 3, ALERT_VERSION_ID, SERVICE_PROVIDER3, NAME2 + "some", 0, TASK_ID)));
    }

    @Test
    public void delete() {
        assertFalse(deleteSync(ALERT_ID, 0));
        assertTrue(createSync(new AlertTemplateLastVersion(ALERT_ID, ALERT_VERSION_ID, SERVICE_PROVIDER, NAME, 0, TASK_ID)));
        assertTrue(deleteSync(ALERT_ID, 0));
    }

    @Test
    public void delete_failed() {
        assertTrue(createSync(new AlertTemplateLastVersion(ALERT_ID, ALERT_VERSION_ID, SERVICE_PROVIDER, NAME, 0, TASK_ID)));
        assertFalse(deleteSync(ALERT_ID, 1));
    }

    protected boolean createSync(AlertTemplateLastVersion version) {
        return getDao().create(version).join();
    }

    protected boolean deleteSync(String id, int version) {
        return getDao().delete(id, version).join();
    }

    protected boolean updateTaskSync(String templateId, int version, String taskId) {
        return getDao().updateDeployTask(templateId, version, taskId).join();
    }

    protected Optional<AlertTemplateLastVersion> findSync(String id) {
        return getDao().findById(id).join();
    }

}
