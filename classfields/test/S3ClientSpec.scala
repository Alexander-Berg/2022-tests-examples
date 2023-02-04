package common.zio.clients.s3.test

import common.zio.clients.s3.S3Client
import common.zio.clients.s3.testkit.TestS3
import zio.stream.{ZSink, ZStream}
import zio.test.Assertion._
import zio.test._
import zio.{Chunk, UIO, ZIO}

import scala.jdk.CollectionConverters._
import scala.util.Random

object S3ClientSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("S3ClientSpec")(
      testM("Create bucket") {
        for {
          _ <- S3Client.createBucket("bucket")
        } yield assertCompletes
      },
      testM("Create bucket, upload and get some data") {
        for {
          _ <- S3Client.createBucket("upload")
          data <- ZIO.succeed("data".getBytes("UTF-8"))
          _ <- S3Client.uploadContent(
            "upload",
            "unique_key",
            data.length,
            "text/plain",
            ZStream.fromChunk(Chunk.fromArray(data)),
            Map("aba" -> "caba"),
            Some("attachment")
          )
          uploaded <- S3Client.getObject("upload", "unique_key").run(ZSink.collectAll[Byte])
          headObject <- S3Client.headObject("upload", "unique_key")
        } yield assert(uploaded.toVector)(equalTo(data.toVector)) &&
          assert(headObject.contentLength.toLong)(equalTo(data.length.toLong)) &&
          assert(headObject.contentType)(equalTo("text/plain")) &&
          assert(headObject.contentDisposition)(equalTo("attachment")) &&
          assert(headObject.metadata.asScala.toMap)(equalTo(Map("aba" -> "caba")))
      },
      testM("rewrite data by upload") {
        for {
          _ <- S3Client.createBucket("rewrite")
          data <- ZIO.succeed("data".getBytes("UTF-8"))
          _ <- S3Client.uploadContent(
            "rewrite",
            "unique_key",
            data.length,
            "text/plain",
            ZStream.fromChunk(Chunk.fromArray(data))
          )
          anotherData <- ZIO.succeed("another".getBytes("UTF-8"))
          _ <- S3Client.uploadContent(
            "rewrite",
            "unique_key",
            anotherData.length,
            "text/plain",
            ZStream.fromChunk(Chunk.fromArray(anotherData))
          )
          currentUploaded <- S3Client.getObject("rewrite", "unique_key").run(ZSink.collectAll[Byte])
        } yield assert(currentUploaded.toVector)(equalTo(anotherData.toVector))
      },
      testM("copy data") {
        for {
          _ <- S3Client.createBucket("copy")
          dataToCopy <- ZIO.succeed("data1".getBytes("UTF-8"))
          _ <- S3Client.uploadContent(
            "copy",
            "first_key/check-url-encoding??&&",
            dataToCopy.length,
            "text/plain",
            ZStream.fromChunk(Chunk.fromArray(dataToCopy))
          )
          _ <- S3Client.copyObject(
            "copy",
            "first_key/check-url-encoding??&&",
            "copy",
            "second_key"
          )
          copiedData <- S3Client.getObject("copy", "second_key").run(ZSink.collectAll[Byte])
        } yield assert(copiedData.toVector)(equalTo(dataToCopy.toVector))
      },
      testM("list objects") {
        for {
          bucketName <- ZIO.succeed("list")
          _ <- S3Client.createBucket(bucketName)
          directoryName = "dir"
          data <- ZIO.succeed("data1".getBytes("UTF-8"))
          _ <- S3Client.uploadContent(
            bucketName,
            s"$directoryName/key1",
            data.length,
            "text/plain",
            ZStream.fromChunk(Chunk.fromArray(data))
          )
          _ <- S3Client.uploadContent(
            bucketName,
            s"$directoryName/key2",
            data.length,
            "text/plain",
            ZStream.fromChunk(Chunk.fromArray(data))
          )
          _ <- S3Client.uploadContent(
            bucketName,
            s"key3",
            data.length,
            "text/plain",
            ZStream.fromChunk(Chunk.fromArray(data))
          )
          objectsInDirectoryResponse <- S3Client.listObjects(bucketName, directoryName)
          allObjectsResponse <- S3Client.listObjects(bucketName, "")
          objectNamesInDirectory = objectsInDirectoryResponse.contents.asScala.map(_.key)
          allObjectNames = allObjectsResponse.contents.asScala.map(_.key)
        } yield assert(objectNamesInDirectory)(hasSameElements(List(s"$directoryName/key1", s"$directoryName/key2"))) &&
          assert(allObjectNames)(hasSameElements(List(s"$directoryName/key1", s"$directoryName/key2", "key3")))
      },
      testM("list objects after") {
        for {
          bucketName <- ZIO.succeed("list")
          _ <- S3Client.createBucket(bucketName)
          directoryName1 = "a"
          directoryName2 = "m"
          data <- ZIO.succeed("data1".getBytes("UTF-8"))
          _ <- S3Client.uploadContent(
            bucketName,
            s"$directoryName1/key1",
            data.length,
            "text/plain",
            ZStream.fromChunk(Chunk.fromArray(data))
          )
          _ <- S3Client.uploadContent(
            bucketName,
            s"$directoryName1/key2",
            data.length,
            "text/plain",
            ZStream.fromChunk(Chunk.fromArray(data))
          )
          _ <- S3Client.uploadContent(
            bucketName,
            s"key3",
            data.length,
            "text/plain",
            ZStream.fromChunk(Chunk.fromArray(data))
          )
          _ <- S3Client.uploadContent(
            bucketName,
            s"$directoryName2/key4",
            data.length,
            "text/plain",
            ZStream.fromChunk(Chunk.fromArray(data))
          )
          objectsInDirectoryResponse <- S3Client.listAllObjectsAfter(
            bucketName,
            directoryName1,
            s"$directoryName1/key1"
          )
          allObjectsResponse <- S3Client.listAllObjectsAfter(bucketName, "", "key2")
          objectNamesInDirectory = objectsInDirectoryResponse.contents.asScala.map(_.key)
          allObjectNames = allObjectsResponse.contents.asScala.map(_.key)
        } yield assert(objectNamesInDirectory)(hasSameElements(List(s"$directoryName1/key2"))) &&
          assert(allObjectNames)(hasSameElements(List(s"$directoryName2/key4", "key3")))
      },
      testM("delete object") {
        for {
          bucketName <- ZIO.effectTotal("delete")
          _ <- S3Client.createBucket(bucketName)
          data <- ZIO.succeed("data".getBytes("UTF-8"))
          _ <- S3Client.uploadContent(
            bucketName,
            "key",
            data.length,
            "text/plain",
            ZStream.fromChunk(Chunk.fromArray(data))
          )
          beforeDeletion <- S3Client.listObjects(bucketName, "")
          _ <- S3Client.deleteObject(bucketName, "key")
          afterDeletion <- S3Client.listObjects(bucketName, "")
        } yield assert(beforeDeletion.contents.asScala)(hasSize(equalTo(1))) &&
          assert(afterDeletion.contents.asScala)(isEmpty)
      },
      testM("delete multiple objects") {
        for {
          bucket <- ZIO.effectTotal("deletemany")
          _ <- S3Client.createBucket(bucket)
          data <- ZIO.succeed("delete_data".getBytes("UTF-8"))
          _ <- S3Client.uploadContent(
            bucket,
            "key1",
            data.length,
            "text/plain",
            ZStream.fromChunk(Chunk.fromArray(data))
          )
          _ <- S3Client.uploadContent(
            bucket,
            "key2",
            data.length,
            "text/plain",
            ZStream.fromChunk(Chunk.fromArray(data))
          )
          beforeDelete <- S3Client.listObjects(bucket, "")
          _ <- S3Client.deleteObjects(bucket, "key1", "key2")
          afterDelete <- S3Client.listObjects(bucket, "")
        } yield assert(beforeDelete.contents.asScala)(hasSize(equalTo(2))) &&
          assert(afterDelete.contents.asScala)(hasSize(equalTo(0)))
      },
      testM("upload large data") {
        for {
          bucket <- UIO("large-data")
          _ <- S3Client.createBucket(bucket)
          key <- UIO("largefile")
          size = 486 * 1024 * 1024
          data = ZStream.repeat(Chunk.fromArray(Random.nextBytes(1024))).flattenChunks.take(size)
          _ <- S3Client.uploadContent(bucket, key, size, "application/octet-stream", data)
          uploaded <- S3Client.headObject(bucket, key)
        } yield assert(uploaded.contentLength().longValue())(equalTo(size.toLong))
      }
    ).provideCustomLayerShared {
      TestS3.live
    }
  }
}
