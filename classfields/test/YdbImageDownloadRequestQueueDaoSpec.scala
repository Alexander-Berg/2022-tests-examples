package vertis.pica

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import vertis.pica.model.Host
import zio.clock.Clock
import zio.test.Assertion.equalTo
import zio.test.{assert, DefaultRunnableSpec, ZSpec}

import java.time.Instant

class YdbImageDownloadRequestQueueDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("YdbImageDownloadRequestQueueDao")(
      testM("Add request to db and get it") {
        val requestId = "0"
        val namespace = "auto"
        val host: Host = "auto.ru"
        val request = ImageDownloadRequest(
          requestId,
          namespace,
          host,
          0.toByte,
          Instant.now(),
          "auto.ru/audi-tt.jpg"
        )
        for {
          _ <- runTx(ImageDownloadRequestQueueDao.add(request))
          result <- runTx(ImageDownloadRequestQueueDao.get(requestId))
        } yield assert(result)(equalTo(Some(request)))
      },
      testM("Add request to db, remove it and get nothing") {
        val requestId = "0"
        val namespace = "auto"
        val host: Host = "auto.ru"
        val request = ImageDownloadRequest(
          requestId,
          namespace,
          host,
          0.toByte,
          Instant.now(),
          "auto.ru/audi-tt.jpg"
        )
        for {
          _ <- runTx(ImageDownloadRequestQueueDao.add(request))
          _ <- runTx(ImageDownloadRequestQueueDao.remove(requestId))
          result <- runTx(ImageDownloadRequestQueueDao.get(requestId))
        } yield assert(result)(equalTo(None))
      },
      testM("Add requests to db and list them") {
        val host1: Host = "auto.ru"
        val namespace1 = "auto"
        val request1 = ImageDownloadRequest(
          "0",
          namespace1,
          host1,
          0.toByte,
          Instant.now(),
          s"$host1/audi-tt.jpg"
        )
        val request2 = ImageDownloadRequest(
          "1",
          namespace1,
          host1,
          0.toByte,
          Instant.now().plusSeconds(2),
          s"$host1/bmw-x5.jpg"
        )
        val host2: Host = "realty.ru"
        val namespace2 = "realty"
        val request3 = ImageDownloadRequest(
          "2",
          namespace2,
          host2,
          0.toByte,
          Instant.now().plusSeconds(1),
          s"$host2/apt.jpg"
        )
        val request4 = ImageDownloadRequest(
          "3",
          namespace2,
          host2,
          1.toByte,
          Instant.now().plusSeconds(3),
          s"$host2/floor.jpg"
        )
        for {
          _ <- runTx(ImageDownloadRequestQueueDao.add(request1))
          _ <- runTx(ImageDownloadRequestQueueDao.add(request2))
          _ <- runTx(ImageDownloadRequestQueueDao.add(request3))
          _ <- runTx(ImageDownloadRequestQueueDao.add(request4))
          first1 <- runTx(ImageDownloadRequestQueueDao.list(namespace1, host1, limit = 1))
          all1 <- runTx(ImageDownloadRequestQueueDao.list(namespace1, host1, limit = 10))
          first2 <- runTx(ImageDownloadRequestQueueDao.list(namespace2, host2, limit = 1))
          all2 <- runTx(ImageDownloadRequestQueueDao.list(namespace2, host2, limit = 10))
        } yield assert(first1)(equalTo(Seq(request1))) && assert(all1)(equalTo(Seq(request1, request2))) && assert(
          first2
        )(equalTo(Seq(request4))) && assert(all2)(equalTo(Seq(request4, request3)))
      }
    ).provideCustomLayerShared(
      TestYdb.ydb >>> (YdbImageDownloadRequestQueueDao.live ++ Ydb.txRunner) ++ Clock.live
    )
  }

}
