package ru.auto.data.model.wizard

import io.qameta.allure.kotlin.junit4.AllureRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.data.model.auction.AuctionDisplayingType
import ru.auto.data.model.auction.AuctionProposalDisplayInfo
import ru.auto.data.model.data.offer.ValidationError
import ru.auto.data.model.network.scala.offer.converter.OfferConverter
import ru.auto.data.network.scala.response.OfferResponse
import ru.auto.testextension.FileTestUtils

@RunWith(AllureRunner::class)
class OfferPublishStepsProviderTest {

    private val offer = FileTestUtils.readJsonAsset(
        assetPath = "/assets/10448426-ce654669.json",
        classOfT = OfferResponse::class.java,
    ).offer?.let { OfferConverter().fromNetwork(it, searchPosition = 0) }!!

    private val fullStepsList = listOf(
        VinStep,
        RecognizedCharacteristicsStep,
        LicenseNumberStep,
        MarkStep,
        ModelStep,
        YearStep,
        GenerationStep,
        BodyTypeStep,
        EngineStep,
        GearStep,
        TransmissionStep,
        ModificationStep,
        ColorStep,
        PtsStep,
        OwnersStep,
        PhotosStep,
        DescriptionStep,
        MileageStep,
        PhoneStep,
        ContactsStep,
        PriceStep,
        PreviewStep,
        FinalStep,
    )

    @Test
    fun `when characteristics from vin weren't recognized and no step to skip should not show recognized characteristics step`() {
        val stepsProvider = OfferPublishStepsProvider(
            isProvenOwnerEnabled = { false },
            wasVinRecognized = { false },
            isPanoramaAvailable = { false },
            wasDraftFilledFromVin = { false },
            auctionDisplayInfoProvider = {
                AuctionProposalDisplayInfo(
                    isAuctionAvailableForOffer = false,
                    auctionDisplayingType = AuctionDisplayingType.DO_NOT_SHOW)
            },
        )
        val steps = stepsProvider.provideSteps(item = offer, proceedToStep = null)
        assertThat(steps).isEqualTo(fullStepsList.filterNot { it == RecognizedCharacteristicsStep })
    }

    @Test
    @Suppress("MaxLineLength")
    fun `when characteristics from vin weren't recognized and provided steps to skip should not show recognized characteristics step`() {
        val stepsProvider = OfferPublishStepsProvider(
            isProvenOwnerEnabled = { false },
            wasVinRecognized = { false },
            isPanoramaAvailable = { false },
            wasDraftFilledFromVin = { false },
            auctionDisplayInfoProvider = {
                AuctionProposalDisplayInfo(
                    isAuctionAvailableForOffer = false,
                    auctionDisplayingType = AuctionDisplayingType.DO_NOT_SHOW)
            },
        )
        val steps = stepsProvider.provideSteps(item = offer, proceedToStep = GenerationStep)
        assertThat(steps).isEqualTo(fullStepsList.filterNot { it == RecognizedCharacteristicsStep })
    }

    @Test
    fun `when characteristics from vin were recognized and not step to skip should not skip steps`() {
        val stepsProvider = OfferPublishStepsProvider(
            isProvenOwnerEnabled = { false },
            wasVinRecognized = { false },
            isPanoramaAvailable = { false },
            wasDraftFilledFromVin = { true },
            auctionDisplayInfoProvider = {
                AuctionProposalDisplayInfo(
                    isAuctionAvailableForOffer = false,
                    auctionDisplayingType = AuctionDisplayingType.DO_NOT_SHOW)
            },
        )
        val steps = stepsProvider.provideSteps(item = offer, proceedToStep = null)
        assertThat(steps).isEqualTo(fullStepsList)
    }

