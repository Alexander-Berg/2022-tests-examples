package ru.auto.ara.filter.screen.user

import io.qameta.allure.kotlin.junit4.AllureRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.auto.ara.consts.Filters.GLOBAL_CATEGORY_FIELD
import ru.auto.ara.consts.Filters.SERVICES_FIELD
import ru.auto.ara.consts.Filters.STATE_FIELD
import ru.auto.ara.consts.Filters.STATUS_FIELD
import ru.auto.ara.data.entities.form.Option
import ru.auto.ara.filter.fields.BaseSegmentField
import ru.auto.ara.filter.fields.GlobalCategoryField
import ru.auto.ara.filter.screen.FilterScreen
import ru.auto.ara.util.android.OptionsProvider
import ru.auto.ara.util.android.StringsProvider
import ru.auto.data.model.Campaign
import ru.auto.data.model.data.offer.ALL
import ru.auto.data.model.data.offer.CAR
import ru.auto.data.model.data.offer.NEW
import ru.auto.data.model.data.offer.USED

/**
 * @author aleien on 16.07.18.
 */
@RunWith(AllureRunner::class) class DealerOffersFilterBuilderTest {
    private val strings: StringsProvider = mock()
    private val options: OptionsProvider<Option> = mock()

    @Before
    fun setup() {
        whenever(strings.get(any())).thenReturn("Label")
        whenever(options.get(any())).thenReturn(listOf(Option("a", "b")))
    }

    // incorrect input section
    @Test
    fun `given categories in incorrect case should set correct values to category field`() {
        val screen = buildScreen(CampaignFactory.buildCarUsed(true))
        assertThat((screen.getFieldById(GLOBAL_CATEGORY_FIELD) as GlobalCategoryField).itemsByKeys).containsOnlyKeys(CAR)
    }

    @Ignore
    @Test
    fun `given categories in incorrect case should set correct values to section field`() {
        val screen = buildScreen(CampaignFactory.buildCarUsed(true))
        assertThat((screen.getFieldById(STATE_FIELD) as BaseSegmentField).itemsByKeys).containsOnlyKeys(USED)
    }

    @Test
    fun `given no campaigns should not show category field`() {
        val screen = buildScreen(emptyList())
        assertThat(screen.getFieldById(GLOBAL_CATEGORY_FIELD).isHidden).isTrue()
    }

    @Test
    fun `given no campaigns should have "all" root category`() {
        val screen = buildScreen(emptyList())
        assertThat(screen.rootCategoryId).isEqualTo(ALL)
    }

    @Test
    fun `given no campaigns should not show state field`() {
        val screen = buildScreen(emptyList())
        assertThat(screen.getFieldById(STATE_FIELD).isHidden).isTrue()
    }

    // fields hidden state section
    @Test
    fun `given only one campaign should not show category field`() {
        val screen = buildScreen(CampaignFactory.buildCarUsed())
        assertThat(screen.getFieldById(GLOBAL_CATEGORY_FIELD).isHidden).isTrue()
    }

    @Test
    fun `given several category campaigns should show category field`() {
        val screen =
            DealerFilterScreen.Builder(ALL, strings, options, CampaignFactory.buildCarMoto(), emptyList(), emptyList())
                .build()
        assertThat(screen.getFieldById(GLOBAL_CATEGORY_FIELD).isHidden).isFalse()
    }

    @Test
    fun `given only used sections should not show state field`() {
        val screen = buildScreen(CampaignFactory.buildCarUsed())
        assertThat(screen.getFieldById(STATE_FIELD).isHidden).isTrue()
    }

    @Test
    fun `given several sections for category should show state field`() {
        val screen = buildScreen(CampaignFactory.buildCarAll())
        screen.getValueFieldById<String>(GLOBAL_CATEGORY_FIELD).value = CAR
        assertThat(screen.getFieldById(STATE_FIELD).isHidden).isFalse()
    }

    @Test
    fun `given both new and used sections should also show 'all' option in state field`() {
        val screen = buildScreen(CampaignFactory.buildCarAll())
        screen.getValueFieldById<String>(GLOBAL_CATEGORY_FIELD).value = CAR
        assertThat((screen.getFieldById(STATE_FIELD) as BaseSegmentField).itemsByKeys.toList().map { it.first }).containsExactly(
            ALL,
            NEW,
            USED
        )
    }

    @Test
    fun `given only one option state field should have it as option`() {
        val screen = buildScreen(CampaignFactory.buildCarUsed())
        screen.getValueFieldById<String>(GLOBAL_CATEGORY_FIELD).value = CAR
        assertThat((screen.getFieldById(STATE_FIELD) as BaseSegmentField).itemsByKeys).containsOnlyKeys(USED)
    }

    @Test
    fun `status field should get options from options provider`() {
        buildScreen(CampaignFactory.buildCarUsed())
        verify(options).get(STATUS_FIELD)
    }

    @Test
    fun `services field should get options from options provider`() {
        buildScreen(CampaignFactory.buildCarUsed())
        verify(options).get(SERVICES_FIELD)
    }

    private fun buildScreen(campaigns: List<Campaign>, category: String? = null): FilterScreen =
        DealerFilterScreen.Builder(
            category ?: campaigns.firstOrNull()?.category ?: ALL,
            strings, options,
            campaigns,
            emptyList(),
            emptyList()
        ).build()
}
