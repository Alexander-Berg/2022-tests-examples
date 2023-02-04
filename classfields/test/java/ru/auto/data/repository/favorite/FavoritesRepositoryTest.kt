package ru.auto.data.repository.favorite

import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.junit.Before
import ru.auto.data.model.action.FavoriteOfferSyncAction
import ru.auto.data.model.data.offer.CAR
import ru.auto.data.model.data.offer.Offer
import ru.auto.data.model.geo.SuggestGeoItem
import ru.auto.data.model.network.scala.NWScalaStatus
import ru.auto.data.network.scala.ScalaApi
import ru.auto.data.network.scala.response.BaseResponse
import ru.auto.data.network.scala.response.OfferListingResponse
import ru.auto.data.prefs.MemoryPrefsDelegate
import ru.auto.data.prefs.MemoryReactivePrefsDelegate
import ru.auto.data.repository.DictionaryRepository
import ru.auto.data.repository.IGeoRepository
import ru.auto.data.repository.ItemsRepository
import ru.auto.data.repository.JsonItemsRepo
import ru.auto.data.repository.sync.offer.FavoriteNewCountBroadcaster
import ru.auto.data.repository.sync.offer.FavoriteOffersRepo
import ru.auto.data.repository.sync.offer.RecommendedFavoritesVisibilityRepository
import rx.Single


/**
 * @author dumchev on 28.09.17.
 */
open class FavoritesRepositoryTest {

    protected val carCategory = CAR
    protected val offerId = "1054508776-886aa0"

    protected val api: ScalaApi = mock()
    protected val mockOffer: Offer = mock()
    protected val successResponse: BaseResponse = mock()
    protected val offerListingResponse: OfferListingResponse = mock()
    protected val geoRepository: IGeoRepository = mock()
    protected val recommendedVisibilityRepository: RecommendedFavoritesVisibilityRepository = mock()

    private val memoryPrefsDelegate = MemoryPrefsDelegate()
    private val memoryReactivePrefsDelegate = MemoryReactivePrefsDelegate()

    protected var syncStorage: ItemsRepository<FavoriteOfferSyncAction> =
        JsonItemsRepo.instance<FavoriteOfferSyncAction>(
            memoryReactivePrefsDelegate, FavoriteOffersRepo.SYNC_ACTION_KEY
        )

    private val dictionaryRepository: DictionaryRepository = mock()
    private val favBroadcasterOnPrefsMock: FavoriteNewCountBroadcaster = FavoriteNewCountBroadcaster(memoryPrefsDelegate)
    protected lateinit var favRepo: FavoriteOffersRepo

    @Before
    open fun setUp() {
        syncStorage = JsonItemsRepo.instance<FavoriteOfferSyncAction>(
            memoryReactivePrefsDelegate, FavoriteOffersRepo.SYNC_ACTION_KEY
        )
        whenever(offerListingResponse.offers).thenReturn(emptyList())
        whenever(offerListingResponse.offers_with_new_price_count).thenReturn(CHANGED_OFFERS_COUNT)
        whenever(api.addFavorite(any(), any())).thenReturn(Single.just(successResponse))
        whenever(api.removeFavorite(any(), any())).thenReturn(Single.just(successResponse))
        whenever(api.getFavorites(any(), any(), any(), any(), anyOrNull(), anyOrNull(), any())).thenReturn(
            Single.just(
                offerListingResponse
            )
        )

        whenever(mockOffer.id).thenReturn(offerId)
        whenever(mockOffer.getCategory()).thenReturn(carCategory)
        whenever(geoRepository.getRadiusNow()).thenReturn(300)
        whenever(geoRepository.getSelectedRegions()).thenReturn(
            Single.just(
                listOf(
                    SuggestGeoItem(
                        id = "213",
                        name = "Москва",
                        geoRadiusSupport = true,
                        radius = 200
                    )
                )
            )
        )

        whenever(successResponse.status).thenReturn(NWScalaStatus.SUCCESS)
        whenever(recommendedVisibilityRepository.shouldShowRecommended()).thenReturn(false)

        favRepo = FavoriteOffersRepo(
            api = api,
            dictionaryRepository = dictionaryRepository,
            dateFromProvider = favBroadcasterOnPrefsMock,
            geoRepository = geoRepository,
            syncRepo = syncStorage,
            logError = {},
            recommendedVisibilityRepository = recommendedVisibilityRepository
        )
    }

    companion object {
        const val CHANGED_OFFERS_COUNT = 0
    }
}
