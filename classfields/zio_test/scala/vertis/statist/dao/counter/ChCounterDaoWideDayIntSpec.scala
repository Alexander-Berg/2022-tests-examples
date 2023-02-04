package vertis.statist.dao.counter

import vertis.statist.db.Database
import vertis.statist.model.{Granularity, WideAggregationFunction}

/** @author zvez
  */
object ChCounterDaoWideDayIntSpec extends ChCounterDaoSpecBase {

  override protected def dao(db: Database): CounterDao =
    new ChCounterDaoWide(
      db,
      "offer_event_counter_wide_day",
      WideAggregationFunction.Sum,
      Set("card_view", "phone_show"),
      Granularity.Day
    )
}
