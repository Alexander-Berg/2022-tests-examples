package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.joda.time.DateTime
import org.mockito.Mockito.{doNothing, times, verify}
import org.scalatest._
import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.utils.NextCheckData
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.autoru.users.passport.PassportManager
import ru.yandex.vos2.commonfeatures.{FeatureRegistryFactory, FeaturesManager}
import ru.yandex.vos2.dao.users.UserDao
import org.junit.runner.RunWith
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UserMigrationWorkerYdbTest extends AnyWordSpec with MockitoSupport with Matchers with OptionValues {
  val featuresRegistry: FeatureRegistry = FeatureRegistryFactory.inMemory()
  val featuresManager = new FeaturesManager(featuresRegistry)

  implicit val trace = Traced.empty

  abstract private class Fixture {

    val passportManager = mock[PassportManager]
    val userDao = mock[UserDao]

    val worker = new UserMigrationWorkerYdb(
      userDao,
      passportManager
    ) with YdbWorkerTestImpl {
      override def features = featuresManager
    }
    val offerBuilder = TestUtils.createOffer(dealer = false)
  }

  "should process with empty state" in new Fixture {
    assert(worker.shouldProcess(offerBuilder.build, None).shouldProcess)
  }

  "should not process with young state" in new Fixture {
    val nextState = utils.getStateStr(NextCheckData(new DateTime().plusDays(1)))
    assert(!worker.shouldProcess(offerBuilder.build, Some(nextState)).shouldProcess)
  }

  "should not update without changes" in new Fixture {
    val user = offerBuilder.getUser
    when(userDao.find(?)).thenReturn(Some(user))
    val res = worker.process(offerBuilder.build(), None)
    assert(res.updateOfferFunc.isEmpty)
  }

  "should update userDao if user was not found" in new Fixture {
    val user = offerBuilder.getUser
    when(userDao.find(?)).thenReturn(None)
    doNothing().when(userDao).update(?, ?)
    when(passportManager.getUpdatedUser(?)(?)).thenReturn(Some(user))
    val res = worker.process(offerBuilder.build(), None)
    assert(res.updateOfferFunc.isEmpty)
    verify(userDao, times(1)).update(?, ?)

  }

}
