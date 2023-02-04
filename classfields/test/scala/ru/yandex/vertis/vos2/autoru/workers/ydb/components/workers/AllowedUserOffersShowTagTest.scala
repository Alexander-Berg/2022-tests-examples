package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.tag_processor.substages.AllowedUserOffersShowTag
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.tag_processor.substages.AllowedUserOffersShowTag._
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.model.ModelUtils.RichOfferBuilder

import scala.jdk.CollectionConverters._

class AllowedUserOffersShowTagTest extends AnyWordSpec {

  implicit val traced: Traced = Traced.empty

  private val OtherTag: String = "other-tag"

  abstract private class Fixture {
    val offerBuilder = TestUtils.createOffer()
    val worker = new AllowedUserOffersShowTag() {}
  }

  private def assertContainsTags(offer: Offer, tags: Set[String], worker: AllowedUserOffersShowTag): Unit = {
    val result = worker.process(offer, None).updateOfferFunc.get(offer)
    assert(result.getTagList.size == tags.size)
    assert(result.getTagList.asScala.forall(tags.contains))
  }

  ("add tag") in new Fixture {
    offerBuilder.getUserBuilder.getProfileBuilder.setAllowOffersShow(true)
    offerBuilder.clearTag().putTag(OtherTag)
    val offer = offerBuilder.build()
    assertContainsTags(offer, Set(OtherTag, Tag), worker)
  }

  ("drop tag") in new Fixture {
    offerBuilder.getUserBuilder.getProfileBuilder.setAllowOffersShow(false)
    offerBuilder.clearTag().putTag(OtherTag).putTag(Tag)
    val offer = offerBuilder.build()
    assertContainsTags(offer, Set(OtherTag), worker)
  }
}
