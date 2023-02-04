package ru.yandex.realty.seller.processing.schedule

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.model.user.PassportUser
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import ru.yandex.realty.seller.model.product.ProductTypes
import ru.yandex.realty.seller.model.schedule.{ProductScheduleContext, ProductScheduleState, ScheduleOnceContext}
import ru.yandex.realty.seller.proto.SellerStorageProtoFormats
import ru.yandex.realty.seller.service.util.ScheduleUtils
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.watching.ProcessingState
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class ScheduleOnceStageSpec extends AsyncSpecBase with SellerModelGenerators with SellerStorageProtoFormats {

  private val processor: ScheduleProductCreationProcessor = mock[ScheduleProductCreationProcessor]
  private val stage: ScheduleOnceStage = new ScheduleOnceStage(processor)
  implicit val traced: Traced = Traced.empty

  private def stateStub(time: DateTime) = {

    val item = ScheduleUtils.scheduleWithDays(time, ScheduleUtils.AllDays)

    ProductScheduleState(
      owner = PassportUser(1),
      offerId = "1",
      productType = ProductTypes.Raising,
      turnedOn = true,
      scheduleContext = ProductScheduleContext(ScheduleOnceContext(manualSchedule = Seq(item))),
      visitTime = Some(time),
      shardKey = 0
    )

  }

  "ScheduleOnceStage" should {
    "create new product if it's not exist" in {
      val time = DateTimeUtil.now().withMillisOfSecond(0).withSecondOfMinute(0)
      val entry = stateStub(time)
      val processingState = ProcessingState(entry)

      (processor
        .hasProductForTemplate(_: ProductTemplate))
        .expects(*)
        .returns(Future.successful(false))

      (processor
        .createProduct(_: ProductTemplate))
        .expects(*)
        .returns(Future.unit)

      val newState = stage.process(processingState).futureValue
      newState.entry.visitTime shouldBe Some(time.plusDays(1))
    }

    "don't create product and update visit time if product exist" in {
      val time = DateTimeUtil.now().withMillisOfSecond(0).withSecondOfMinute(0)
      val entry = stateStub(time)
      val processingState = ProcessingState(entry)

      (processor
        .hasProductForTemplate(_: ProductTemplate))
        .expects(*)
        .returns(Future.successful(true))

      val newState = stage.process(processingState).futureValue
      newState.entry.visitTime shouldBe Some(time.plusDays(1))
    }
  }

}
