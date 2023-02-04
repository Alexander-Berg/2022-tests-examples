package ru.auto.ara.interactor

import io.qameta.allure.kotlin.junit4.AllureParametrizedRunner
import junit.framework.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.data.feed.loader.SoldItemFeedRandomPositionProvider
import ru.auto.data.model.Pagination
import ru.auto.data.model.VehicleCategory
import ru.auto.data.model.common.Page
import ru.auto.data.model.data.offer.Offer
import ru.auto.data.model.data.offer.SellerType
import ru.auto.data.model.feed.FeedInfo
import ru.auto.data.model.feed.OffersSearchRequest
import ru.auto.data.model.feed.SearchType
import ru.auto.data.model.feed.factory.DefaultFeedFactory
import ru.auto.data.model.feed.factory.DefaultFeedFactory.Companion.PAGE_SIZE
import ru.auto.data.model.feed.factory.FeedItemType
import ru.auto.data.model.feed.factory.SearchFeedFactory
import ru.auto.data.model.feed.model.IDataFeedItemModel
import ru.auto.data.model.feed.model.OfferDataFeedItemModel
import ru.auto.data.model.filter.CarParams
import ru.auto.data.model.filter.CarSearch
import ru.auto.data.model.filter.CommonVehicleParams
import ru.auto.data.model.filter.SearchRequestByParams
import ru.auto.data.model.filter.SearchSort
import ru.auto.data.model.offer.FeedFlags
import ru.auto.data.model.offer.OfferListingResult
import ru.auto.data.model.search.SearchContext
import ru.auto.data.repository.feed.loader.post.FeedRequest
import ru.auto.data.repository.feed.loader.post.IFeedState
import ru.auto.data.repository.feed.loader.post.IPostFeedLoader
import ru.auto.data.repository.feed.loader.post.PageContext
import rx.Observable
import kotlin.math.max
import kotlin.math.min

@RunWith(AllureParametrizedRunner::class)
class SearchFeedSchemeTest(private val args: Arguments) {

    private val defaultFeedFactory: DefaultFeedFactory = SearchFeedFactory(
        emptyListingLoader = getMockedPostFeedLoader(),
        adLoader = getMockedPostFeedLoader(),
        videosLoader = getMockedPostFeedLoader(),
        specialsLoader = getMockedPostFeedLoader(),
        reviewsLoader = getMockedPostFeedLoader(),
        filterPromoLoader = getMockedPostFeedLoader(),
        recommendedLoader = getMockedPostFeedLoader(),
        matchApplicationLoader = getMockedPostFeedLoader(),
        endlessListingTitleLoader = getMockedPostFeedLoader(),
        loanPromoLoader = getMockedPostFeedLoader(),
        offerPostLoader = getMockedPostFeedLoader(),
        soldOfferPostLoader = getMockedPostFeedLoader()
    )

    @Test
    fun checkFeedScheme() {
        checkFeedSchemePage1()
        checkFeedSchemePage2()
    }

    private fun checkFeedSchemePage1() {
        val feedInfo = getFeedInfo(
            feedArgs = args.feedArgs,
            pageIndex = 0
        )
        val expected = getExpectedScheme(
            scheme = args.expectedSchemePage1,
            soldItemPosition = args.expectedSoldItemPositionPage1
        )
        val actual = defaultFeedFactory.getTypedFeedScheme(feedInfo).map { it.itemType }
        assertEquals("${args.name}\n Second page scheme for arguments '${args.feedArgs}' should be the same as", expected, actual)
    }

    private fun checkFeedSchemePage2() {
        if (args.feedArgs.lastPage == 1) return
        val feedInfo = getFeedInfo(
            feedArgs = args.feedArgs,
            pageIndex = 1
        )
        val expected = getExpectedScheme(
            scheme = args.expectedSchemePage2,
            soldItemPosition = args.expectedSoldItemPositionPage2
        )
        val actual = defaultFeedFactory.getTypedFeedScheme(feedInfo).map { it.itemType }
        assertEquals("${args.name}\n Second page scheme for arguments '${args.feedArgs}' should be the same as", expected, actual)
    }

    private fun getExpectedScheme(scheme: List<Pair<FeedItemType, Int>>, soldItemPosition: Int?) = scheme
        .flatMap { (type, count) -> (0 until count).map { type } }
        .toMutableList()
        .apply {
            if (soldItemPosition != null) {
                add(soldItemPosition, FeedItemType.SOLD_OFFER)
            }
        }

