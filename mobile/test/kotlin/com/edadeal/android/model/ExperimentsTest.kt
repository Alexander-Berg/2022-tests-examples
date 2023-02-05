package com.edadeal.android.model

import com.edadeal.android.data.endpoints.EndpointsRepository
import com.edadeal.android.dto.Experiment
import com.edadeal.android.dto.Experiment.Companion.EDADEAL_API_ADS
import com.edadeal.android.dto.Experiment.Companion.EDADEAL_API_CALIBRATOR
import com.edadeal.android.dto.Experiment.Companion.EDADEAL_API_CB
import com.edadeal.android.dto.Experiment.Companion.EDADEAL_API_CONTENT
import com.edadeal.android.dto.Experiment.Companion.EDADEAL_API_REC
import com.edadeal.android.dto.Experiment.Companion.EDADEAL_API_USR
import com.edadeal.android.dto.Experiment.Companion.EDADEAL_API_WALLET
import com.edadeal.android.dto.Experiment.Companion.EDADEAL_UI
import com.edadeal.android.model.api.endpoints.EndpointName
import com.edadeal.android.model.api.endpoints.Endpoints
import com.edadeal.android.model.api.endpoints.VitalEndpointsDelegate
import com.edadeal.android.model.auth.passport.PassportContext
import com.edadeal.android.util.DefaultUrls
import com.edadeal.android.util.adapter
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okhttp3.HttpUrl
import java.util.Random
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("SpellCheckingInspection")
class ExperimentsTest {

    private val defaultUrls = DefaultUrls.Edadeal
    private val passportContext = PassportContext.PRODUCTION
    private val defaultEndpoints = Endpoints.create(defaultUrls, passportContext)
    private val endpointsRepository: EndpointsRepository = mock()
    private val targetExperiment = mapOf("A" to setOf("1", "2", "3"))
    private val dummyExperiments = mapOf("B" to setOf("4", "5", "6"))
    private val jsonAdapter: JsonAdapter<List<Experiment>> = run {
        val adapterType = Types.newParameterizedType(List::class.java, Experiment::class.java)
        Moshi.Builder().build().adapter(adapterType)
    }

    private lateinit var experiments: Experiments
    private lateinit var storage: Experiments.Storage
    private lateinit var experimentsUrlToSourceMapper: ExperimentsUrlToSourceMapper

    @BeforeTest
    fun setup() {
        storage = mock()
        val vitalEndpointsDelegate = VitalEndpointsDelegate(mock(), mock(), PassportContext.PRODUCTION, mock(), mock())
        experimentsUrlToSourceMapper = ExperimentsUrlToSourceMapper(mock(), endpointsRepository, vitalEndpointsDelegate)
    }

    @Test
    fun `getParameters should return empty experiments when going to unknown host`() {
        val url = HttpUrl.parse("https://usr.unknown.ru/auth/v1/device?a=111")!!
        prepareExperiments(getDifferentExperiments())
        assertEquals(experiments.getParameters(url), emptyMap())
    }

    @Test
    fun `getParameters should return empty experiments when url setted incorrect`() {
        val url = HttpUrl.parse("https://usr.edadeal.ru/auth/v1/device?a=111")!!
        val endpoints = decorateEndpoints(Endpoints.USR_URL_KEY to "Hey! I'm url, just trust me :3")
        prepareExperiments(getDifferentExperiments(), endpoints)
        assertEquals(emptyMap(), experiments.getParameters(url))
    }

    @Test
    fun `getParameters should return USR experiments when go to usr host`() {
        val url = HttpUrl.parse("https://usr.edadeal.ru/auth/v1/device?a=111")!!
        prepareExperiments(getDifferentExperiments { it[EDADEAL_API_USR] = targetExperiment })
        assertEquals(experiments.getParameters(url), targetExperiment)
    }

