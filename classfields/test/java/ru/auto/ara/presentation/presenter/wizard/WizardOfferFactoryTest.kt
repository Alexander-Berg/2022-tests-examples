package ru.auto.ara.presentation.presenter.wizard

import io.qameta.allure.kotlin.junit4.AllureRunner
 import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
 import ru.auto.data.model.VehicleCategory
import ru.auto.data.model.catalog.Pts
import ru.auto.data.model.common.Phone
import ru.auto.data.model.data.offer.AdditionalInfo
import ru.auto.data.model.data.offer.CarInfo
import ru.auto.data.model.data.offer.Documents
import ru.auto.data.model.data.offer.Entity
import ru.auto.data.model.data.offer.GenerationInfo
import ru.auto.data.model.data.offer.Location
import ru.auto.data.model.data.offer.Offer
import ru.auto.data.model.data.offer.PriceInfo
import ru.auto.data.model.data.offer.Seller
import ru.auto.data.model.data.offer.SellerType
import ru.auto.data.model.data.offer.State
import ru.auto.data.model.data.offer.TechParam
import ru.auto.data.model.data.offer.TransmissionEntity

/**
 * @author aleien on 02.04.18.
 */
@RunWith(AllureRunner::class) class WizardOfferFactoryTest {
    private val offerFactory = WizardOfferFactory()

    private val fullOffer = Offer(VehicleCategory.CARS,
            carInfo = CarInfo(markCode = "mark", modelCode = "model", generationId = "generation",
                    generationInfo = GenerationInfo(id = "generation.id", name = "generation.name", from = 2000, to = 2018),
                    bodyType = Entity("bodytype.id", "bodytype.label"),
                    engineType = Entity("engine.id", "engine.label"),
                    drive = Entity("drivie.id", "drive.label"),
                    transmission = TransmissionEntity("transmission.id", "transmission.label", ""),
                    techParam = TechParam("techparam.id", configurationId = null, name = "")
            ), sellerType = SellerType.PRIVATE,
            color = Entity("color.id", "color.label"),
            documents = Documents(year = 2018, ownersNumber = 1, pts = Pts.ORIGINAL),
            state = State(mileage = 5000, uploadUrl = null),
            priceInfo = PriceInfo(150000000),
            additional = AdditionalInfo(null, exchange = true),
            seller = Seller(phones = listOf(), name = "AA", unconfirmedEmail = "a@a.a", location = Location(),
                    arePhonesRedirected = true),
            fallbackUrl = "",
            id = ""
    )

    private val emptyOffer = Offer(
            VehicleCategory.CARS,
            id = "DONT_CARE",
            sellerType = SellerType.PRIVATE,
            fallbackUrl = "DONT_CARE"
    )

    @Test
    fun `setting mark clears only model, year, generation, bodytype, engine, transmission, gear, modification`() {
        val newMark = "new mark"
        val updatedOffer = offerFactory.setMark(fullOffer, newMark)

        assertThat(updatedOffer).isNotNull()
        assertThat(updatedOffer?.carInfo).isNotNull()
        assertThat(updatedOffer?.carInfo?.markCode).isEqualTo(newMark)

        assertThat(updatedOffer?.documents?.year).isNull()

        assertThat(updatedOffer?.carInfo?.armored).isEqualTo(fullOffer.carInfo?.armored)
        assertThat(updatedOffer?.color).isEqualToComparingFieldByField(fullOffer.color)
        assertThat(updatedOffer?.documents?.pts).isEqualToComparingFieldByField(fullOffer.documents?.pts)
        assertThat(updatedOffer?.documents?.ownersNumber).isEqualToComparingFieldByField(fullOffer.documents?.ownersNumber)
        assertThat(updatedOffer?.state).isEqualToComparingFieldByField(fullOffer.state)
        assertThat(updatedOffer?.priceInfo).isEqualToComparingFieldByField(fullOffer.priceInfo)
        assertThat(updatedOffer?.additional).isEqualToComparingFieldByField(fullOffer.additional)
        assertThat(updatedOffer?.seller).isEqualToComparingFieldByField(fullOffer.seller)
        assertThat(updatedOffer?.sellerType).isEqualToComparingFieldByField(fullOffer.sellerType)
    }

    @Test
    fun `setting year on empty offer succeeds`() {
        val year = 2011
        val updatedOffer = offerFactory.setYear(emptyOffer, year)

        assertThat(updatedOffer).isNotNull()
        assertThat(updatedOffer?.documents?.year).isEqualTo(year)
    }

    @Test
    fun `setting contacts doesn't clear phones`() {
        val phones = listOf(Phone("12345", 11, 23))
        val updatedOffer = offerFactory.setPhones(fullOffer, phones)

        assertThat(updatedOffer?.seller?.location).isEqualToComparingFieldByField(fullOffer.seller?.location)
        assertThat(updatedOffer?.seller?.name).isEqualTo(fullOffer.seller?.name)
    }
}
