package ru.yandex.market.clean.data.mapper

import org.assertj.core.api.Assertions
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.data.fapi.dto.livestreams.LiveStreamMergedDto
import ru.yandex.market.clean.data.fapi.dto.white.WhiteFrontApiImageDto
import ru.yandex.market.clean.data.fapi.dto.white.WhiteFrontApiLiveStreamConfigDto
import ru.yandex.market.clean.data.fapi.dto.white.WhiteFrontApiLiveStreamContentDto
import ru.yandex.market.clean.data.fapi.dto.white.WhiteFrontApiLiveStreamPresenterDto
import ru.yandex.market.clean.data.fapi.dto.white.WhiteFrontApiLiveStreamPromoInfoDto
import ru.yandex.market.clean.data.mapper.sku.DetailedSkuMapper
import ru.yandex.market.clean.domain.model.livestream.LiveStreamContent
import ru.yandex.market.common.datetimeparser.DateTimeParser
import ru.yandex.market.common.featureconfigs.models.OfferMapperConfig
import ru.yandex.market.utils.Duration
import ru.yandex.market.utils.TimeUnit
import java.util.Date

class LiveStreamMapperTest {

    private val dateTimeParser = mock<DateTimeParser>()
    private val imageMapper = mock<ImageMapper>()
    private val fapiPresenterDto = mock<WhiteFrontApiLiveStreamPresenterDto>()
    private val previewImage = mock<WhiteFrontApiImageDto>()
    private val promoInfo = mock<WhiteFrontApiLiveStreamPromoInfoDto>()
    private val skuIds = listOf(SKU_ID)
    private val streamConfig = mock<WhiteFrontApiLiveStreamConfigDto> {
        on { translationId } doReturn TRANSLATION_ID
    }
    private val detailedSkuMapper = mock<DetailedSkuMapper>()
    private val offerMapperConfig = mock<OfferMapperConfig>()

    private val mapper = LiveStreamMapper(dateTimeParser, imageMapper, detailedSkuMapper)

    private val streamContent = WhiteFrontApiLiveStreamContentDto(
        chatInviteHash = CHAT_INVITE_HASH,
        description = DESCRIPTION,
        duration = DURATION,
        id = ID,
        presenter = fapiPresenterDto,
        previewImage = previewImage,
        promoInfo = promoInfo,
        skuIds = skuIds,
        startTime = START_TIME,
        streamConfig = streamConfig,
        title = TITLE,
        previewConfig = null,
        onlineViewers = null,
        totalViews = null
    )

    private val dto = LiveStreamMergedDto(streamContent, null)

    @Test
    fun `Should map correct with all fields`() {
        val startDate = Date(0L)
        whenever(dateTimeParser.parseGmt(START_TIME)).thenReturn(startDate)
        val expected = LiveStreamContent(
            semanticId = ID,
            chatId = CHAT_INVITE_HASH,
            startTime = startDate,
            duration = Duration(DURATION.toDouble(), TimeUnit.MINUTES),
            title = TITLE,
            description = DESCRIPTION,
            translationId = TRANSLATION_ID,
            preview = null,
            promo = null,
            skus = emptyList(),
            previewVideoId = null,
            onlineViewers = null,
            totalViews = 0
        )
        val result = mapper.map(dto, offerMapperConfig)
        Assertions.assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `Should return null when id is null`() {
        val expected = null

        val result = mapper.map(LiveStreamMergedDto(streamContent.copy(id = null), null), offerMapperConfig)
        Assertions.assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `Should return null when start time is null`() {
        val expected = null
        val result = mapper.map(LiveStreamMergedDto(streamContent.copy(startTime = null), null), offerMapperConfig)
        Assertions.assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `Should return null when duration is null`() {
        val expected = null
        val result = mapper.map(LiveStreamMergedDto(streamContent.copy(duration = null), null), offerMapperConfig)
        Assertions.assertThat(result).isEqualTo(expected)
    }

    private companion object {
        const val CHAT_INVITE_HASH = "CHAT_INVITE_HASH"
        const val DESCRIPTION = "DESCRIPTION"
        const val ID = "ID"
        const val SKU_ID = 0L
        const val START_TIME = "START_TIME"
        const val TITLE = "TITLE"
        const val DURATION = 10L
        const val TRANSLATION_ID = "TRANSLATION_ID"
    }
}