    @Test
    fun `getParameters should return empty when go to abt segment of usr host`() {
        val url = HttpUrl.parse("https://config.edadeal.ru/auth/v1/abt?a=111")!!
        prepareExperiments(getDifferentExperiments { it[EDADEAL_API_CALIBRATOR] = targetExperiment })
        assertEquals(experiments.getParameters(url), emptyMap())
    }

    @Test
    fun `getParameters should return WALLET experiments when go COUPONS URL`() {
        val url = HttpUrl.parse("${defaultEndpoints.couponsUrl}?a=111")!!
        prepareExperiments(getDifferentExperiments { it[EDADEAL_API_WALLET] = targetExperiment })
        assertEquals(experiments.getParameters(url), targetExperiment)
    }

    @Test
    fun `getParameters should return WALLET experiments when go COUPONS URL with CONTENT experiment`() {
        val url = HttpUrl.parse("${defaultEndpoints.couponsUrl}?a=111")!!
        prepareExperiments(getDifferentExperiments {
            it[EDADEAL_API_WALLET] = targetExperiment
            it[EDADEAL_API_WALLET] = targetExperiment
            it[EDADEAL_API_CONTENT] = dummyExperiments
        })
        assertEquals(experiments.getParameters(url), targetExperiment)
    }

    @Test
    fun `getParameters should return all WALLET experiments when go WALLET URL`() {
        val url = HttpUrl.parse("${defaultEndpoints.couponsUrl}?a=111")!!
        prepareExperiments(listOf(
            getExperiment("1", mapOf(EDADEAL_API_WALLET to targetExperiment)),
            getExperiment("2", mapOf(EDADEAL_API_WALLET to dummyExperiments))
        ))
        assertEquals(experiments.getParameters(url), dummyExperiments + targetExperiment)
    }

    @Test
    fun `getParameters should return CASHBACK experiments when go CASHBACK_URL`() {
        val url = HttpUrl.parse("https://cb.edadeal.ru/v2/a?a=111")!!
        prepareExperiments(getDifferentExperiments { it[EDADEAL_API_CB] = targetExperiment })
        assertEquals(experiments.getParameters(url), targetExperiment)
    }

    @Test
    fun `getParameters should return ADS experiments when go ADS_URL`() {
        val url = HttpUrl.parse("${defaultUrls.adsUrl}a?a=111")!!
        prepareExperiments(getDifferentExperiments { it[EDADEAL_API_ADS] = targetExperiment })
        assertEquals(experiments.getParameters(url), targetExperiment)
    }

    @Test
    fun `getParameters should return CONTENT experiments when go CONTENT_URL`() {
        val url = HttpUrl.parse("${defaultUrls.contentUrl}a?a=111")!!
        prepareExperiments(getDifferentExperiments { it[EDADEAL_API_CONTENT] = targetExperiment })
        assertEquals(experiments.getParameters(url), targetExperiment)
    }

    @Test
    fun `getParameters should return empty experiments when go CONTENT_URL with WALLET experiment`() {
        val url = HttpUrl.parse("${defaultUrls.contentUrl}a?a=111")!!
        prepareExperiments(listOf(getExperiment("1", mapOf(EDADEAL_API_WALLET to targetExperiment))))
        assertEquals(experiments.getParameters(url), emptyMap())
    }

    @Test
    fun `getParameters should return USR experiments when go USR_URL with url replaces`() {
        val usrUrl = "https://otheruser.ru/v3/"
        val url = HttpUrl.get("${usrUrl}dropdatabase/and/run?speed=300")
        val endpoints = decorateEndpoints(Endpoints.USR_URL_KEY to usrUrl)
        prepareExperiments(getDifferentExperiments { it[EDADEAL_API_USR] = targetExperiment }, endpoints)
        assertEquals(experiments.getParameters(url), targetExperiment)
    }

    @Test
    fun `getParameters should return distinct experiments`() {
        val usrUrl = "https://otheruser.ru/v3/"
        val url = HttpUrl.get("${usrUrl}dropdatabase/and/run?speed=300")
        val endpoints = decorateEndpoints(Endpoints.USR_URL_KEY to usrUrl)
        prepareExperiments(getDifferentExperiments(5) { it[EDADEAL_API_USR] = targetExperiment }, endpoints)
        assertEquals(experiments.getParameters(url), targetExperiment)
    }

