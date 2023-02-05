package ru.yandex.yandexbus.experiments

import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.SchedulerProvider
import ru.yandex.yandexbus.inhouse.common.session.TestAppStateNotifier
import rx.schedulers.Schedulers

class DebugExperimentManagerTest : BaseTest() {

    private lateinit var schedulerProvider: SchedulerProvider
    private lateinit var appStateNotifier: TestAppStateNotifier

    private lateinit var testActivityLifecycleNotifier: TestActivityLifecycleNotifier

    private lateinit var testMapkitExperimentsManager: TestUiExperimentsManager
    private lateinit var experimentsStorage: ExperimentsStorage
    private lateinit var originalExperimentsManager: ExperimentsManager
    private lateinit var experimentsManager: DebugExperimentsManager

    private lateinit var expQuery: ExperimentQuery
    private lateinit var exp: ExperimentGroup
    private lateinit var newExp: ExperimentGroup

    @Before
    override fun setUp() {
        super.setUp()

        schedulerProvider = SchedulerProvider(io = Schedulers.immediate())
        appStateNotifier = TestAppStateNotifier()

        testActivityLifecycleNotifier = TestActivityLifecycleNotifier()

        testMapkitExperimentsManager = TestUiExperimentsManager()

        originalExperimentsManager = ExperimentsManagerImpl(
            schedulerProvider,
            testMapkitExperimentsManager,
            TestExperimentsStorage(),
            appStateNotifier
        )

        experimentsStorage = TestExperimentsStorage()
        this.experimentsManager = DebugExperimentsManager(originalExperimentsManager, experimentsStorage)

        expQuery = ExperimentQuery("key1")
        exp = ExperimentGroup(expQuery.name, "value")
        newExp = ExperimentGroup(expQuery.name, "new value")

        experimentsManager.init()
        appStateNotifier.onAppGoesInit()
        appStateNotifier.onAppGoesForeground()
    }

    @Test
    fun `forced experiment takes priority`() {
        testMapkitExperimentsManager.addOrUpdateExperiment(exp)
        Assert.assertFalse(experimentsManager.isForced(exp))

        val forcedExp1 = exp.copy(value = "forced ${exp.value}")
        experimentsManager.force(forcedExp1)
        assertTrue(experimentsManager.isForced(forcedExp1))

        assertEquals(forcedExp1, experimentsManager.current(expQuery))
    }

    @Test
    fun `forced experiments still present after experiment life ends`() {
        experimentsManager.force(exp)
        testMapkitExperimentsManager.removeExperiment(expQuery.name)

        assertEquals(exp, experimentsManager.current(expQuery))
    }

    @Test
    fun `unforce hides actual group`() {
        testMapkitExperimentsManager.addOrUpdateExperiment(exp)

        experimentsManager.forceNoGroup(exp.name)

        assertNull(experimentsManager.current(expQuery))
    }

    @Test
    fun `unforce will reveal actual value after publish`() {
        experimentsManager.force(exp)
        testMapkitExperimentsManager.addOrUpdateExperiment(newExp)

        assertEquals(exp, experimentsManager.current(expQuery))

        experimentsManager.unforce(exp)

        assertEquals(newExp, experimentsManager.current(expQuery))
    }

    @Test
    fun `unforce will reveal actual value before publish`() {
        experimentsManager.force(exp)
        testMapkitExperimentsManager.addOrUpdateExperiment(newExp)
        experimentsManager.unforce(exp)

        assertEquals(newExp, experimentsManager.current(expQuery))
    }

    @Test
    fun `unforce of another group skipped`() {
        experimentsManager.force(exp)
        experimentsManager.unforce(exp.copy(value = "${exp.value}copy"))

        assertTrue(experimentsManager.isForced(exp))
    }

    @Test
    fun `forced experiment does not override received`() {
        testMapkitExperimentsManager.addOrUpdateExperiment(exp)
        experimentsManager.force(newExp)

        // see BUS-5074 - we still should report original
        experimentsManager.receivedExperiments()
            .test()
            .assertValues(setOf(exp))
    }

    @Test
    fun `forced experiment is not added to received`() {
        testMapkitExperimentsManager.addOrUpdateExperiment(exp)

        experimentsManager.current(expQuery)

        val forced = exp.copy(name = "copy${exp.name}")

        experimentsManager.force(forced)

        // see BUS-5074 - we still should report original
        experimentsManager.receivedExperiments()
            .test()
            .assertValues(setOf(exp))
    }

    @Test
    fun `no force still reports received`() {
        testMapkitExperimentsManager.addOrUpdateExperiment(exp)

        originalExperimentsManager.receivedExperiments()
            .first()
            .test()
            .assertValue(setOf(exp))

        experimentsManager.receivedExperiments()
            .first()
            .test()
            .assertValue(setOf(exp))
    }
}
