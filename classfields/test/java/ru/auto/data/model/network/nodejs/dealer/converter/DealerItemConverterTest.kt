package ru.auto.data.model.network.nodejs.dealer.converter

import io.qameta.allure.kotlin.junit4.AllureRunner
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.data.model.dealer.DealerItem
import ru.auto.data.model.network.scala.dealer.converter.DealerItemConverter
import ru.auto.data.model.network.scala.offer.NWSalon
import ru.auto.data.model.network.scala.response.NWSalonResponse
import ru.auto.testextension.FileTestUtils

/**
 * @author dumchev on 21.06.17.
 */
@RunWith(AllureRunner::class) class DealerItemConverterTest {

    @Test
    fun `dealer convertion is correct`() {

        val nwDealerItem: NWSalon = FileTestUtils.readJsonAsset(
                "/assets/dealer_response.json", NWSalonResponse::class.java).salon

        print(nwDealerItem.car_marks)
        val dealerItem: DealerItem? = DealerItemConverter.fromSalon(nwDealerItem)
        checkNotNull(dealerItem?.marks?.firstOrNull()) { "dealer should have at least one mark" }

        println(dealerItem)
    }
}