    @Test
    fun `when characteristics from vin were recognized and set step to skip should skip steps inclusive`() {
        val stepsProvider = OfferPublishStepsProvider(
            isProvenOwnerEnabled = { false },
            wasVinRecognized = { false },
            isPanoramaAvailable = { false },
            wasDraftFilledFromVin = { true },
            auctionDisplayInfoProvider = {
                AuctionProposalDisplayInfo(
                    isAuctionAvailableForOffer = false,
                    auctionDisplayingType = AuctionDisplayingType.DO_NOT_SHOW)
            },
        )
        val steps = stepsProvider.provideSteps(item = offer, proceedToStep = ColorStep)
        assertThat(steps).isEqualTo(
            listOf(
                VinStep,
                RecognizedCharacteristicsStep,
                LicenseNumberStep,
                PtsStep,
                OwnersStep,
                PhotosStep,
                DescriptionStep,
                MileageStep,
                PhoneStep,
                ContactsStep,
                PriceStep,
                PreviewStep,
                FinalStep,
            )
        )
    }

    @Test
    fun `when vin was recognized but not filled should should not show recognized characteristics`() {
        val stepsProvider = OfferPublishStepsProvider(
            isProvenOwnerEnabled = { false },
            wasVinRecognized = { true },
            isPanoramaAvailable = { false },
            wasDraftFilledFromVin = { false },
            auctionDisplayInfoProvider = {
                AuctionProposalDisplayInfo(
                    isAuctionAvailableForOffer = false,
                    auctionDisplayingType = AuctionDisplayingType.DO_NOT_SHOW)
            },
        )
        val steps = stepsProvider.provideSteps(item = offer, proceedToStep = null)
        assertThat(steps).isEqualTo(fullStepsList.filterNot { it == RecognizedCharacteristicsStep })
    }

    @Test
    fun `when vin was recognized and filled should should show recognized characteristics`() {
        val stepsProvider = OfferPublishStepsProvider(
            isProvenOwnerEnabled = { false },
            wasVinRecognized = { true },
            isPanoramaAvailable = { false },
            wasDraftFilledFromVin = { true },
            auctionDisplayInfoProvider = {
                AuctionProposalDisplayInfo(
                    isAuctionAvailableForOffer = false,
                    auctionDisplayingType = AuctionDisplayingType.DO_NOT_SHOW)
            },
        )
        val steps = stepsProvider.provideSteps(item = offer, proceedToStep = null)
        assertThat(steps).isEqualTo(fullStepsList)
    }

    @Test
    fun `when vin recognized and proven owned enabled should add proven owner after license step`() {
        val stepsProvider = OfferPublishStepsProvider(
            isProvenOwnerEnabled = { true },
            wasVinRecognized = { true },
            isPanoramaAvailable = { false },
            wasDraftFilledFromVin = { true },
            auctionDisplayInfoProvider = {
                AuctionProposalDisplayInfo(
                    isAuctionAvailableForOffer = false,
                    auctionDisplayingType = AuctionDisplayingType.DO_NOT_SHOW)
            },
        )
        val steps = stepsProvider.provideSteps(item = offer, proceedToStep = null)
        assertThat(steps).isEqualTo(
            listOf(
                VinStep,
                RecognizedCharacteristicsStep,
                LicenseNumberStep,
                ProvenOwnerStep,
                MarkStep,
                ModelStep,
                YearStep,
                GenerationStep,
                BodyTypeStep,
                EngineStep,
                GearStep,
                TransmissionStep,
                ModificationStep,
                ColorStep,
                PtsStep,
                OwnersStep,
                PhotosStep,
                DescriptionStep,
                MileageStep,
                PhoneStep,
                ContactsStep,
                PriceStep,
                PreviewStep,
                FinalStep,
            )
        )
    }

