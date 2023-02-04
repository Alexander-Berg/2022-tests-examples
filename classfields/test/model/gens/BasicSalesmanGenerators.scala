package ru.auto.salesman.test.model.gens

import cats.data.{NonEmptyList, NonEmptySet}
import cats.kernel.Order
import com.google.protobuf.util.Timestamps
import com.google.protobuf.{ProtocolMessageEnum, Timestamp}
import org.scalacheck.{Arbitrary, Gen}
import ru.auto.salesman.model.{
  DeprecatedDomain,
  DeprecatedDomains,
  ProductDuration,
  ProductStatus,
  ProductStatuses
}
import ru.auto.salesman.util.DateTimeInterval
import ru.yandex.vertis.generators.{BasicGenerators, DateTimeGenerators}

import scala.concurrent.duration._

trait BasicSalesmanGenerators extends BasicGenerators with DateTimeGenerators {

  val dateTimeIntervalGen: Gen[DateTimeInterval] = for {
    from <- dateTimeInPast()
    to <- dateTimeInFuture()
  } yield DateTimeInterval(from, to)

  val timestampGen: Gen[Timestamp] = for {
    dateTime <- dateTime()
  } yield
    Timestamp.newBuilder
      .setSeconds(dateTime.getMillis / 1000)
      .build

  val timestampInFutureGen: Gen[Timestamp] = for {
    dateTimeInFuture <- dateTimeInFuture()
  } yield Timestamps.fromMillis(dateTimeInFuture.getMillis)

  def enumGen[E <: Enumeration](enu: E): Gen[E#Value] =
    Gen.oneOf(enu.values.toSeq)

  def protoEnumWithZeroGen[T <: ProtocolMessageEnum](values: Seq[T]): Gen[T] =
    Gen.oneOf(
      values
        .filter(_.toString != "UNRECOGNIZED")
    ) //skips UNRECOGNIZED(-1)

  val productStatusGen: Gen[ProductStatus] = enumGen(ProductStatuses)

  val domainGen: Gen[DeprecatedDomain] = enumGen(DeprecatedDomains)

  val nonEmptyStringGen: Gen[String] =
    Gen.nonEmptyListOf[Char](Arbitrary.arbChar.arbitrary).map(_.mkString)

  def nonEmptySetOf[A: Order](gen: Gen[A]): Gen[NonEmptySet[A]] =
    Gen
      .nonEmptyListOf(gen)
      .map(NonEmptyList.fromListUnsafe)
      .map(_.toNes)

  val productDurationGen: Gen[ProductDuration] =
    Gen.choose(1, 180).map(ProductDuration.days)

  implicit class RichGen(private val gen: Gen.type) {

    def differentPair[A](seq: Traversable[A]): Gen[(A, A)] =
      for {
        listOf2 <- Gen.pick(2, seq.toSet).map(_.toList)
      } yield (listOf2.head, listOf2.last)

    def days(minDays: Int, maxDays: Int) =
      Gen.choose(minDays, maxDays).map(_.days)
  }
}

object BasicSalesmanGenerators extends BasicSalesmanGenerators
