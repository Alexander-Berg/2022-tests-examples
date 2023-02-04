package ru.auto.ara.repository

import io.qameta.allure.kotlin.junit4.AllureRunner
 import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
 import ru.auto.ara.RxTest
import ru.auto.data.model.DealerOffersFilter
import ru.auto.data.model.data.offer.ACTIVE
import ru.auto.data.model.data.offer.ALL
import ru.auto.data.model.data.offer.BANNED
import ru.auto.data.model.data.offer.CAR
import ru.auto.data.model.data.offer.INACTIVE
import ru.auto.data.repository.DealerOffersFilterRepository
import rx.observers.TestSubscriber

/**
 * @author aleien on 12.07.18.
 */
@RunWith(AllureRunner::class) class FilterRepositoryTest : RxTest() {

    private val filterRepository = DealerOffersFilterRepository()

    @Test
    fun `when getting filter first time, we should get ACTIVE`() {
        assertThat(filterRepository.getFilter()).isEqualTo(TEST_START_FILTER)
    }

    @Test
    fun `filter should return updated result after change`() {
        val newFilter = DealerOffersFilter(ACTIVE)
        filterRepository.updateFilter(newFilter)

        assertThat(filterRepository.getFilter()).isEqualTo(newFilter)
    }

    @Test
    fun `after clearing filter repository it should return default value`() {
        val newFilter = DealerOffersFilter(ACTIVE)
        filterRepository.updateFilter(newFilter)

        filterRepository.clearFilter()
        assertThat(filterRepository.getFilter()).isEqualTo(TEST_DEFAULT_FILTER)
    }

    @Test
    fun `after updating filter observers should get updated filter`() {
        val newFilter = DealerOffersFilter(category = CAR, status = ACTIVE)

        val testSub = TestSubscriber<DealerOffersFilter>()
        filterRepository.observeFilter().subscribe(testSub)

        filterRepository.updateFilter(newFilter)

        assertThat(testSub.onNextEvents)
                .size()
                .isEqualTo(2)
                .returnToIterable()
                .element(1)
                .isEqualTo(newFilter)
    }

    @Test
    fun `observeFilter should emit filter even if filter wasn't changed`() {
        val newFilter = DealerOffersFilter(CAR, ACTIVE)
        val newFilter2 = DealerOffersFilter(CAR, ACTIVE)

        val testSub = TestSubscriber<DealerOffersFilter>()
        filterRepository.observeFilter().subscribe(testSub)

        filterRepository.updateFilter(newFilter)
        filterRepository.updateFilter(newFilter2)


        assertThat(testSub.onNextEvents)
                .size()
                .isEqualTo(3)
                .returnToIterable()
                .element(1)
                .isEqualTo(newFilter)
    }

    @Test
    fun `observeFilter should emit filter if filter was changed`() {
        val newFilter = DealerOffersFilter(CAR, BANNED)
        val newFilter2 = DealerOffersFilter(CAR, INACTIVE)

        val testSub = TestSubscriber<DealerOffersFilter>()
        filterRepository.observeFilter().subscribe(testSub)

        filterRepository.updateFilter(newFilter)
        filterRepository.updateFilter(newFilter2)


        assertThat(testSub.onNextEvents)
                .containsExactlyElementsOf(listOf(TEST_START_FILTER, newFilter, newFilter2))
    }

    companion object {
        private val TEST_DEFAULT_FILTER = DealerOffersFilter(ALL)
        private val TEST_START_FILTER = DealerOffersFilter(ALL, status = ACTIVE)
    }

}