    private fun getFeedInfo(feedArgs: FeedArguments, pageIndex: Int): FeedInfo<OffersSearchRequest, OfferListingResult> {
        val pageOffersCount = if (pageIndex == 0) feedArgs.offersCountForPage1 else feedArgs.offersCountForPage2
        val offers = (0 until pageOffersCount).map { getMockedOffer() }
        return FeedInfo(
            feedState = getFeedState(
                feedArgs = feedArgs,
                request = OffersSearchRequest(
                    savedSearchId = null,
                    searchRequestByParams = SearchRequestByParams(
                        search = CarSearch(
                            carParams = CarParams(),
                            commonParams = CommonVehicleParams()
                        ),
                        context = SearchContext.LISTING,
                        sort = SearchSort.RELEVANCE
                    )
                ),
                pageIndex = pageIndex
            ),
            primaryFeedItems = offers.map { it.toViewModel() },
            result = OfferListingResult(
                offers = offers,
                pagination = Pagination(
                    page = pageIndex,
                    pageSize = PAGE_SIZE,
                    totalOffers = feedArgs.offersCountForPage1 + feedArgs.offersCountForPage2,
                    totalPages = -1
                ),
                savedSearchId = null,
                search = null,
                searchId = null,
                priceRange = null,
                yearRange = null,
                feedFlags = FeedFlags(showMatchApplicationForm = false)
            )
        )
    }

    private fun Offer.toViewModel() = OfferDataFeedItemModel(
        offer = this,
        isFavorite = false,
        isViewed = false,
        searchPosition = -1,
        hasMatchApplicationForm = false,
        options = emptyList(),
        note = null,
        isInComparison = null,
        searchType = SearchType.DefaultSearchType(),
        calculatorParams = null
    )


    private fun getMockedOffer(): Offer = Offer(
        category = VehicleCategory.CARS,
        id = "",
        sellerType = SellerType.PRIVATE
    )

    private fun getMockedPostFeedLoader() = object : IPostFeedLoader<OffersSearchRequest, OfferListingResult> {
        override fun load(
            feedInfo: FeedInfo<OffersSearchRequest, OfferListingResult>,
            pageContext: PageContext
        ): Observable<IDataFeedItemModel?> = Observable.empty()
    }

    private fun getFeedState(
        feedArgs: FeedArguments,
        request: OffersSearchRequest,
        pageIndex: Int
    ): IFeedState<OffersSearchRequest> {
        val page = Page(index = pageIndex, size = PAGE_SIZE)
        val soldItemPosProvider = SoldItemFeedRandomPositionProvider { from, until ->
            if (until <= from) null else min(until - 1, max(from, feedArgs.soldItemPosition))
        }
        return TestFeedState(
            totalCount = feedArgs.offersCountForPage1 + feedArgs.offersCountForPage2,
            currentCount = if (pageIndex == 0) {
                feedArgs.offersCountForPage1
            } else {
                feedArgs.offersCountForPage1 + feedArgs.offersCountForPage2
            },
            feedRequest = FeedRequest(request, page),
            soldItemPositionProvider = { soldItemPosProvider.getNextPosition(it) },
            isLastPage = pageIndex == feedArgs.lastPage - 1
        )
    }

    data class Arguments(
        val name: String,
        val feedArgs: FeedArguments,
        val expectedSchemePage1: List<Pair<FeedItemType, Int>> = DEFAULT_FIRST_PAGE_SCHEME,
        val expectedSoldItemPositionPage1: Int? = null,
        val expectedSchemePage2: List<Pair<FeedItemType, Int>> = DEFAULT_SECOND_PAGE_SCHEME,
        val expectedSoldItemPositionPage2: Int? = null
    ) {
        override fun toString(): String = name
    }

