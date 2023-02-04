package ru.yandex.vertis.billing.yandexkassa.api.gens

import ru.yandex.vertis.billing.yandexkassa.api.model.Recipient

case class NotificationGenParams(recipient: Option[Recipient] = None)
