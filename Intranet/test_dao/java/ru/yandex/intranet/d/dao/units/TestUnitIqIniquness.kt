package ru.yandex.intranet.d.dao.units

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.model.units.UnitModel
import java.util.Map
import java.util.stream.Collectors

/**
 * Test for units IDs uniqueness.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 * @since 14-05-2021
 */
private const val GRPC_URI = "in-process:test"

@IntegrationTest
class TestUnitsIdsUniqueness {
    @Autowired
    private lateinit var tableClient: YdbTableClient

    @Autowired
    private lateinit var unitsEnsemblesDao: UnitsEnsemblesDao

    @Test
    fun test() {
        val unitsEnsembles = tableClient.usingSessionMonoRetryable { session ->
            unitsEnsemblesDao.getAll(session).collect(Collectors.toList());
        }.block().orEmpty()

        val duplicates = unitsEnsembles
            .stream().flatMap { it.units.stream() }.collect(Collectors.groupingBy { it.id })
            .entries.stream().filter { it.value.size > 1 }.collect(Collectors.toMap({ it.key }, { it.value }))
        val empty: MutableMap<String, MutableList<UnitModel>> = Map.of()

        Assertions.assertEquals(empty, duplicates)
    }
}