    data class FeedArguments(
        val soldItemPosition: Int,
        val offersCountForPage1: Int = 20,
        val offersCountForPage2: Int = 20,
        val lastPage: Int = 2
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index} - {0}")
        fun parameters(): Collection<Array<Any>> = listOf<Array<Any>>(
            arrayOf(
                Arguments(
                    name = "check sold offer on 13th position for first page",
                    feedArgs = FeedArguments(soldItemPosition = 13),
                    expectedSoldItemPositionPage1 = 20
                )
            ),
            arrayOf(
                Arguments(
                    name = "check sold offer on 13th position for first page",
                    feedArgs = FeedArguments(soldItemPosition = 13),
                    expectedSoldItemPositionPage1 = 20
                )
            ),
            arrayOf(
                Arguments(
                    name = "check sold offer on 14th position for first page",
                    feedArgs = FeedArguments(soldItemPosition = 14),
                    expectedSoldItemPositionPage1 = 20
                )
            ),
            arrayOf(
                Arguments(
                    name = "check sold offer on 15th position for first page",
                    feedArgs = FeedArguments(soldItemPosition = 15),
                    expectedSoldItemPositionPage1 = 21
                )
            ),
            arrayOf(
                Arguments(
                    name = "check sold offer on 16th position for first page",
                    feedArgs = FeedArguments(soldItemPosition = 16),
                    expectedSoldItemPositionPage1 = 22
                )
            ),
            arrayOf(
                Arguments(
                    name = "check sold offer on 17th position for first page",
                    feedArgs = FeedArguments(soldItemPosition = 17),
                    expectedSoldItemPositionPage1 = 23
                )
            ),
            arrayOf(
                Arguments(
                    name = "check sold offer on 18th position for first page",
                    feedArgs = FeedArguments(soldItemPosition = 18),
                    expectedSoldItemPositionPage1 = 25
                )
            ),
            arrayOf(
                Arguments(
                    name = "check sold offer on 19th position for first page",
                    feedArgs = FeedArguments(soldItemPosition = 19),
                    expectedSoldItemPositionPage1 = 26
                )
            ),
            arrayOf(
                Arguments(
                    name = "check no sold offer on 20th position on second page",
                    feedArgs = FeedArguments(soldItemPosition = 20),
                    expectedSoldItemPositionPage2 = 0
                )
            ),
            arrayOf(
                Arguments(
                    name = "check sold offer on 21th position for second page",
                    feedArgs = FeedArguments(soldItemPosition = 21),
                    expectedSoldItemPositionPage2 = 1
                )
            ),
            arrayOf(
                Arguments(
                    name = "check sold offer on 22th position for second page",
                    feedArgs = FeedArguments(soldItemPosition = 22),
                    expectedSoldItemPositionPage2 = 2
                )
            ),
            arrayOf(
                Arguments(
                    name = "check sold offer on 23th position for second page",
                    feedArgs = FeedArguments(soldItemPosition = 23),
                    expectedSoldItemPositionPage2 = 4
                )
            ),
            arrayOf(
                Arguments(
                    name = "check no sold offer on 14th position on first page, if page has only 13 offers",
                    feedArgs = FeedArguments(
                        soldItemPosition = 15,
                        offersCountForPage1 = 10,
                        offersCountForPage2 = 0,
                        lastPage = 1
                    ),
                    expectedSoldItemPositionPage1 = null,
                    expectedSchemePage1 = listOf(
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.FIRST_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.MATCH_APPLICATION to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.SECOND_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.LOAN_PROMO to 1,
                        FeedItemType.PRIMARY to 1,
                        FeedItemType.FIRST_FILTER_PROMO to 1
                    ),
                    expectedSchemePage2 = emptyList()
                )
            ),
            arrayOf(
                Arguments(
                    name = "check sold offer on 20th position inserted on first page at 15 offer, if page has only 15 offers",
                    feedArgs = FeedArguments(
                        soldItemPosition = 19,
                        offersCountForPage1 = 15,
                        offersCountForPage2 = 0,
                        lastPage = 1
                    ),
                    expectedSoldItemPositionPage1 = 20,
                    expectedSchemePage1 = listOf(
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.FIRST_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.MATCH_APPLICATION to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.SECOND_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.LOAN_PROMO to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.ODD_SPECIAL to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.THIRD_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.FIRST_FILTER_PROMO to 1
                    ),
                    expectedSchemePage2 = emptyList()
                )
            ),
            arrayOf(
                Arguments(
                    name = "check sold offer on 19th position inserted on first page at 15 offer, if page has only 15 offers",
                    feedArgs = FeedArguments(
                        soldItemPosition = 20,
                        offersCountForPage1 = 15,
                        offersCountForPage2 = 0,
                        lastPage = 1
                    ),
                    expectedSoldItemPositionPage1 = 20,
                    expectedSchemePage1 = listOf(
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.FIRST_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.MATCH_APPLICATION to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.SECOND_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.LOAN_PROMO to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.ODD_SPECIAL to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.THIRD_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.FIRST_FILTER_PROMO to 1
                    ),
                    expectedSchemePage2 = emptyList()
                )
            ),
            arrayOf(
                Arguments(
                    name = "check sold offer on 20th position inserted on first page at 15 offer, if page has only 15 offers",
                    feedArgs = FeedArguments(
                        soldItemPosition = 24,
                        offersCountForPage1 = 15,
                        offersCountForPage2 = 0,
                        lastPage = 1
                    ),
                    expectedSoldItemPositionPage1 = 20,
                    expectedSchemePage1 = listOf(
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.FIRST_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.MATCH_APPLICATION to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.SECOND_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.LOAN_PROMO to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.ODD_SPECIAL to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.THIRD_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.FIRST_FILTER_PROMO to 1
                    ),
                    expectedSchemePage2 = emptyList()
                )
            ),
            arrayOf(
                Arguments(
                    name = "check that 1 offer is inserted on the first page and 1 offer on the second",
                    feedArgs = FeedArguments(
                        soldItemPosition = 12,
                        offersCountForPage1 = 1,
                        offersCountForPage2 = 1,
                    ),
                    expectedSchemePage1 = listOf(
                        FeedItemType.PRIMARY to 1
                    ),
                    expectedSchemePage2 = listOf(
                        FeedItemType.PRIMARY to 1,
                        FeedItemType.FIRST_FILTER_PROMO to 1
                    )
                )
            ),
            arrayOf(
                Arguments(
                    name = "check that 1 offer is inserted on the first page and 2 offer on the second",
                    feedArgs = FeedArguments(
                        soldItemPosition = 12,
                        offersCountForPage1 = 1,
                        offersCountForPage2 = 2,
                    ),
                    expectedSchemePage1 = listOf(
                        FeedItemType.PRIMARY to 1,
                    ),
                    expectedSchemePage2 = listOf(
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.FIRST_AD to 1,
                        FeedItemType.FIRST_FILTER_PROMO to 1
                    )
                )
            ),
            arrayOf(
                Arguments(
                    name = "check that 1 offer is inserted on the first page and 2 offer on the second",
                    feedArgs = FeedArguments(
                        soldItemPosition = 12,
                        offersCountForPage1 = 2,
                        offersCountForPage2 = 1,
                    ),
                    expectedSchemePage1 = listOf(
                        FeedItemType.PRIMARY to 2,
                    ),
                    expectedSchemePage2 = listOf(
                        FeedItemType.PRIMARY to 1,
                        FeedItemType.FIRST_AD to 1,
                        FeedItemType.FIRST_FILTER_PROMO to 1
                    )
                )
            ),
            arrayOf(
                Arguments(
                    name = "check that 2 offer is inserted on the first page and 2 offer on the second",
                    feedArgs = FeedArguments(
                        soldItemPosition = 12,
                        offersCountForPage1 = 2,
                        offersCountForPage2 = 2,
                    ),
                    expectedSchemePage1 = listOf(
                        FeedItemType.PRIMARY to 2,
                    ),
                    expectedSchemePage2 = listOf(
                        FeedItemType.PRIMARY to 1,
                        FeedItemType.FIRST_AD to 1,
                        FeedItemType.PRIMARY to 1,
                        FeedItemType.FIRST_FILTER_PROMO to 1
                    )
                )
            ),
            arrayOf(
                Arguments(
                    name = "check that 3 offer is inserted on the first page and 2 offer on the second",
                    feedArgs = FeedArguments(
                        soldItemPosition = 12,
                        offersCountForPage1 = 3,
                        offersCountForPage2 = 2,
                    ),
                    expectedSchemePage1 = listOf(
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.FIRST_AD to 1
                    ),
                    expectedSchemePage2 = listOf(
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.FIRST_FILTER_PROMO to 1
                    )
                )
            ),
            arrayOf(
                Arguments(
                    name = "check that 3 offer is inserted on the first page and 3 offer on the second",
                    feedArgs = FeedArguments(
                        soldItemPosition = 12,
                        offersCountForPage1 = 3,
                        offersCountForPage2 = 3,
                    ),
                    expectedSchemePage1 = listOf(
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.FIRST_AD to 1
                    ),
                    expectedSchemePage2 = listOf(
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.MATCH_APPLICATION to 1,
                        FeedItemType.PRIMARY to 1,
                        FeedItemType.FIRST_FILTER_PROMO to 1
                    )
                )
            ),
            arrayOf(
                Arguments(
                    name = "check that 3 offer is inserted on the first page and 4 offer on the second",
                    feedArgs = FeedArguments(
                        soldItemPosition = 12,
                        offersCountForPage1 = 3,
                        offersCountForPage2 = 4,
                    ),
                    expectedSchemePage1 = listOf(
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.FIRST_AD to 1
                    ),
                    expectedSchemePage2 = listOf(
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.MATCH_APPLICATION to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.SECOND_AD to 1,
                        FeedItemType.FIRST_FILTER_PROMO to 1
                    )
                )
            ),
            arrayOf(
                Arguments(
                    name = "check that 4 offer is inserted on the first page and 3 offer on the second",
                    feedArgs = FeedArguments(
                        soldItemPosition = 12,
                        offersCountForPage1 = 4,
                        offersCountForPage2 = 3,
                    ),
                    expectedSchemePage1 = listOf(
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.FIRST_AD to 1,
                        FeedItemType.PRIMARY to 1
                    ),
                    expectedSchemePage2 = listOf(
                        FeedItemType.PRIMARY to 1,
                        FeedItemType.MATCH_APPLICATION to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.SECOND_AD to 1,
                        FeedItemType.FIRST_FILTER_PROMO to 1
                    )
                )
            ),
            arrayOf(
                Arguments(
                    name = "check that 4 offer is inserted on the first page and 4 offer on the second",
                    feedArgs = FeedArguments(
                        soldItemPosition = 12,
                        offersCountForPage1 = 4,
                        offersCountForPage2 = 4,
                    ),
                    expectedSchemePage1 = listOf(
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.FIRST_AD to 1,
                        FeedItemType.PRIMARY to 1
                    ),
                    expectedSchemePage2 = listOf(
                        FeedItemType.PRIMARY to 1,
                        FeedItemType.MATCH_APPLICATION to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.SECOND_AD to 1,
                        FeedItemType.PRIMARY to 1,
                        FeedItemType.FIRST_FILTER_PROMO to 1
                    )
                )
            ),
            arrayOf(
                Arguments(
                    name = "check that 10 offer is inserted on the first page and 5 offer on the second",
                    feedArgs = FeedArguments(
                        soldItemPosition = 14,
                        offersCountForPage1 = 10,
                        offersCountForPage2 = 5,
                    ),
                    expectedSoldItemPositionPage2 = 6,
                    expectedSchemePage1 = listOf(
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.FIRST_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.MATCH_APPLICATION to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.SECOND_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.LOAN_PROMO to 1,
                        FeedItemType.PRIMARY to 1
                    ),
                    expectedSchemePage2 = listOf(
                        FeedItemType.PRIMARY to 1,
                        FeedItemType.ODD_SPECIAL to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.THIRD_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.FIRST_FILTER_PROMO to 1
                    )
                )
            ),
            arrayOf(
                Arguments(
                    name = "check that 5 offer is inserted on the first page and 10 offer on the second",
                    feedArgs = FeedArguments(
                        soldItemPosition = 12,
                        offersCountForPage1 = 5,
                        offersCountForPage2 = 10,
                    ),
                    expectedSoldItemPositionPage2 = 13,
                    expectedSchemePage1 = listOf(
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.FIRST_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.MATCH_APPLICATION to 1
                    ),
                    expectedSchemePage2 = listOf(
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.SECOND_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.LOAN_PROMO to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.ODD_SPECIAL to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.THIRD_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.FIRST_FILTER_PROMO to 1
                    )
                )
            ),
            arrayOf(
                Arguments(
                    name = "check that 10 offer is inserted on the first page and 10 offer on the second",
                    feedArgs = FeedArguments(
                        soldItemPosition = 12,
                        offersCountForPage1 = 10,
                        offersCountForPage2 = 10,
                    ),
                    expectedSoldItemPositionPage2 = 6,
                    expectedSchemePage1 = listOf(
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.FIRST_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.MATCH_APPLICATION to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.SECOND_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.LOAN_PROMO to 1,
                        FeedItemType.PRIMARY to 1
                    ),
                    expectedSchemePage2 = listOf(
                        FeedItemType.PRIMARY to 1,
                        FeedItemType.ODD_SPECIAL to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.THIRD_AD to 1,
                        FeedItemType.PRIMARY to 5,
                        FeedItemType.VIDEO to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.FIRST_FILTER_PROMO to 1
                    )
                )
            ),
            arrayOf(
                Arguments(
                    name = "check that 15 offer is inserted on the first page and 5 offer on the second",
                    feedArgs = FeedArguments(
                        soldItemPosition = 14,
                        offersCountForPage1 = 15,
                        offersCountForPage2 = 5,
                    ),
                    expectedSoldItemPositionPage1 = 20,
                    expectedSchemePage1 = listOf(
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.FIRST_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.MATCH_APPLICATION to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.SECOND_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.LOAN_PROMO to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.ODD_SPECIAL to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.THIRD_AD to 1,
                        FeedItemType.PRIMARY to 2
                    ),
                    expectedSchemePage2 = listOf(
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.VIDEO to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.FIRST_FILTER_PROMO to 1
                    )
                )
            ),
            arrayOf(
                Arguments(
                    name = "check that 5 offer is inserted on the first page and 15 offer on the second",
                    feedArgs = FeedArguments(
                        soldItemPosition = 12,
                        offersCountForPage1 = 5,
                        offersCountForPage2 = 15,
                    ),
                    expectedSoldItemPositionPage2 = 13,
                    expectedSchemePage1 = listOf(
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.FIRST_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.MATCH_APPLICATION to 1
                    ),
                    expectedSchemePage2 = listOf(
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.SECOND_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.LOAN_PROMO to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.ODD_SPECIAL to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.THIRD_AD to 1,
                        FeedItemType.PRIMARY to 5,
                        FeedItemType.VIDEO to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.FIRST_FILTER_PROMO to 1
                    )
                )
            ),
            arrayOf(
                Arguments(
                    name = "check that 15 offer is inserted on the first page and 6 offer on the second",
                    feedArgs = FeedArguments(
                        soldItemPosition = 14,
                        offersCountForPage1 = 15,
                        offersCountForPage2 = 6,
                    ),
                    expectedSoldItemPositionPage1 = 20,
                    expectedSchemePage1 = listOf(
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.FIRST_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.MATCH_APPLICATION to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.SECOND_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.LOAN_PROMO to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.ODD_SPECIAL to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.THIRD_AD to 1,
                        FeedItemType.PRIMARY to 2
                    ),
                    expectedSchemePage2 = listOf(
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.VIDEO to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.FIRST_FILTER_PROMO to 1,
                        FeedItemType.PRIMARY to 1
                    )
                )
            ),
            arrayOf(
                Arguments(
                    name = "check that 15 offer is inserted on the first page and 7 offer on the second",
                    feedArgs = FeedArguments(
                        soldItemPosition = 14,
                        offersCountForPage1 = 15,
                        offersCountForPage2 = 7,
                    ),
                    expectedSoldItemPositionPage1 = 20,
                    expectedSchemePage1 = listOf(
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.FIRST_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.MATCH_APPLICATION to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.SECOND_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.LOAN_PROMO to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.ODD_SPECIAL to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.THIRD_AD to 1,
                        FeedItemType.PRIMARY to 2
                    ),
                    expectedSchemePage2 = listOf(
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.VIDEO to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.FIRST_FILTER_PROMO to 1,
                        FeedItemType.PRIMARY to 2
                    )
                )
            ),
            arrayOf(
                Arguments(
                    name = "check that 15 offer is inserted on the first page and 8 offer on the second",
                    feedArgs = FeedArguments(
                        soldItemPosition = 14,
                        offersCountForPage1 = 15,
                        offersCountForPage2 = 8,
                    ),
                    expectedSoldItemPositionPage1 = 20,
                    expectedSchemePage1 = listOf(
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.FIRST_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.MATCH_APPLICATION to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.SECOND_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.LOAN_PROMO to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.ODD_SPECIAL to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.THIRD_AD to 1,
                        FeedItemType.PRIMARY to 2
                    ),
                    expectedSchemePage2 = listOf(
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.VIDEO to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.FIRST_FILTER_PROMO to 1,
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.FIRST_AD to 1
                    )
                )
            ),
            arrayOf(
                Arguments(
                    name = "check that 15 offer is inserted on the first page and 9 offer on the second",
                    feedArgs = FeedArguments(
                        soldItemPosition = 14,
                        offersCountForPage1 = 15,
                        offersCountForPage2 = 9,
                    ),
                    expectedSoldItemPositionPage1 = 20,
                    expectedSchemePage1 = listOf(
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.FIRST_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.MATCH_APPLICATION to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.SECOND_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.LOAN_PROMO to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.ODD_SPECIAL to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.THIRD_AD to 1,
                        FeedItemType.PRIMARY to 2
                    ),
                    expectedSchemePage2 = listOf(
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.VIDEO to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.FIRST_FILTER_PROMO to 1,
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.FIRST_AD to 1,
                        FeedItemType.PRIMARY to 1
                    )
                )
            ),
            arrayOf(
                Arguments(
                    name = "check that 15 offer is inserted on the first page and 10 offer on the second",
                    feedArgs = FeedArguments(
                        soldItemPosition = 14,
                        offersCountForPage1 = 15,
                        offersCountForPage2 = 10,
                    ),
                    expectedSoldItemPositionPage1 = 20,
                    expectedSchemePage1 = listOf(
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.FIRST_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.MATCH_APPLICATION to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.SECOND_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.LOAN_PROMO to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.ODD_SPECIAL to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.THIRD_AD to 1,
                        FeedItemType.PRIMARY to 2
                    ),
                    expectedSchemePage2 = listOf(
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.VIDEO to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.FIRST_FILTER_PROMO to 1,
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.FIRST_AD to 1,
                        FeedItemType.PRIMARY to 2
                    )
                )
            ),
            arrayOf(
                Arguments(
                    name = "check that 15 offer is inserted on the first page and 15 offer on the second",
                    feedArgs = FeedArguments(
                        soldItemPosition = 14,
                        offersCountForPage1 = 15,
                        offersCountForPage2 = 15,
                    ),
                    expectedSoldItemPositionPage1 = 20,
                    expectedSchemePage1 = listOf(
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.FIRST_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.MATCH_APPLICATION to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.SECOND_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.LOAN_PROMO to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.ODD_SPECIAL to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.THIRD_AD to 1,
                        FeedItemType.PRIMARY to 2
                    ),
                    expectedSchemePage2 = listOf(
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.VIDEO to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.FIRST_FILTER_PROMO to 1,
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.FIRST_AD to 1,
                        FeedItemType.PRIMARY to 4,
                        FeedItemType.SECOND_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.LOAN_PROMO to 1,
                        FeedItemType.PRIMARY to 1
                    )
                )
            ),
            arrayOf(
                Arguments(
                    name = "check that 15 offer is inserted on the first page and 20 offer on the second",
                    feedArgs = FeedArguments(
                        soldItemPosition = 14,
                        offersCountForPage1 = 15,
                        offersCountForPage2 = 20,
                    ),
                    expectedSoldItemPositionPage1 = 20,
                    expectedSchemePage1 = listOf(
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.FIRST_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.MATCH_APPLICATION to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.SECOND_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.LOAN_PROMO to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.ODD_SPECIAL to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.THIRD_AD to 1,
                        FeedItemType.PRIMARY to 2
                    ),
                    expectedSchemePage2 = listOf(
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.VIDEO to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.FIRST_FILTER_PROMO to 1,
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.FIRST_AD to 1,
                        FeedItemType.PRIMARY to 4,
                        FeedItemType.SECOND_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.LOAN_PROMO to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.RECOMMENDED_CARS to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.THIRD_AD to 1,
                        FeedItemType.PRIMARY to 2
                    )
                )
            ),
            arrayOf(
                Arguments(
                    name = "check that 15 offer is inserted on the first page and 24 offer on the second",
                    feedArgs = FeedArguments(
                        soldItemPosition = 14,
                        offersCountForPage1 = 15,
                        offersCountForPage2 = 24,
                    ),
                    expectedSoldItemPositionPage1 = 20,
                    expectedSchemePage1 = listOf(
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.FIRST_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.MATCH_APPLICATION to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.SECOND_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.LOAN_PROMO to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.ODD_SPECIAL to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.THIRD_AD to 1,
                        FeedItemType.PRIMARY to 2
                    ),
                    expectedSchemePage2 = listOf(
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.VIDEO to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.FIRST_FILTER_PROMO to 1,
                        FeedItemType.PRIMARY to 3,
                        FeedItemType.FIRST_AD to 1,
                        FeedItemType.PRIMARY to 4,
                        FeedItemType.SECOND_AD to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.LOAN_PROMO to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.RECOMMENDED_CARS to 1,
                        FeedItemType.PRIMARY to 2,
                        FeedItemType.THIRD_AD to 1,
                        FeedItemType.PRIMARY to 5,
                        FeedItemType.VIDEO to 1,
                        FeedItemType.PRIMARY to 1
                    )
                )
            )
        )

