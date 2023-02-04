package ru.auto.ara.network.api.converter.search.request

import com.yandex.mobile.vertical.dynamicscreens.model.Screen
import ru.auto.ara.consts.Filters
import ru.auto.ara.data.entities.Category
import ru.auto.ara.data.entities.form.ColorItem
import ru.auto.ara.data.entities.form.Option
import ru.auto.ara.data.models.FormState
import ru.auto.ara.filter.FilterScreenFactory
import ru.auto.ara.screens.mapper.FieldWithValueToFieldStateMapper
import ru.auto.ara.screens.mapper.ScreenToFormStateMapper
import ru.auto.ara.screens.serializers.IFormStateScreenSerializer
import ru.auto.ara.util.android.OptionsProvider
import ru.auto.ara.util.android.StringsProvider
import ru.auto.ara.util.toListOfPairs
import ru.auto.data.util.AGRICULTURAL_SUB_CATEGORY_OLD_ID
import ru.auto.data.util.ATVS_SUB_CATEGORY_OLD_ID
import ru.auto.data.util.AUTOLOADER_SUB_CATEGORY_OLD_ID
import ru.auto.data.util.BULLDOZERS_SUB_CATEGORY_OLD_ID
import ru.auto.data.util.BUS_SUB_CATEGORY_OLD_ID
import ru.auto.data.util.CONSTRUCTION_SUB_CATEGORY_OLD_ID
import ru.auto.data.util.CRANE_HYDRAULICS_SUB_CATEGORY_OLD_ID
import ru.auto.data.util.CRANE_SUB_CATEGORY_OLD_ID
import ru.auto.data.util.DREDGE_SUB_CATEGORY_OLD_ID
import ru.auto.data.util.LIGHT_COMMERCIAL_SUB_CATEGORY_OLD_ID
import ru.auto.data.util.MOTO_SUB_CATEGORY_OLD_ID
import ru.auto.data.util.MUNICIPAL_SUB_CATEGORY_OLD_ID
import ru.auto.data.util.SCOOTERS_SUB_CATEGORY_OLD_ID
import ru.auto.data.util.SNOWMOBILES_SUB_CATEGORY_OLD_ID
import ru.auto.data.util.TRAILER_SUB_CATEGORY_OLD_ID
import ru.auto.data.util.TRUCK_SUB_CATEGORY_OLD_ID
import ru.auto.data.util.TRUCK_TRACTOR_SUB_CATEGORY_OLD_ID
import rx.functions.Func1

/**
 * very sad impl, manual dependency creation =(
 */
internal object FormstateFilterPairsExtractor {
    private val DUMB_TAG = "DUMB_TAG"

    operator fun invoke(formState: FormState, subcategory: String?): List<Pair<String, String>> {
        val stubScreenSerializer = object : IFormStateScreenSerializer {
            override fun restore(screen: Screen, formState: FormState): Screen = TODO("not needed")
        }

        val dumbStringProvider = object : StringsProvider {
            override fun plural(pluralRes: Int, count: Int, zeroResource: Int): String {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun plural(pluralRes: Int, count: Int): String = ""
            override fun plural(pluralRes: Int, count: Int, vararg formatArgs: Any?): String = ""
            override fun get(stringRes: Int): String = ""
            override fun get(stringRes: Int, vararg formatArgs: Any?): String = ""
        }

        val colorsOptionsProvider = OptionsProvider<ColorItem> { listOf(ColorItem("", "", "")) }
        val categoryOptionsProvider = OptionsProvider<Category> {
            listOf(
                TRAILER_SUB_CATEGORY_OLD_ID,
                BUS_SUB_CATEGORY_OLD_ID,
                TRUCK_SUB_CATEGORY_OLD_ID,
                TRUCK_TRACTOR_SUB_CATEGORY_OLD_ID,
                LIGHT_COMMERCIAL_SUB_CATEGORY_OLD_ID,

                AGRICULTURAL_SUB_CATEGORY_OLD_ID,
                CONSTRUCTION_SUB_CATEGORY_OLD_ID,
                AUTOLOADER_SUB_CATEGORY_OLD_ID,
                CRANE_SUB_CATEGORY_OLD_ID,
                DREDGE_SUB_CATEGORY_OLD_ID,
                BULLDOZERS_SUB_CATEGORY_OLD_ID,
                CRANE_HYDRAULICS_SUB_CATEGORY_OLD_ID,
                MUNICIPAL_SUB_CATEGORY_OLD_ID,

                ATVS_SUB_CATEGORY_OLD_ID,
                MOTO_SUB_CATEGORY_OLD_ID,
                SCOOTERS_SUB_CATEGORY_OLD_ID,
                SNOWMOBILES_SUB_CATEGORY_OLD_ID
            ).map { Category(it) }
        }
        val multiselectOptionsProvider =
            Func1<String, OptionsProvider<Option>> {
                OptionsProvider {
                    listOf(
                        Option(Filters.BEATEN_FIELD, ""),
                        Option("", "")
                    )
                }
            }

        val filterScreenFactory = FilterScreenFactory(
            stubScreenSerializer,
            dumbStringProvider,
            colorsOptionsProvider,
            categoryOptionsProvider,
            multiselectOptionsProvider,
        )

        val filterScreen = filterScreenFactory.create(DUMB_TAG, subcategory).toBlocking().value()
        ScreenToFormStateMapper(FieldWithValueToFieldStateMapper()).inflateScreen(filterScreen, formState)

        return filterScreen.searchParams.toListOfPairs()
    }
}
