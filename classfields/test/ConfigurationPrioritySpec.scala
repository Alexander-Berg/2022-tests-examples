package common.zio.config.test

import common.zio.config.Configuration
import zio.test._
import zio.test.Assertion._

object ConfigurationPrioritySpec extends DefaultRunnableSpec {

  def spec =
    suite("Configuration.live")(
      testM("application.conf > vertis.defaults.conf > reference.conf") {
        for {
          a <- Configuration.getString("a") // application.conf wins reference.conf
          b <- Configuration.getString("b") // vertis.defaults.conf wins reference.conf
          c <- Configuration.getString("c") // defined only in reference.conf
          d <- Configuration.getString("d") // application.conf wins vertis.defaults.conf
        } yield assertTrue(a == "application.conf") &&
          assertTrue(b == "vertis.defaults") &&
          assertTrue(c == "reference") &&
          assertTrue(d == "application.conf")
      },
      testM("resolve references from reference.conf") {
        assertM(Configuration.getString("reference-ref"))(equalTo("defined"))
      }
    ).provideCustomLayer(Configuration.live.orDie) +
      suite("Configuration.development")(
        testM("application.development.conf > application.conf") {
          for {
            a <- Configuration.getString("a") // application.development.conf wins application.conf
          } yield assertTrue(a == "application.development.conf")
        }
      ).provideCustomLayer(Configuration.development.orDie)
}
