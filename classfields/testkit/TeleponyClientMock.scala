package auto.dealers.dealer_pony.clients.testkit

import auto.dealers.dealer_pony.clients.telepony.TeleponyClient
import zio.test.mock.mockable

@mockable[TeleponyClient.Service]
object TeleponyClientMock {}
