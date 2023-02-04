package vertis.pica

import cats.data.NonEmptyList
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import zio.clock.Clock
import zio.test.Assertion.equalTo
import zio.test.{assert, DefaultRunnableSpec, ZSpec}

class YdbImageStorageDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("YdbImageStorageDao")(
      testM("Get nothing with empty db") {
        for {
          result <- runTx(ImageStorageDao.get("0"))
        } yield assert(result)(equalTo(None))
      },
      testM("Upsert image to db and get it") {
        val id = "0"
        val namespace = "auto"
        val image = Image(
          id,
          namespace,
          "auto.ru/audi-tt.jpg",
          0.toByte,
          ImageStatus.Queued
        )
        for {
          _ <- runTx(ImageStorageDao.upsert(NonEmptyList.of(image)))
          result <- runTx(ImageStorageDao.get(id))
        } yield assert(result)(equalTo(Some(image)))
      },
      testM("Upsert image to db, change it and and get the right version") {
        val id = "1"
        val namespace = "auto"
        val image = Image(
          id,
          namespace,
          "auto.ru/audi-tt.jpg",
          0.toByte,
          ImageStatus.Queued
        )
        val changedImage = image.copy(status = ImageStatus.Processed)
        for {
          _ <- runTx(ImageStorageDao.upsert(NonEmptyList.of(image)))
          _ <- runTx(ImageStorageDao.upsert(NonEmptyList.of(changedImage)))
          result <- runTx(ImageStorageDao.get(id))
        } yield assert(result)(equalTo(Some(changedImage)))
      },
      testM("Upsert images to db, and get them") {
        val id1 = "1"
        val namespace = "auto"
        val image1 = Image(
          id1,
          namespace,
          "auto.ru/audi-tt.jpg",
          0.toByte,
          ImageStatus.Queued
        )
        val id2 = "1"
        val image2 = Image(
          id2,
          namespace,
          "auto.ru/audi-tt.jpg",
          0.toByte,
          ImageStatus.Queued
        )
        for {
          _ <- runTx(ImageStorageDao.upsert(NonEmptyList.of(image1, image2)))
          result1 <- runTx(ImageStorageDao.get(id1))
          result2 <- runTx(ImageStorageDao.get(id2))
        } yield assert(result1)(equalTo(Some(image1))) && assert(result2)(equalTo(Some(image2)))
      }
    ).provideCustomLayerShared(
      TestYdb.ydb >>> (YdbImageStorageDao.live ++ Ydb.txRunner) ++ Clock.live
    )
  }

}
