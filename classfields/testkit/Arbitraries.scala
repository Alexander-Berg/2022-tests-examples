package ru.yandex.vertis.karp.model

import com.softwaremill.tagging._
import org.scalacheck.Gen.{alphaChar, alphaLowerChar, alphaNumChar, choose, frequency, numChar, numStr, oneOf}
import org.scalacheck.{Arbitrary, Gen}
import ru.yandex.vertis.common.Domain
import ru.yandex.vertis.karp.proto

import java.net.URL
import java.time.{Instant, LocalDate, ZoneOffset}
import scala.concurrent.duration._
import scala.language.implicitConversions

object Arbitraries {

  import org.scalacheck.magnolia._

  def generate[T](implicit arb: Arbitrary[T]): Gen[T] = arb.arbitrary

  implicit private def wrap[A](g: Gen[A]): Arbitrary[A] = Arbitrary(g)

  implicit class RichGen[T](val gen: Gen[T]) extends AnyVal {
    def tagged[U]: Arbitrary[T @@ U] = gen.map(_.taggedWith[U])

    def ? : Gen[Option[T]] = Gen.option(gen)
  }

  implicit class RichArbitrary[T](val arb: Arbitrary[T]) extends AnyVal {
    def tagged[U]: Arbitrary[T @@ U] = Arbitrary(arb.arbitrary.map(_.taggedWith[U]))
  }

  /** Generates russian upper-case alpha character */
  def cyrillicUpperChar: Gen[Char] = frequency((32, choose(1040.toChar, 1071.toChar)), (1, 1025.toChar))

  /** Generates russian lower-case alpha character */
  def cyrillicLowerChar: Gen[Char] = frequency((32, choose(1072.toChar, 1103.toChar)), (1, 1105.toChar))

  /** Generates russian  alpha character */
  def cyrillicChar: Gen[Char] = frequency((1, cyrillicUpperChar), (9, cyrillicLowerChar))

  def cyrillicNumChar: Gen[Char] = frequency((1, numChar), (9, cyrillicChar))

  def alphaNumLowerChar: Gen[Char] = frequency((1, numChar), (9, alphaLowerChar))

  def generateSeq[T](n: Int, filter: T => Boolean = (_: T) => true)(implicit arb: Arbitrary[T]): Seq[T] =
    Iterator
      .continually(arb.arbitrary.suchThat(filter).sample)
      .flatten
      .take(n)
      .toSeq

  def generate[T](filter: T => Boolean = (_: T) => true)(implicit arb: Arbitrary[T]): T =
    generateSeq[T](1, filter).head

  def generateId(ranges: Seq[Int], gen: Gen[Char], separator: String): Arbitrary[String] = {
    val gs = ranges.map(makeStr(_, gen).arbitrary)
    val str = gs
      .fold(Gen.const("")) { (prev, next) =>
        Gen.sequence[Seq[String], String](Seq(prev, next)).map(_.mkString(separator))
      }
      .map(_.drop(1))
    Arbitrary(str)
  }

  val alphaNumStr: Arbitrary[String] = makeStr(alphaNumChar)

  val alphaStr: Arbitrary[String] = makeStr(alphaChar)

  val cyrillicNumStr: Arbitrary[String] = makeStr(cyrillicNumChar)

  val cyrillicStr: Arbitrary[String] = makeStr(cyrillicChar)

  val shortCyrillicNumStr: Arbitrary[String] = makeStr(1, 5, cyrillicNumChar)

  private def makeStr(min: Int, max: Int, gen: Gen[Char]): Arbitrary[String] =
    for {
      n <- Gen.chooseNum(min, max)
      str <- Gen.listOfN(n, gen).map(_.mkString)
    } yield str

  private def makeStr(length: Int, gen: Gen[Char]): Arbitrary[String] = makeStr(length, length, gen)

  private def makeStr(gen: Gen[Char]): Arbitrary[String] = makeStr(3, 15, gen)

  implicit lazy val InstantArb: Arbitrary[Instant] = {
    val maxDelta = 500.days.toSeconds
    Gen.chooseNum(-maxDelta, maxDelta).map(Instant.now().minusSeconds(2 * maxDelta).plusSeconds)
  }

  val dlSecondPartGenAlpha: Gen[String] = Gen.listOfN(2, oneOf("авекмнорстухabekmhopctyx")).map(_.mkString)
  val dlSecondPartGenNumber: Gen[String] = Gen.listOfN(2, numChar).map(_.mkString)
  val dlSecondPartGen: Arbitrary[String] = frequency((1, dlSecondPartGenAlpha), (1, dlSecondPartGenNumber))

  implicit lazy val LocalDateArb: Arbitrary[LocalDate] = InstantArb.arbitrary.map(_.atZone(ZoneOffset.UTC).toLocalDate)

  implicit lazy val UrlArb: Arbitrary[URL] = for {
    path <- alphaStr.arbitrary
    host = "domain"
    domain = "com"
    schema = "http://"
  } yield new URL(s"$schema$host.$domain/$path")

  implicit lazy val PhoneArb: Arbitrary[Phone] = alphaNumStr.tagged[Tag.Phone]

  implicit lazy val EmailArb: Arbitrary[Email] = for {
    firstChar <- alphaChar.arbitrary
    name <- alphaNumStr.arbitrary
    domainName <- alphaNumStr.arbitrary
    zone <- alphaStr.arbitrary
  } yield s"$firstChar$name@$domainName.$zone".taggedWith[Tag.Email]
  implicit lazy val AutoruUserIdArb: Arbitrary[AutoruUserId] = alphaNumStr.tagged[Tag.AutoruUserId]
  implicit lazy val YandexUidArb: Arbitrary[YandexUid] = numStr.tagged[Tag.YandexUid]
  implicit lazy val VehicleVinArb: Arbitrary[VehicleVin] = alphaNumStr.tagged[Tag.VehicleVin]
  implicit lazy val TaskIdArb: Arbitrary[TaskId] = alphaNumStr.tagged[Tag.TaskId]
  implicit lazy val IdempotencyKeyArb: Arbitrary[IdempotencyKey] = alphaNumStr.tagged[Tag.IdempotencyKey]
  implicit lazy val TvmIdArb: Arbitrary[TvmId] = numStr.tagged[Tag.TvmId]
  implicit lazy val OperatorNameArb: Arbitrary[OperatorName] = alphaNumStr.tagged[Tag.OperatorName]

  implicit lazy val DomainArb: Arbitrary[Domain] =
    Gen.oneOf(Domain.values.filter(KarpTask.SupportedDomains.contains))

  implicit lazy val ProviderTypeArb: Arbitrary[proto.common.ProviderType] =
    Gen.oneOf(proto.common.ProviderType.values.filterNot(_.isUnknownDataType))

  implicit lazy val IdentifierArb: Arbitrary[Identifier] = Gen.oneOf(
    gen[PhoneIdentifier].arbitrary,
    gen[EmailIdentifier].arbitrary,
    gen[AutoruUserIdentifier].arbitrary,
    gen[YandexUidIdentifier].arbitrary,
    gen[VehicleIdentifier].arbitrary
  )
}
