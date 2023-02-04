package auto.dealers.dealer_calls_auction.logic.test

import auto.common.model.ClientId
import auto.dealers.dealer_calls_auction.logic.PromoCampaignFilterRefinement
import ru.auto.api.search.search_model.SearchRequestParameters
import ru.auto.dealer_calls_auction.proto.promo_campaign_service.DaysNumberFilters
import zio.ZIO
import zio.test._
import zio.test.Assertion._

import java.time.temporal.ChronoUnit
import java.time.{OffsetDateTime, ZoneOffset}

object PromoCampaignFilterRefinementSpec extends DefaultRunnableSpec {

  private val currentDateTime = OffsetDateTime
    .of(2022, 5, 20, 12, 0, 0, 0, ZoneOffset.ofHours(3))
    .toInstant
  private val dealerStartOfTheDay = currentDateTime.minus(12, ChronoUnit.HOURS)
  private val dealerId = ClientId(20101L)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("PromoCampaignFilterRefinement")(
      testM("should refine filters") {
        val rawParams = SearchRequestParameters.defaultInstance
        val promoCampaignLastChanges = Some(currentDateTime.minus(1, ChronoUnit.HOURS))
        for {
          refinedResult <- ZIO.effect(
            PromoCampaignFilterRefinement.refine(
              rawParams,
              currentDateTime,
              dealerStartOfTheDay,
              dealerId,
              promoCampaignLastChanges,
              daysNumberParams = DaysNumberFilters(
                daysOnStockTo = Some(6),
                daysOnStockFrom = None,
                daysWithoutCallsTo = Some(2),
                daysWithoutCallsFrom = Some(0)
              )
            )
          )
        } yield assert(refinedResult.clientId)(equalTo(Some(dealerId.value.toString))) &&
          assert(refinedResult.lastCallTimestampFrom)(
            equalTo(Some(dealerStartOfTheDay.minus(2, ChronoUnit.DAYS).toEpochMilli))
          ) &&
          assert(refinedResult.lastCallTimestampTo)(equalTo(promoCampaignLastChanges.map(_.toEpochMilli))) &&
          assert(refinedResult.creationDateFrom)(
            equalTo(Some(currentDateTime.minus(6, ChronoUnit.DAYS).toEpochMilli))
          ) &&
          assert(refinedResult.creationDateTo)(isNone)
      }
    )
  }
}
