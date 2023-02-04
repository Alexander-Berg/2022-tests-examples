package ru.yandex.intranet.d.tms.jobs.refreshTransferRequests

import org.junit.jupiter.api.Assertions
import ru.yandex.intranet.d.web.model.tracker.TrackerUpdateTicketDto

/**
 * TicketHelper.
 *
 * @author Petr Surkov <petrsurkov@yandex-team.ru>
 */

/**
 * Проверяет равенство двух [TrackerUpdateTicketDto] допуская перестановку ответственных.
 */
fun assertEqualsUpdateTicketDtoDespiteOrderOfResponsible(expected: TrackerUpdateTicketDto, actual: TrackerUpdateTicketDto) {
    Assertions.assertEquals(expected.components, actual.components)
    Assertions.assertEquals(expected.summary, actual.summary)
    Assertions.assertEquals(expected.tags, actual.tags)
    // В регулярке ниже вопросик у пробела нужен:
    // если подтверждающих нет, то в реальном тикете стоит пробел после двоеточия и дальше уже перенос строки.
    // Но в тестовых данных в raw строчках котлина пробел на конце иногда стирается, видимо, автоформатером.
    val responsibleLineRegex = Regex("^Подтверждающие: ?(.*)$", RegexOption.MULTILINE)
    Assertions.assertEquals(
        responsibleLineRegex.split(expected.description),
        responsibleLineRegex.split(actual.description)
    )
    val expectedResponsible = responsibleLineRegex.findAll(expected.description)
        .map { it.groupValues[1].split(", ").toSet() }
        .toList()
    val actualResponsible = responsibleLineRegex.findAll(actual.description)
        .map { it.groupValues[1].split(", ").toSet() }
        .toList()
    Assertions.assertEquals(expectedResponsible, actualResponsible)
}
