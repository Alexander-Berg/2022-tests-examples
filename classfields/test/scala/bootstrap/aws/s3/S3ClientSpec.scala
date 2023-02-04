package bootstrap.aws.s3

import bootstrap.aws.Auth
import bootstrap.aws.s3.S3Client.Config
import bootstrap.config.{cfg, envConfigSourceTest}
import bootstrap.testcontainers.aws.AWSContainer
import bootstrap.testcontainers.aws.AWSContainer.S3
import org.apache.http.entity.ContentType
import zio.{Chunk, Clock, Scope, ZIO, ZLayer, durationInt}
import zio.stream.ZStream
import zio.test.Assertion.hasSameElements
import zio.test.{
  Spec,
  TestAspect,
  TestEnvironment,
  ZIOSpecDefault,
  assert,
  assertTrue,
}

import scala.jdk.CollectionConverters.*

object S3ClientDockerSpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment with Scope, Any] =
    S3Suite
      .spec("testcontainers", 1.second)
      .provideLayerShared(S3Suite.dockerLayer) @@
      TestAspect.tag("testcontainers") // @@ TestAspect.ignore

}

object S3ClientTestingSpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment with Scope, Any] =
    S3Suite
      .spec("yandex s3", 0.second)
      .provideLayerShared(S3Suite.testingLayer) @@ TestAspect.ignore

}

object S3Suite {

  val bucket      = "vs-indices"
  val key         = "key"
  val revertedKey = "key_reverted"

  import zio.test.{suite, test}