    @Test
    fun `when vin not recognized and proven owned enabled should add proven owner after contacts step`() {
        val stepsProvider = OfferPublishStepsProvider(
            isProvenOwnerEnabled = { true },
            wasVinRecognized = { false },
            isPanoramaAvailable = { false },
            wasDraftFilledFromVin = { true },
            auctionDisplayInfoProvider = {
                AuctionProposalDisplayInfo(
                    isAuctionAvailableForOffer = false,
                    auctionDisplayingType = AuctionDisplayingType.DO_NOT_SHOW)
            },
        )
        val steps = stepsProvider.provideSteps(item = offer, proceedToStep = null)
        assertThat(steps).isEqualTo(
            listOf(
                VinStep,
                RecognizedCharacteristicsStep,
                LicenseNumberStep,
                MarkStep,
                ModelStep,
                YearStep,
                GenerationStep,
                BodyTypeStep,
                EngineStep,
                GearStep,
                TransmissionStep,
                ModificationStep,
                ColorStep,
                PtsStep,
                OwnersStep,
                PhotosStep,
                DescriptionStep,
                MileageStep,
                PhoneStep,
                ContactsStep,
                ProvenOwnerStep,
                PriceStep,
                PreviewStep,
                FinalStep,
            )
        )
    }

    @Test
    fun `when offer has no license or vin should add LicenseOrVin step`() {
        val stepsProvider = OfferPublishStepsProvider(
            isProvenOwnerEnabled = { false },
            wasVinRecognized = { false },
            isPanoramaAvailable = { false },
            wasDraftFilledFromVin = { false },
            auctionDisplayInfoProvider = {
                AuctionProposalDisplayInfo(
                    isAuctionAvailableForOffer = false,
                    auctionDisplayingType = AuctionDisplayingType.DO_NOT_SHOW)
            },
        )
        val offerWithLicenseValidationError = offer.copy(validations = listOf(ValidationError("required.vin_or_license_plate")))
        val steps = stepsProvider.provideSteps(item = offerWithLicenseValidationError, proceedToStep = null)
        assertThat(steps).isEqualTo(
            listOf(
                VinStep,
                LicenseNumberStep,
                MarkStep,
                ModelStep,
                YearStep,
                GenerationStep,
                BodyTypeStep,
                EngineStep,
                GearStep,
                TransmissionStep,
                ModificationStep,
                ColorStep,
                PtsStep,
                OwnersStep,
                PhotosStep,
                DescriptionStep,
                MileageStep,
                PhoneStep,
                ContactsStep,
                LicenceOrVinStep,
                PriceStep,
                PreviewStep,
                FinalStep,
            )
        )
    }

    @Test
    fun `when offer has no license should add license step`() {
        val stepsProvider = OfferPublishStepsProvider(
            isProvenOwnerEnabled = { false },
            wasVinRecognized = { false },
            isPanoramaAvailable = { false },
            wasDraftFilledFromVin = { false },
            auctionDisplayInfoProvider = {
                AuctionProposalDisplayInfo(
                    isAuctionAvailableForOffer = false,
                    auctionDisplayingType = AuctionDisplayingType.DO_NOT_SHOW)
            },
        )
        val offerWithLicenseValidationError = offer.copy(validations = listOf(ValidationError("required.license_plate")))
        val steps = stepsProvider.provideSteps(item = offerWithLicenseValidationError, proceedToStep = null)
        assertThat(steps).isEqualTo(
            listOf(
                VinStep,
                LicenseNumberStep,
                MarkStep,
                ModelStep,
                YearStep,
                GenerationStep,
                BodyTypeStep,
                EngineStep,
                GearStep,
                TransmissionStep,
                ModificationStep,
                ColorStep,
                PtsStep,
                OwnersStep,
                PhotosStep,
                DescriptionStep,
                MileageStep,
                PhoneStep,
                ContactsStep,
                LicenseNumberStep,
                PriceStep,
                PreviewStep,
                FinalStep,
            )
        )
    }

