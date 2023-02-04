package ru.yandex.vertis.general.wisp.clients.testkit

import ru.yandex.vertis.general.wisp.clients.messenger_meta_api.MessengerMetaApiClient
import zio.test.mock.mockable

@mockable[MessengerMetaApiClient.Service]
object TestMessengerMetaApiClient
