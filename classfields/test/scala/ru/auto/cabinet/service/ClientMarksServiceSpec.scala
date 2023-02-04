package ru.auto.cabinet.service

import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.scalacheck.{ScalaCheckPropertyChecks => PropertyChecks}
import org.scalatest.flatspec.{AnyFlatSpec => FlatSpec}
import org.scalatest.matchers.should.Matchers
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.api.ResponseModel.MarkModelsResponse.MarkModels
import ru.auto.api.ResponseModel.MarkModelsResponse
import ru.auto.cabinet.dao.jdbc.{ClientDealerDao, MarkDao}
import ru.auto.cabinet.model.{ClientId, ClientMarks, Mark}
import ru.auto.cabinet.service.ClientMarksService.ClientMarkInfo
import ru.auto.cabinet.service.instr.{EmptyInstr, Instr}
import ru.auto.cabinet.service.vos.VosClient
import org.scalatestplus.mockito.MockitoSugar.mock
import org.mockito.Mockito.when
import ru.auto.cabinet.test.TestUtil._
import ru.auto.cabinet.service.ClientMarksServiceSpec._
import ru.auto.cabinet.trace.Context

import scala.jdk.CollectionConverters._
import scala.concurrent.ExecutionContext.Implicits.global

class ClientMarksServiceSpec
    extends FlatSpec
    with Matchers
    with PropertyChecks
    with ScalaFutures {

  implicit private val rc = Context.unknown

  private val vosClient = mock[VosClient]
  private val clientMarksDao = mock[ClientDealerDao]
  private val catalogMarksDao = mock[MarkDao]

  implicit private val instr: Instr = new EmptyInstr("test")

  private val service = new ClientMarksService(
    vosClient,
    clientMarksDao,
    catalogMarksDao
  )

  "getClientMarks" should "get marks from VOS with category and section" in {
    val category = Category.MOTO
    val section = Section.USED

    when(vosClient.getMarkModels(testClientId, category, section))
      .thenReturnF(Some(markModelsResponse(Seq("yamaha", "kawasaki"))))

    val expectedResult = List(
      ClientMarkInfo("YAMAHA"),
      ClientMarkInfo("KAWASAKI")
    )

    val result = service
      .getClientMarks(testClientId, Some(category), Some(section))
      .futureValue
    result shouldBe expectedResult
  }

  "getClientMarks" should "get marks from VOS with default category and section" in {
    when(vosClient.getMarkModels(testClientId, Category.CARS, Section.NEW))
      .thenReturnF(Some(markModelsResponse(Seq("opel"))))

    val expectedResult = List(
      ClientMarkInfo("OPEL")
    )

    val result = service
      .getClientMarks(testClientId, category = None, section = None)
      .futureValue
    result shouldBe expectedResult
  }

  "getClientMarks" should "get marks from database if list from VOS is empty" in {
    when(vosClient.getMarkModels(testClientId, Category.CARS, Section.NEW))
      .thenReturnF(Some(markModelsResponse(Seq.empty)))

    when(clientMarksDao.findClientMarks(testClientId))
      .thenReturnF(ClientMarks(testClientId, markIds = Seq(12L, 14L)))

    when(catalogMarksDao.findByIds(Set(12L, 14L)))
      .thenReturnF(
        Seq(Mark(1, Some("opel"), "Opel"), Mark(2, Some("skoda"), "Skoda")))

    val expectedResult = List(
      ClientMarkInfo("OPEL"),
      ClientMarkInfo("SKODA")
    )

    val result = service
      .getClientMarks(testClientId, category = None, section = None)
      .futureValue
    result shouldBe expectedResult
  }

  "getClientMarks" should "get marks from database if VOS response is None" in {
    when(vosClient.getMarkModels(testClientId, Category.CARS, Section.NEW))
      .thenReturnF(None)

    when(clientMarksDao.findClientMarks(testClientId))
      .thenReturnF(ClientMarks(testClientId, markIds = Seq(12L, 14L)))

    when(catalogMarksDao.findByIds(Set(12L, 14L)))
      .thenReturnF(
        Seq(Mark(1, Some("opel"), "Opel"), Mark(2, Some("skoda"), "Skoda")))

    val expectedResult = List(
      ClientMarkInfo("OPEL"),
      ClientMarkInfo("SKODA")
    )

    val result = service
      .getClientMarks(testClientId, category = None, section = None)
      .futureValue
    result shouldBe expectedResult
  }

  "getClientMarks" should "get marks from database if list from VOS is empty, ignore marks wo verba code" in {
    when(vosClient.getMarkModels(testClientId, Category.CARS, Section.NEW))
      .thenReturnF(Some(markModelsResponse(Seq.empty)))

    when(clientMarksDao.findClientMarks(testClientId))
      .thenReturnF(ClientMarks(testClientId, markIds = Seq(12L, 14L)))

    when(catalogMarksDao.findByIds(Set(12L, 14L)))
      .thenReturnF(
        Seq(Mark(1, Some("opel"), "Opel"), Mark(2, verbaId = None, "Skoda")))

    val expectedResult = List(
      ClientMarkInfo("OPEL")
    )

    val result = service
      .getClientMarks(testClientId, category = None, section = None)
      .futureValue
    result shouldBe expectedResult
  }

  "getClientMarkNames" should "get mark names from database and ignore marks wo verba code" in {
    when(vosClient.getMarkModels(testClientId, Category.CARS, Section.NEW))
      .thenReturnF(Some(markModelsResponse(Seq.empty)))

    when(clientMarksDao.findClientMarks(testClientId))
      .thenReturnF(ClientMarks(testClientId, markIds = Seq(12L, 14L)))

    when(catalogMarksDao.findByIds(Set(12L, 14L)))
      .thenReturnF(
        Seq(Mark(1, Some("opel"), "Opel"), Mark(2, verbaId = None, "Skoda")))

    val expectedResult = List(
      ClientMarkInfo("Opel")
    )

    val result = service
      .getClientMarkNames(testClientId)
      .futureValue
    result shouldBe expectedResult
  }

}

object ClientMarksServiceSpec {

  val testClientId: ClientId = 222L

  def markModelsResponse(marks: Seq[String]) = {
    MarkModelsResponse
      .newBuilder()
      .addAllMarkModels {
        marks.map(MarkModels.newBuilder().setMark(_).build()).asJava
      }
      .build()
  }

}
