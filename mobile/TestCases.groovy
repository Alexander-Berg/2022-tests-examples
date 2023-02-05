package com.yandex.frankenstein

import com.yandex.frankenstein.properties.info.StartrekInfo
import com.yandex.frankenstein.properties.info.TestPalmInfo
import com.yandex.frankenstein.startrek.StartrekCommunicator
import com.yandex.frankenstein.startrek.Ticket
import com.yandex.frankenstein.startrek.TicketStatus
import com.yandex.frankenstein.utils.TestPalmCommunicator
import groovy.json.JsonOutput
import groovy.json.StringEscapeUtils
import groovy.transform.CompileStatic
import org.gradle.api.logging.Logger

@CompileStatic
class TestCases {

    private static final Map<String, Ticket> tickets = new HashMap<>()
    private static Map<String, String> defaultAttributeMap = [
        has_bugs: "Has bugs",
        priority: "Priority",
        functionality: "Functionality",
        runType: "Run type"
    ]

    private static List<String> defaultClosedStatuses = [
        "closed"
    ]

    static boolean download(
        final Logger logger,
        final TestPalmInfo testPalmInfo,
        final StartrekInfo startrekInfo,
        final File testCasesFile,
        final Map<String, String> attributeMap = defaultAttributeMap,
        final List<String> closedStatuses = defaultClosedStatuses,
        final TestPalmCommunicator testPalmCommunicator = new TestPalmCommunicator(testPalmInfo, logger),
        final StartrekCommunicator startrekCommunicator = new StartrekCommunicator(logger, startrekInfo)
    ) {
        logger.info("Downloading test cases for test suite $testPalmInfo.suiteId of project $testPalmInfo.projectId...")
        logger.info("Testpalm info is ${testPalmInfo.toString()}")
        if (testPalmCommunicator.isEnoughInfo() == false) {
            logger.info("Required info (TestSuite Id|Project Id|Test Palm Api Token) for downloading test cases is not specified...")
            return false
        }

        try {
            final List<Map<String, ?>> definition = testPalmCommunicator.getDefinitionsInfo()
            final List<Map<String, ?>> testCases = testPalmCommunicator.getSuiteInfo()

            final String json = JsonOutput.prettyPrint(JsonOutput.toJson(
                testCases
                    .sort{ it.id }
                    .collect {
                        [
                            id: it.id,
                            attributes: getAttributes(
                                logger,
                                attributeMap,
                                it.get("attributes", [:]) as Map<String, List<String>>,
                                definition
                            ),
                            bugs: getBugs(
                                logger,
                                closedStatuses,
                                startrekInfo.baseUiUrl,
                                it.get("bugs", [:]) as List<Map<String, ?>>,
                                startrekCommunicator
                            )
                        ]
                    }
            ))

            testCasesFile.parentFile.mkdirs()
            testCasesFile.write(StringEscapeUtils.unescapeJavaScript(json))
        } catch (final Exception e) {
            logger.lifecycle("Test cases download failed: $e")
            testCasesFile.delete()
            return false
        }

        return true
    }

    private static Map<String, List<String>> getAttributes(
        final Logger logger,
        final Map<String, String> attributeMap,
        final Map<String, List<String>> attributes,
        final List<Map<String, ?>> definition
    ) {
        return attributeMap.collectEntries { String attributeName, String testPalmName ->
            final String attributeId = getAttributeId(definition, testPalmName)
            List<String> attributesById
            if (attributeId != null) {
                attributesById = attributes.get(attributeId, [])
            } else {
                logger.info("Not found attributes for $testPalmName")
                attributesById = []
            }
            return [attributeName, attributesById]
        } as Map<String, List<String>>
    }

    private static String getAttributeId(final List<Map<String, ?>> definition, final String attributeTitle) {
        return definition.find{ it.title == attributeTitle }?.id
    }

    private static List<Map<String, ?>> getBugs(
        final Logger logger,
        final List<String> closedStatuses,
        final String baseUrl,
        final List<Map<String, ?>> bugs,
        final StartrekCommunicator startrekCommunicator
    ) {
        return bugs.collect { final Map<String, ?> bug ->
            getTicket(logger, bug.id as String, startrekCommunicator)
        }.findAll { final Ticket ticket ->
            ticket != null && ticket.status.key in closedStatuses == false
        }.collect { final Ticket ticket ->
            return [
                id: ticket.key,
                status: ticket.status.key,
                url: "$baseUrl/${ticket.key}",
            ]
        } as List<Map<String, ?>>
    }

    private static Ticket getTicket(
        final Logger logger,
        final String ticketId,
        final StartrekCommunicator startrekCommunicator
    ) {
        if (!tickets.containsKey(ticketId)) {
            try {
                tickets[ticketId] = new Ticket(startrekCommunicator.getTicketInfo(ticketId))
            } catch (Exception e) {
                logger.lifecycle("Failed to download ticket $ticketId. Exception is ${e.message}")
                return new Ticket(ticketId, new TicketStatus("open"))
            }
        }
        return tickets[ticketId]
    }
}