        private val DEFAULT_FIRST_PAGE_SCHEME = listOf(
            FeedItemType.PRIMARY to 3,
            FeedItemType.FIRST_AD to 1,
            FeedItemType.PRIMARY to 2,
            FeedItemType.MATCH_APPLICATION to 1,
            FeedItemType.PRIMARY to 2,
            FeedItemType.SECOND_AD to 1,
            FeedItemType.PRIMARY to 2,
            FeedItemType.LOAN_PROMO to 1,
            FeedItemType.PRIMARY to 2,
            FeedItemType.ODD_SPECIAL to 1,
            FeedItemType.PRIMARY to 2,
            FeedItemType.THIRD_AD to 1,
            FeedItemType.PRIMARY to 5,
            FeedItemType.VIDEO to 1,
            FeedItemType.PRIMARY to 2,
            FeedItemType.FIRST_FILTER_PROMO to 1
        )

        private val DEFAULT_SECOND_PAGE_SCHEME = listOf(
            FeedItemType.PRIMARY to 3,
            FeedItemType.FIRST_AD to 1,
            FeedItemType.PRIMARY to 4,
            FeedItemType.SECOND_AD to 1,
            FeedItemType.PRIMARY to 2,
            FeedItemType.LOAN_PROMO to 1,
            FeedItemType.PRIMARY to 2,
            FeedItemType.RECOMMENDED_CARS to 1,
            FeedItemType.PRIMARY to 2,
            FeedItemType.THIRD_AD to 1,
            FeedItemType.PRIMARY to 5,
            FeedItemType.VIDEO to 1,
            FeedItemType.PRIMARY to 2,
            FeedItemType.EVEN_SPECIAL to 1
        )
    }
}

