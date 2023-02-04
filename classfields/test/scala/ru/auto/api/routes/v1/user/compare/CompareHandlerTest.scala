package ru.auto.api.routes.v1.user.compare

import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{MediaTypes, StatusCodes}
import org.mockito.Mockito.{reset, verify, verifyNoMoreInteractions}
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.ApiSuite
import ru.auto.api.BreadcrumbsModel.Entity
import ru.auto.api.model.ModelGenerators
import ru.auto.api.services.compare.CompareClient.CatalogCardInfo
import ru.auto.api.services.{MockedClients, MockedPassport}
import ru.auto.catalog.model.api.ApiModel.{ConfigurationCard, RawCatalog, SuperGenCard, TechParamCard}

import scala.concurrent.Future
import scala.jdk.CollectionConverters._

/**
  * Created by ndmelentev on 22.05.17.
  */
class CompareHandlerTest extends ApiSuite with MockedClients with MockedPassport {

  before {
    reset(passportManager)
    when(passportManager.getClientId(?)(?)).thenReturnF(None)
  }

  after {
    verifyNoMoreInteractions(passportManager)
  }

  private val catalogRespone = {
    val catalog = RawCatalog.newBuilder()
    val techParam = List("110", "220", "330").map { tpId =>
      tpId -> TechParamCard
        .newBuilder()
        .setEntity(Entity.newBuilder.setId(tpId))
        .setParentConfiguration((tpId.toLong / 100 * 100).toString)
        .build()
    }.toMap
    val configuration = List("100", "200", "300").map { confId =>
      confId -> ConfigurationCard
        .newBuilder()
        .setEntity(Entity.newBuilder.setTechParamId(confId))
        .setParentSuperGen("1337")
        .build()
    }.toMap
    val superGen = List("1337").map { superGenId =>
      superGenId -> SuperGenCard
        .newBuilder()
        .setEntity(Entity.newBuilder.setId(superGenId))
        .setParentModel("X1")
        .build()
    }.toMap
    catalog.putAllTechParam(techParam.asJava)
    catalog.putAllConfiguration(configuration.asJava)
    catalog.putAllSuperGen(superGen.asJava)
    catalog.build()
  }

  test("get catalog cards") {
    val user = ModelGenerators.PrivateUserRefGen.next
    val category = Category.CARS
    val catalogCardId = "1_2_3"

    when(compareClient.getCatalogCards(?, ?)(?))
      .thenReturnF(Seq(CatalogCardInfo(catalogCardId, 0, 0, None)))

    Get(s"/1.0/user/compare/cars/") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
        }
        verify(compareClient).getCatalogCards(eq(category), eq(user))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("add catalog card") {
    val user = ModelGenerators.PrivateUserRefGen.next
    val category = Category.CARS
    val catalogCardId = "100__"

    when(compareClient.addCatalogCard(?, ?, ?, ?)(?))
      .thenReturn(Future.unit)
    when(catalogClient.filter(?, ?)(?))
      .thenReturnF(catalogRespone)

    Post(s"/1.0/user/compare/cars/$catalogCardId") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        val response = responseAs[String]
        withClue(response) {
          status shouldBe StatusCodes.OK
        }
        verify(compareClient).addCatalogCard(eq(category), eq(user), eq("100__110"), any[Long]())(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("add multiple catalog cards") {
    val user = ModelGenerators.PrivateUserRefGen.next
    val category = Category.CARS
    val catalogCardIds = List("100_111_110", "200__220", "300__")
    val modifiedCatalogCardIds = List("100_111_110", "200__220", "300__330")

    when(compareClient.addCatalogCard(?, ?, ?, ?)(?))
      .thenReturn(Future.unit)
    when(catalogClient.filter(?, ?)(?))
      .thenReturnF(catalogRespone)

    Post(s"/1.0/user/compare/cars/${catalogCardIds.mkString(",")}") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        val response = responseAs[String]
        withClue(response) {
          status shouldBe StatusCodes.OK
        }
        modifiedCatalogCardIds.foreach(id =>
          verify(compareClient).addCatalogCard(eq(category), eq(user), eq(id), any[Long]())(?)
        )
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("upsert catalog cards") {
    val user = ModelGenerators.PrivateUserRefGen.next
    val category = Category.CARS
    val catalogCardIds = List("100_111_110", "200__220", "300__")
    val modifiedCatalogCardIds = List("100_111_110", "200__220", "300__330")

    when(compareClient.upsertCatalogCard(?, ?, ?, ?)(?))
      .thenReturn(Future.unit)
    when(catalogClient.filter(?, ?)(?))
      .thenReturnF(catalogRespone)

    Put(s"/1.0/user/compare/cars/${catalogCardIds.mkString(",")}") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        val response = responseAs[String]
        withClue(response) {
          status shouldBe StatusCodes.OK
        }

        modifiedCatalogCardIds.foreach(id =>
          verify(compareClient).upsertCatalogCard(eq(category), eq(user), eq(id), any[Long]())(?)
        )
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("delete catalog card") {
    val user = ModelGenerators.PrivateUserRefGen.next
    val category = Category.CARS
    val catalogCardId = "1_2_3"

    when(compareClient.deleteCatalogCard(?, ?, ?)(?))
      .thenReturn(Future.unit)

    Delete(s"/1.0/user/compare/cars/$catalogCardId") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        val response = responseAs[String]
        withClue(response) {
          status shouldBe StatusCodes.OK
        }
        verify(compareClient).deleteCatalogCard(eq(category), eq(user), eq(catalogCardId))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("delete multiple catalog cards") {
    val user = ModelGenerators.PrivateUserRefGen.next
    val category = Category.CARS
    val catalogCardIds = List("1_2_3", "4_5_6", "7_8_9")

    when(compareClient.deleteCatalogCard(?, ?, ?)(?))
      .thenReturn(Future.unit)

    Delete(s"/1.0/user/compare/cars/${catalogCardIds.mkString(",")}") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        val response = responseAs[String]
        withClue(response) {
          status shouldBe StatusCodes.OK
        }

        verify(compareClient).deleteCatalogCard(eq(category), eq(user), eq(catalogCardIds.mkString(",")))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }
}
