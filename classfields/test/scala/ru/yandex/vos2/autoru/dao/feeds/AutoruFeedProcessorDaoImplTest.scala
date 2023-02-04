package ru.yandex.vos2.autoru.dao.feeds

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.model.UserRef

import scala.concurrent.duration.DurationInt

/**
  * Created by sievmi on 22.03.18
  */
class AutoruFeedProcessorDaoImplTest extends AnyFunSuite with InitTestDbs with BeforeAndAfterAll {

  override def beforeAll(): Unit = {
    initDbs()
  }

  val feedprocessorDao: AutoruFeedProcessorDao = components.feedprocessorDao

  test("CRUD test (errors) ") {
    val userRef = UserRef(1L)

    //empty table
    assert(feedprocessorDao.findCntErrorByTaskId(userRef, 1L).isEmpty)

    //insert
    feedprocessorDao.upsertCntError(userRef, 1L, 20)
    assert(feedprocessorDao.findCntErrorByTaskId(userRef, 1L).contains(20))
    assert(feedprocessorDao.findCntCriticalErrorsByTaskId(userRef, 1L).contains(0))

    //update
    feedprocessorDao.upsertCntError(userRef, 1L, 30)
    assert(feedprocessorDao.findCntErrorByTaskId(userRef, 1L).contains(50))
    assert(feedprocessorDao.findCntCriticalErrorsByTaskId(userRef, 1L).contains(0))

    //update critical errors
    feedprocessorDao.upsertCntCriticalErrors(userRef, 1L, 3)
    assert(feedprocessorDao.findCntErrorByTaskId(userRef, 1L).contains(53))
    assert(feedprocessorDao.findCntCriticalErrorsByTaskId(userRef, 1L).contains(3))

    //delete
    feedprocessorDao.deleteCntError(userRef, 1L)
    assert(feedprocessorDao.findCntErrorByTaskId(userRef, 1L).isEmpty)
  }

  test("insert critical errors") {
    val userRef = UserRef(2L)

    assert(feedprocessorDao.findCntErrorByTaskId(userRef, 2L).isEmpty)

    feedprocessorDao.upsertCntCriticalErrors(userRef, 1L, 22)
    assert(feedprocessorDao.findCntErrorByTaskId(userRef, 1L).contains(22))
    assert(feedprocessorDao.findCntCriticalErrorsByTaskId(userRef, 1L).contains(22))

    feedprocessorDao.deleteCntError(userRef, 1L)
    assert(feedprocessorDao.findCntErrorByTaskId(userRef, 1L).isEmpty)
  }

  test("CRU test (task id)") {
    val userRef = UserRef(1L)
    val feedId = "CARS/NEW/1"

    assert(feedprocessorDao.findTaskIdByFeedId(userRef, feedId).isEmpty)

    feedprocessorDao.upsertTaskIdByFeedId(userRef, feedId, 1L)
    assert(feedprocessorDao.findTaskIdByFeedId(userRef, feedId).contains(1))

    feedprocessorDao.upsertTaskIdByFeedId(userRef, feedId, 2L)
    assert(feedprocessorDao.findTaskIdByFeedId(userRef, feedId).contains(2))
  }

  test("deduplication without exception") {
    implicit val trace: Traced = Traced.empty
    val taskId = 1
    components.featureRegistry.updateFeature(components.featuresManager.FeedProcessorDeduplication.name, true)
    feedprocessorDao.checkAndCreateDeduplicationRecord(taskId, 180.seconds)
  }

  test("deduplication with exception") {
    implicit val trace: Traced = Traced.empty
    val taskId = 1
    components.featureRegistry.updateFeature(components.featuresManager.FeedProcessorDeduplication.name, true)
    intercept[IllegalStateException] {
      feedprocessorDao.checkAndCreateDeduplicationRecord(taskId, 180.seconds)
    }

  }
}
