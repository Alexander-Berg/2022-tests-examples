package common

import org.scalacheck.Gen
import ru.yandex.auto.catboost.CatboostModelSize
import ru.yandex.auto.core.model.UnifiedCarInfo
import ru.yandex.auto.core.model.catboost.NumericFeaturePair

import java.time.Instant

trait UnifiedInfoFixture {

  def genUnifiedInfo(): Gen[UnifiedCarInfo] =
    for {
      freshRelevance1 <- Gen.choose(1L, Long.MaxValue)
      id <- Gen.choose(1L, 10000)
      prices <- Gen.infiniteStream(Gen.choose(0L, 999999999L))
      floats <- Gen.infiniteStream(Gen.choose(0f, 1f))
    } yield {
      val relevance = floats.iterator
      val priceIterator = prices.iterator
      val info = new UnifiedCarInfo(id.toString)
      info.setFreshRelevance1(freshRelevance1)
      info.setCreationDate(Instant.now().toEpochMilli)
      info.setUpdateDate(Instant.now().toEpochMilli)
      info.setFreshDate(Instant.now().toEpochMilli)
      info.setBodyTypeFull("SEDAN" + priceIterator.next())
      info.setColorFull(priceIterator.next().toString)

      info.setNumericFeaturePairs(
        CatboostModelSize.offerFeatures
          .filter(_.name().contains("_STAT_"))
          .map(f => new NumericFeaturePair(f.name(), relevance.next()))
          .toArray
      )

      info.setAuctionCallsStat(id)
      info.setAuctionBasePrice(priceIterator.next())
      info.setAuctionBidPrice(priceIterator.next())
      info
    }

}
