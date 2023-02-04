package common.autoru.clients.public_api.testkit

import common.autoru.clients.public_api.PublicApiClient
import zio.test.mock.mockable

@mockable[PublicApiClient.Service]
object PublicApiClientMock {}
