package ru.yandex.vertis.passport.service.session

import com.couchbase.client.java.{AsyncBucket, Bucket}
import com.couchbase.client.java.document.{JsonLongDocument, RawJsonDocument}
import org.joda.time.{DateTime, DateTimeUtils}
import ru.yandex.vertis.mockito.MockitoSupport.{?, mock, when}
import ru.yandex.vertis.passport.dao.impl.couchbase.JsonCouchbaseUtils.JsonModelHelper
import ru.yandex.vertis.passport.dao.impl.couchbase.{CouchbaseUserWasSeenDao, Prefixes}
import ru.yandex.vertis.passport.model.JsonFormats.UserWasSeenCountersEntryFormat
import ru.yandex.vertis.passport.model.Platforms
import ru.yandex.vertis.passport.model.visits.{UserWasSeenCounters, UserWasSeenCountersEntry}
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{CouchbaseSupport, ModelGenerators}
import ru.yandex.vertis.passport.util.RxJavaForScala.RichObservable
import rx.Observable

/**
  *
  * @author zvez
  */
class CouchbaseUserWasSeenDaoSpec extends UserWasSeenDaoSpec with CouchbaseSupport {
  import scala.concurrent.ExecutionContext.Implicits.global

  override val dao = new CouchbaseUserWasSeenDao(testBucket)

  private val mockedAsyncBucket: AsyncBucket = mock[AsyncBucket]
  private val mockedBucket: Bucket = mock[Bucket]
  when(mockedBucket.async()).thenReturn(mockedAsyncBucket)
  private val mockedDao = new CouchbaseUserWasSeenDao(mockedBucket)

  "UserWasSeenDao.touch" should {
    "return exception on couchbase get operation fail" in {
      val userId = ModelGenerators.userId.next
      val moment = DateTime.now().withMillisOfDay(0)
      DateTimeUtils.setCurrentMillisFixed(moment.getMillis)
      when(mockedAsyncBucket.get(?, ?)).thenReturn(Observable.error(new RuntimeException("error!")))
      mockedDao.touch(userId, Platforms.Android).failed.futureValue shouldBe an[RuntimeException]
    }

    "return exception on couchbase insert operation fail" in {
      val userId = ModelGenerators.userId.next
      val moment = DateTime.now().withMillisOfDay(0)
      DateTimeUtils.setCurrentMillisFixed(moment.getMillis)
      when(mockedAsyncBucket.get(?, ?)).thenReturn(Observable.empty())
      when(mockedAsyncBucket.insert(?)).thenReturn(Observable.error(new RuntimeException("error!")))
      mockedDao.touch(userId, Platforms.Android).failed.futureValue shouldBe an[RuntimeException]
    }

    "return exception on couchbase replace operation fail" in {
      val userId = ModelGenerators.userId.next
      val entityId = Prefixes.UserWasSeenStat.key(userId)
      val moment = DateTime.now().withMillisOfDay(0)
      DateTimeUtils.setCurrentMillisFixed(moment.getMillis)
      when(mockedAsyncBucket.get[RawJsonDocument](?, ?))
        .thenReturn(Observable.from(Array(UserWasSeenCountersEntry(entityId, UserWasSeenCounters()).asDocument())))
      when(mockedAsyncBucket.replace(?)).thenReturn(Observable.error(new RuntimeException("error!")))
      mockedDao.touch(userId, Platforms.Android).failed.futureValue shouldBe an[RuntimeException]
    }
  }

  "UserWasSeenDao.getLastSeen" should {
    "return legacy counter" in {
      val userId = ModelGenerators.userId.next
      val moment = DateTime.now().withMillisOfDay(0)
      val f =
        testBucket
          .async()
          .upsert(JsonLongDocument.create(Prefixes.UserLastSeen.key(userId), moment.getMillis))
          .asScalaFutureUnit
      f.futureValue
      dao.getLastSeen(userId).futureValue.value shouldBe moment
    }
  }
}