    @Test
    fun `getPlaceholderExperiments should return experiments in correct format`() {
        val items = listOf(
            getExperiment(
                "1",
                mapOf(
                    EDADEAL_API_ADS to mapOf("A" to setOf("A1", "A2"), "B" to setOf("B1")),
                    EDADEAL_API_CALIBRATOR to mapOf("C" to setOf("C1"))
                )
            ),
            getExperiment(
                "2",
                mapOf(
                    EDADEAL_UI to mapOf("D" to emptySet())
                )
            ),
            getExperiment(
                "2",
                mapOf(
                    EDADEAL_UI to mapOf("E" to emptySet())
                ),
                releasedWithCalibrator = true
            )
        )
        prepareExperiments(items)

        val allExperiments = "1(EDADEAL_API_ADS(A:[A1,A2],B:B1),EDADEAL_API_CALIBRATOR(C:C1)),2(EDADEAL_UI(D))"
        assertEquals(allExperiments, experiments.getPlaceholderExperiments())
    }

    @Test
    fun `getPlaceholderExperiments should return new experiments after update`() {
        val initialExperiment = getExperiment(
            "1",
            mapOf(
                EDADEAL_API_ADS to mapOf("A" to setOf("A1"))
            )
        )
        val newExperiment = getExperiment(
            "2",
            mapOf(
                EDADEAL_API_CALIBRATOR to mapOf("B" to setOf("B1"))
            )
        )
        val storage = MockStorage(listOf(initialExperiment))
        val experiments = Experiments(storage, experimentsUrlToSourceMapper)

        experiments.update()
        assertEquals("1(EDADEAL_API_ADS(A:A1))", experiments.getPlaceholderExperiments())

        storage.experiments = listOf(newExperiment)
        experiments.update()
        assertEquals("2(EDADEAL_API_CALIBRATOR(B:B1))", experiments.getPlaceholderExperiments())
    }

    @Test
    fun `startNewIfAvailable should start received experiments with correct params`() {
        val items = listOf(
            Experiment(
                ExperimentID = "1",
                Bucket = "19",
                CONTEXT = Experiment.Context(
                    EDADEAL = Experiment.ContextType(
                        source = mapOf(
                            EDADEAL_API_CONTENT to mapOf("A" to setOf("AA")),
                            EDADEAL_API_ADS to mapOf("B" to setOf("BB"))
                        )
                    )
                )
            ),
            Experiment(
                ExperimentID = "2",
                Bucket = "95",
                CONTEXT = Experiment.Context(
                    EDADEAL = Experiment.ContextType(
                        source = mapOf(
                            EDADEAL_API_USR to mapOf("C" to setOf("CC")),
                            EDADEAL_API_ADS to mapOf("B" to setOf("CC"))
                        )
                    )
                )
            )
        )
        val storage = MockStorage(items)
        whenever(endpointsRepository.endpoints).thenReturn(defaultEndpoints)
        val experiments = Experiments(storage, experimentsUrlToSourceMapper)
        experiments.update()
        experiments.startNewIfAvailable()

        assertEquals(setOf("1", "2"), experiments.metricExperimentIds)
        assertEquals("1,19;2,95", experiments.metricExperimentBuckets)
        assertEquals(
            mapOf("A" to setOf("AA")),
            experiments.getParameters(HttpUrl.parse("${defaultUrls.contentUrl}123")!!)
        )
        assertEquals(
            mapOf("B" to setOf("BB", "CC")),
            experiments.getParameters(HttpUrl.parse("${defaultUrls.adsUrl}123")!!)
        )
    }

