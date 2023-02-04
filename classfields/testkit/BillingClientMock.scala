package auto.common.clients.billing.testkit

import auto.common.clients.billing.BillingClient
import zio.test.mock.mockable

@mockable[BillingClient.Service]
object BillingClientMock {}
