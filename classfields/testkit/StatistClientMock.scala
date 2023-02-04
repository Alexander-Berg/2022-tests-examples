package common.clients.statist.testkit

import common.clients.statist.StatistClient
import zio.test.mock.mockable

@mockable[StatistClient.Service]
object StatistClientMock {}
