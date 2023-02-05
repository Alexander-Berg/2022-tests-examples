package com.yandex.launcher.vanga

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.AsyncTask
import androidx.collection.ArrayMap
import androidx.test.core.app.ApplicationProvider
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThanOrEqualTo
import com.natpryce.hamkrest.startsWith
import org.mockito.kotlin.*
import com.yandex.launcher.BaseRobolectricTest
import com.yandex.launcher.ProgramList
import com.yandex.launcher.app.ComponentInitHelper
import com.yandex.launcher.app.GlobalAppState
import com.yandex.launcher.common.loaders.http2.LoadQueue
import com.yandex.launcher.common.loaders.http2.Request
import com.yandex.launcher.common.util.JobUtils
import com.yandex.launcher.loaders.ServerConfiguration
import org.hamcrest.CoreMatchers
import org.junit.After
import org.junit.Assume.assumeThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.`when`
import org.mockito.internal.invocation.InterceptedInvocation
import org.robolectric.shadow.api.Shadow.extract
import org.robolectric.shadows.ShadowJobScheduler
import org.robolectric.util.ReflectionHelpers
import java.util.concurrent.ExecutorService

class UpdateVangaRatingJobTest: BaseRobolectricTest() {

    private val testValuesArrayMap = ArrayMap<Int, Int>()
    private val serverConfig = mock<ServerConfiguration> {
        on { getAddress(any()) } doReturn "http://test"
        on { getAddress(any(), any()) }.thenCallRealMethod()
    }
    private val requestCaptor = ArgumentCaptor.forClass(Request::class.java)
    private val loadQueue = mock<LoadQueue>()
    private val jobParameters = mock<JobParameters>()
    private val updateVangaRatingTask = createUpdateVangaRatingTask()
    private val executor = mock<ExecutorService>()

    private lateinit var jobScheduler: JobScheduler
    private lateinit var shadowJobScheduler: ShadowJobScheduler

    @Before
    override fun setUp() {
        super.setUp()
        testValuesArrayMap[1] = 1
        jobScheduler = appContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        shadowJobScheduler = extract(jobScheduler)
        ReflectionHelpers.setStaticField(ServerConfiguration::class.java, "instance", serverConfig)

        val mockProgramList = spy(ProgramList(ApplicationProvider.getApplicationContext(), mock()))
        whenever(mockProgramList.isHistoryLoaded).thenReturn(true)

        val mockGlobalAppState = mock<GlobalAppState>()
        ReflectionHelpers.setStaticField(GlobalAppState::class.java, "instance", mockGlobalAppState)
        ReflectionHelpers.setField(mockGlobalAppState, "programList", mockProgramList)

        ReflectionHelpers.setStaticField(ComponentInitHelper::class.java, "sInstance", mock<ComponentInitHelper>())
    }

    private fun createUpdateVangaRatingTask(launcherItems: MutableList<LauncherVangaItem> = ArrayList()) =
        spy(UpdateVangaRatingTask(launcherItems, null, mock()))

    @After
    override fun tearDown() {
        super.tearDown()
        ReflectionHelpers.setStaticField(ServerConfiguration::class.java, "instance", null)
    }

    @Test
    fun `schedule update vanga rating job, job not exists, job scheduled successfully`() {
        assumeThat(jobScheduler.allPendingJobs.isEmpty(), CoreMatchers.equalTo(true))

        UpdateVangaRatingJob.schedule(appContext)

        assertThat(jobScheduler.allPendingJobs.size, equalTo(1))
        assertThat(jobScheduler.allPendingJobs[0].id, greaterThanOrEqualTo(JobUtils.JOB_ID_START_VALUE))
        assertThat(jobScheduler.allPendingJobs[0].service, equalTo(ComponentName("com.yandex.launcher", "com.yandex.launcher.vanga.UpdateVangaRatingJob")))
    }

    @Test
    fun `schedule job, job requires network`() {
        assumeThat(jobScheduler.allPendingJobs.isEmpty(), CoreMatchers.equalTo(true))

        UpdateVangaRatingJob.schedule(appContext)

        assertThat(jobScheduler.allPendingJobs[0].networkType, equalTo(JobInfo.NETWORK_TYPE_ANY))
    }

    @Test
    fun `schedule job, job is persisted`() {
        assumeThat(jobScheduler.allPendingJobs.isEmpty(), CoreMatchers.equalTo(true))

        UpdateVangaRatingJob.schedule(appContext)

        assertThat(jobScheduler.allPendingJobs[0].isPersisted, equalTo(true))
    }

    @Test
    fun `start worker, only one request added to load queue`() {
        val worker = initWorker()
        verifyNoInteractions(loadQueue)

        worker.onStart()

        verify(loadQueue).addRequest(requestCaptor.capture())
        assertThat(requestCaptor.allValues.size, equalTo(1))
    }

