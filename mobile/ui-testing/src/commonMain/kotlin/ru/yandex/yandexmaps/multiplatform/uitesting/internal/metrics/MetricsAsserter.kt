package ru.yandex.yandexmaps.multiplatform.uitesting.internal.metrics

import ru.yandex.yandexmaps.multiplatform.uitesting.api.Application
import ru.yandex.yandexmaps.multiplatform.uitesting.api.AssertionProvider

internal class MetricsAsserter {

    fun assertMetricsConsistency(testingApplication: Application, referenceApplication: Application, assertionProvider: AssertionProvider) {
        val testingEvents = testingApplication.getMetricsEvents()
        val referenceEvents = referenceApplication.getMetricsEvents()

        var currentReferenceEventIndex = 0

        for (testingEvent in testingEvents) {
            if (currentReferenceEventIndex < referenceEvents.size) {
                val referenceEvent = referenceEvents[currentReferenceEventIndex]
                if (referenceEvent == testingEvent) {
                    ++currentReferenceEventIndex
                }
            } else {
                return
            }
        }

        if (currentReferenceEventIndex < referenceEvents.size) {
            val remainingReferenceEventsAsString = referenceEvents.listIterator(currentReferenceEventIndex)
                .asSequence()
                .joinToString(prefix = "[", postfix = "]")
            assertionProvider.fail("The following events were not logged: $remainingReferenceEventsAsString")
        }
    }
}
