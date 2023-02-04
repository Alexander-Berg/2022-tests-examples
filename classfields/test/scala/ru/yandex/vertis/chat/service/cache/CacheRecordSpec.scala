package ru.yandex.vertis.chat.service.cache

import ru.yandex.vertis.chat.SpecBase
import ru.yandex.vertis.chat.components.cache.CacheRecord

/**
  * Specs on [[CacheRecord]] serialization/deserialization.
  *
  * @author dimas
  */
class CacheRecordSpec extends SpecBase {

  "CacheRecord" should {
    "round trip through plain representation" in {
      rt(CacheRecord.Room("room-123"))
      rt(CacheRecord.Rooms("user-42"))
      rt(CacheRecord.UnreadFlag("user-42"))
      rt(CacheRecord.Room("room:123"))
      rt(CacheRecord.Rooms("rooms:123"))
      rt(CacheRecord.UnreadFlag("unread-flag:123"))
    }

    "not be parsed from trash" in {
      CacheRecord("abc") should be(None)
      CacheRecord("room:") should be(None)
      CacheRecord("") should be(None)
    }
  }

  private def rt(record: CacheRecord) = {
    CacheRecord(record.plain) should be(Some(record))
  }

}
