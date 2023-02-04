package ru.yandex.vertis.feedprocessor.dao

import ru.yandex.vertis.feedprocessor.BaseSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * @author pnaydenov
  */
class KVClientSpec extends BaseSpec {
  "KVClient" should {
    "correctly work with prefix" in {
      val underlying = mock[KVClient]
      val kvClient = underlying.withPrefix("myprefix/")(global)

      (underlying.set _).expects("myprefix/key1", "value1", *).returningF(())
      (underlying.get _).expects("myprefix/key2").returningF(Some("value2"))
      (underlying.remove _).expects("myprefix/key3").returningF(())
      (underlying.bulkGet _)
        .expects(Seq("myprefix/key4", "myprefix/key5"))
        .returningF(Map("myprefix/key4" -> "value4", "myprefix/key5" -> "value5"))
      (underlying.bulkSet _).expects(Map("myprefix/key6" -> "value6", "myprefix/key7" -> "value7")).returningF(())
      (underlying.bulkSetWithTTL _)
        .expects(
          Map("myprefix/key8" -> ("value8" -> 8.seconds), "myprefix/key9" -> ("value9" -> 9.minutes))
        )
        .returningF(())

      kvClient.set("key1", "value1").futureValue.shouldEqual(())
      kvClient.get("key2").futureValue.get shouldEqual "value2"
      kvClient.remove("key3").futureValue.shouldEqual(())
      kvClient.bulkGet(Seq("key4", "key5")).futureValue shouldEqual Map("key4" -> "value4", "key5" -> "value5")
      kvClient.bulkSet(Map("key6" -> "value6", "key7" -> "value7")).futureValue.shouldEqual(())
      kvClient
        .bulkSetWithTTL(Map("key8" -> ("value8" -> 8.seconds), "key9" -> ("value9" -> 9.minutes)))
        .futureValue
        .shouldEqual(())
    }
  }
}
