package ru.yandex.realty.managers.export

import org.junit.runner.RunWith
import org.scalatest.PrivateMethodTester
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.common.util.currency.Currency
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.api.routes.v1.export.ExportParams
import ru.yandex.realty.clients.archive.ArchiveClient
import ru.yandex.realty.clients.searcher.SearcherClient
import ru.yandex.realty.clients.searcher.SearcherResponseModel._
import ru.yandex.realty.controllers.export.ExportEntry
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.model.offer.{
  AreaUnit,
  CategoryType,
  DealStatus,
  OfferType,
  PricingPeriod,
  SalesAgentCategory,
  TransactionCondition
}
import ru.yandex.realty.persistence.OfferId
import ru.yandex.realty.proto.unified.offer.state.OfferState
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future

/**
  * @author abulychev
  */
@RunWith(classOf[JUnitRunner])
class ExportManagerSpec extends AsyncSpecBase with PropertyChecks with RequestAware with PrivateMethodTester {

  val searcherClient: SearcherClient = mock[SearcherClient]
  val archiveClient: ArchiveClient = mock[ArchiveClient]

  val manager: ExportManager =
    new ExportManager(searcherClient, archiveClient)

  "ExportManager prepareExport" should {

    "build correct text" in {
      (searcherClient
        .getCard(
          _: OfferId
        )(_: Traced))
        .expects("7705036036560018432", *)
        .returning(
          Future.successful(
            Some(
              SearchCard(
                offerId = "7705036036560018432",
                offerType = OfferType.SELL,
                offerCategory = CategoryType.APARTMENT,
                location = Location(address = "Балашиха, микрорайон Никольско-Архангельский, Вишняковское шоссе, 38"),
                price = Price(
                  currency = Currency.RUR,
                  value = 3290000,
                  period = PricingPeriod.WHOLE_LIFE,
                  unit = AreaUnit.WHOLE_OFFER,
                  valuePerPart = Some(65149),
                  unitPerPart = Some(AreaUnit.SQUARE_METER)
                ),
                author = Author(
                  category = SalesAgentCategory.OWNER,
                  phones = List("+79652910076", "+79857406328"),
                  agentName = Some("Мирида Королёва"),
                  organization = None
                ),
                area = Some(
                  Area(
                    value = 50.5f,
                    unit = AreaUnit.SQUARE_METER
                  )
                ),
                dealStatus = Some(DealStatus.SALE),
                transactionConditionsMap = Some(
                  Map(
                    TransactionCondition.MORTGAGE -> true,
                    TransactionCondition.HAGGLE -> false
                  )
                ),
                floorsOffered = Some(List(3)),
                floorsTotal = Some(5),
                active = true,
                offerState = OfferState.getDefaultInstance
              )
            )
          )
        )

      val exportParams =
        ExportParams(offerIds = List("7705036036560018432"), link = true, contacts = true, archive = false)

      val offersToExport = manager.prepareExport(exportParams)
      offersToExport.futureValue should be(
        Seq(
          ExportEntry(
            address = "Балашиха, микрорайон Никольско-Архангельский, Вишняковское шоссе, 38",
            link = Some("https://realty.yandex.ru/offer/7705036036560018432"),
            price = "3.3 млн ₽\n65149 ₽ за м²",
            area = "Общая: 50.5 м²",
            description = "Нет описания",
            details = "3 этаж из 5",
            building = "",
            neighborhood = "",
            archive = None,
            dealType = "Свободная продажа",
            mortgage = "Да",
            haggle = "Нет",
            contacts = Some("Мирида Королёва (собственник); телефоны: +79652910076\n+79857406328")
          )
        )
      )

    }

  }

}