    @Test
    fun `start worker, request has correct content type`() {
        val worker = initWorker()
        verifyNoInteractions(loadQueue)

        worker.onStart()

        verify(loadQueue).addRequest(requestCaptor.capture())
        assertThat(requestCaptor.value.contentType, equalTo("application/json"))
    }

    @Test
    fun `start worker, request has correct url`() {
        val worker = initWorker()
        verifyNoInteractions(loadQueue)

        worker.onStart()

        verify(loadQueue).addRequest(requestCaptor.capture())
        assertThat(requestCaptor.value.url, startsWith("http://test/api/v2/vanga"))
    }

    @Test
    fun `start worker, reporting error to metrica enabled`() {
        val worker = initWorker()
        verifyNoInteractions(loadQueue)

        worker.onStart()

        verify(loadQueue).addRequest(requestCaptor.capture())
        assertThat(requestCaptor.value.sendErrorsToMetrica, equalTo(true))
    }

    @Test
    fun `start worker, loaded with empty response, job finished, no needs re-schedule`() {
        val worker = spy(initWorker { params, wantsReschedule ->
            // mockito isn't able to mock kotlin functions with Robolectric https://github.com/nhaarman/mockito-kotlin/issues/145#issuecomment-273747263
            assertThat(params, equalTo(jobParameters))
            assertThat(wantsReschedule, equalTo(false))
        })

        whenever(loadQueue.addRequest(any())).thenAnswer { ReflectionHelpers.getField<VangaLoadCallbacks>(getRequest(it as InterceptedInvocation), "loadCallbacks").onDataLoaded(VangaCountersResponse(100500, ArrayList()), mock()) }

        worker.onStart()

        // TODO: may be re-schedule?
        verifyNoInteractions(updateVangaRatingTask)
    }

    @Test
    fun `start worker, loaded with not empty response, task started on executor`() {
        val worker = spy(initWorker())
        whenever(loadQueue.addRequest(any())).thenAnswer {
            ReflectionHelpers.getField<VangaLoadCallbacks>(
                getRequest(it as InterceptedInvocation),
                "loadCallbacks"
            ).onDataLoaded(
                VangaCountersResponse(
                    100500,
                    arrayListOf(
                        LauncherVangaItem(
                            "test1",
                            "test2",
                            1,
                            testValuesArrayMap,
                            testValuesArrayMap,
                            1,
                            testValuesArrayMap,
                            testValuesArrayMap
                        )
                    )
                ), mock()
            )
        }
        verifyNoInteractions(updateVangaRatingTask)

        worker.onStart()

        verify(updateVangaRatingTask).executeOnExecutor(eq(AsyncTask.THREAD_POOL_EXECUTOR), any())
    }

    @Test
    fun `on stop worker, all requests removed`() {
        val worker = initWorker()

        verifyNoInteractions(loadQueue)

        worker.onStop()

        verify(loadQueue).removeAll(eq(false))
    }

    @Test
    fun `on stop job, means job must be re-scheduled`() {
        val job = initJob()

        val needReSchedule = job.onStopJob(jobParameters)

        assertThat(needReSchedule, equalTo(true))
    }

    @Test
    fun `on start job twice, two workers created`() {
        val job = initJob()

        val params1: JobParameters = mock()
        val params2: JobParameters = mock()
        `when`(params1.jobId).thenReturn(1)
        `when`(params2.jobId).thenReturn(2)

        job.onStartJob(params1)
        job.onStartJob(params2)

        assertThat(job.workersByJobId.containsKey(1), equalTo(true))
        assertThat(job.workersByJobId.containsKey(2), equalTo(true))
    }

    @Test
    fun `on stop twice, two workers are removed by one`() {
        val job = initJob()

        val params1: JobParameters = mock()
        val params2: JobParameters = mock()
        `when`(params1.jobId).thenReturn(1)
        `when`(params2.jobId).thenReturn(2)

        job.onStartJob(params1)
        job.onStartJob(params2)

        job.onStopJob(params1)
        assertThat(job.workersByJobId.containsKey(1), equalTo(false))
        assertThat(job.workersByJobId.containsKey(2), equalTo(true))

        job.onStopJob(params2)
        assertThat(job.workersByJobId.containsKey(2), equalTo(false))
    }

    @Test
    fun `on stop, verify worker onStop called`() {
        val jobId = jobParameters.jobId

        val job = initJob()

        job.onStartJob(jobParameters)

        val worker = mock<UpdateVangaRatingJob.Worker>()
        job.workersByJobId.put(jobId, worker)

        job.onStopJob(jobParameters)

        verify(worker).onStop()
    }

    private fun initWorker(onFinished: OnWorkerFinishedCallback = { _, _ -> }) =
        UpdateVangaRatingJob.Worker(
            appContext,
            jobParameters,
            onFinished,
            loadQueue,
            updateVangaRatingTask,
            executor
        )

    private fun initJob() =
        UpdateVangaRatingJob().apply {
            ReflectionHelpers.setField(this, "mBase", appContext)
    }

    private fun getRequest(invokation: InterceptedInvocation): Request = invokation.getArgument(0) as Request
}
