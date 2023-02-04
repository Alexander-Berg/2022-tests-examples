package ru.auto.ara.interactor

import com.google.gson.Gson
import io.qameta.allure.kotlin.junit4.AllureParametrizedRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import ru.auto.ara.util.bindWithLog
import ru.auto.data.model.SortType
import ru.auto.data.model.search.Order
import ru.auto.data.model.search.Sort
import ru.auto.data.prefs.IReactivePrefsDelegate
import ru.auto.data.repository.SortSettingsRepository
import rx.Observable
import java.util.*
import java.util.stream.Collectors
import kotlin.random.Random.Default.nextInt
import kotlin.test.assertEquals

/**
 * @author danser on 22/11/2018.
 */
/**
 * @author danser on 22/11/2018.
 */
@RunWith(AllureParametrizedRunner::class)
class SortSettingsRepositoryTest(private val randomSeed: Int) {

    private val prefs: IReactivePrefsDelegate = mock()
    private val gson: Gson = mock()
    private lateinit var repo: SortSettingsRepository
    private val defaultSort = Sort(Order.ASC, "default")
    private val random: Random get() = Random(randomSeed.toLong())

    @Before
    fun setUp() {
        repo = SortSettingsRepository(
            prefs = prefs,
            gson = gson,
            shouldSaveToPrefs = false,
            defaultSortOptionsProvider = { defaultSort }
        )
    }

    @Test
    fun check_repository_emits_correct_sort_items_on_update() {
        val countRandom = random
        val autoSortCount = nextInt(MAX_SORT_COUNT)
        val motoSortCount = nextInt(MAX_SORT_COUNT)
        val trucksSortCount = nextInt(MAX_SORT_COUNT)

        val sortsAuto = getSortList(SortType.AUTO, autoSortCount).map { SortType.AUTO to it }
        val sortsMoto = getSortList(SortType.MOTO, motoSortCount).map { SortType.MOTO to it }
        val sortsTrucks = getSortList(SortType.TRUCKS, trucksSortCount).map { SortType.TRUCKS to it }

        var allSorts = sortsAuto + sortsMoto + sortsTrucks
        allSorts = allSorts.shuffled(random)

        val actualSorts: Map<SortType, List<Sort>> = SortType.values().map { sortType ->
            sortType to subscribeToSort(sortType)
        }.toMap()

        Observable.from(allSorts)
            .flatMapCompletable { (sortType, sort) -> repo.updateSort(sortType, sort) }
            .bindWithLog()

        verify(prefs, never()).saveString(any(), any())
        verify(gson, never()).fromJson(any<String>(), eq(Sort::class.java))
        verify(gson, never()).toJson(any())

        SortType.values().forEach { sortType ->
            val actualSorts = actualSorts.getValue(sortType)
            assertSort(sortType, allSorts, actualSorts)
        }
    }


    private fun getSortList(sortType: SortType, size: Int): List<Sort> {
        val random = random
        return (0..size).map { it to random.nextBoolean() }
            .map { (num, asc) ->
                Sort(
                    order = if (asc) Order.ASC else Order.DESC,
                    name = "$sortType$num"
                )
            }

    }

    private fun subscribeToSort(sortType: SortType): MutableList<Sort> {
        val actualSorts = mutableListOf<Sort>()
        repo.observeSort(sortType)
            .map { actualSorts.add(it) }
            .test()
            .assertNotCompleted()
        return actualSorts
    }

    private fun assertSort(
        sortType: SortType,
        expectedSorts: List<Pair<SortType, Sort>>,
        actualSorts: List<Sort>
    ) {
        val expectedSorts = (listOf(sortType to defaultSort) + expectedSorts)
            .filter { it.first == sortType }.map { it.second }
        assertEquals<List<*>>(expectedSorts, actualSorts)
    }

    companion object {
        private const val RUNS_COUNT = 5L
        private const val MAX_SORT_COUNT = 10

        @JvmStatic
        @Parameterized.Parameters
        fun parameters(): Collection<Array<Any>> = Random().ints(RUNS_COUNT)
            .boxed().collect(Collectors.toList())
            .map { arrayOf<Any>(it) }
    }
}
