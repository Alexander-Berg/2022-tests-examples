package ru.yandex.vertis.moderation.dao.impl.ydb

import org.joda.time.DateTime
import ru.yandex.vertis.moderation.dao.CvHashDao
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{AutoruEssentials, ExternalId, Instance, User}
import ru.yandex.vertis.quality.cats_utils.Awaitable._
import ru.yandex.vertis.quality.ydb_utils.WithTransaction
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.YdbSpecBase
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.duration.Duration.Inf
import scala.util.{Random, Try}

@RunWith(classOf[JUnitRunner])
class YdbCvHashDaoSpec extends YdbSpecBase {
  override val resourceSchemaFileName: String = "/cv-hashes.sql"
  lazy val dao: CvHashDao = new YdbCvHashDao[F, WithTransaction[F, *]](ydbWrapper)

  before {
    Try(ydbWrapper.runTx(ydbWrapper.execute("DELETE FROM photo_cv_hashes;")).await)

  }

  def newRealtyInstance(cvHashes: Seq[String]): Instance = {
    val photos = cvHashes.map(p => RealtyPhotoInfoGen.next.copy(cvHash = Some(p), deleted = Some(false)))
    val essentials = RealtyEssentialsGen.next.copy(photos = photos)
    val externalId = ExternalIdGen.next
    instanceGen(externalId, essentials).next
  }

  def newAutoruInstance(cvHashes: Seq[String], deletedCvs: Boolean = false): Instance = {
    val photos = cvHashes.map(p => AutoPhotoInfoOptGen.next.copy(cvHash = Some(p), deleted = Some(deletedCvs)))
    val essentials = AutoruEssentialsGen.next.copy(photos = photos)
    val user = User.Autoru(Random.nextInt.toString)
    val externalId = ExternalId(user, Random.nextInt.toString)
    instanceGen(externalId, essentials).next
  }

  "YdbCvHashDao" should {
    "upsert and then get cv hashes realty" in {
      val instance = newRealtyInstance(Seq("1", "2", "3"))

      Await.ready(dao.updateCvHashes(instance), 20.seconds)

      val result =
        Await
          .result(
            dao.getInstancesIds(Set("1", "2", "3"), instance.createTime.plus(20L).toInstant),
            20.seconds
          )

      result should be(Seq.fill(3)(instance.externalId))

    }

    "upsert and then get cv hashes autoru" in {
      val instance = newAutoruInstance(Seq("1", "2"))

      Await.ready(dao.updateCvHashes(instance), 20.seconds)

      val result =
        Await
          .result(
            dao.getInstancesIds(Set("1", "2"), instance.createTime.plus(1L).toInstant),
            20.seconds
          )

      result should be(Seq.fill(2)(instance.externalId))

    }

    "remove deleted photos" in {
      val instance = newAutoruInstance(Seq("1", "2"))
      val instance2 = newAutoruInstance(Seq("1", "2")).copy(createTime = instance.createTime.plus(20L))

      val withDeletedPics =
        instance.essentials match {
          case a: AutoruEssentials =>
            instance.copy(essentials = a.copy(photos = a.photos.map(p => p.copy(deleted = Some(true)))))
          case _ => instance
        }

      Await.ready(dao.updateCvHashes(Seq(instance, instance2)), 20.seconds)

      Await.ready(dao.updateCvHashes(Seq(withDeletedPics)), 20.seconds)

      val checkDeleted =
        Await
          .result(
            dao.getInstancesIds(Set("1", "2"), instance2.createTime.plus(1L).toInstant),
            20.seconds
          )

      checkDeleted should be(Seq.fill(2)(instance2.externalId))

    }

    "add,keep and delete photos" in {
      val instance = newAutoruInstance(Seq("1", "2", "3", "4"))

      val withDeletedAndNewPics =
        instance.essentials match {
          case a: AutoruEssentials =>
            instance.copy(essentials =
              a.copy(photos =
                a.photos.map(p =>
                  if (p.cvHash.get == "1" || p.cvHash.get == "2")
                    p.copy(deleted = Some(true))
                  else p
                )
                  :+ a.photos.head.copy(cvHash = Some("5"))
              )
            )
          case _ => instance
        }

      Await.ready(dao.updateCvHashes(Seq(instance)), 20.seconds)

      Await.ready(dao.updateCvHashes(Seq(withDeletedAndNewPics)), 20.seconds)

      val checkDeleted =
        Await
          .result(
            dao.getInstancesIds(Set("3", "4", "5"), instance.createTime.plus(1L).toInstant),
            20.seconds
          )

      checkDeleted should be(Seq.fill(3)(instance.externalId))

    }
    "move cursor correctly" in {
      val now = DateTime.now()
      val hashes = Seq.range(0, 1500).map(_.toString)
      val instances = hashes.map(h => (newAutoruInstance(Seq(h)).copy(createTime = now)))
      Await.ready(dao.updateCvHashes(instances), Inf)

      val result =
        Await
          .result(
            dao.getInstancesIds(hashes.toSet, now.plus(1L).toInstant),
            20.seconds
          )

      result.toSet.size should be(1500)
    }

    "move cursor correctly again" in {

      val autoruInstance =
        (essentials: AutoruEssentials, externalId: ExternalId) =>
          instanceGen(externalId, essentials).next.copy(createTime = DateTime.now())

      val essentials =
        (s: String) => {
          val p = AutoPhotoInfoOptGen.next.copy(cvHash = Some(s), deleted = None)
          AutoruEssentialsGen.next.copy(photos = Seq(p))
        }
      val user1 = User.Autoru("1")

      val firstPart = (1 to 1001).map(num => autoruInstance(essentials("1"), ExternalId(user1, num.toString())))
      val secondPart =
        (1 to 300).map(num => autoruInstance(essentials("1"), ExternalId(User.Autoru(num.toString()), "1")))
      val thirdPart =
        (2 to 501).map(num => autoruInstance(essentials(num.toString()), ExternalId(User.Autoru(num.toString()), "1")))

      Await.ready(dao.updateCvHashes(firstPart ++ secondPart ++ thirdPart), Inf)

      val result =
        Await
          .result(
            dao.getInstancesIds((1 to 501).map(_.toString).toSet, DateTime.now().plus(1000L).toInstant),
            20.seconds
          )
          .sortBy(_.user.id)

      result.size should be(1800)
    }

    "do nothing if hashes are empty" in {
      val instance = newAutoruInstance(Seq.empty)
      Await.ready(dao.updateCvHashes(instance), Inf)

      val result =
        Await
          .result(
            dao.getInstancesIds(Set.empty, instance.createTime.plus(1L).toInstant),
            20.seconds
          )

      result.size should be(0)
    }

  }
}
