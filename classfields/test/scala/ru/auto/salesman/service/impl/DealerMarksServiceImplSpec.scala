package ru.auto.salesman.service.impl

import ru.auto.api.ApiOfferModel.{Category, OfferStatus, Section}
import ru.auto.api.ResponseModel.MarkModelsResponse
import ru.auto.api.ResponseModel.MarkModelsResponse.MarkModels
import ru.auto.salesman.client.VosClient
import ru.auto.salesman.client.VosClient.GetMarkModelsQuery
import ru.auto.salesman.dao.ClientDao
import ru.auto.salesman.model.{ClientId, OfferMark}
import ru.auto.salesman.test.BaseSpec

import scala.collection.JavaConverters._

class DealerMarksServiceImplSpec extends BaseSpec {
  import DealerMarksServiceImplSpec._

  private val clientDao: ClientDao = mock[ClientDao]
  private val vosClient: VosClient = mock[VosClient]

  private val getClientMarksFromVOS = toMockFunction1 {
    vosClient.getMarkModels(_: GetMarkModelsQuery)
  }

  private val getClientMarksFromDatabase = toMockFunction1 {
    clientDao.loadMarks(_: ClientId)
  }

  private val service = new DealerMarksServiceImpl(clientDao, vosClient)

  "getMarks" should {
    "return marks from vos" in {
      getClientMarksFromVOS
        .expects(getMarkModelQuery)
        .returningZ {
          Some(markModelsResponse(Seq("audi", "citroen")))
        }

      val expectedResult = List("AUDI", "CITROEN")

      val result = service
        .getMarks(TestClientId, Category.CARS, Some(Section.NEW))
        .success
        .value

      result should contain theSameElementsAs expectedResult
    }

    "return marks from db if vos return None" in {
      getClientMarksFromVOS
        .expects(getMarkModelQuery)
        .returningZ(None)

      getClientMarksFromDatabase
        .expects(TestClientId)
        .returningZ(List("opel", "bmw"))

      val expectedResult = List("OPEL", "BMW")

      val result = service
        .getMarks(TestClientId, Category.CARS, Some(Section.NEW))
        .success
        .value

      result should contain theSameElementsAs expectedResult
    }
  }

}

object DealerMarksServiceImplSpec {
  private val TestClientId: ClientId = 123L

  private val getMarkModelQuery =
    GetMarkModelsQuery(
      s"dealer:$TestClientId",
      Category.CARS,
      Some(Section.NEW),
      includeRemoved = false,
      statuses = Seq(OfferStatus.ACTIVE)
    )

  private def markModelsResponse(marks: Seq[OfferMark]) =
    MarkModelsResponse
      .newBuilder()
      .addAllMarkModels {
        marks.map(MarkModels.newBuilder().setMark(_).build()).asJava
      }
      .build()
}
