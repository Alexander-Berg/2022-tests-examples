package vertis.zio.util

import vertis.zio.managed.ManagedUtils._
import vertis.zio.test.ZioSpecBase
import zio.{Managed, Ref, ZIO}

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class HalfManagedSpec extends ZioSpecBase {

  "HalfManaged" should {
    "close resources on close" in ioTest {
      for {
        innerResource <- Ref.make(0)
        m = Managed.make(innerResource.update(_ + 1))(_ => innerResource.update(_ - 1))
        hm <- m.acquire
        _ <- checkAcquired(innerResource)
        _ <- hm.close
        _ <- checkReleased(innerResource)
      } yield ()
    }

    (0 to 1).foreach { i =>
      s"close resources on failed acquisition $i" in ioTest {
        for {
          innerResource <- Ref.make(0)
          m = Managed.make(innerResource.update(_ + 1))(_ => innerResource.update(_ - 1)) *>
            ZIO.fail("oops").toManaged_
          _ <- m.acquire.ignore
          _ <- checkReleased(innerResource)
        } yield ()
      }
    }

    (0 to 1).foreach { i =>
      s"close resources on died acquisition $i" in ioTest {
        for {
          innerResource <- Ref.make(0)
          m = Managed.make(innerResource.update(_ + 1))(_ => innerResource.update(_ - 1)) *>
            ZIO.die(new RuntimeException("oops")).toManaged_
          _ <- m.acquire.absorb.ignore
          _ <- checkReleased(innerResource)
        } yield ()
      }
    }

    (0 to 1).foreach { i =>
      s"close resources on interrupted acquisition $i" in ioTest {
        for {
          innerResource <- Ref.make(0)
          m = Managed.make(innerResource.update(_ + 1))(_ => innerResource.update(_ - 1)) *>
            ZIO.interrupt.toManaged_
          _ <- m.acquire.absorb.ignore
          _ <- checkReleased(innerResource)
        } yield ()
      }
    }

    // this test is known to flap, HalfManaged is to be dropped
    "close resources on fiber's finish if acquired with outliveFiber=false" ignore ioTest {
      for {
        resource <- Ref.make(0)
        m = Managed.make(resource.update(_ + 1))(_ => resource.update(_ - 1))
        fiber <- {
          m.acquireWith(outliveFiber = false).ignore *>
            checkAcquired(resource)
        }.fork
        _ <- fiber.join
        _ <- checkReleased(resource)
      } yield ()
    }

    "not close resources on fiber's finish by default" in ioTest {
      for {
        resource <- Ref.make(0)
        m = Managed.make(resource.update(_ + 1))(_ => resource.update(_ - 1))
        fiber <- {
          m.acquire.ignore *>
            checkAcquired(resource)
        }.fork
        _ <- fiber.join
        _ <- checkAcquired(resource)
      } yield ()
    }
  }

  private def checkAcquired(ref: Ref[Int]) =
    checkM("inner resource acquired")(ref.get.map(_ shouldBe 1))

  private def checkReleased(ref: Ref[Int]) =
    checkM("inner resource released")(ref.get.map(_ shouldBe 0))
}
