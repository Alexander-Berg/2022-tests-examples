package com.yandex.frankenstein.testing

import com.google.common.collect.ImmutableSet
import groovy.json.JsonSlurper
import org.gradle.api.internal.DocumentationRegistry
import org.gradle.api.internal.classpath.ModuleRegistry
import org.gradle.api.internal.tasks.testing.JvmTestExecutionSpec
import org.gradle.api.internal.tasks.testing.TestClassProcessor
import org.gradle.api.internal.tasks.testing.TestExecuter
import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.gradle.api.internal.tasks.testing.detection.DefaultTestClassScanner
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.internal.tasks.testing.processors.PatternMatchTestClassProcessor
import org.gradle.api.internal.tasks.testing.processors.RestartEveryNTestClassProcessor
import org.gradle.api.internal.tasks.testing.processors.RunPreviousFailedFirstTestClassProcessor
import org.gradle.api.internal.tasks.testing.processors.TestMainAction
import org.gradle.api.internal.tasks.testing.worker.ForkingTestClassProcessor
import org.gradle.api.logging.Logger
import org.gradle.internal.Factory
import org.gradle.internal.actor.ActorFactory
import org.gradle.internal.operations.BuildOperationExecutor
import org.gradle.internal.time.Clock
import org.gradle.internal.work.WorkerLeaseRegistry
import org.gradle.process.internal.worker.WorkerProcessFactory
import java.io.File
import java.lang.RuntimeException

open class CustomTestExecuter(
    private val logger: Logger,
    private val testClassNameToPriorityMapper: TestClassNameToPriorityMapper,
    private val workerFactory: WorkerProcessFactory,
    private val actorFactory: ActorFactory,
    private val moduleRegistry: ModuleRegistry,
    private val workerLeaseRegistry: WorkerLeaseRegistry,
    private val buildOperationExecutor: BuildOperationExecutor,
    private val maxWorkerCount: Int,
    private val clock: Clock,
    private val documentationRegistry: DocumentationRegistry,
    private val testFilter: DefaultTestFilter,
    private val buildDir: File? = null
) : TestExecuter<JvmTestExecutionSpec> {

    private lateinit var processor: TestClassProcessor

    override fun execute(testExecutionSpec: JvmTestExecutionSpec, testResultProcessor: TestResultProcessor) {
        val testFramework = testExecutionSpec.testFramework

        val testInstanceFactory = testFramework.processorFactory
        val currentWorkerLease = workerLeaseRegistry.currentWorkerLease
        val classpath = ImmutableSet.copyOf(testExecutionSpec.classpath)
        val modulePath = ImmutableSet.copyOf(testExecutionSpec.modulePath)
        val forkingProcessorFactory = Factory<TestClassProcessor> {
            ForkingTestClassProcessor(
                currentWorkerLease,
                workerFactory,
                testInstanceFactory,
                testExecutionSpec.javaForkOptions,
                classpath,
                modulePath,
                testFramework.testWorkerImplementationModules,
                testFramework.workerConfigurationAction,
                moduleRegistry, documentationRegistry
            )
        }
        val reforkingProcessorFactory = Factory<TestClassProcessor> {
            RestartEveryNTestClassProcessor(forkingProcessorFactory, testExecutionSpec.forkEvery)
        }
        processor = PatternMatchTestClassProcessor(
            testFilter,
            RunPreviousFailedFirstTestClassProcessor(
                testExecutionSpec.previousFailedTestClasses,
                getParallelTestClassProcessor(testExecutionSpec, reforkingProcessorFactory)
            )
        )

        val testClassFiles = testExecutionSpec.candidateClassFiles

        val detector = if (testExecutionSpec.isScanForTestClasses && testFramework.detector != null) {
            val testFrameworkDetector = testFramework.detector
            testFrameworkDetector.setTestClasses(testExecutionSpec.testClassesDirs.files)
            testFrameworkDetector.setTestClasspath(classpath)
            DefaultTestClassScanner(testClassFiles, testFrameworkDetector, processor)
        } else {
            DefaultTestClassScanner(testClassFiles, null, processor)
        }

        val testTaskOperationId = buildOperationExecutor.currentOperation.parentId

        TestMainAction(
            detector,
            processor,
            testResultProcessor,
            clock,
            testTaskOperationId,
            testExecutionSpec.path,
            "Gradle Test Run " + testExecutionSpec.identityPath
        ).run()
    }

    override fun stopNow() = processor.stopNow()

    fun getParallelTestClassProcessor(
        testExecutionSpec: JvmTestExecutionSpec,
        reforkingProcessorFactory: Factory<TestClassProcessor>
    ): TestClassProcessor {
        return CustomMaxNParallelTestClassProcessor(
            logger,
            getMaxParallelForks(testExecutionSpec),
            reforkingProcessorFactory,
            actorFactory,
            testClassNameToPriorityMapper
        )
    }

    private fun getMaxParallelForks(testExecutionSpec: JvmTestExecutionSpec): Int {
        var maxParallelForks = testExecutionSpec.maxParallelForks
        val readyWorkersCount = getReadyWorkersCount()
        if (readyWorkersCount < 1) {
            throw RuntimeException("No workers ready")
        }
        if (maxParallelForks > readyWorkersCount) {
            logger.warn(
                "{}.maxParallelForks ({}) is larger than ready-workers ({}), forcing it to {}",
                testExecutionSpec.path,
                maxParallelForks,
                readyWorkersCount,
                readyWorkersCount
            )
            maxParallelForks = readyWorkersCount
        }
        if (maxParallelForks > maxWorkerCount) {
            logger.info(
                "{}.maxParallelForks ({}) is larger than max-workers ({}), forcing it to {}",
                testExecutionSpec.path,
                maxParallelForks,
                maxWorkerCount,
                maxWorkerCount
            )
            maxParallelForks = maxWorkerCount
        }
        return maxParallelForks
    }

    private fun getReadyWorkersCount(): Int {
        val devicesInfo = JsonSlurper().parse(File(buildDir, "devices_info.json")) as Map<String, List<String>>

        return (devicesInfo["booted"]?.size ?: 0) + (devicesInfo["running"]?.size ?: 0)
    }
}
