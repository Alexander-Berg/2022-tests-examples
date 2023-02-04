package ru.auto.salesman

import cats.data.NonEmptyList
import ru.auto.salesman.exceptions.CompositeException
import ru.auto.salesman.test.{BaseSpec, TestException}
import zio.{Ref, ZIO}

class RichZIOObjectSpec extends BaseSpec {

  "executeAll" should {

    "succeed if all effects succeed" in {
      ZIO
        .executeAll[Any](ZIO.unit, ZIO.unit)
        .success
        .value shouldBe unit
    }

    "return error if one effect failed" in {
      val e = new TestException
      ZIO
        .executeAll(ZIO.unit, ZIO.fail(e))
        .failure
        .exception shouldBe CompositeException(NonEmptyList.one(e))
    }

    "return two errors in proper order if two effects failed" in {
      val e1 = new TestException("e1")
      val e2 = new TestException("e2")
      ZIO
        .executeAll(ZIO.unit, ZIO.fail(e1), ZIO.unit, ZIO.fail(e2), ZIO.unit)
        .failure
        .exception shouldBe CompositeException(NonEmptyList.of(e1, e2))
    }

    "execute all given effects" in {
      val z = for {
        ref <- Ref.make(0)
        plus = (n: Int) => ref.update(_ + n)
        _ <- ZIO.executeAll(plus(1), plus(2), plus(3))
        result <- ref.get
      } yield result shouldBe 6
      z.success
    }

    "execute all given effects even if there is a failure" in {
      val z = for {
        ref <- Ref.make(0)
        plus = (n: Int) => ref.update(_ + n)
        failure = ZIO.fail(new TestException)
        _ <- ZIO.executeAll(plus(1), plus(2), failure, plus(3)).ignore
        result <- ref.get
      } yield result shouldBe 6
      z.success
    }
  }
}
