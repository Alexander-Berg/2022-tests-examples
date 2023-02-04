package ru.yandex.vertis.passport.service.user.social

import ru.yandex.mds.DummyMdsClientImpl
import ru.yandex.vertis.passport.dao.impl.memory.InMemoryPerSessionStorage
import ru.yandex.vertis.passport.service.user.InMemoryDaoProvider
import ru.yandex.vertis.passport.util.crypt.{DummySigner, Signer}
import ru.yandex.vertis.passport.service.log.EventLog

/**
  *
  * @author zvez
  */
class SocialUserServiceInMemorySpec extends SocialUserServiceSpec with InMemoryDaoProvider {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def signer: Signer = DummySigner

  override val socialUserService =
    new SocialUserServiceImpl(
      userDao,
      socialProviders,
      linkDecider,
      banService,
      new InMemoryPerSessionStorage,
      new DummyMdsClientImpl,
      signer,
      featureManager
    ) with EventLoggingSocialUserService {

      override def eventLog: EventLog = SocialUserServiceInMemorySpec.this.eventLog

    }
}
