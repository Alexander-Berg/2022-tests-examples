package vsmoney.auction.converters.test

import ru.vsmoney.auction.palma.proto.{auction_params_palma => palma}
import common.models.finance.Money.Kopecks
import vsmoney.auction.converters.ProtoConverterError.UnexpectedMessageError
import vsmoney.auction.model.FirstStep.BasePricePlusAmount
import vsmoney.auction.model.NextStep.ArithmeticProgression
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

object PalmaProtoConvertersSpec extends DefaultRunnableSpec {
  import vsmoney.auction.converters.PalmaProtoConverters._

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("PalmaProtoConverters")(
      testM("should convert first step properly") {
        val palmaFirstStep = palma.FirstStep(
          palma.FirstStep.Algorithm.BasePricePlusAmount(
            palma.FirstStep.BasePricePlusAmount(firstStep)
          )
        )
        val expected = BasePricePlusAmount(Kopecks(firstStep * 100))

        for {
          res <- firstStepFromPalmaConverter.convert(palmaFirstStep)
        } yield assert(res)(equalTo(expected))
      },
      testM("should convert next step properly") {
        val palmaNextStep = palma.NextStep(
          palma.NextStep.Algorithm.ArithmeticProgression(
            palma.NextStep.ArithmeticProgression(nextStep)
          )
        )
        val expected = ArithmeticProgression(Kopecks(nextStep * 100))

        for {
          res <- stepFromPalmaConverter.convert(palmaNextStep)
        } yield assert(res)(equalTo(expected))
      },
      testM("should fail on empty first step") {
        val badFirstStep = palma.FirstStep(palma.FirstStep.Algorithm.Empty)
        for {
          res <- firstStepFromPalmaConverter.convert(badFirstStep).run
        } yield assert(res)(fails(equalTo(UnexpectedMessageError(badFirstStep))))
      },
      testM("should fail on empty next step") {
        val badNextStep = palma.NextStep(palma.NextStep.Algorithm.Empty)
        for {
          res <- stepFromPalmaConverter.convert(badNextStep).run
        } yield assert(res)(fails(equalTo(UnexpectedMessageError(badNextStep))))
      }
    )
  }

  private val firstStep = 100L
  private val nextStep = 50L

}