const val NOT_INTENDED = "doesn't use these for now"

@Suppress("NotImplementedDeclaration")
data class TestFeedState(
    override var totalCount: Int,
    private val currentCount: Int,
    private val feedRequest: FeedRequest<OffersSearchRequest>,
    private val isLastPage: Boolean = false,
    private val isRequestCompletedNow: Boolean = false,
    private val soldItemPositionProvider: (size: Int) -> Int?,
) : IFeedState<OffersSearchRequest> {

    private var soldItemPositionInitialized = false
    private var soldItemPosition: Int? = null

    override fun getPage() = feedRequest.page

    override fun isLastPage() = isLastPage

    override fun getActualRequest() = feedRequest

    override fun getMainRequest() = feedRequest

    override fun increasePage() = throw NotImplementedError(NOT_INTENDED)

    override fun updateActualRequest(totalCount: Int, totalPages: Int) = throw NotImplementedError(NOT_INTENDED)

    override fun initRequest(request: OffersSearchRequest) = throw NotImplementedError(NOT_INTENDED)

    override fun addRequests(requests: List<OffersSearchRequest>) = throw NotImplementedError(NOT_INTENDED)

    override fun isAdditionalRequest(): Boolean = false

    override fun isRequestCompletedNow(requestPosition: Int) = isRequestCompletedNow

    override fun getCurrentCount(): Int = currentCount

    override fun addCurrentCount(count: Int) = throw NotImplementedError(NOT_INTENDED)

    override fun getSoldItemPosition(): Int? {
        if (!soldItemPositionInitialized) {
            soldItemPositionInitialized = true
            soldItemPosition = soldItemPositionProvider(totalCount)
        }
        return soldItemPosition
    }

    override fun getAndIncSpecialsPage(): Int = 0

    override fun getAndIncAdsPage(): Int = 0

    override fun getAndIncVideosPage(): Int = 0
}
