package auto.dealers.multiposting.clients.s3.testkit

import zio._
import zio.test.mock._
import zio.stream.ZStream
import zio.blocking.Blocking
import common.zio.clients.s3.S3Client
import auto.dealers.multiposting.clients.s3.S3MultipostingClient
import auto.dealers.multiposting.clients.s3.S3MultipostingClient.S3MultipostingClient

object S3MultipostingClientMock extends Mock[S3MultipostingClient] {
  object ListObjects extends Effect[(String, String), S3Client.S3ClientException, List[String]]
  object ListObjectsAfter extends Effect[(String, String, String), S3Client.S3ClientException, List[String]]
  object ListNewObjects extends Effect[(String, String, Option[String]), S3Client.S3ClientException, List[String]]
  object ReadLines extends Stream[(String, String), S3Client.S3ClientException, String]

  val compose: URLayer[Has[Proxy], S3MultipostingClient] =
    ZLayer.fromServiceM { proxy: Proxy =>
      withRuntime.map { rts =>
        new S3MultipostingClient.Service {
          override def listObjects(
              bucket: String,
              prefix: String): ZIO[Any, S3Client.S3ClientException, List[String]] =
            proxy(ListObjects, bucket, prefix)

          override def listObjectsAfter(
              bucket: String,
              prefix: String,
              startAfter: String): ZIO[Any, S3Client.S3ClientException, List[String]] =
            proxy(ListObjectsAfter, bucket, prefix, startAfter)

          override def listNewObjects(
              bucket: String,
              prefix: String,
              startAfter: Option[String]): ZIO[Any, S3Client.S3ClientException, List[String]] =
            proxy(ListNewObjects, bucket, prefix, startAfter)

          override def readLines(bucket: String, key: String): ZStream[Blocking, S3Client.S3ClientException, String] =
            rts.unsafeRun(proxy(ReadLines, bucket, key))
        }
      }
    }

}
