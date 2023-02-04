package com.yandex.mobile.verticalcore.migration;

import com.yandex.mobile.verticalcore.BaseTest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

/**
 * TODO: Add destription
 *
 * @author ironbcc on 06.05.2015.
 */
public class MigrationManagerTest extends BaseTest {
    @Mock private Step stepTo;
    @Mock private Step stepTo2;
    private SerialMigrationManager manager;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        manager = new SerialMigrationManager();
    }

    @Test
    public void singleStepForSingleVersionMigration() {
        manager.register(0, stepTo);

        when(stepTo.migrate(0, 1)).thenReturn(true);

        manager.migrate(0, 1);

        verify(stepTo, times(1)).migrate(0, 1);
    }

    @Test
    public void singleStepForSingleVersionMigration2() {
        manager.register(0, stepTo);

        when(stepTo.migrate(0, 1)).thenReturn(true);
        when(stepTo.migrate(1, 2)).thenReturn(true);

        manager.migrate(0, 2);

        verify(stepTo, times(1)).migrate(0, 1);
        verify(stepTo, times(0)).migrate(1, 2);
    }

    @Test
    public void singleStepForSingleVersionMigrationFail() {
        manager.register(0, stepTo);

        when(stepTo.migrate(0, 1)).thenReturn(false);

        try {
            manager.migrate(0, 1);
            Assert.fail("Migration should fail");
        } catch (MigrationFailException e) {
            Assert.assertEquals(0, e.from);
            Assert.assertEquals(1, e.to);
        }

        verify(stepTo, times(1)).migrate(0, 1);
    }

    @Test
    public void singleStepForMultipleVersionsMigrationIgnored() {
        manager.register(0, stepTo);
        manager.register(1, stepTo);

        when(stepTo.migrate(0, 1)).thenReturn(true);
        when(stepTo.migrate(1, 2)).thenReturn(true);

        manager.migrate(2, 3);

        verify(stepTo, times(0)).migrate(0, 1);
        verify(stepTo, times(0)).migrate(1, 2);
        verify(stepTo, times(0)).migrate(2, 3);
    }

    @Test
    public void singleStepForMultipleVersionsMigration() {
        manager.register(0, stepTo);
        manager.register(1, stepTo);

        when(stepTo.migrate(0, 1)).thenReturn(true);
        when(stepTo.migrate(1, 2)).thenReturn(true);

        manager.migrate(0, 3);

        verify(stepTo, times(1)).migrate(0, 1);
        verify(stepTo, times(1)).migrate(1, 2);
        verify(stepTo, times(0)).migrate(2, 3);
    }

    @Test
    public void multipleStepsForSingleVersionMigration() {
        manager.register(0, stepTo);
        manager.register(2, stepTo);
        manager.register(2, stepTo2);

        when(stepTo.migrate(2, 3)).thenReturn(true);
        when(stepTo2.migrate(2, 3)).thenReturn(true);

        manager.migrate(2, 3);

        verify(stepTo, times(1)).migrate(2, 3);
        verify(stepTo2, times(1)).migrate(2, 3);
    }

    @Test
    public void singleStepForEveryUpdate() {
        manager.registerEveryUpdate(stepTo);

        when(stepTo.migrate(anyInt(), anyInt())).thenReturn(true);

        manager.migrate(0, 2);
        manager.migrate(2, 3);

        verify(stepTo, times(1)).migrate(0, 2);
        verify(stepTo, times(1)).migrate(2, 3);
    }

    @Test
    public void singleStepWithCondition() {
        manager.registerDynamic((from, to) -> from == 0 && to == 2, stepTo);

        when(stepTo.migrate(anyInt(), anyInt())).thenReturn(true);

        manager.migrate(0, 1);
        manager.migrate(0, 2);

        verify(stepTo, times(0)).migrate(0, 1);
        verify(stepTo, times(1)).migrate(0, 2);
    }
}
