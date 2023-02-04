package ru.auto.ara.interactor

import io.qameta.allure.kotlin.junit4.AllureParametrizedRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.auto.ara.util.assertValuesNoErrors
import ru.auto.data.factory.ISortItemFactory
import ru.auto.data.interactor.SortSettingsInteractor
import ru.auto.data.model.SortType
import ru.auto.data.model.search.Order
import ru.auto.data.model.search.Sort
import ru.auto.data.model.search.SortItem
import ru.auto.data.repository.ISortSettingsRepository
import ru.auto.data.util.SortUtils
import rx.Observable
import java.util.*
import java.util.stream.Collectors

/**
 * @author danser on 22/11/2018.
 */
@RunWith(AllureParametrizedRunner::class)
class SortSettingsInteractorTest(private val randomSeed: Int) {

    private val repo: ISortSettingsRepository = mock()
    private val sortItemFactory: ISortItemFactory = mock()

    private lateinit var sortSettingsInteractor: SortSettingsInteractor

    private val defaultNewAutoSort = Sort(Order.ASC, "new_auto")
    private var sortsAuto: List<Sort> =
        listOf(Sort(Order.ASC, "auto0"), Sort(Order.DESC, "auto1"), Sort(Order.DESC, "auto3"))
    private var sortsMoto: List<Sort> =
        listOf(Sort(Order.DESC, "moto1"), Sort(Order.ASC, "moto2"), Sort(Order.ASC, "moto3"))
    private lateinit var sortsAll: List<Sort>
    private lateinit var sortsAllExpected: List<Sort>

    @Before
    fun setUp() {
        sortSettingsInteractor = SortSettingsInteractor(repo, sortItemFactory, SortUtils)
        whenever(repo.observeSort(any())).thenReturn(Observable.from(emptyList()))
        whenever(sortItemFactory.createList(any())).thenReturn(emptyList())
        whenever(sortItemFactory.createDefault(any(), any())).thenReturn(Sort(Order.ASC, "").toSortItem())
        whenever(sortItemFactory.createDefault(eq(SortType.NEW_AUTO), any()))
            .thenReturn(defaultNewAutoSort.toSortItem())

        sortsAuto = sortsAuto.shuffled()
        sortsMoto = sortsMoto.shuffled()

        sortsAll = sortsAuto + sortsMoto
        sortsAllExpected = sortsAuto + sortsMoto.map { defaultNewAutoSort }
        sortsAll = sortsAll.shuffled()
        sortsAllExpected = sortsAllExpected.shuffled()

        whenever(repo.observeSort(SortType.AUTO)).thenReturn(Observable.from(sortsAuto))
        whenever(repo.observeSort(SortType.MOTO)).thenReturn(Observable.from(sortsMoto))
        whenever(sortItemFactory.createList(SortType.NEW_AUTO)).thenReturn(sortsAuto.map { it.toSortItem() })
    }

    @Test
    fun check_emits_correct_auto_sorts() {
        testSort(SortType.AUTO, sortsAuto)
    }

    @Test
    fun check_emits_correct_moto_sorts() {
        testSort(SortType.MOTO, sortsMoto)
    }

    @Test
    fun check_emits_correct_trucks_sorts() {
        testSort(SortType.TRUCKS, emptyList())
    }

    @Test
    fun check_emits_correct_new_auto_sorts() {
        //new auto returns last auto, because the sort is in options for new auto
        testSort(SortType.NEW_AUTO, sortsAuto)
    }

    private fun testSort(sortType: SortType, expectedSorts: List<Sort>) {
        sortSettingsInteractor.observeSort(sortType)
            .test()
            .assertCompleted()
            .assertValuesNoErrors(*expectedSorts.toTypedArray())
    }

    private fun Sort.toSortItem() = SortItem(
        name,
        name,
        isDesc()
    )

    private fun List<Sort>.shuffled(): List<Sort> = shuffled(Random(randomSeed.toLong()))

    companion object {
        private const val RUNS_COUNT = 5L

        @JvmStatic
        @Parameterized.Parameters
        fun parameters(): Collection<Array<Any>> = Random().ints(RUNS_COUNT)
            .boxed().collect(Collectors.toList())
            .map { arrayOf<Any>(it) }
    }
}
