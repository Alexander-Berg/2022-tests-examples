package ru.yandex.vertis.picapica.serivice.impl


import java.util.concurrent.atomic.AtomicBoolean

import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.vertis.picapica.client.ApiVersion
import ru.yandex.vertis.picapica.client.msg.PicaPicaSchema.Metadata
import ru.yandex.vertis.picapica.dao.{AvatarsMetaDao, StorageDao}
import ru.yandex.vertis.picapica.model.AvatarsResponse.AvatarsData
import ru.yandex.vertis.picapica.model._
import ru.yandex.vertis.picapica.service.AvatarsMetaService._
import ru.yandex.vertis.picapica.service.impl.AvatarsMetaServiceImpl

import scala.concurrent.{ExecutionContext, Future}

/**
  * @author @logab
  */
// scalastyle:off
@RunWith(classOf[JUnitRunner])
class AvatarsMetaServiceImplSpec extends WordSpecLike with Matchers with ScalaFutures with MockitoSugar {
  implicit val ec = new ExecutionContext {
    override def reportFailure(cause: Throwable): Unit = {}

    override def execute(runnable: Runnable): Unit = runnable.run()
  }

  val RequiredField = "required_field"

  abstract class Fixture {
    val avatarMetaDao = mock[AvatarsMetaDao]
    val storageDao = mock[StorageDao]
    val metaFactory = mock[MetaFactory]
    val id: Id = Id("a", "b")
    val actualGroupId: Int = 1
    val actualName: String = "fffuuu"
    val ava = AvatarsData(actualGroupId, actualName, None)
    val partitionId = 1

    def taskRequest(regenerate: Option[Condition], download: Option[Condition], actualVersion: Option[Int] = Some(5),
                    meta: Map[String, String] = Map.empty, apiVer: ApiVersion = ApiVersion.V3) = {
      val regenerateArg = regenerate
      val downloadArg = download
      new MetaTaskRequest(Realty, id, ava, partitionId, actualVersion, newRecord = false, apiVersion = apiVer,
                          priority = 0) {
        override def regenerate(storedData: Option[StoredAvatarsData],
                                requiredFields: Set[String]): Option[Condition] = regenerateArg
        override def download(storedData: Option[StoredAvatarsData],
                              requiredFields: Set[String]): Option[Condition] = downloadArg
      }
    }

    when(avatarMetaDao.getMeta(eqTo(id), eqTo(ava))(any())).
      thenReturn(Future.successful((Map("new_field" -> "new_value"), Metadata.newBuilder().setIsFinished(true))))
    when(avatarMetaDao.deleteMeta(eqTo(id), eqTo(ava))).thenReturn(Future.successful(true))
    when(storageDao.storeMeta(eqTo(id), any(), any())(any())).thenReturn(Future.successful(()))
    when(storageDao.get(eqTo(partitionId), eqTo(Vector(id)))).thenReturn(Future.successful(
      List(id → StoredAvatarsData(AvatarsData(actualGroupId, actualName, None), Map.empty, None))
    ))
  }

  trait Mocks { this: AvatarsMetaServiceImpl =>
    override protected def shouldDo: Boolean = true
  }

