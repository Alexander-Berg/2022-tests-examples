package ru.yandex.intranet.d.dao.units

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.datasource.model.YdbTableClient
import ru.yandex.intranet.d.model.units.GrammaticalCase
import ru.yandex.intranet.d.model.units.UnitModel

@IntegrationTest
class TestUnitsNames {
    @Autowired
    private lateinit var tableClient: YdbTableClient

    @Autowired
    private lateinit var unitsEnsemblesDao: UnitsEnsemblesDao

    private fun String.isEnglish(): Boolean {
        return matches("^[a-zA-Z/ ]+$".toRegex())
    }

    private fun String.isRussian(): Boolean {
        return matches("^[а-яА-Я/ ]+$".toRegex())
    }

    private fun Map<GrammaticalCase, String>.isRussian(): Boolean {
        return values.all { it.isRussian() }
    }

    private fun getAllUnits(): List<UnitModel> {
        return tableClient.usingSessionMonoRetryable {
            unitsEnsemblesDao.getAll(it).collectList()
        }.block().orEmpty().flatMap { it.units }
    }

    @Test
    fun russianNamesContainsOnlyRussianLettersTest() {
        val notRussianUnits = getAllUnits().filterNot {
            it.realLongNamePluralRu().isRussian() &&
                it.realLongNameSingularRu().isRussian() &&
                it.realShortNamePluralRu().isRussian() &&
                it.realShortNameSingularRu().isRussian()
        }
        Assertions.assertEquals(emptyList<UnitModel>(), notRussianUnits)
    }

    @Test
    fun englishNamesContainOnlyEnglishLettersTest() {
        val notEnglishUnits = getAllUnits().filterNot {
            it.longNamePluralEn.isEnglish() &&
                it.longNameSingularEn.isEnglish() &&
                it.shortNamePluralEn.isEnglish() &&
                it.shortNameSingularEn.isEnglish()
        }
        Assertions.assertEquals(emptyList<UnitModel>(), notEnglishUnits)
    }
}