  /** @param env
    *   среда выполнения
    * @param lag
    *   см. https://st.yandex-team.ru/VS-1537
    * @return
    */
  def spec(env: String, lag: zio.Duration) =
    suite(s"S3Client: $env")(
      test("buckets") {
        for {
          client  <- ZIO.service[S3Client]
          buckets <- client.listBuckets.map(_.buckets().asScala.map(_.name()))
        } yield assertTrue(buckets.nonEmpty) &&
          assertTrue(buckets.contains(bucket))
      },
      test("put and get object") {
        for {
          client  <- ZIO.service[S3Client]
          buckets <- client.listBuckets
          _ <- client.multipartUpload(
            _.bucket(bucket)
              .key(key)
              .metadata(
                Map("length" -> (S3Client.minPartSize + 1).toString).asJava,
              )
              .contentType(ContentType.APPLICATION_OCTET_STREAM.getMimeType),
            ZStream.fromChunk(
              Chunk.fill(S3Client.minPartSize + 1)(42.toByte),
            ), // testing that sending > 1 parts work
            S3Client.minPartSize,
          )
          (resp, _) <- client.getObject(_.bucket(bucket).key(key))
        } yield assertTrue(
          buckets.buckets().asScala.map(_.name()).contains(bucket),
        ) && assertTrue(resp.sdkHttpResponse.isSuccessful)
      },
      test("list objects") {
        for {
          client <- ZIO.service[S3Client]
          _ <- client.multipartUpload(
            _.bucket(bucket)
              .key(revertedKey)
              .metadata(Map("length" -> "26").asJava)
              .contentType(ContentType.APPLICATION_OCTET_STREAM.getMimeType),
            ZStream.fromChunk(
              Chunk.fromArray("abcdefghijklmnopqrstuvwxyz".reverse.getBytes),
            ),
          )
          list <- client
            .listObjects(_.bucket(bucket).prefix("key"))
            .runCollect
            .map(_.toList.flatMap(_.contents().asScala).map(_.key()))
        } yield assertTrue(list.size >= 2) &&
          assert(list)(hasSameElements(Seq(revertedKey, key)))
      },
      test("download if modified since") {
        for {
          client    <- ZIO.service[S3Client]
          (resp, _) <- client.getObject(_.bucket(bucket).key(key))
          exit <-
            client
              .getObject(
                _.bucket(bucket)
                  .key(key)
                  .ifModifiedSince(resp.lastModified().plus(lag)),
              )
              .exit
          (requestBeforeUpdate, _) <- client.getObject(
            _.bucket(bucket)
              .key(key)
              .ifModifiedSince(resp.lastModified().minus(10.seconds)),
          )
        } yield assertTrue(
          requestBeforeUpdate.sdkHttpResponse().isSuccessful,
        ) && assertTrue(exit.isFailure) &&
          assertTrue(
            exit
              .fold(
                e => e.failureOption.map(_.getMessage).getOrElse(""),
                suc => suc._1.sdkHttpResponse().statusCode().toString,
              )
              .contains("304"),
          )
      },
      test("download if modified since: copy case") {
        for {
          client    <- ZIO.service[S3Client]
          clock     <- ZIO.service[Clock]
          (resp, _) <- client.getObject(_.bucket(bucket).key(key))
          _         <- clock.sleep(2.seconds)
          beforeCopy <-
            client
              .getObject(
                _.bucket(bucket)
                  .key(key)
                  .ifModifiedSince(resp.lastModified().plus(lag)),
              )
              .exit
          _ <- clock.sleep(2.seconds)
          _ <- client.copy(
            _.sourceBucket(bucket)
              .sourceKey(revertedKey)
              .destinationBucket(bucket)
              .destinationKey(key),
          )
          (afterCopy, _) <- client.getObject(
            _.bucket(bucket)
              .key(key)
              .ifModifiedSince(resp.lastModified().plus(lag)),
          )
        } yield assertTrue(beforeCopy.isFailure) &&
          assertTrue(afterCopy.sdkHttpResponse().isSuccessful)
      },
      test("copy multipart") { // works in tests, not works on actual MDS
        for {
          s3 <- ZIO.service[S3Client]
          _ <- s3.multipartUpload(
            _.bucket(bucket)
              .key(key)
              .metadata(
                Map("length" -> (S3Client.minPartSize + 1).toString).asJava,
              )
              .contentType(ContentType.APPLICATION_OCTET_STREAM.getMimeType),
            ZStream.fromChunk(Chunk.fill(S3Client.minPartSize + 1)(42.toByte)),
          )
          _ <- s3.multipartCopy(
            _.bucket(bucket).key(revertedKey),
            sourceBucket = bucket,
            sourceKey = key,
            sourceSize = S3Client.minPartSize + 1L,
            partSize = S3Client.minPartSize.toLong,
          )
          (afterCopy, _) <- s3.getObject(_.bucket(bucket).key(revertedKey))

        } yield assertTrue(afterCopy.sdkHttpResponse().isSuccessful) &&
          assertTrue(afterCopy.contentLength() == S3Client.minPartSize + 1L)

      },
    ).mapError(e => ZIO.debug(e)) @@ TestAspect.sequential @@
      TestAspect.afterAll(
        for {
          client <- ZIO.service[S3Client]
          _      <- client.deleteObject(_.bucket(bucket).key(key)).orDie
          _      <- client.deleteObject(_.bucket(bucket).key(revertedKey)).orDie
        } yield (),
      )

  val configFromContainer: ZLayer[AWSContainer, Nothing, Config] = {
    ZLayer.fromZIO {
      for {
        container <- ZIO.service[AWSContainer]
        url       <- container.endpointOverride
        accessKey <- container.accessKey
        secretKey <- container.secretKey
      } yield Config(url = url, auth = Auth(accessKey, secretKey))
    }
  }

  val dockerLayer = ZLayer.make[S3Client & Any & zio.Scope & Clock](
    S3Client.live,
    configFromContainer,
    AWSContainer.live(S3(Seq(bucket))),
    Scope.default,
    Clock.live,
  )

  val testingLayer = ZLayer.make[S3Client & Any & zio.Scope & Clock](
    S3Client.live,
    Scope.default,
    Clock.live,
    envConfigSourceTest,
    cfg[Config].orDie,
    zio.System.live,
  )

}
