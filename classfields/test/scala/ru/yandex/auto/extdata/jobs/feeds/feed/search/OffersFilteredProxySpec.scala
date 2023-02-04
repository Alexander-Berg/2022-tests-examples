package ru.yandex.auto.extdata.jobs.feeds.feed.search

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.message.CarAdSchema.CarAdMessage
import ru.yandex.auto.yocto.SearcherProxy

import scala.util.Random

/**
  * Created by theninthowl on 11/1/21
  */
@RunWith(classOf[JUnitRunner])
class OffersFilteredProxySpec extends WordSpecLike with Matchers {

  private val carAdMessageGen: Gen[CarAdMessage] =
    for {
      id <- Gen.chooseNum(10000000, 20000000)
    } yield {
      val builder = CarAdMessage
        .newBuilder()
        .setVersion(1)
        .setId(id.toString)
      builder.build()
    }

  private val offers = Gen.listOfN(100, carAdMessageGen).sample.get.toSet
  private val forbiddenOfferIds = offers.take(30).map(_.getId)

  class BaseProxy extends SearcherProxy[AnyRef, CarAdMessage] {
    override def foreach(q: AnyRef)(f: CarAdMessage => Unit): Unit = offers.foreach(f)
  }

  private def createSearcherProxy(ids: Iterable[String]): SearcherProxy[AnyRef, CarAdMessage] =
    new BaseProxy with OffersFilteredProxy[AnyRef, CarAdMessage] {
      override protected def offerIds: Set[String] = ids.toSet
      override protected def idExtractor: OffersFilteredProxy.IdExtractor[CarAdMessage] = implicitly
    }

  private def collect[T](searcherProxy: SearcherProxy[AnyRef, T]): Seq[T] = {
    var collector = List.empty[T]
    searcherProxy.foreach(null)(msg => collector = msg :: collector)
    collector
  }

  "OfferFilteredProxy" should {
    "correctly filter offers" in {
      val proxy = createSearcherProxy(forbiddenOfferIds)
      val filteredOffers = collect(proxy)
      filteredOffers.map(_.getId) should not contain forbiddenOfferIds
    }
  }

  "OfferFilteredProxy" should {
    "ignore empty filter" in {
      val proxy = createSearcherProxy(Iterable.empty[String])
      val filteredOffers = collect(proxy)
      offers.size shouldBe filteredOffers.size
    }
  }

}
