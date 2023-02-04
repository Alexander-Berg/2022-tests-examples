package ru.yandex.auto.searcher.http.action.api

import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.auto.searcher.filters.MarkModelFilters.MarkModelFiltersResultMessage
import scala.collection.JavaConverters._

class MarkModelFilterDataHolderSpec extends FlatSpec with Matchers {

  def handlingResult(offers: (String, String)*): MarkModelFiltersResultMessage = {
    val holder = new MarkModelFilterDataHolder()
    offers.foreach {
      case (mark, model) =>
        holder.handle(mark, model)
    }
    holder.buildResult
  }

  "holder" should "handle single offer" in {

    val result = handlingResult(
      ("FORD", "FOCUS")
    )

    val markList = result.getMarkEntriesList.asScala
    val mark = markList.find(x => x.getMarkCode == "FORD").get

    val modelList = mark.getModelsList.asScala
    val model = modelList.find(_.getModelCode == "FOCUS").get

    val total = result.getHandledOffers

    total shouldBe 1
    markList.size shouldBe 1

    modelList.size shouldBe 1
    model.getOffersCount shouldBe 1
  }

  "holder" should "handle two identical offers" in {

    val result = handlingResult(
      ("FORD", "FOCUS"),
      ("FORD", "FOCUS")
    )

    val markList = result.getMarkEntriesList.asScala
    val mark = markList.find(x => x.getMarkCode == "FORD").get

    val modelList = mark.getModelsList.asScala
    val model = modelList.find(_.getModelCode == "FOCUS").get

    val total = result.getHandledOffers

    total shouldBe 2
    markList.size shouldBe 1

    modelList.size shouldBe 1
    model.getOffersCount shouldBe 2
  }

  "holder" should "handle two different offers" in {

    val result = handlingResult(
      ("FORD", "FOCUS"),
      ("AUDI", "TT")
    )

    val markList = result.getMarkEntriesList.asScala
    val ford = markList.find(x => x.getMarkCode == "FORD").get
    val audi = markList.find(x => x.getMarkCode == "AUDI").get

    val fordModels = ford.getModelsList.asScala
    val fordFocus = ford.getModelsList.asScala.find(_.getModelCode == "FOCUS").get

    val audiModels = audi.getModelsList.asScala
    val audiTT = audi.getModelsList.asScala.find(_.getModelCode == "TT").get

    val total = result.getHandledOffers

    total shouldBe 2
    markList.size shouldBe 2

    fordModels.size shouldBe 1
    audiModels.size shouldBe 1

    fordFocus.getOffersCount shouldBe 1
    audiTT.getOffersCount shouldBe 1
  }

  "holder" should "handle two identical offers and one different offer" in {

    val result = handlingResult(
      ("FORD", "FOCUS"),
      ("FORD", "FOCUS"),
      ("AUDI", "TT")
    )

    val markList = result.getMarkEntriesList.asScala
    val ford = markList.find(x => x.getMarkCode == "FORD").get
    val audi = markList.find(x => x.getMarkCode == "AUDI").get

    val fordModels = ford.getModelsList.asScala
    val fordFocus = ford.getModelsList.asScala.find(_.getModelCode == "FOCUS").get

    val audiModels = audi.getModelsList.asScala
    val audiTT = audi.getModelsList.asScala.find(_.getModelCode == "TT").get

    val total = result.getHandledOffers

    total shouldBe 3
    markList.size shouldBe 2

    fordModels.size shouldBe 1
    audiModels.size shouldBe 1

    fordFocus.getOffersCount shouldBe 2
    audiTT.getOffersCount shouldBe 1
  }
}
