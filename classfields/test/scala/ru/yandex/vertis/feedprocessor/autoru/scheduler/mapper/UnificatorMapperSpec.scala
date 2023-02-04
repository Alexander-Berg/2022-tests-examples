package ru.yandex.vertis.feedprocessor.autoru.scheduler.mapper

import org.mockito.Mockito._
import ru.auto.api.ApiOfferModel.Section
import ru.auto.api.CatalogModel
import ru.auto.api.CatalogModel.{TechInfo, TechInfoList}
import ru.auto.api.unification.Unification.{CarsUnificationCollection, CarsUnificationEntry}
import ru.yandex.vertis.feedprocessor.app.TestApplication
import ru.yandex.vertis.feedprocessor.autoru.model
import ru.yandex.vertis.feedprocessor.autoru.model.Generators._
import ru.yandex.vertis.feedprocessor.autoru.model.Messages.{OfferMessage, StreamEndMessage}
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.AutoruExternalOffer
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.AutoruExternalOffer.Unification
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.CarExternalOffer._
import ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.mapper
import ru.yandex.vertis.feedprocessor.autoru.scheduler.services.unificator.UnificatorClient
import ru.yandex.vertis.feedprocessor.autoru.scheduler.util.AutoruGenerators._
import ru.yandex.vertis.feedprocessor.dao.KVClient
import ru.yandex.vertis.feedprocessor.util.{DummyOpsSupport, StreamTestBase}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.annotation.nowarn
import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * @author pnaydenov
  */
@nowarn("cat=other-match-analysis")
class UnificatorMapperSpec extends StreamTestBase with MockitoSupport with DummyOpsSupport with TestApplication {
  implicit val meters = new mapper.Mapper.Meters(prometheusRegistry)
  val config = environment.config.getConfig("feedprocessor.autoru")

  val task = tasksGen(serviceInfoGen = carServiceInfoGen(Section.NEW)).next
  val tc = model.TaskContext(task)
  val offerGen = carExternalOfferGen(newTasksGen).map(_.copy(taskContext = tc))
  val kvClient = mock[KVClient]
  when(kvClient.bulkGet(?)).thenReturn(Future.successful(Map.empty[String, String]))
  when(kvClient.bulkSet(?)).thenReturn(Future.unit)
  val otherEntry = CarsUnificationEntry.newBuilder().setRawMark("Foo").setRawModel("Bar").setTechParamId(100).build()

  "UnificatorMapper" should {
    "handle offers with modification by modification params and tech_param_id" in {
      val offer1 = offerGen.next.copy(
        modification = EngineInfo(None, 1000, "Бензин", "MT", "Передний", None),
        bodyType = "body1",
        mark = "mark1",
        model = "model1",
        year = 2001
      )
      val offer2 = offerGen.next.copy(
        modification = ModificationString("3.5d AT (290 л.с.)"),
        bodyType = "body2",
        mark = "mark2",
        model = "model2",
        year = 2002
      )
      val offer3 = offerGen.next.copy(
        modification = TechParam(103),
        bodyType = "body3",
        mark = "mark3",
        model = "model3",
        year = 2003
      )
      val unificationResponse = CarsUnificationCollection
        .newBuilder()
        .addEntries(
          CarsUnificationEntry
            .newBuilder()
            .setRawMark("mark1")
            .setRawModel("model1")
            .setRawBodyType("body1")
            .setRawYear("2001")
            .setRawPower("1000 л.с.")
            .setRawTransmission("MT")
            .setRawGearType("Передний")
            .setRawEngineType("Бензин")
            .setTechParamId(101)
        )
        .addEntries(
          CarsUnificationEntry
            .newBuilder()
            .setRawMark("mark2")
            .setRawModel("model2")
            .setRawBodyType("body2")
            .setRawYear("2002")
            .setRawPower("290 л.с.")
            .setRawTransmission("AT")
            .setRawIs4Wd("false")
            .setRawDisplacement("3.5")
            .setRawEngineType("Дизель")
            .setTechParamId(102)
        )
        .addEntries(otherEntry)
        .build()
      val unificatorResponseByTechParam = TechInfoList
        .newBuilder()
        .addEntities(
          TechInfo
            .newBuilder()
            .setMarkInfo(
              CatalogModel.Mark
                .newBuilder()
                .setCode("Acura")
            )
            .setModelInfo(
              CatalogModel.Model
                .newBuilder()
                .setCode("MDX, III")
            )
            .setSuperGen(
              CatalogModel.SuperGeneration
                .newBuilder()
                .setId(155003)
            )
            .setConfiguration(
              CatalogModel.Configuration
                .newBuilder()
                .setId(111003)
                .setBodyType("Внедорожник 5 дв.")
            )
            .setTechParam(
              CatalogModel.TechParam
                .newBuilder()
                .setId(103)
                .setPower(290)
                .setTransmissionAutoru("AT")
                .setDisplacement(3500)
                .setEngineType("GASOLINE")
                .setGearTypeAutoru("REAR")
            )
        )
        .addEntities(TechInfo.newBuilder().build())
        .build()

      val unificatorClient = mock[UnificatorClient]
      when(unificatorClient.carsUnify(?)).thenReturn(Future.successful(unificationResponse))
      when(unificatorClient.infoByTechParamIds(?))
        .thenReturn(Future.successful(unificatorResponseByTechParam))
      val mapper = new UnificatorMapper(unificatorClient, config, kvClient)
      val (pub, sub) = createPubSub(mapper.flow())
      sub.request(5)
      pub.sendNext(OfferMessage(offer1))
      pub.sendNext(OfferMessage(offer2))
      pub.sendNext(OfferMessage(offer3))
      pub.sendNext(StreamEndMessage(tc))
      val offers = sub
        .expectNextN(3)
        .map { case OfferMessage(offer: AutoruExternalOffer) => offer }
        .sortBy(_.unification.get.techParamId.get)
      sub.expectNextPF { case StreamEndMessage(_, _, _) => }
      sub.expectNoMessage(50.millis)

      offers(0).unification.get.techParamId.get shouldEqual 101
      offers(1).unification.get.techParamId.get shouldEqual 102
      offers(2).unification.get shouldEqual Unification(
        Some("Acura"),
        Some("MDX, III"),
        Some("Внедорожник 5 дв."),
        Some("AT"),
        Some(155003),
        Some(111003),
        Some(103),
        Some("GASOLINE"),
        Some("REAR"),
        Some(290),
        Some(3500),
        None
      )

      verify(unificatorClient).carsUnify(?)
      verify(unificatorClient).infoByTechParamIds(Seq(103))
    }
  }
}
