package ru.yandex.yandexnavi.projected.testapp.impl

import android.graphics.Color
import android.net.Uri
import com.yandex.mapkit.geometry.Point
import com.yandex.navikit.GeoObjectUtils
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import ru.yandex.yandexnavi.projected.platformkit.dependencies.SearchApi
import ru.yandex.yandexnavi.projected.platformkit.dependencies.SearchApiState
import ru.yandex.yandexnavi.projected.testapp.R

class SearchApiImpl : SearchApi {
    private var state: SearchApiState = SearchApiState()

    private val history = listOf(
        SearchApiState.SuggestEntry.History(id = "1", name = "Заправки"),
        SearchApiState.SuggestEntry.History(id = "2", name = "ул. Льва Толстого 16"),
        SearchApiState.SuggestEntry.History(id = "3", name = "Республика Татарстан, Казань, ул. Карамыз д 62"),
        SearchApiState.SuggestEntry.History(id = "4", name = "Еда"),
        SearchApiState.SuggestEntry.History(id = "5", name = "ул. Набережная им. Габдуллы Тукая")
    )

    private val results = listOf(
        createSearchResult("Заправки", null, 2000.0),
        createSearchResult("ул. Льва Толстого 16", null, 3000.0),
        createSearchResult("Республика Татарстан, Казань, ул. Карамыз д 62", null, 5000.0),
        createSearchResult("У Бориса", "Кафе", 5600.0),
        createSearchResult("I Like Wine", "Кафе", 10000.0)
    )

    private val statesSubject: BehaviorSubject<SearchApiState> = BehaviorSubject.create()

    private fun setState(state: SearchApiState) {
        this.state = state
        statesSubject.onNext(state)
    }

    private fun createSearchResult(name: String, category: String?, distanceInMeters: Double): SearchApiState.SearchResult {
        return SearchApiState.SearchResult(
            name,
            category,
            distanceInMeters,
            GeoObjectUtils.createGeoObject(name, "description", Point(55.735520, 37.642474)),
            reqId = "",
        )
    }

    override val states: Observable<SearchApiState>
        get() = this.statesSubject
    override val currentState: SearchApiState
        get() = this.state

    override fun startSearch() {
        setState(
            state.copy(
                categories = SearchApiState.StatePart.Success(
                    listOf(
                        SearchApiState.Category(
                            id = "1",
                            name = "Заправки",
                            icon = SearchApiState.Category.Icon.ByRubric(
                                icon = R.drawable.yandexmaps_rubrics_services_gasstation_24,
                                iconTintColor = Color.WHITE,
                                backgroundColor = Color.parseColor("#9C89FA")
                            ),
                            isAd = false
                        ),
                        SearchApiState.Category(
                            id = "2",
                            name = "Где поесть",
                            icon = SearchApiState.Category.Icon.ByRubric(
                                icon = R.drawable.yandexmaps_rubrics_food_drink_restaurants_24,
                                iconTintColor = Color.WHITE,
                                backgroundColor = Color.parseColor("#FFA15E")
                            ),
                            isAd = false
                        ),
                        SearchApiState.Category(
                            id = "3",
                            name = "Продукты",
                            icon = SearchApiState.Category.Icon.ByRubric(
                                icon = R.drawable.yandexmaps_rubrics_shopping_supermarket_24,
                                iconTintColor = null,
                                backgroundColor = null
                            ),
                            isAd = false
                        ),
                        SearchApiState.Category(
                            id = "4",
                            name = "Burger King",
                            icon = SearchApiState.Category.Icon.ByDrawable(
                                icon = R.drawable.projected_kit_aa_search_ads_burgerking_44
                            ),
                            isAd = true
                        ),
                        SearchApiState.Category(
                            id = "5",
                            name = "Macdonalds",
                            icon = SearchApiState.Category.Icon.ByDrawable(
                                icon = R.drawable.projected_kit_aa_search_ads_macdonalds_44
                            ),
                            isAd = true
                        ),
                        SearchApiState.Category(
                            id = "6",
                            name = "ПИК",
                            icon = SearchApiState.Category.Icon.ByUri(uri = Uri.parse("https://example.com/image.png")),
                            isAd = true
                        )
                    )
                )
            )
        )
    }

    override fun goToSearchResults(category: SearchApiState.Category) {
        setState(state.copy(results = SearchApiState.StatePart.Success(results.shuffled())))
    }

    override fun goToSearchResults(suggestEntry: SearchApiState.SuggestEntry) {
        setState(state.copy(results = SearchApiState.StatePart.Success(results.shuffled())))
    }

    override fun goToSearchResults() {
        setState(state.copy(results = SearchApiState.StatePart.Success(results.shuffled())))
    }

    override fun backToSuggest() {
    }

    override fun exitSearch() {
        setState(SearchApiState())
    }

    override fun goToSearchInput() {
        setState(state.copy(suggest = SearchApiState.StatePart.Success(history)))
    }

    override fun prepareSuggest(query: String) {
        if (query.isEmpty()) {
            return setState(state.copy(suggest = SearchApiState.StatePart.Success(history)))
        }
        setState(
            state.copy(
                suggest = SearchApiState.StatePart.Success(
                    listOf(
                        SearchApiState.SuggestEntry.Ordinary(id = "1", name = "Заправки", icon = null),
                        SearchApiState.SuggestEntry.History(id = "2", name = "ул. Льва Толстого 16"),
                        SearchApiState.SuggestEntry.History(id = "3", name = "Республика Татарстан, Казань, ул. Карамыз д 62"),
                        SearchApiState.SuggestEntry.Ordinary(id = "4", name = "Еда", icon = null),
                        SearchApiState.SuggestEntry.Ordinary(id = "5", name = "ул. Набережная им. Габдуллы Тукая", icon = null)
                    )
                        .shuffled()
                )
            )
        )
    }
}
