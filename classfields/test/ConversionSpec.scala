package ru.yandex.vertis.billing.shop.domain.test

import billing.log_model.TargetType
import ru.yandex.vertis.billing.shop.model.Constants.{OfferTargetType, RaiseFreeVasCode}
import ru.yandex.vertis.billing.shop.model.{ProductCode, Target}
import zio.{Task, UIO, ZIO}
import zio.test.Assertion._
import zio.test._

import scala.util.{Random, Try}

object ConversionSpec extends DefaultRunnableSpec {

  override def spec =
    suite("ConversionSpec")(
      testM("Legacy 'Offer'-string should produce Target(TargetType.Offer, _)") {
        for {
          strType <- UIO.succeed(OfferTargetType)
          target <- UIO.succeed(Target(strType, Random.nextString(10)))
        } yield assert(target.`type`)(equalTo(TargetType.Offer))
      },
      testM("Not-TargetType-like Random string should throw IllegalArgumentException") {
        val invalidTargetType = "SomeRandomString";
        val test = for {
          str <- UIO.succeed(invalidTargetType)
          res <- ZIO.fromEither(Try(Target(str, Random.nextString(10))).toEither)
        } yield res

        val result = test.run

        assertM(result)(fails(hasMessage(containsString("Invalid TargetType params:"))))
      },
      testM("Legacy 'raise_1'-string should produce ProductCode(billing.log_model.ProductCode.raise_1)") {
        for {
          strCode <- UIO.succeed(RaiseFreeVasCode)
          code <- UIO.succeed(ProductCode(strCode))
        } yield assert(code.code)(equalTo(billing.log_model.ProductCode.raise_1)) &&
          assert(code.code.name)(equalTo(RaiseFreeVasCode))
      },
      testM("Not-ProductCode-like Random string should throw IllegalArgumentException") {
        val invalidTargetType = "SomeRandomString";
        val test = for {
          str <- UIO.succeed(invalidTargetType)
          res <- ZIO.fromEither(Try(ProductCode(str)).toEither)
        } yield res

        val result = test.run

        assertM(result)(fails(hasMessage(containsString("Invalid ProductCode params:"))))
      }
    )
}
