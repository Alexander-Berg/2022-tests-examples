package ru.yandex.market.clean.data.source.cms

import com.annimon.stream.Optional
import io.reactivex.Observable
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.junit.Test
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.eq
import ru.beru.android.R
import ru.yandex.market.clean.domain.model.cms.IconType
import ru.yandex.market.clean.domain.model.cms.LinkButtonItem
import ru.yandex.market.clean.domain.model.cms.garson.StaticDataType
import ru.yandex.market.domain.models.region.Country
import ru.yandex.market.domain.models.region.DeliveryLocality
import ru.yandex.market.internal.PreferencesDataStore
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.util.help.HelpLink

class CmsStaticDataSourceTest {

    private val deliveryLocality = mock<DeliveryLocality> {
        on { country } doReturn Country.UNKNOWN
    }

    private val preferenceDataStore = mock<PreferencesDataStore> {
        on { selectedDeliveryLocalityStream } doReturn Observable.just(Optional.of(deliveryLocality))
    }

    private val resourcesDataStore = mock<ResourcesManager> {
        on { getString(R.string.how_to_get) } doReturn FIRST_BUTTON_TEXT
        on { getString(R.string.how_to_use) } doReturn SECOND_BUTTON_TEXT
        on { getFormattedString(eq(R.string.smart_shopping_how_to_get_link), anyVararg()) } doReturn FIRST_BUTTON_URL
        on { getFormattedString(eq(R.string.smart_shopping_how_to_spent_link), anyVararg()) } doReturn SECOND_BUTTON_URL
    }

    private val helpLink = HelpLink(resourcesDataStore)

    private val dataSource = CmsStaticDataSource(preferenceDataStore, resourcesDataStore, helpLink)

    @Test
    fun `Get smart coins link buttons data`() {
        dataSource.getStaticData(StaticDataType.SMART_COINS_LINK_BUTTONS, true)
            .test()
            .assertNoErrors()
            .assertValue(EXPECTED_LINK_BUTTONS_DATA)
    }

    companion object {
        private const val FIRST_BUTTON_ID = "HOW-TO-GET"
        private const val SECOND_BUTTON_ID = "HOW-TO-USE"
        private const val FIRST_BUTTON_TEXT = "how to get"
        private const val SECOND_BUTTON_TEXT = "how to use"
        private const val FIRST_BUTTON_URL = "https://beru.ru/first.button"
        private const val SECOND_BUTTON_URL = "https://beru.ru/second.button"
        private val EXPECTED_LINK_BUTTONS_DATA = listOf(
            LinkButtonItem(FIRST_BUTTON_ID, FIRST_BUTTON_TEXT, FIRST_BUTTON_URL, IconType.STAR),
            LinkButtonItem(SECOND_BUTTON_ID, SECOND_BUTTON_TEXT, SECOND_BUTTON_URL, IconType.BASKET)
        )
    }
}