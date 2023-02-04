package common.zio.s3edr.testkit

import common.zio.s3edr.S3EdrUploader.S3EdrUploader
import common.zio.s3edr.UploadResult.{AlreadyExists, UploadOk}
import common.zio.s3edr.{S3EdrUploader, UploadResult}
import izumi.reflect.Tag
import ru.yandex.vertis.s3edr.core.storage.DataType
import scalapb.GeneratedMessage
import zio.stm.STM.succeed
import zio.stm.TMap
import zio.{Task, UIO, ULayer, URIO, ZIO}

class TestS3EdrUploader[Message <: GeneratedMessage](map: TMap[DataType, Message])
  extends S3EdrUploader.Service[Message] {

  def upload(dataType: DataType)(msg: Message): Task[UploadResult] =
    map
      .get(dataType)
      .flatMap {
        case Some(`msg`) => succeed(AlreadyExists)
        case _ => map.put(dataType, msg).as(UploadOk)
      }
      .commit

  def get(dataType: DataType): UIO[Option[Message]] =
    map.get(dataType).commit
}

object TestS3EdrUploader {

  def get[Message <: GeneratedMessage: Tag](dataType: DataType): URIO[S3EdrUploader[Message], Option[Message]] =
    ZIO.accessM[S3EdrUploader[Message]](_.get match {
      case testS3EdrUploader: TestS3EdrUploader[Message] =>
        testS3EdrUploader.get(dataType)
    })

  def live[Message <: GeneratedMessage: Tag]: ULayer[S3EdrUploader[Message]] =
    TMap.make[DataType, Message]().commit.map(new TestS3EdrUploader[Message](_)).toLayer
}
