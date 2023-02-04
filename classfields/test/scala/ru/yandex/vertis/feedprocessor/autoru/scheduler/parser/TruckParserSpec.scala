package ru.yandex.vertis.feedprocessor.autoru.scheduler.parser

import java.io.InputStream
import ru.auto.api.ApiOfferModel.Section
import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.autoru.model.{ExternalOfferError, SaleCategories, TaskContext}
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.TruckExternalOffer
import ru.yandex.vertis.feedprocessor.autoru.scheduler.parser.truck.TruckParser
import ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.parser.Parser
import ru.yandex.vertis.feedprocessor.autoru.model.Generators._

import scala.annotation.nowarn

/**
  * @author pnaydenov
  */
@nowarn("msg=deprecated")
class TruckParserSpec extends WordSpecBase {

  val task = newTasksGen.next

  val parser = new Parser[TruckExternalOffer] {

    override def parse(
        feed: InputStream
      )(implicit tc: TaskContext): Iterator[Either[ExternalOfferError, TruckExternalOffer]] =
      new TruckParser(feed)
  }

  "TruckParser" should {
    "parse float price" in {
      implicit val tc = TaskContext(
        task.copy(serviceInfo =
          task.serviceInfo
            .copy(autoru =
              task.serviceInfo.autoru.copy(sectionId = Section.NEW.getNumber, saleCategoryId = SaleCategories.Bus.id)
            )
        )
      )
      val feed = getClass.getResourceAsStream("/bus_feed_new.xml")
      val offers = parser.parse(feed).toList
      offers should have size (1)
      assert(offers.head.isRight)
      val offer = offers.head.right.get
      offer.price shouldEqual 1993800
      offer.badges shouldEqual Seq("Парктроник", "Коврики в подарок", "На гарантии")
    }

    "don't touch cache breaker if unnecessary" in {
      implicit val tc: TaskContext = TaskContext(
        task.copy(serviceInfo =
          task.serviceInfo
            .copy(autoru =
              task.serviceInfo.autoru.copy(
                sectionId = Section.NEW.getNumber,
                saleCategoryId = SaleCategories.Bus.id,
                imagesCacheBreaker = None
              )
            )
        )
      )
      val feed = getClass.getResourceAsStream("/bus_feed_new.xml")
      val offersP = parser.parse(feed).toList
      val offers = offersP.head.right.get
      offers.images.toSet shouldEqual Set(
        "http://aaa24.ru/files/ad/1255/930520_15029717370.jpg",
        "http://aaa24.ru/files/ad/1255/930520_15029717381.jpg?foo=baz",
        "http://aaa24.ru/files/ad/1255/930520_15029717392.jpg"
      )
    }

    "add cache breaker if required" in {
      implicit val tc: TaskContext = TaskContext(
        task.copy(serviceInfo =
          task.serviceInfo
            .copy(autoru =
              task.serviceInfo.autoru.copy(
                sectionId = Section.NEW.getNumber,
                saleCategoryId = SaleCategories.Bus.id,
                imagesCacheBreaker = Some(7)
              )
            )
        )
      )
      val feed = getClass.getResourceAsStream("/bus_feed_new.xml")
      val offers = parser.parse(feed).toList.head.right.get
      offers.images.toSet shouldEqual Set(
        "http://aaa24.ru/files/ad/1255/930520_15029717370.jpg?autorucb=7",
        "http://aaa24.ru/files/ad/1255/930520_15029717381.jpg?foo=baz&autorucb=7",
        "http://aaa24.ru/files/ad/1255/930520_15029717392.jpg?autorucb=7"
      )
    }
  }
}