  "meta service" should {
    "not download when not required" in new Fixture {
      val service = new AvatarsMetaServiceImpl(avatarMetaDao, storageDao, Auto, metaFactory) {
        override protected def shouldDo: Boolean = false
      }
      service.processMeta(taskRequest(regenerate = None, download = Some(Sometimes))).futureValue
      verify(avatarMetaDao, never()).deleteMeta(any(), any())
      verify(avatarMetaDao, never()).getMeta(any(), any())(any())
      verify(storageDao, never()).storeMeta(any(), any(), any())(any())
    }

    "always regenerate" in new Fixture {
      val service = new AvatarsMetaServiceImpl(avatarMetaDao, storageDao, Auto, metaFactory) {
        override protected def shouldDo: Boolean = fail("should not be invoked")
      }
      service.processMeta(taskRequest(regenerate = Some(Always), download = None)).futureValue
      verify(avatarMetaDao).getMeta(eqTo(id), eqTo(ava))(any())
      verify(avatarMetaDao).deleteMeta(eqTo(id), eqTo(ava))
      verify(storageDao).storeMeta(any(), any(), any())(any())
    }

    "conditionally regenerate" in new Fixture {
      val invoked = new AtomicBoolean(false)
      val service = new AvatarsMetaServiceImpl(avatarMetaDao, storageDao, Auto, metaFactory) {
        override protected def shouldDo: Boolean = {
          invoked.set(true)
          true
        }
      }
      service.processMeta(taskRequest(regenerate = Some(Sometimes), download = None)).futureValue
      verify(avatarMetaDao).getMeta(eqTo(id), eqTo(ava))(any())
      verify(avatarMetaDao).deleteMeta(eqTo(id), eqTo(ava))
      verify(storageDao).storeMeta(any(), any(), any())(any())
      assert(invoked.get(), "Should check probability")
    }

    "always download" in new Fixture {
      val service = new AvatarsMetaServiceImpl(avatarMetaDao, storageDao, Auto, metaFactory){
        override protected def shouldDo: Boolean = fail("should not be invoked")
      }
      service.processMeta(taskRequest(regenerate = None, download = Some(Always))).futureValue
      verify(avatarMetaDao).getMeta(eqTo(id), any())(any())
      verify(avatarMetaDao, never()).deleteMeta(any(), any())
      verify(storageDao).storeMeta(eqTo(id), any(), any())(any())
    }

    "conditionally download" in new Fixture {
      val invoked = new AtomicBoolean(false)
      val service = new AvatarsMetaServiceImpl(avatarMetaDao, storageDao, Auto, metaFactory){
        override protected def shouldDo: Boolean = {
          invoked.set(true)
          false
        }
      }
      service.processMeta(taskRequest(regenerate = None, download = Some(Sometimes))).futureValue
      verify(avatarMetaDao, never()).getMeta(any(), any())(any())
      verify(avatarMetaDao, never()).deleteMeta(any(), any())
      verify(storageDao, never()).storeMeta(any(), any(), any())(any())
      assert(invoked.get(), "Should check probability")
    }

    "append version value to new meta" in new Fixture {
      val service = new AvatarsMetaServiceImpl(avatarMetaDao, storageDao, Auto, metaFactory) with Mocks
      service.processMeta(taskRequest(regenerate = None, download = Some(Always),
        actualVersion = Some(5), meta = Map("old_field" -> "old_value"),
        apiVer = ApiVersion.V2)).futureValue
      verify(storageDao).storeMeta(any(),
        eqTo(Map(/*"old_field" -> "old_value",*/
                 "new_field" -> "new_value",
                 "version"   -> "5")), any()) (any())
    }

    "regeneration rule has greater priority" in new Fixture {
      val invoked = new AtomicBoolean(false)
      val service = new AvatarsMetaServiceImpl(avatarMetaDao, storageDao, Auto, metaFactory) {
        override protected def shouldDo: Boolean = {
          invoked.set(true)
          true
        }
      }
      service.processMeta(taskRequest(regenerate = Some(Sometimes), download = Some(Always))).futureValue
      verify(avatarMetaDao).getMeta(eqTo(id), any())(any())
      verify(avatarMetaDao).deleteMeta(eqTo(id), any())
      verify(storageDao).storeMeta(eqTo(id), any(), any())(any())
      assert(invoked.get(), "Should check probability")
    }

    "save meta in unfinished state with correct is_finished value" in new Fixture {
      when(avatarMetaDao.getMeta(eqTo(id), eqTo(ava))(any())).
        thenReturn(Future.successful((Map.empty[String, String], Metadata.newBuilder().setIsFinished(false))))

      val service = new AvatarsMetaServiceImpl(avatarMetaDao, storageDao, Auto, metaFactory) with Mocks
      service.processMeta(taskRequest(regenerate = Some(Always), download = None)).futureValue
      verify(avatarMetaDao).getMeta(eqTo(id), eqTo(ava))(any())
      verify(avatarMetaDao).deleteMeta(eqTo(id), eqTo(ava))
      val metadataArg = ArgumentCaptor.forClass(classOf[Option[Metadata]])
      verify(storageDao).storeMeta(any(), any(), metadataArg.capture())(any())
      metadataArg.getValue.get.getIsFinished should be (false)
    }

    "assign initial version if actual version if unknown" in new Fixture {
      val service = new AvatarsMetaServiceImpl(avatarMetaDao, storageDao, Auto, metaFactory) with Mocks
      service.processMeta(taskRequest(regenerate = Some(Always), download = None, actualVersion = None,
        apiVer = ApiVersion.V2)).futureValue
      verify(storageDao).storeMeta(any(),
        eqTo(Map(
          "new_field" -> "new_value",
          "version"   -> "1")), any()) (any())
    }

    "save correct binary meta" in new Fixture {
      val metadataExpected = Metadata.newBuilder().setIsFinished(true).setGlobalSemidupDescriptor64("AABBCC")
      when(avatarMetaDao.getMeta(eqTo(id), eqTo(ava))(any())).
        thenReturn(Future.successful((Map.empty[String, String], metadataExpected)))

      val service = new AvatarsMetaServiceImpl(avatarMetaDao, storageDao, Auto, metaFactory) with Mocks
      service.processMeta(taskRequest(regenerate = Some(Always), download = None)).futureValue
      verify(avatarMetaDao).getMeta(eqTo(id), eqTo(ava))(any())
      verify(avatarMetaDao).deleteMeta(eqTo(id), eqTo(ava))
      verify(storageDao).
        storeMeta(any(), any(), eqTo(Some(metadataExpected.clone().setVersion(5).build())))(any())
    }

    "ignore old meta for new API format" in new Fixture {
      val service = new AvatarsMetaServiceImpl(avatarMetaDao, storageDao, Auto, metaFactory) with Mocks
      service.processMeta(taskRequest(regenerate = Some(Always), download = None, actualVersion = None,
        apiVer = ApiVersion.V3)).futureValue
      verify(storageDao).storeMeta(any(), eqTo(Map.empty), any()) (any())
    }
  }
}
// scalastyle:on
