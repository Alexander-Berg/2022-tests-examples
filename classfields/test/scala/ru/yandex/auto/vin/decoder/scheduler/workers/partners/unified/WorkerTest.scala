package ru.yandex.auto.vin.decoder.scheduler.workers.partners.unified

import ru.yandex.auto.vin.decoder.partners.infiniti.Infiniti
import ru.yandex.auto.vin.decoder.partners.jlr.JLR
import ru.yandex.auto.vin.decoder.partners.nissan.Nissan
import ru.yandex.auto.vin.decoder.partners.suzuki.Suzuki

class NissanWorkerTest extends StandardPartnerWorkerTest(Nissan)
class JLRWorkerTest extends StandardPartnerWorkerTest(JLR)
class InfinitiWorkerTest extends StandardPartnerWorkerTest(Infiniti)
class SuzukiWorkerTest extends StandardPartnerWorkerTest(Suzuki)
