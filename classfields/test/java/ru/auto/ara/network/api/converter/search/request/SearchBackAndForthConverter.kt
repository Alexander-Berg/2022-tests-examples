package ru.auto.ara.network.api.converter.search.request

import ru.auto.ara.data.models.FormState
import ru.auto.ara.filter.mapper.VehicleSearchExtractor
import ru.auto.ara.ui.helpers.form.util.VehicleSearchToFormStateConverter
import ru.auto.data.model.filter.VehicleSearch
import ru.auto.data.model.network.scala.search.NWSearchRequestParams
import ru.auto.data.model.network.scala.search.NWSearchVehicleCategory
import ru.auto.data.model.network.scala.search.NWSearchView
import ru.auto.data.model.network.scala.search.converter.VehicleSearchConverter

/**
 *
 * @author jagger on 28.04.18.
 */
object SearchBackAndForthConverter {

    fun invoke(
            category: NWSearchVehicleCategory,
            oldCategory: String?,
            searchRequest: NWSearchRequestParams,
            view: NWSearchView
    ): NWSearchRequestParams {
        val search: VehicleSearch = VehicleSearchConverter.fromNetwork(category, searchRequest, view)
        val formState: FormState = VehicleSearchToFormStateConverter.convert(search)
        val formParams: List<Pair<String, String>> = FormstateFilterPairsExtractor(formState, oldCategory)
        val convertedSearch: VehicleSearch = VehicleSearchExtractor.createSearch(formParams, null)
        return VehicleSearchConverter.toNetwork(convertedSearch)
    }
}
