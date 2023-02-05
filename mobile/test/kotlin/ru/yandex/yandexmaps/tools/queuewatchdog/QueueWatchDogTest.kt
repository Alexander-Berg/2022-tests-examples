package ru.yandex.yandexmaps.tools.queuewatchdog

import kotlin.test.Test
import kotlin.test.assertEquals

typealias MutableQueueWorkflows = MutableMap<String, MutableMap<String, Workflow>>

internal class QueueWatchDogTest {
    @Test
    fun checkEmptyDiff() {
        val watchDog = QueueWatchDog("STARTREK_TOKEN")
        watchDog.loadData(createMockData())
        val diff = watchDog.compareQueues()
        assert(diff.isEmpty())
    }

    @Test
    fun checkNotEmptyDiff() {
        val data = createMockData()
        data["MAPSIOS"]?.set("untestable", createPatchedMockWorkflow("MAPSIOS", "untestable"))
        val watchDog = QueueWatchDog("STARTREK_TOKEN")
        watchDog.loadData(data)
        val diff = watchDog.compareQueues()
        assert(diff.isNotEmpty())
        val message = watchDog.makeMessage(diff)
        assertEquals("Changed by badGuy workflow 'untestable' in MAPSIOS differs from MOBNAVI.", message)
    }

    private fun createMockData(): MutableQueueWorkflows {
        val data = mutableMapOf<String, MutableMap<String, Workflow>>()
        for (queue in QueueWatchDog.queues) {
            val workflows = mutableMapOf<String, Workflow>()
            for (workflowName in QueueWatchDog.knownWorkflows) {
                workflows[workflowName] = createMockWorkflow(queue, workflowName)
            }
            data[queue] = workflows
        }
        return data
    }

    private fun createMockWorkflow(queue: String, name: String): Workflow {
        return Workflow(
            name = name,
            queue = Queue(queue),
            steps = createMockSteps("open", "closed"),
            updated = "2022-03-31",
            updatedBy = UpdatedBy(display = "someUser")
        )
    }

    private fun createPatchedMockWorkflow(queue: String, name: String): Workflow {
        return Workflow(
            name = name,
            queue = Queue(queue),
            steps = createMockSteps("open", "newStatus", "closed"),
            updated = "2022-04-01",
            updatedBy = UpdatedBy(display = "badGuy")
        )
    }

    private fun createMockSteps(vararg statuses: String): List<Step> {
        return statuses.map { Step(Status(it)) }.toList()
    }

    @Test
    fun testGetPrevAndChangedWorkflows() {
        val workflow = createMockWorkflow("QUEUE1", "name1")
        val changedWorkflow = createPatchedMockWorkflow("QUEUE2", "patchedWorkflow")
        assert(workflow.updated < changedWorkflow.updated)

        val result = QueueWatchDog.getPrevAndChangedWorkflows(workflow, changedWorkflow)
        assert(result.first == workflow)
        assert(result.second == changedWorkflow)

        // Change order of arguments in getPrevAndChangedWorkflows
        assert(result == QueueWatchDog.getPrevAndChangedWorkflows(changedWorkflow, workflow))
    }

    @Test
    fun testDumpWorkflowToString() {
        val workflow = createMockWorkflow("QUEUE", "myWorkflow")
        assertEquals("closed\nopen", workflow.dumpToString())
    }
}
