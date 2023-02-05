package ru.yandex.market.clean.domain.usecase

import androidx.annotation.CheckResult
import io.reactivex.Completable
import io.reactivex.Single
import ru.yandex.market.clean.data.repository.OrderOptionsRepository
import ru.yandex.market.clean.data.repository.RegionsRepository
import ru.yandex.market.clean.domain.usecase.cart.GetCartItemsUseCase
import ru.yandex.market.common.featureconfigs.provider.FeatureConfigsProvider
import ru.yandex.market.common.schedulers.DataSchedulers
import ru.yandex.market.utils.zipToPair
import javax.inject.Inject

class TestSyncUseCase @Inject constructor(
    private val cartAffectingDataUseCase: CartAffectingDataUseCase,
    private val getCartItemsUseCase: GetCartItemsUseCase,
    private val featureConfigsProvider: FeatureConfigsProvider,
    private val orderOptionsRepository: OrderOptionsRepository,
    private val regionsRepository: RegionsRepository,
    private val dataSchedulers: DataSchedulers,
) {
    @CheckResult
    fun execute(): Completable {
        return Single.fromCallable {
            featureConfigsProvider.techPushWarmUpCacheToggleManager.get().isEnabled
        }.flatMapCompletable {
            if (it) {
                warmUpCache()
            } else {
                Completable.complete()
            }
        }.subscribeOn(dataSchedulers.worker)
    }

    @CheckResult
    private fun warmUpCache(): Completable {
        return cartAffectingDataUseCase.getCartAffectingDataStream()
            .firstOrError()
            .flatMap {
                regionsRepository.currentOrDefaultRegionId.zipToPair(
                    getCartItemsUseCase.getCartItems(),
                )
            }
            .flatMapCompletable {
                orderOptionsRepository.validateOrder(
                    it.first,
                    it.second,
                    emptyList(),
                    "",
                    "",
                    true
                ).ignoreElement()
            }
    }
}
