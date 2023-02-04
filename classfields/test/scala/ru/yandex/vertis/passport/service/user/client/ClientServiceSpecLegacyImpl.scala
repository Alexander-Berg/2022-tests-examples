package ru.yandex.vertis.passport.service.user.client

import ru.yandex.vertis.passport.service.user.ServiceProviderLegacyImpl
import ru.yandex.vertis.passport.test.{RedisStartStopSupport, RedisSupport}

class ClientServiceSpecLegacyImpl
  extends ClientServiceSpec
  with RedisSupport
  with RedisStartStopSupport
  with ServiceProviderLegacyImpl