    @Test
    fun `when offer has no vin should add vin step`() {
        val stepsProvider = OfferPublishStepsProvider(
            isProvenOwnerEnabled = { false },
            wasVinRecognized = { false },
            isPanoramaAvailable = { false },
            wasDraftFilledFromVin = { false },
            auctionDisplayInfoProvider = {
                AuctionProposalDisplayInfo(
                    isAuctionAvailableForOffer = false,
                    auctionDisplayingType = AuctionDisplayingType.DO_NOT_SHOW)
            },
        )
        val offerWithLicenseValidationError = offer.copy(validations = listOf(ValidationError("required.vin")))
        val steps = stepsProvider.provideSteps(item = offerWithLicenseValidationError, proceedToStep = null)
        assertThat(steps).isEqualTo(
            listOf(
                VinStep,
                LicenseNumberStep,
                MarkStep,
                ModelStep,
                YearStep,
                GenerationStep,
                BodyTypeStep,
                EngineStep,
                GearStep,
                TransmissionStep,
                ModificationStep,
                ColorStep,
                PtsStep,
                OwnersStep,
                PhotosStep,
                DescriptionStep,
                MileageStep,
                PhoneStep,
                ContactsStep,
                VinStep,
                PriceStep,
                PreviewStep,
                FinalStep,
            )
        )
    }

    @Test
    fun `when offer has license error and proven owner enabled should add ProvenOwnerStep only after first LicenseStep`() {
        val stepsProvider = OfferPublishStepsProvider(
            isProvenOwnerEnabled = { true },
            wasVinRecognized = { true },
            isPanoramaAvailable = { false },
            wasDraftFilledFromVin = { false },
            auctionDisplayInfoProvider = {
                AuctionProposalDisplayInfo(
                    isAuctionAvailableForOffer = false,
                    auctionDisplayingType = AuctionDisplayingType.DO_NOT_SHOW)
            },
        )
        val offerWithLicenseValidationError =
            offer.copy(validations = listOf(ValidationError("required.license_plate"), ValidationError("required.vin")))
        val steps = stepsProvider.provideSteps(item = offerWithLicenseValidationError, proceedToStep = null)
        assertThat(steps).isEqualTo(
            listOf(
                VinStep,
                LicenseNumberStep,
                ProvenOwnerStep,
                MarkStep,
                ModelStep,
                YearStep,
                GenerationStep,
                BodyTypeStep,
                EngineStep,
                GearStep,
                TransmissionStep,
                ModificationStep,
                ColorStep,
                PtsStep,
                OwnersStep,
                PhotosStep,
                DescriptionStep,
                MileageStep,
                PhoneStep,
                ContactsStep,
                LicenseNumberStep,
                VinStep,
                PriceStep,
                PreviewStep,
                FinalStep,
            )
        )
    }

    @Test
    fun `when offer has no license nor vin should add license step and vin step`() {
        val stepsProvider = OfferPublishStepsProvider(
            isProvenOwnerEnabled = { false },
            wasVinRecognized = { false },
            isPanoramaAvailable = { false },
            wasDraftFilledFromVin = { false },
            auctionDisplayInfoProvider = {
                AuctionProposalDisplayInfo(
                    isAuctionAvailableForOffer = false,
                    auctionDisplayingType = AuctionDisplayingType.DO_NOT_SHOW)
            },
        )
        val offerWithLicenseValidationError =
            offer.copy(validations = listOf(ValidationError("required.license_plate"), ValidationError("required.vin")))
        val steps = stepsProvider.provideSteps(item = offerWithLicenseValidationError, proceedToStep = null)
        assertThat(steps).isEqualTo(
            listOf(
                VinStep,
                LicenseNumberStep,
                MarkStep,
                ModelStep,
                YearStep,
                GenerationStep,
                BodyTypeStep,
                EngineStep,
                GearStep,
                TransmissionStep,
                ModificationStep,
                ColorStep,
                PtsStep,
                OwnersStep,
                PhotosStep,
                DescriptionStep,
                MileageStep,
                PhoneStep,
                ContactsStep,
                LicenseNumberStep,
                VinStep,
                PriceStep,
                PreviewStep,
                FinalStep,
            )
        )
    }

}
