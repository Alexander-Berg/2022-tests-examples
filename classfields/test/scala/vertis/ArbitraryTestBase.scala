package vertis

import com.google.common.base.Charsets
import com.google.common.hash.{BloomFilter, Funnels}
import org.scalacheck.rng.Seed
import org.scalacheck.{Arbitrary, Gen}
import shapeless.Witness

import java.time.Instant
import scala.annotation.nowarn
import scala.reflect.runtime.universe._
import scala.util.{Success, Try}

/** Use in combination with either https://github.com/chocpanda/scalacheck-magnolia or
  * https://github.com/alexarchambault/scalacheck-shapeless to derive Arbitrary.
 *
 * @author Ratskevich Natalia reimai@yandex-team.ru
  */
trait ArbitraryTestBase {

  protected def genParams: Gen.Parameters =
    Gen.Parameters.default.withSize(5)

  protected def seed: Seed = Seed.random()

  implicit lazy val arbitraryInstant: Arbitrary[Instant] =
    Arbitrary(Instant.now)

  // magnolia generator has strings in chinese
  // todo or empty?
  implicit lazy val nonEmptyStr: Arbitrary[String] = Arbitrary(Gen.nonEmptyListOf(Gen.alphaChar).map(_.mkString))

  lazy val uniqString: Gen[String] = {
    val allStrings: BloomFilter[String] = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_8), 10000)
    Gen.alphaStr
      .filterNot(_.isEmpty)
      .retryUntil(allStrings.put, 10)
  }

  implicit def arbEnum[E <: Enumeration](implicit w: Witness.Aux[E]): Arbitrary[E#Value] =
    Arbitrary(Gen.oneOf((w.value.values)))

  protected def getUniqString = uniqString.sample.get

  def random[T: WeakTypeTag: Arbitrary]: T = random(1).head

  def random[T: WeakTypeTag: Arbitrary](n: Int): Seq[T] =
    randomThat[T](n)(_ => true)

  @nowarn("msg=type Stream in package scala is deprecated")
  def randomThat[T: WeakTypeTag: Arbitrary](n: Int)(filter: T => Boolean): Seq[T] = {
    val gen = Gen.infiniteStream(implicitly[Arbitrary[T]].arbitrary.filter(filter))
    Try(gen.apply(genParams, seed)) match {
      case Success(Some(v)) => v.take(n)
      case _ => explode[T]()
    }
  }

  private def explode[T: WeakTypeTag]() = {
    val tpe = implicitly[WeakTypeTag[T]].tpe
    val msg =
      s"""Could not generate a random value for $tpe.
         |Please, make use that the Arbitrary instance for type $tpe is not too restrictive""".stripMargin
    throw new RuntimeException(msg)
  }

}
