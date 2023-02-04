package vsmoney.auction_auto_strategy.converters.test

import vsmoney.auction.common_model.{AuctionContext => ProtoAuctionContext, CriteriaValue => ProtoCriteriaValue}
import vsmoney.auction_auto_strategy.converters.{AuctionContextProtoConverter, ConverterError}
import vsmoney.auction_auto_strategy.model.auction.{CriteriaContext, Criterion, CriterionKey, CriterionValue}
import zio.test.Assertion._
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

object AuctionContextProtoConverterSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("AuctionContextProtoConverter")(
      suite("toProto")(
        testM("should covert CriteriaContext to AuctionContext") {
          assertM(AuctionContextProtoConverter.toProto.convert(testCriteriaContext).run)(
            succeeds(equalTo(testProtoAuctionContext))
          )
        }
      ),
      suite("fromProto")(
        testM("should convert AuctionContext to CriteriaContext") {
          assertM(AuctionContextProtoConverter.fromProto.convert(Some(testProtoAuctionContext)).run)(
            succeeds(equalTo(testCriteriaContext))
          )
        },
        testM("should throw exception if AuctionContext is None") {
          assertM(AuctionContextProtoConverter.fromProto.convert(None).run)(
            fails(isSubtype[ConverterError.IllegalFieldValueError](anything))
          )
        },
        testM("should throw exception if CriteriaContext in AuctionContext is None") {
          assertM(
            AuctionContextProtoConverter.fromProto
              .convert(Some(testProtoAuctionContext.copy(context = ProtoAuctionContext.Context.Empty)))
              .run
          )(
            fails(isSubtype[ConverterError.IllegalFieldValueError](anything))
          )
        }
      )
    )
  }

  private val testCriteriaContext = CriteriaContext(criteria =
    Seq(
      Criterion(key = CriterionKey("region_id"), value = CriterionValue("42")),
      Criterion(key = CriterionKey("mark"), value = CriterionValue("bmw")),
      Criterion(key = CriterionKey("model"), value = CriterionValue("x5"))
    )
  )

  private val testProtoAuctionContext = ProtoAuctionContext(context =
    ProtoAuctionContext.Context.CriteriaContext(
      ProtoAuctionContext.CriteriaContext(criteriaValues =
        Seq(
          ProtoCriteriaValue(key = "region_id", value = "42"),
          ProtoCriteriaValue(key = "mark", value = "bmw"),
          ProtoCriteriaValue(key = "model", value = "x5")
        )
      )
    )
  )
}
