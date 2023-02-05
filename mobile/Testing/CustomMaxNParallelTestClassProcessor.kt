package com.yandex.frankenstein.testing

import org.gradle.api.internal.tasks.testing.TestClassProcessor
import org.gradle.api.internal.tasks.testing.TestClassRunInfo
import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.gradle.api.logging.Logger
import org.gradle.internal.Factory
import org.gradle.internal.UncheckedException
import org.gradle.internal.actor.Actor
import org.gradle.internal.actor.ActorFactory
import org.gradle.internal.concurrent.CompositeStoppable
import org.gradle.internal.dispatch.DispatchException
import java.lang.Math.min
import java.util.ArrayList
import java.util.concurrent.CountDownLatch

class CustomMaxNParallelTestClassProcessor(
    private val logger: Logger,
    private val maxProcessors: Int,
    private val factory: Factory<TestClassProcessor>,
    private val actorFactory: ActorFactory,
    private val testClassNameToPriorityMapper: TestClassNameToPriorityMapper
) : TestClassProcessor {

    private lateinit var resultProcessor: TestResultProcessor
    private lateinit var processorsSemaphore: CountDownLatch
    private lateinit var resultProcessorActor: Actor
    private val allTestClasses = ArrayList<TestClassRunInfo>()
    private val processors = ArrayList<TestClassProcessor>()
    private val rawProcessors = ArrayList<TestClassProcessor>()
    private val actors = ArrayList<Actor>()
    private var nextTestClassIndex = 0
    @Volatile
    private var stoppedNow: Boolean = false

    override fun startProcessing(resultProcessor: TestResultProcessor) {
        resultProcessorActor = actorFactory.createActor(resultProcessor)
        this.resultProcessor = resultProcessorActor.getProxy(TestResultProcessor::class.java)
    }

    override fun processTestClass(testClass: TestClassRunInfo) {
        allTestClasses.add(testClass)
    }

    override fun stop() {
        allTestClasses.sortBy { testClassNameToPriorityMapper.getPriority(it.testClassName) }
        allTestClasses.reverse()
        val processorsCount = min(maxProcessors, allTestClasses.size)
        processorsSemaphore = CountDownLatch(processorsCount)
        repeat(processorsCount) { index: Int ->
            var processor = factory.create()
            rawProcessors.add(processor!!)
            val actor = actorFactory.createActor(processor)
            processor = actor.getProxy(TestClassProcessor::class.java)
            actors.add(actor)
            processors.add(processor)
            processor!!.startProcessing(
                CustomTestResultProcessor(
                    logger,
                    resultProcessor,
                    index,
                    object : TestClassCompletedListener {
                        override fun onTestClassCompleted(processorIndex: Int) {
                            onTestClassCompleted(processorIndex, processorsSemaphore)
                        }
                    }
                )
            )
        }
        processors.forEachIndexed { index, _ -> onTestClassCompleted(index, processorsSemaphore) }
        processorsSemaphore.await()

        try {
            CompositeStoppable.stoppable(processors).add(actors).add(resultProcessorActor).stop()
        } catch (e: DispatchException) {
            throw UncheckedException.throwAsUncheckedException(e.cause)
        }
    }

    override fun stopNow() {
        stoppedNow = true
        for (processor in rawProcessors) {
            processor.stopNow()
            processorsSemaphore.countDown()
        }
    }

    @Synchronized
    fun onTestClassCompleted(processorIndex: Int, processorsSemaphore: CountDownLatch) {
        logger.info("Processor $processorIndex is free")
        if (nextTestClassIndex != allTestClasses.size && stoppedNow == false) {
            logger.info("Processor $processorIndex got test class ${allTestClasses[nextTestClassIndex].testClassName}")
            processors[processorIndex].processTestClass(allTestClasses[nextTestClassIndex])
            nextTestClassIndex++
        } else {
            logger.info("Processor $processorIndex is down")
            processorsSemaphore.countDown()
        }
    }
}
