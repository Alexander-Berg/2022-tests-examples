package ru.yandex.yandexbus.experiments

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.SchedulerProvider
import ru.yandex.yandexbus.inhouse.common.session.TestAppStateNotifier
import rx.schedulers.Schedulers

class ExperimentsManagerTest : BaseTest() {

    private lateinit var schedulerProvider: SchedulerProvider
    private lateinit var appStateNotifier: TestAppStateNotifier

    private lateinit var testActivityLifecycleNotifier: TestActivityLifecycleNotifier

    private lateinit var experimentsStorage: ExperimentsStorage
    private lateinit var debugExperimentsStorage: ExperimentsStorage
    private lateinit var testMapkitExperimentsManager: TestUiExperimentsManager

    private lateinit var experimentsManager: ExperimentsManager

    private lateinit var exp1Query: ExperimentQuery
    private lateinit var exp1: ExperimentGroup
    private lateinit var newExp1: ExperimentGroup

    private lateinit var exp2Query: ExperimentQuery
    private lateinit var exp2: ExperimentGroup
    private lateinit var newExp2: ExperimentGroup

    @Before
    override fun setUp() {
        super.setUp()

        schedulerProvider = SchedulerProvider(io = Schedulers.immediate())
        appStateNotifier = TestAppStateNotifier()

        testActivityLifecycleNotifier = TestActivityLifecycleNotifier()

        experimentsStorage = TestExperimentsStorage()
        debugExperimentsStorage = TestExperimentsStorage()
        testMapkitExperimentsManager = TestUiExperimentsManager()

        experimentsManager = createExperimentsManager()

        exp1Query = ExperimentQuery("key1")
        exp1 = ExperimentGroup(exp1Query.name, "value")
        newExp1 = ExperimentGroup(exp1Query.name, "new value")

        exp2Query = ExperimentQuery("key2")
        exp2 = ExperimentGroup(exp2Query.name, "value")
        newExp2 = ExperimentGroup(exp2Query.name, "new value")

        experimentsManager.init()
        appStateNotifier.onAppGoesInit()
        appStateNotifier.onAppGoesForeground()
    }

    private fun createExperimentsManager(): ExperimentsManagerImpl {
        return ExperimentsManagerImpl(
            schedulerProvider,
            testMapkitExperimentsManager,
            experimentsStorage,
            appStateNotifier
        )
    }

    @Test
    fun `experiments not changed after publish`() {
        testMapkitExperimentsManager.addOrUpdateExperiment(exp1)
        val clientExperimentGroup = experimentsManager.current(exp1Query)

        testMapkitExperimentsManager.addOrUpdateExperiment(newExp1)
        assertEquals(clientExperimentGroup, experimentsManager.current(exp1Query))

        testMapkitExperimentsManager.removeExperiment(exp1Query.name)
        assertEquals(clientExperimentGroup, experimentsManager.current(exp1Query))
    }

    @Test
    fun `absent experiment will not be updated after publish`() {
        val absent = experimentsManager.current(exp1Query)
        testMapkitExperimentsManager.addOrUpdateExperiment(newExp1)

        assertNull(absent)
        assertEquals(absent, experimentsManager.current(exp1Query))
    }

    @Test
    fun `experiments updated before publish`() {
        testMapkitExperimentsManager.addOrUpdateExperiment(exp1)
        testMapkitExperimentsManager.addOrUpdateExperiment(newExp1)

        assertEquals(newExp1, experimentsManager.current(exp1Query))
    }

    @Test
    fun `failed precondition gives no experiment`() {
        val query = exp1Query.copy(precondition = { false })

        testMapkitExperimentsManager.addOrUpdateExperiment(exp1)

        assertNull(experimentsManager.current(query))
    }

    @Test
    fun `persistent experiment overridden before publish`() {
        experimentsStorage.write(exp1Query.name, exp1.value)

        val experimentsManager = createExperimentsManager()

        experimentsManager.init()
        appStateNotifier.onAppGoesForeground()

        testMapkitExperimentsManager.removeExperiment(exp1Query.name)

        assertNull(experimentsManager.current(exp1Query))
    }

    @Test
    fun `storage will not become junk`() {
        experimentsStorage.write(exp1Query.name, exp1.value)

        val experimentsManager = createExperimentsManager()

        experimentsManager.init()
        appStateNotifier.onAppGoesForeground()

        testMapkitExperimentsManager.removeExperiment(exp1Query.name)

        assertTrue(experimentsStorage.readAll().isEmpty())
    }

    @Test
    fun `whenLoaded notifies with fresh values`() {
        experimentsStorage.write(exp1Query.name, exp1.value)

        val experimentsManager = createExperimentsManager()

        experimentsManager.init()
        appStateNotifier.onAppGoesForeground()

        testMapkitExperimentsManager.addOrUpdateExperiment(newExp1)

        experimentsManager.whenLoaded(exp1Query).test().assertValue(newExp1)
    }

    @Test(expected = IllegalStateException::class)
    fun `uninitialized experiments manager fails`() {
        createExperimentsManager().current(exp1Query)
    }
}
