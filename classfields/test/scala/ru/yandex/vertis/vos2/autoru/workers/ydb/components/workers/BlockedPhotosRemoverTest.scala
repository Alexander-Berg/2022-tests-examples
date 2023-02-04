package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.junit.runner.RunWith
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.BeforeAndAfterAll
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.autoru.dao.blockedphotohashes.BlockedPhotoHashesDao

@RunWith(classOf[JUnitRunner])
class BlockedPhotosRemoverTest extends AnyWordSpec with MockitoSupport with Matchers with BeforeAndAfterAll {
  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {
    val dao = mock[BlockedPhotoHashesDao]

    val worker = new BlockedPhotosRemover(dao) with YdbWorkerTestImpl
  }

  "BlockedPhotosRemover" should {
    "block photos" in new Fixture {
      // TODO
    }
  }
}
