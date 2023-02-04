package ru.yandex.vertis.feedprocessor.autoru.scheduler.services.unificator

import ru.auto.api.unification.Unification.{CarsUnificationCollection, CarsUnificationEntry}
import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.autoru.scheduler.util.HttpClientSuite
import ru.yandex.vertis.feedprocessor.http.HttpClientConfig

import scala.jdk.CollectionConverters._

/**
  * @author pnaydenov
  */
class UnificatorClientIntTest extends WordSpecBase with HttpClientSuite {

  override protected def config: HttpClientConfig =
    HttpClientConfig("auto2-searcher-api.vrts-slb.test.vertis.yandex.net", 80)

  val client = new UnificatorClientImpl(http)

  "UnificatorClientImpl" should {
    "unify mark" in {
      val entry = CarsUnificationEntry
        .newBuilder()
        .setRawMark("Форд")
        .build()
      val response = client.carsUnify(CarsUnificationCollection.newBuilder().addEntries(entry).build()).futureValue
      val entries = response.getEntriesList.asScala
      entries should have size 1
      entries.head.getMark shouldEqual "FORD"
    }

    "get info by tech_param_id" in {
      val response = client.infoByTechParamIds(Seq(4986817)).futureValue
      response.getEntitiesList should have size 1
      val entity = response.getEntities(0)
      entity.getMarkInfo.getCode shouldEqual "MERCEDES"
      entity.getModelInfo.getCode shouldEqual "GL_KLASSE"
      entity.getSuperGen.getId shouldEqual 4986814L
      entity.getConfiguration.getId shouldEqual 4986815L
      entity.getConfiguration.getBodyType shouldEqual "ALLROAD_5_DOORS"
      entity.getTechParam.getId shouldEqual 4986817L
      entity.getTechParam.getEngineType shouldEqual "GASOLINE"
      entity.getTechParam.getTransmission shouldEqual "AUTOMATIC"
      entity.getTechParam.getGearType shouldEqual "ALL_WHEEL_DRIVE"
      entity.getTechParam.getDisplacement shouldEqual 4663
      entity.getTechParam.getPower shouldEqual 340
    }

    "unify options by aliases" in {
      val response =
        client.unifyOptions(Seq("салон кожаный руль", "усилитель руля гидро-", "неизвестная опция")).futureValue
      response shouldEqual Map("салон кожаный руль" -> "wheel-leather", "усилитель руля гидро-" -> "wheel-power")
    }

    "unify options by names" in {
      pending // TODO: wait AUTO-10540
      val response =
        client.unifyOptions(Seq("отделка кожей рулевого колеса", "усилитель руля", "неизвестная опция")).futureValue
      response shouldEqual Map("отделка кожей рулевого колеса" -> "wheel-leather", "усилитель руля" -> "wheel-power")
    }
  }
}
