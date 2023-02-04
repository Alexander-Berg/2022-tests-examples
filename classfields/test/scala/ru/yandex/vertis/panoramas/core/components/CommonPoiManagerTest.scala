package ru.yandex.vertis.panoramas.core.components

import ru.yandex.vertis.mockito.MockitoSupport.mock
import ru.yandex.vertis.panoramas.core.components.db.Shard
import ru.yandex.vertis.panoramas.core.dao.KafkaEventsDao
import ru.yandex.vertis.panoramas.core.dao.exterior.ExteriorPoiDao
import ru.yandex.vertis.panoramas.core.models.common.Poi.Point
import ru.yandex.vertis.panoramas.core.models.common.Poi.Property.Image
import ru.yandex.vertis.panoramas.core.models.common.Poi.Property.Image.Preview
import ru.yandex.vertis.panoramas.core.models.exterior.ExteriorPoi
import ru.yandex.vertis.panoramas.core.models.exterior.ExteriorPoi.ExteriorProperties
import ru.yandex.vertis.panoramas.core.services.cv.CvClient
import ru.yandex.vertis.panoramas.core.services.mds.MdsClient
import ru.yandex.vertis.panoramas.util.BaseSpec

import scala.concurrent.ExecutionContext

class CommonPoiManagerTest extends BaseSpec {

  trait mocks {
    val shard = mock[Shard]
    val poiDao = mock[ExteriorPoiDao]
    val panoramaManager = mock[ExteriorPanoramasManager]
    val cvClient = mock[CvClient]
    val cvPrefix = "prefix"
    val mdsClient = mock[MdsClient]
    val kafkaEventsDao = mock[KafkaEventsDao]
    val ec = mock[ExecutionContext]

    val merger =
      new ExteriorPoiManager(shard, poiDao, panoramaManager, cvClient, cvPrefix, mdsClient, kafkaEventsDao)(ec)
  }

  "DefaultMerger" should {
    "update previous poi if next poi is different" in new mocks {
      val previewPrev = Preview(1, 24, 24, "123".getBytes().toVector)
      val imagePrev = Image("link1", Option(previewPrev), Seq.empty, Image.State.Used)
      val poiPrev = ExteriorPoi(
        point = Point("id1", None, None),
        properties = ExteriorProperties(None, None, None, Seq(imagePrev)),
        frameInfo = None
      )

      val previewCurr = Preview(1, 24, 24, "123".getBytes().toVector)
      val imageCurr = Image("link1", Option(previewCurr), Seq.empty, Image.State.Used)
      val poiCurr = ExteriorPoi(
        point = Point("id1", None, None),
        properties = ExteriorProperties(None, None, None, Seq(imageCurr)),
        frameInfo = None
      )

      val previewNext = Preview(1, 24, 24, "456".getBytes().toVector)
      val imageNext = Image("link2", Option(previewNext), Seq.empty, Image.State.New)
      val poiNext = ExteriorPoi(
        point = Point("id1", None, None),
        properties = ExteriorProperties(None, None, None, Seq(imageNext)),
        frameInfo = None
      )

      val resultPoi = merger.merge(poiPrev, poiCurr, poiNext)

      resultPoi.point.id shouldEqual "id1"

      val resultImage = resultPoi.properties.images.head
      resultImage.link shouldEqual "link2"
      resultImage.preview.head.data shouldEqual "456".getBytes().toVector
    }
  }
}
