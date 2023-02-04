package common.zio.clients.s3.testkit

import common.zio.clients.s3.S3Client
import common.zio.clients.s3.S3Client.S3ClientException
import software.amazon.awssdk.services.s3.model.{
  HeadObjectResponse,
  ListObjectsV2Response,
  NoSuchKeyException,
  S3Object
}
import zio.stream.ZStream
import zio.{Chunk, IO, Ref, ZIO}

import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters._

class InMemoryS3(buckets: Ref[Map[String, Map[String, InMemoryS3.S3Obj]]]) extends S3Client.Service {

  override def uploadContent[R](
      bucket: String,
      key: String,
      contentLength: Long,
      contentType: String,
      content: ZStream[R, Throwable, Byte],
      metadata: Map[String, String],
      contentDisposition: Option[String]): ZIO[R, S3Client.S3ClientException, Unit] =
    content.runCollect.mapError(e => S3Client.S3ClientException("Failed to materialize content", e)).flatMap { bytes =>
      updateBucket(bucket)(
        _.updated(key, InMemoryS3.S3Obj(bucket, key, metadata, bytes, contentType, contentDisposition))
      )
    }

  override def copyObject(
      sourceBucket: String,
      sourceKey: String,
      destinationBucket: String,
      destinationKey: String): IO[S3Client.S3ClientException, Unit] =
    getS3Object(sourceBucket, sourceKey).flatMap { obj =>
      updateBucket(destinationBucket)(_.updated(destinationKey, obj))
    }

  override def getObject(bucket: String, key: String): ZStream[Any, S3Client.S3ClientException, Byte] =
    ZStream.fromEffect(getS3Object(bucket, key).map(_.content)).flattenChunks

  override def deleteObject(bucket: String, key: String): IO[S3Client.S3ClientException, Unit] =
    updateBucket(bucket)(_.removed(key))

  override def deleteObjects(bucket: String, keys: String*): IO[S3Client.S3ClientException, Unit] =
    updateBucket(bucket)(_.removedAll(keys))

  override def createBucket(bucket: String): IO[S3Client.S3ClientException, Unit] =
    updateBucket(bucket)(identity)

  /** Returns object metadata */
  override def headObject(bucket: String, key: String): IO[S3Client.S3ClientException, HeadObjectResponse] =
    for {
      obj <- getS3Object(bucket, key)
      responseBuilder <- ZIO.effectTotal {
        val builder = HeadObjectResponse
          .builder()
          .contentType(obj.contentType)
          .contentLength(obj.content.length.toLong)
          .metadata(obj.metadata.asJava)
        obj.contentDisposition.foreach(builder.contentDisposition)
        builder
      }
    } yield responseBuilder.build()

  override def listObjects(
      bucket: String,
      prefix: String,
      continuationToken: Option[String]): IO[S3Client.S3ClientException, ListObjectsV2Response] =
    for {
      map <- buckets.get
      content = for {
        (key, content) <- map.getOrElse(bucket, Map.empty).toSeq
        if key.startsWith(prefix)
      } yield content
    } yield ListObjectsV2Response
      .builder()
      .name(bucket)
      .prefix(prefix)
      .contents(content.map(_.asS3Object): _*)
      .build()

  override def listAllObjectsAfter(
      bucket: String,
      prefix: String,
      startAfter: String): IO[S3ClientException, ListObjectsV2Response] =
    for {
      map <- buckets.get
      content = for {
        (key, content) <- map.getOrElse(bucket, Map.empty).toSeq
        if key.startsWith(prefix) && key > startAfter
      } yield content
    } yield ListObjectsV2Response
      .builder()
      .name(bucket)
      .prefix(prefix)
      .startAfter(startAfter)
      .contents(content.map(_.asS3Object): _*)
      .build()

  override def getUrl(bucket: String, key: String): IO[S3Client.S3ClientException, String] =
    ZIO.succeed(s"https://s3.mock/$bucket/$key")

  override def signGet(bucket: String, key: String, ttl: FiniteDuration): IO[S3ClientException, String] =
    ZIO.succeed(s"https://s3.signed.get.mock/$bucket/$key")

  override def signPut(bucket: String, key: String, ttl: FiniteDuration): IO[S3ClientException, String] =
    ZIO.succeed(s"https://s3.signed.put.mock/$bucket/$key")

  private def updateBucket(
      bucket: String
    )(action: Map[String, InMemoryS3.S3Obj] => Map[String, InMemoryS3.S3Obj]): IO[Nothing, Unit] = {
    buckets.update(map => map.updated(bucket, action(map.getOrElse(bucket, Map.empty))))
  }

  private def getS3Object(bucket: String, key: String): ZIO[Any, S3ClientException, InMemoryS3.S3Obj] = {
    for {
      map <- buckets.get
      obj <- map.getOrElse(bucket, Map.empty).get(key) match {
        case Some(value) => ZIO.succeed(value)
        case None =>
          ZIO.fail(
            S3ClientException(
              s"Object $key not found in bucket $bucket",
              NoSuchKeyException.builder().statusCode(404).message(s"Object $key not found in bucket $bucket").build()
            )
          )
      }
    } yield obj
  }
}

object InMemoryS3 {

  def make: ZIO[Any, Nothing, InMemoryS3] = Ref.make(Map.empty[String, Map[String, S3Obj]]).map(new InMemoryS3(_))

  case class S3Obj(
      bucket: String,
      key: String,
      metadata: Map[String, String],
      content: Chunk[Byte],
      contentType: String,
      contentDisposition: Option[String]) {

    def asS3Object: S3Object = S3Object
      .builder()
      .key(key)
      .size(content.length.toLong)
      .build()
  }
}
