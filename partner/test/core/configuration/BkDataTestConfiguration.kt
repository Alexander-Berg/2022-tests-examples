package ru.yandex.partner.core.configuration

import NPartner.Page.TPartnerPage
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import ru.yandex.direct.validation.builder.Validator
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.ValidationResult
import ru.yandex.partner.core.entity.block.type.custombkdata.CustomBkDataService
import ru.yandex.partner.core.entity.block.type.custombkdata.BlockWithCustomBkDataValidatorProvider
import ru.yandex.partner.core.entity.dsp.model.Dsp

@TestConfiguration
class BkDataTestConfiguration {
    @Primary
    @Bean
    fun blockBkDataValidatorProvider(
        @Value("\${validation.bkdata.ignoreEmptySizesForInterstitial:true}")
        ignoreEmptySizesForInterstitial: Boolean,
        customBkDataService: CustomBkDataService
    ) = BlockWithCustomBkDataValidatorProviderStub(ignoreEmptySizesForInterstitial, customBkDataService)

    class BlockWithCustomBkDataValidatorProviderStub(
        ignoreEmptySizesForInterstitial: Boolean,
        customBkDataService: CustomBkDataService
    ) : BlockWithCustomBkDataValidatorProvider(ignoreEmptySizesForInterstitial, customBkDataService) {
        var disableBkDataValidation: Boolean = false

        override fun bkDataValidator(
            pageId: Long,
            maybePublicId: String?,
            allDsps: Map<Long, Dsp>
        ): Validator<TPartnerPage.TBlock, Defect<Any>> {
            return if (disableBkDataValidation) Validator { ValidationResult(it) }
            else super.bkDataValidator(pageId, maybePublicId, allDsps)
        }
    }
}
