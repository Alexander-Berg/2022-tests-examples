package vsmoney.auction_auto_strategy.converters.test

import common.models.finance.Money.Kopecks
import vsmoney.auction.common_model.AuctionContext.Context.{CriteriaContext => ProtoContext}
import vsmoney.auction.common_model.{AuctionContext, CriteriaValue, Money => ProtoMoney, Project => ProtoProject}
import vsmoney.auction_auto_strategy.converters.{CommonProtoConverter, ConverterError}
import vsmoney.auction_auto_strategy.model.auction.{Bid, CriteriaContext, Criterion, CriterionKey, CriterionValue}
import vsmoney.auction_auto_strategy.model.common.Project
import zio.test.Assertion._
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

object CommonProtoConverterSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("CommonProtoConverter")(
      suite("someMoneyToBidOrFail")(
        testM("should be convert Option[ProtoMoney] to Bid") {
          val request = ProtoMoney(13334)
          assertM(CommonProtoConverter.someMoneyToBidOrFail.convert(Some(request)).run)(
            succeeds(equalTo(Bid(Kopecks(13334))))
          )
        },
        testM("should throw exception if ProtoMoney is None") {
          assertM(CommonProtoConverter.someMoneyToBidOrFail.convert(None).run)(
            fails(isSubtype[ConverterError.IllegalFieldValueError](anything))
          )
        },
        testM("should throw exception if Bid is negative") {
          val request = ProtoMoney(-13334)
          assertM(CommonProtoConverter.someMoneyToBidOrFail.convert(Some(request)).run)(
            fails(isSubtype[ConverterError.IllegalFieldValueError](anything))
          )
        }
      ),
      suite("projectFromProto")(
        testM("should convert ProtoProject.AUTORU to Project") {
          assertM(CommonProtoConverter.projectFromProto.convert(ProtoProject.AUTORU).run)(
            succeeds(equalTo(Project.Autoru))
          )
        },
        testM("should convert ProtoProject.REALTY to Project") {
          assertM(CommonProtoConverter.projectFromProto.convert(ProtoProject.REALTY).run)(
            succeeds(equalTo(Project.Realty))
          )
        },
        testM("should convert ProtoProject.GENERAL to Project") {
          assertM(CommonProtoConverter.projectFromProto.convert(ProtoProject.GENERAL).run)(
            succeeds(equalTo(Project.General))
          )
        },
        testM("should throw exception if ProtoProject.UNKNOWN_PROJECT") {
          assertM(CommonProtoConverter.projectFromProto.convert(ProtoProject.UNKNOWN_PROJECT).run)(
            fails(isSubtype[ConverterError.IllegalFieldValueError](anything))
          )
        }
      ),
      suite("projectToProto")(
        testM("should convert Project.Autoru to ProtoProject") {
          assertM(CommonProtoConverter.projectToProto.convert(Project.Autoru).run)(
            succeeds(equalTo(ProtoProject.AUTORU))
          )
        },
        testM("should convert Project.Realty to ProtoProject") {
          assertM(CommonProtoConverter.projectToProto.convert(Project.Realty).run)(
            succeeds(equalTo(ProtoProject.REALTY))
          )
        },
        testM("should convert Project.General to ProtoProject") {
          assertM(CommonProtoConverter.projectToProto.convert(Project.General).run)(
            succeeds(equalTo(ProtoProject.GENERAL))
          )
        }
      ),
      suite("contextToProto")(
        testM("should convert CriteriaContext to Proto AuctionContext") {

          val testRequest = CriteriaContext(List(Criterion(CriterionKey("key"), CriterionValue("value"))))

          val testResult = AuctionContext(
            ProtoContext(AuctionContext.CriteriaContext(List(CriteriaValue("key", "value"))))
          )

          assertM(CommonProtoConverter.contextToProto.convert(testRequest).run)(
            succeeds(equalTo(testResult))
          )
        }
      )
    )
  }
}
