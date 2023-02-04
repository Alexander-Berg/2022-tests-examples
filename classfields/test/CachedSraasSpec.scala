package common.sraas.test

import com.google.protobuf.{FloatValue, Timestamp, UInt32Value}
import common.sraas.Sraas.SraasDescriptor
import common.sraas.{CachedSraas, Sraas, TestSraas}
import zio.test.Assertion.equalTo
import zio.test.TestAspect.sequential
import zio.test.environment.TestClock
import zio.test.{assert, assertTrue, DefaultRunnableSpec}
import zio.{UIO, ZIO, ZLayer}

import java.time.Duration
import scala.concurrent.duration.DurationInt

object CachedSraasSpec extends DefaultRunnableSpec {

  override def spec = {
    suite("CachedSraas")(
      testM("cache descriptor") {
        val descriptor1 = SraasDescriptor(Timestamp.getDescriptor, Timestamp.getDescriptor.getFullName, "42")
        val descriptor2 = SraasDescriptor(FloatValue.getDescriptor, FloatValue.getDescriptor.getFullName, "43")

        for {
          _ <- TestClock.setTime(Duration.ofMinutes(1))
          _ <- TestSraas.setJavaDescriptor(_ => UIO(descriptor1))
          result1 <- Sraas.getDescriptor(Timestamp.getDescriptor.getFullName)
          _ <- TestSraas.setJavaDescriptor(_ => UIO(descriptor2))
          result2 <- Sraas.getDescriptor(Timestamp.getDescriptor.getFullName)
        } yield {
          assertTrue(result1 == descriptor1, result2 == descriptor1)
        }
      },
      testM("cache last version") {
        val descriptor1 = SraasDescriptor(Timestamp.getDescriptor, Timestamp.getDescriptor.getFullName, "42")
        val descriptor2 = SraasDescriptor(UInt32Value.getDescriptor, UInt32Value.getDescriptor.getFullName, "43")
        for {
          _ <- TestClock.setTime(Duration.ofMinutes(1))
          _ <- TestSraas.setJavaDescriptor(_ => UIO(descriptor1))
          result1 <- Sraas.getDescriptor(Timestamp.getDescriptor.getFullName)
          result2 <- Sraas.getDescriptor(FloatValue.getDescriptor.getFullName)
          _ <- TestSraas.setJavaDescriptor(_ => UIO(descriptor2))
          result3 <- Sraas.getDescriptor(Timestamp.getDescriptor.getFullName)
        } yield {
          assertTrue(result1 == descriptor1, result2 == descriptor1, result3 == descriptor1)
        }
      },
      testM("refresh cached last version") {
        val descriptor1 = SraasDescriptor(Timestamp.getDescriptor, Timestamp.getDescriptor.getFullName, "42")
        val descriptor2 = SraasDescriptor(UInt32Value.getDescriptor, UInt32Value.getDescriptor.getFullName, "43")
        for {
          _ <- TestClock.setTime(Duration.ofMinutes(1))
          _ <- TestSraas.setJavaDescriptor(_ => UIO(descriptor1))
          result1 <- Sraas.getDescriptor(Timestamp.getDescriptor.getFullName)
          result2 <- Sraas.getDescriptor(FloatValue.getDescriptor.getFullName)
          _ <- TestClock.setTime(Duration.ofMinutes(3))
          _ <- TestSraas.setJavaDescriptor(_ => UIO(descriptor2))
          result3 <- Sraas.getDescriptor(Timestamp.getDescriptor.getFullName)
        } yield {
          assertTrue(result1 == descriptor1, result2 == descriptor1, result3 == descriptor2)
        }
      }
    ) @@ sequential
  }.provideCustomLayerShared {
    val testSraas = TestSraas.layer
    val clock = TestClock.default
    clock ++ ZLayer.succeed(CachedSraas.Config(1.minute, 2)) ++ testSraas >+> CachedSraas.live
  }
}
