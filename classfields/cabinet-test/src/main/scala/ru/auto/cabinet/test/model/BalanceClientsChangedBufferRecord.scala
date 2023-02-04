package ru.auto.cabinet.test.model

import ru.auto.cabinet.model.ClientId

final case class BalanceClientsChangedBufferRecord(
    clientId: ClientId,
    event: String
)
