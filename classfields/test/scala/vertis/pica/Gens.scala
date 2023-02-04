package vertis.pica

import common.clients.avatars.model.{ImageMeta, OrigSize}

import java.time.Instant
import java.util.UUID
import org.scalacheck.Gen
import ru.yandex.vertis.generators.BasicGenerators
import ru.yandex.vertis.pica.model.model.{AvatarsError, Image, ImageServiceInfo}
import vertis.pica.model.{ImageId, ImagePutRequest, ImageRecord, Namespaces, ProcessingStatuses, Url}

import java.time.temporal.ChronoUnit

/** @author ruslansd
  */
object Gens extends BasicGenerators {

  val UrlGen: Gen[Url] = for {
    domain <- readableString
    host = s"$domain.ru"
    paths <- Gen.listOfN(3, readableString)
    url = s"http://$host/${paths.mkString("/")}"
  } yield Url(url, url, host)

  def urlGen(host: String): Gen[Url] =
    for {
      paths <- Gen.listOfN(3, readableString)
      url = s"http://$host/${paths.mkString("/")}"
    } yield Url(url, url, host)

  val NamespaceGen: Gen[Namespace] =
    Gen.oneOf(Namespaces.values.toSeq)

  val OrigSizeGen: Gen[OrigSize] =
    for {
      x <- Gen.choose(10, 100)
      y <- Gen.choose(10, 100)
    } yield OrigSize(x, y)

  def metaGen(metaFinished: Boolean = true): Gen[ImageMeta] =
    for {
      content <- readableString
      origSize <- OrigSizeGen
    } yield ImageMeta(origSize, None, content, metaFinished, None)

  def imageGen(metaFinished: Boolean = true): Gen[Image] =
    for {
      groupId <- Gen.choose(1, 10000)
      imageName <- readableString
      metaContent <- readableString
      namespace <- NamespaceGen
    } yield Image(
      groupId.toString,
      imageName,
      namespace.toString,
      metaContent,
      metaFinished
    )

  private def getInstant = Instant.now().truncatedTo(ChronoUnit.MICROS)

  val QueuedImageRecordGen: Gen[ImageRecord] =
    for {
      url <- UrlGen
      imageName = UUID.randomUUID().toString
    } yield ImageRecord(
      ImageId(url).id,
      imageName,
      url.originalUrl,
      None,
      None,
      getInstant,
      None,
      None,
      false,
      ProcessingStatuses.Queued,
      ImageServiceInfo.defaultInstance
    )

  val ImageRequestGen: Gen[ImagePutRequest] =
    for {
      url <- UrlGen
      imageName = UUID.randomUUID().toString
    } yield ImagePutRequest(
      ImageId(url),
      getInstant,
      None,
      None,
      imageName
    )

  val WaitingForCaptureImageRecordGen: Gen[ImageRecord] =
    for {
      record <- QueuedImageRecordGen
      image <- imageGen(false)
    } yield record.copy(
      image = Some(image),
      status = ProcessingStatuses.WaitingMeta
    )

  val ImageRecordGen: Gen[ImageRecord] =
    Gen.oneOf(QueuedImageRecordGen, WaitingForCaptureImageRecordGen)

  val AvatarsErrorGen: Gen[AvatarsError] = for {
    msg <- readableString
    code <- Gen.posNum[Int]
  } yield AvatarsError(msg, code)

}
