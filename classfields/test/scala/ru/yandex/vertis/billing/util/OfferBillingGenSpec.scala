package ru.yandex.vertis.billing.util

import org.scalatest.Ignore
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.DynamicPrice.Constraints
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.gens._
import ru.yandex.vertis.billing.model_core.proto.Conversions

import java.io.{FileOutputStream, PrintWriter}

/**
  * Generates [[ru.yandex.vertis.billing.model_core.OfferBilling]]s
  *
  * @author ruslansd
  */
@Ignore
class OfferBillingGenSpec extends AnyWordSpec with Matchers {

  private val product =
    Product(Placement(CostPerIndexing(DynamicPrice(Some(1), Constraints()))))

  private val SingleOfferBilling =
    generate(PartnerOfferId("1438536", "autoru-1041551840"))

  private val OfferBillings =
    Iterable(generate(PartnerOfferId("333333", "444444")), generate(PartnerOfferId("555555", "666666")))

  "OfferBilling" should {
    "should be written as single offer billing" in {
      new PrintWriter("offer_billing.json") {
        write(ProtoJsonFormat.printer.print(Conversions.toMessage(SingleOfferBilling)))
        write("\n")
        close()
      }

      new FileOutputStream("offer_billing.proto") {
        write(bytes.Conversions.toByteArray(Iterable(SingleOfferBilling)))
        close()
      }
    }
  }

//  "Set of OfferBillings" should {
//    "should be written as write delimited" in {
//      new PrintWriter("offer_billing.json") {
//        OfferBillings foreach { o =>
//          write(JsonFormat.printToString(Conversions.toMessage(o)))
//        }
//        close()
//      }
//
//      new FileOutputStream("offer_billing.proto") {
//        write(bytes.Conversions.toByteArray(Iterable(SingleOfferBilling)))
//        close()
//      }
//    }
//  }
//
  private def generate(offerId: OfferId) = {
    val offerBilling = OfferBillingKnownCampaignGen.next
    offerBilling.copy(offer = offerId).copy(campaign = offerBilling.campaign.copy(product = product))
  }
}
