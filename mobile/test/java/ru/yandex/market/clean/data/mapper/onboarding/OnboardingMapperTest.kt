package ru.yandex.market.clean.data.mapper.onboarding

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.data.mapper.ImageMapper
import ru.yandex.market.data.media.color.mapper.ColorMapper
import ru.yandex.market.data.onboarding.network.dto.OnboardingActionButtonDto
import ru.yandex.market.data.onboarding.network.dto.OnboardingInfoDto
import ru.yandex.market.data.onboarding.network.dto.onboardingActionButtonDtoTestInstance
import ru.yandex.market.data.onboarding.network.dto.onboardingBackgroundDtoTestInstance
import ru.yandex.market.data.onboarding.network.dto.onboardingImageDtoTestInstance
import ru.yandex.market.data.onboarding.network.dto.onboardingInfoDtoTestInstance
import ru.yandex.market.data.onboarding.network.dto.onboardingPageDtoTestInstance
import ru.yandex.market.data.onboarding.network.dto.onboardingTextDtoTestInstance
import ru.yandex.market.data.onboarding.network.dto.viewOptionsDtoTestInstance
import ru.yandex.market.domain.media.model.measuredImageReferenceTestInstance
import ru.yandex.market.domain.onboarding.model.OnboardingActionButton
import ru.yandex.market.domain.onboarding.model.OnboardingInfo
import ru.yandex.market.domain.onboarding.model.onboardingActionButtonTestInstance
import ru.yandex.market.domain.onboarding.model.onboardingBackgroundTestInstance
import ru.yandex.market.domain.onboarding.model.onboardingInfoTestInstance
import ru.yandex.market.domain.onboarding.model.onboardingPageTestInstance
import ru.yandex.market.domain.onboarding.model.onboardingTextTestInstance
import java.lang.IllegalArgumentException

class OnboardingMapperTest {

    private val imageMapper = ImageMapper(mock(), mock())

    private val colorMapper = mock<ColorMapper>() {
        on { map(any()) } doReturn COLOR
    }
    private val mapper = OnboardingMapper(imageMapper, colorMapper)

    @Test
    fun `Test mapping`() {
        val actual = getActual(VALID_COLOR)
        val expected = getExpected()
        val result = mapper.map(actual).throwError()
        assertThat(result).isEqualTo(expected)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Required field is null`() {
        val actual = getActual(VALID_COLOR).copy(
            id = null,
            type = null,
            expiredDate = null,
            pages = null
        )
        mapper.map(actual).throwError()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Color is invalid`() {
        whenever(colorMapper.map(any())) doReturn null
        val actual = getActual(INVALID_COLOR)
        mapper.map(actual).throwError()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Pages is empty`() {
        val actual = getActual(VALID_COLOR).copy(
            pages = emptyList()
        )
        mapper.map(actual).throwError()
    }

    private fun getActual(color: String): OnboardingInfoDto {
        return onboardingInfoDtoTestInstance(
            pages = listOf(
                onboardingPageDtoTestInstance(
                    background = onboardingBackgroundDtoTestInstance(
                        color = color,
                        contentColor = null,
                        image = onboardingImageDtoTestInstance()
                    ),
                    title = onboardingTextDtoTestInstance(
                        viewOptions = viewOptionsDtoTestInstance(
                            color = color,
                        ),
                    ),
                    description = onboardingTextDtoTestInstance(
                        viewOptions = viewOptionsDtoTestInstance(
                            color = color,
                        ),
                    ),
                    disclaimer = null,
                    actionButton = onboardingActionButtonDtoTestInstance(
                        viewOptions = viewOptionsDtoTestInstance(
                            buttonColor = color,
                            textColor = color
                        ),
                        type = OnboardingActionButtonDto.TypeDto.SKIP
                    ),
                    additionalButton = null
                )
            )
        )
    }

    private fun getExpected(): OnboardingInfo {
        return onboardingInfoTestInstance(
            pages = listOf(
                onboardingPageTestInstance(
                    background = onboardingBackgroundTestInstance(
                        contentColor = null,
                        image = measuredImageReferenceTestInstance().copy(
                            url = "https:$URL",
                            isRestrictedAge18 = false,
                            alternativeText = null
                        )
                    ),
                    title = onboardingTextTestInstance(),
                    description = onboardingTextTestInstance(),
                    disclaimer = null,
                    actionButton = onboardingActionButtonTestInstance().copy(
                        type = OnboardingActionButton.ActionType.Skip
                    ),
                    additionalButton = null
                )
            )
        )
    }

    companion object {
        private const val VALID_COLOR = "#FFFAAA"
        private const val INVALID_COLOR = "#PQ"
        private const val URL = "url"
        private const val COLOR = 42
    }
}