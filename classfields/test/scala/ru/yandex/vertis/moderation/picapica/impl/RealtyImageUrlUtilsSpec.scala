package ru.yandex.vertis.moderation.picapica.impl

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.picapica.PicaService.ImageId
import ru.yandex.vertis.moderation.picapica.impl.RealtyImageUrlUtilsSpec.Source
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer._

/**
  * Specs for [[RealtyImageUrlUtils]]
  *
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
class RealtyImageUrlUtilsSpec extends SpecBase {

  "RealtyImageUrlUtils" should {

    val SourcesForImageId: Seq[Source] =
      Seq(
        Source(
          "//avatars.mdst.yandex.net/get-realty/3019/offer.6300461494541429459.1870183845150999684",
          Some("1870183845150999684")
        ),
        Source(
          "//avatars.mdst.yandex.net/get-realty/3019/offer.6300461494541429459.1870183845150999684/",
          Some("1870183845150999684")
        ),
        Source(
          "//avatars.mdst.yandex.net/get-realty/3220/add.b58eb7ba62ef8817e285f95deb5da497.realty-api-vos/",
          Some("add.b58eb7ba62ef8817e285f95deb5da497.realty-api-vos")
        ),
        Source(
          "//avatars.mdst.yandex.net/get-realty/3220/add.b58eb7ba62ef8817e285f95deb5da497.realty-api-vos",
          Some("add.b58eb7ba62ef8817e285f95deb5da497.realty-api-vos")
        ),
        Source(
          "//avatars.mdst.yandex.net/get-realty/2957/add.1518462104179ae7f81dbfd/",
          Some("add.1518462104179ae7f81dbfd")
        ),
        Source(
          "//avatars.mdst.yandex.net/get-realty/2957/add.1518462104179ae7f81dbfd",
          Some("add.1518462104179ae7f81dbfd")
        ),
        Source(
          "//avatars.mds.yandex.net/get-realty/965271/add.1518528878621fc628ba125.15185288611431516e17c86.90/",
          Some("add.1518528878621fc628ba125.15185288611431516e17c86.90")
        ),
        Source(
          "//avatars.mds.yandex.net/get-realty/965271/add.1518528878621fc628ba125.15185288611431516e17c86.90",
          Some("add.1518528878621fc628ba125.15185288611431516e17c86.90")
        ),
        Source("", None),
        Source("//", None),
        Source("//avatars.mdst.yandex.net/get-realty_3019/offer.6300461494541429459.1870183845150999684", None),
        Source("//avatars.mdst.yandex.net/get-realtY/3019/offer.6300461494541429459.1870183845150999684", None),
        Source("//avatars.mdst.yandex.net/get-realty/3019/offer_6300461494541429459.1870183845150999684", None),
        Source("//avatars.mdst.yandex.net/get-realty/3019/add_b58eb7ba62ef8817e285f95deb5da497.realty-api-vos", None),
        Source("//avatars.mdst.yandex.net/get-realty/3019/add.Ab58eb7ba62ef8817e285f95deb5da497.realty-api-vos", None),
        Source(
          "//avatars.mds.yandex.net/get-realty/965271/add.1518528878621fc628ba125.15185288611431516e17c86/90",
          None
        )
      )

    SourcesForImageId.foreach { source =>
      s"provide correct ImageId for $source" in {
        val srcUrl = StringGen.next
        RealtyImageUrlUtils.getImageId(source.url, Some(srcUrl)) should be(source.expected.map(ImageId(_, srcUrl)))
      }
    }

    val SourcesForTaskId: Seq[Source] =
      Seq(
        Source(
          "//avatars.mdst.yandex.net/get-realty/3019/offer.6300461494541429459.1870183845150999684",
          Some("offer.6300461494541429459")
        ),
        Source(
          "//avatars.mdst.yandex.net/get-realty/3019/offer.6300461494541429459.1870183845150999684/",
          Some("offer.6300461494541429459")
        ),
        Source("//avatars.mdst.yandex.net/get-realty/3019/offEr.6300461494541429459.1870183845150999684/", None),
        Source("//avatars.mdst.yandex.net/get-realty/3220/add.b58eb7ba62ef8817e285f95deb5da497.realty-api-vos/", None),
        Source("//avatars.mdst.yandex.net/get-realty/3220/add_b58eb7ba62ef8817e285f95deb5da497.realty-api-vos/", None)
      )

    SourcesForTaskId.foreach { source =>
      s"provide correct TaskId for $source" in {
        RealtyImageUrlUtils.getTaskId(source.url) should be(source.expected)
      }
    }

  }

}

object RealtyImageUrlUtilsSpec {

  case class Source(url: String, expected: Option[String])
}
