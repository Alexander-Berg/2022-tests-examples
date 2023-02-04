package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.MultipostingWorkerYdb.{AutoruTag, AvitoTag, DromTag, NotPostedTag}
import ru.yandex.vos2.AutoruModel.AutoruOffer.PaidService
import ru.yandex.vos2.BasicsModel.CompositeStatus
import ru.yandex.vos2.OfferModel
import ru.yandex.vos2.OfferModel.Multiposting.Classified
import ru.yandex.vos2.OfferModel.Multiposting.Classified.ClassifiedName
import ru.yandex.vos2.OfferModel.OfferFlag
import ru.yandex.vos2.autoru.model.TestUtils.{createClassified, createMultiposting, createOffer}

import scala.jdk.CollectionConverters._
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class MultipostingWorkerYdbTest extends AnyWordSpec with MockitoSupport with Matchers with BeforeAndAfterAll {
  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {

    val worker = new MultipostingWorkerYdb with YdbWorkerTestImpl
  }
  "Multiposting tag actualizer stage" should {
    "process offer" in new Fixture {
      val offerBuilder = createOffer()
      offerBuilder.setMultiposting(OfferModel.Multiposting.newBuilder().build())
      val offer = offerBuilder.build()

      assert(worker.shouldProcess(offer, None).shouldProcess)
    }

    "not process offer" in new Fixture {
      val offerBuilder = createOffer()
      val offer = offerBuilder.build()

      assert(!worker.shouldProcess(offer, None).shouldProcess)
    }

    "add NotPosted tag - empty classified list" in new Fixture {
      val offerBuilder = createOffer()
      offerBuilder.setMultiposting(createMultiposting().build())
      val offer = offerBuilder.build()

      val result = worker.process(offer, None).updateOfferFunc.get(offer)
      val tagSet = result.getTagList.asScala.toSet

      tagSet shouldEqual Set(NotPostedTag)
    }

    "add NotPosted tag - no active classifieds" in new Fixture {
      val offerBuilder = createOffer()
      val multiposting = createMultiposting()
        .addAllClassifieds(
          Seq(
            createClassified(name = ClassifiedName.AUTORU, enabled = false).build(),
            createClassified(name = ClassifiedName.AVITO, enabled = false).build(),
            createClassified(name = ClassifiedName.DROM, enabled = false).build()
          ).asJava
        )
      offerBuilder.setMultiposting(multiposting)
      val offer = offerBuilder.build()

      val result = worker.process(offer, None).updateOfferFunc.get(offer)
      val tagSet = result.getTagList.asScala.toSet

      tagSet shouldEqual Set(NotPostedTag)
    }

    "add active classifieds tags" in new Fixture {
      val offerBuilder = createOffer()
      val multiposting = createMultiposting()
        .addAllClassifieds(
          Seq(
            createClassified(name = ClassifiedName.AUTORU).build(),
            createClassified(name = ClassifiedName.AVITO).build(),
            createClassified(name = ClassifiedName.DROM).build()
          ).asJava
        )
      offerBuilder.setMultiposting(multiposting)
      val offer = offerBuilder.build()

      val result = worker.process(offer, None).updateOfferFunc.get(offer)
      val tagSet = result.getTagList.asScala.toSet

      tagSet shouldEqual Set(AutoruTag, AvitoTag, DromTag)
    }

    "filter not active classifieds tags" in new Fixture {
      val offerBuilder = createOffer()
      val multiposting = createMultiposting()
        .addAllClassifieds(
          Seq(
            createClassified(name = ClassifiedName.AUTORU).build(),
            createClassified(name = ClassifiedName.AVITO, enabled = false).build(),
            createClassified(name = ClassifiedName.DROM, enabled = false).build()
          ).asJava
        )
      offerBuilder.setMultiposting(multiposting)
      val offer = offerBuilder.build()

      val result = worker.process(offer, None).updateOfferFunc.get(offer)
      val tagSet = result.getTagList.asScala.toSet

      tagSet shouldEqual Set(AutoruTag)
    }
  }

  "Multiposting services actualizer stage" should {
    "sync auto.ru services" in new Fixture {
      val offer = createOffer(dealer = true)

      offer.getOfferAutoruBuilder
        .clearServices()
        .addServices {
          PaidService
            .newBuilder()
            .setServiceType(PaidService.ServiceType.ADD)
            .setCreated(123L)
            .setExpireDate(456L)
            .setIsActive(true)
        }
        .addServices {
          PaidService
            .newBuilder()
            .setServiceType(PaidService.ServiceType.ADD)
            .setCreated(234L)
            .setExpireDate(567L)
            .setIsActive(false)
        }
        .addServices {
          PaidService
            .newBuilder()
            .setServiceType(PaidService.ServiceType.COLOR)
            .setCreated(555L)
            .setExpireDate(777L)
            .setIsActive(true)
        }

      offer.setMultiposting {
        createMultiposting().addClassifieds {
          createClassified(name = ClassifiedName.AUTORU)
            .addServices {
              Classified.Service
                .newBuilder()
                .setService("SOME_CLASSIFIED_SERVICE")
                .setCreateDate(111L)
                .setExpireDate(333L)
                .setIsActive(false)
            }
            .build()
        }
      }

      val result = worker.process(offer.build(), None).updateOfferFunc.get(offer.build())

      val autoRuClassified = result.getMultiposting.getClassifiedsList.asScala
        .find(_.getName == ClassifiedName.AUTORU)
        .get

      autoRuClassified.getServicesList.asScala.toList should contain theSameElementsAs Seq(
        Classified.Service
          .newBuilder()
          .setService("ADD")
          .setCreateDate(123L)
          .setExpireDate(456L)
          .setIsActive(true)
          .build(),
        Classified.Service
          .newBuilder()
          .setService("ADD")
          .setCreateDate(234L)
          .setExpireDate(567L)
          .setIsActive(false)
          .build(),
        Classified.Service
          .newBuilder()
          .setService("COLOR")
          .setCreateDate(555L)
          .setExpireDate(777L)
          .setIsActive(true)
          .build()
      )
    }

    "sync auto.ru services remove multiposting services when they are empty in autoru offer" in new Fixture {
      val offer = createOffer(dealer = true)

      offer.getOfferAutoruBuilder
        .clearServices()

      offer.setMultiposting {
        createMultiposting().addClassifieds {
          createClassified(name = ClassifiedName.AUTORU)
            .addServices {
              Classified.Service
                .newBuilder()
                .setService("SOME_CLASSIFIED_SERVICE")
                .setCreateDate(110L)
                .setExpireDate(332L)
                .setIsActive(false)
            }
            .addServices {
              Classified.Service
                .newBuilder()
                .setService("SOME_CLASSIFIED_SERVICE_2")
                .setCreateDate(111L)
                .setExpireDate(333L)
                .setIsActive(false)
            }
            .build()
        }
      }

      val result = worker.process(offer.build(), None).updateOfferFunc.get(offer.build())

      val autoRuClassified = result.getMultiposting.getClassifiedsList.asScala
        .find(_.getName == ClassifiedName.AUTORU)
        .get

      autoRuClassified.getServicesList should have size 0
    }

    "set expire_date for classifieds services if empty" in new Fixture {
      val offer = createOffer(dealer = true)
      val testClassified = ClassifiedName.AVITO

      offer.setMultiposting {
        createMultiposting().addClassifieds {
          createClassified(name = testClassified)
            .addServices {
              Classified.Service
                .newBuilder()
                .setService("X10_1")
                .setCreateDate(111L)
                .setIsActive(false)
            }
            .addServices {
              Classified.Service
                .newBuilder()
                .setService("X10_7")
                .setCreateDate(222L)
                .setIsActive(false)
            }
            .addServices {
              Classified.Service
                .newBuilder()
                .setService("X10_7")
                .setCreateDate(333L)
                .setExpireDate(444L)
                .setIsActive(true)
            }
            .build()
        }
      }

      val result = worker.process(offer.build(), None).updateOfferFunc.get(offer.build())

      val classified = result.getMultiposting.getClassifiedsList.asScala
        .find(_.getName == testClassified)
        .get

      classified.getServicesList.asScala.toList should contain theSameElementsAs Seq(
        Classified.Service
          .newBuilder()
          .setService("X10_1")
          .setCreateDate(111L)
          .setExpireDate(86400111L) // 1 day
          .setIsActive(false)
          .build(),
        Classified.Service
          .newBuilder()
          .setService("X10_7")
          .setCreateDate(222L)
          .setExpireDate(604800222L) // 7 days
          .setIsActive(false)
          .build(),
        Classified.Service
          .newBuilder()
          .setService("X10_7")
          .setCreateDate(333L)
          .setExpireDate(444L) // already filled
          .setIsActive(true)
          .build()
      )
    }
    "check already processed offer" in new Fixture {
      val offer = createOffer(dealer = true)
      val testClassified = ClassifiedName.AVITO

      offer.setMultiposting {
        createMultiposting().addClassifieds {
          createClassified(name = testClassified)
            .addServices {
              Classified.Service
                .newBuilder()
                .setService("X10_1")
                .setCreateDate(111L)
                .setIsActive(false)
            }
            .addServices {
              Classified.Service
                .newBuilder()
                .setService("X10_7")
                .setCreateDate(222L)
                .setIsActive(false)
            }
            .addServices {
              Classified.Service
                .newBuilder()
                .setService("X10_7")
                .setCreateDate(333L)
                .setExpireDate(444L)
                .setIsActive(true)
            }
            .build()
        }
      }

      val result = worker.process(offer.build(), None).updateOfferFunc.get(offer.build())
      assert(!worker.shouldProcess(result, None).shouldProcess)

    }
  }

  "Multiposting stage: sync auto.ru status" should {
    "sync auto.ru classified status" in new Fixture {
      val offer = createOffer(dealer = true)

      offer.addFlag(OfferFlag.OF_MIGRATED).addFlag(OfferFlag.OF_EXPIRED)

      offer.setMultiposting {
        createMultiposting().addClassifieds {
          createClassified(name = ClassifiedName.AUTORU)
            .setStatus(CompositeStatus.CS_UNKNOWN)
            .build()
        }
      }

      val result = worker.process(offer.build(), None).updateOfferFunc.get(offer.build())

      val autoRuClassified = result.getMultiposting.getClassifiedsList.asScala
        .find(_.getName == ClassifiedName.AUTORU)
        .get

      autoRuClassified.getStatus shouldEqual CompositeStatus.CS_EXPIRED
    }

    "don't sync another classified status" in new Fixture {
      val offer = createOffer(dealer = true)

      offer.setMultiposting {
        createMultiposting().addClassifieds {
          createClassified(name = ClassifiedName.AVITO)
            .setStatus(CompositeStatus.CS_TRUSTED)
            .build()
        }
      }

      val result = worker.process(offer.build(), None).updateOfferFunc.get(offer.build())

      val classified = result.getMultiposting.getClassifiedsList.asScala
        .find(_.getName == ClassifiedName.AVITO)
        .get

      classified.getStatus shouldEqual CompositeStatus.CS_TRUSTED
    }
  }

}
