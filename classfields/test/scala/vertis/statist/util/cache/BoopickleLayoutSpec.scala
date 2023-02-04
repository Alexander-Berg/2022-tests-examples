package vertis.statist.util.cache

import vertis.statist.dao.counter.CachingCounterDao
import vertis.statist.dao.counter.CachingCounterDao.CacheValue
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.generators.ProducerProvider
import vertis.statist.Generators

/** @author zvez
  */
class BoopickleLayoutSpec extends AnyWordSpec with Matchers with ScalaFutures with ProducerProvider {

  "BoopickleLayout" should {

    "work!" in {
      val layout = CachingCounterDao.byDayCacheLayout

      val data = Generators.objectDailyValues(Set("offer_show", "phone_show")).next
      val cacheValue = CacheValue(data, DateTime.now)
      val bytes = layout.encode(cacheValue)
      val parsedValue = layout.decode(bytes).get
      parsedValue shouldBe cacheValue
    }
  }

}