    @Test
    fun `getStartSessionExperiments should return experiments with ReleasedWithCalibrator = true`() {
        val releasedWithoutCalibratorExp = getExperiment(
            "1",
            mapOf(
                EDADEAL_API_ADS to mapOf("A" to setOf("A1", "A2"), "B" to setOf("B1"))
            )
        )
        val releasedWithCalibratorExp = getExperiment(
            "2",
            mapOf(
                EDADEAL_UI to mapOf("B" to setOf("B1")),
                EDADEAL_API_CALIBRATOR to mapOf("C" to setOf("C1"))
            ),
            releasedWithCalibrator = true
        )
        prepareExperiments(listOf(releasedWithoutCalibratorExp, releasedWithCalibratorExp))
        val moshiAdapter = Moshi.Builder().build().adapter<Set<Any>>()
        val expectedExpString = "[{\"Bucket\":\"\",\"CONTEXT\":{\"EDADEAL\":{\"source\":{\"EDADEAL_UI\":{\"B\":[\"B1\"]},\"EDADEAL_API_CALIBRATOR\":{\"C\":[\"C1\"]}}}},\"ExperimentID\":\"2\",\"ReleasedWithCalibrator\":true}]"
        val expected = moshiAdapter.fromJson(expectedExpString)
        assertEquals(expected, moshiAdapter.fromJson(experiments.getStartSessionExperiments()))
    }

    private fun prepareExperiments(experimentItems: List<Experiment>, endpoints: Endpoints = defaultEndpoints) {
        whenever(storage.loadExperimentsJson()).thenReturn(jsonAdapter.toJson(experimentItems))
        whenever(storage.loadExperiments()).thenReturn(experimentItems)
        whenever(endpointsRepository.endpoints).thenReturn(endpoints)

        experiments = Experiments(storage, experimentsUrlToSourceMapper)
        experiments.update()
    }

    private fun getRandomSource() = mapOf(
        EDADEAL_API_USR to getRandomExperiment(),
        EDADEAL_API_WALLET to getRandomExperiment(),
        EDADEAL_API_CONTENT to getRandomExperiment(),
        EDADEAL_UI to getRandomExperiment(),
        EDADEAL_API_CB to getRandomExperiment(),
        EDADEAL_API_REC to getRandomExperiment()
    )

    private fun getExperiment(
        id: String,
        source: Map<String, Map<String, Set<String>>>,
        releasedWithCalibrator: Boolean = false
    ): Experiment {
        return Experiment(
            ExperimentID = id,
            CONTEXT = Experiment.Context(EDADEAL = Experiment.ContextType(source = source)),
            ReleasedWithCalibrator = releasedWithCalibrator
        )
    }

    private fun getDifferentExperiments(
        n: Int = 1,
        transform: ((MutableMap<String, Map<String, Set<String>>>) -> Unit)? = null
    ): List<Experiment> = Array(n) {
        val source: MutableMap<String, Map<String, Set<String>>> = getRandomSource().toMutableMap()
        transform?.let { it(source) }
        getExperiment(it.toString(), source)
    }.toList()

    private fun getRandom(bound: Int = 100, minimum: Int = 1) = Random().nextInt(bound - minimum) + minimum
    private fun getRandomExperiment() = mapOf(getRandom().toString() to getRandomSet())
    private fun getRandomSet(bound: Int = 10) = (0..getRandom(bound)).mapTo(mutableSetOf()) { getRandom().toString() }

    private fun decorateEndpoints(vararg urls: Pair<EndpointName, String>): Endpoints {
        return Endpoints.create(
            environmentName = defaultEndpoints.environmentName,
            passportContext = passportContext,
            defaultUrls = defaultUrls,
            params = defaultEndpoints.params,
            urls = defaultEndpoints.urls.toMutableMap().apply { putAll(urls) }
        )
    }

    private inner class MockStorage(
        var experiments: List<Experiment>
    ) : Experiments.Storage {

        override fun loadActualExperimentsJson(): String? = null
        override fun saveActualExperiments(json: String) {}
        override fun loadExperiments() = experiments
        override fun loadExperimentsJson() = jsonAdapter.toJson(experiments).orEmpty()
        override fun saveExperiments(json: String) {}
    }
}
