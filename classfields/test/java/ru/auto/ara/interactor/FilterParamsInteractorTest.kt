package ru.auto.ara.interactor

import io.qameta.allure.kotlin.junit4.AllureRunner
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import ru.auto.ara.data.search.MultiMarkValue
import ru.auto.ara.search.mapper.OfferSearchRequestMapper
import ru.auto.ara.util.SerializablePair
import ru.auto.core_ui.util.Consts
import ru.auto.core_ui.util.Consts.CATALOG_NAMEPLATE
import ru.auto.core_ui.util.Consts.CATALOG_NEW_GENERATION
import ru.auto.core_ui.util.Consts.CATALOG_NEW_MARK_ID
import ru.auto.core_ui.util.Consts.CATALOG_NEW_MODEL_ID
import ru.auto.data.model.search.Mark
import ru.auto.data.model.search.Model
import ru.auto.data.model.search.Nameplate
import ru.auto.data.repository.IOffersRepository
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AllureRunner::class) class FilterParamsInteractorTest {

    private val offersRepository: IOffersRepository = mock()
    private val offerSearchRequestMapper: OfferSearchRequestMapper = mock()
    private val interactor: FilterParamsInteractor = FilterParamsInteractor(offersRepository, offerSearchRequestMapper)

    @Test
    fun `interactor removes all occurrences of marks, models, nameplates and generations`() {

        val wrongParams: List<SerializablePair<String, String>> = listOf(
                SerializablePair.create(CATALOG_NEW_MARK_ID, "1"),
                SerializablePair.create(CATALOG_NEW_MODEL_ID, "2"),
                SerializablePair.create(CATALOG_NAMEPLATE, "3"),
                SerializablePair.create(CATALOG_NEW_GENERATION, "4")
        )

        interactor.buildMarkModelGenerationParams(MARK_VALUE, wrongParams).apply {
            assertFalse { any { it.first == CATALOG_NEW_MARK_ID } }
            assertFalse { any { it.first == CATALOG_NEW_MODEL_ID } }
            assertFalse { any { it.first == CATALOG_NAMEPLATE } }
            assertFalse { any { it.first == CATALOG_NEW_GENERATION } }
        }
    }

    @Test
    fun `interactor removes mark-model-nameplate template`() {

        val wrongParams: List<SerializablePair<String, String>> = listOf(
                SerializablePair.create(Consts.FILTER_PARAM_MARK_MODEL_NAMEPLATE, "1")
        )

        interactor.buildMarkModelGenerationParams(MARK_VALUE, wrongParams).apply {
            assertFalse { any { it.first == Consts.FILTER_PARAM_MARK_MODEL_NAMEPLATE && it.second == "1" } }
        }
    }

    @Test
    fun `interactor build correct nameplate params`() {
        interactor.buildMarkModelGenerationParams(MARK_VALUE, emptyList())
                .filter { it.first == Consts.FILTER_PARAM_MARK_MODEL_NAMEPLATE }
                .apply {
                    assertEquals(1, size)
                    assertTrue { any { it.second == getNameplateParam(MARK, MODEL, NAMEPLATE1) } }
                }
    }

    private fun getNameplateParam(mark: String, model: String, nameplate: String): String =
            listOf(
                    "mark%$mark%$mark",
                    "model%$model%$model",
                    "nameplate%$nameplate%$nameplate"
            ).joinToString(separator = "#")

    private companion object {

        const val MARK = "BMW"
        const val MODEL = "1ER"
        const val NAMEPLATE1 = "1"

        val MARK_VALUE = MultiMarkValue(
                baseMarks = listOf(
                        Mark(
                                id = MARK,
                                name = MARK,
                                models = listOf(
                                        Model(
                                                id = MODEL,
                                                name = MODEL,
                                                nameplates = listOf(
                                                        Nameplate(
                                                                id = NAMEPLATE1,
                                                                name = NAMEPLATE1
                                                        )
                                                ),
                                                generations = emptyList()
                                        )
                                )
                        )
                )
        )
    }
}
