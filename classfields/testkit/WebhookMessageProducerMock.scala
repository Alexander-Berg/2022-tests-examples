package amogus.logic

import amogus.logic.producer.WebhookMessageProducer
import zio.test.mock.mockable

@mockable[WebhookMessageProducer.Service]
object WebhookMessageProducerMock
