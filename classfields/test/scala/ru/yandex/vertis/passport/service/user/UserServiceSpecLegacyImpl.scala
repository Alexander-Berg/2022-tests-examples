package ru.yandex.vertis.passport.service.user

import ru.yandex.vertis.passport.test.RedisStartStopSupport

/**
  *
  * @author zvez
  */
class UserServiceSpecLegacyImpl
  extends UserServiceSpec
  with DbDaoProvider
  with TestSocialProviders
  with ServiceProviderLegacyImpl
  with RedisStartStopSupport
