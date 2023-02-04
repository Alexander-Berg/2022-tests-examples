package auto.dealers.trade_in_notifier.storage.testkit

import auto.dealers.trade_in_notifier.storage.EmailNotificationRepository
import zio.test.mock._

@mockable[EmailNotificationRepository.Service]
object EmailNotificationRepositoryMock
