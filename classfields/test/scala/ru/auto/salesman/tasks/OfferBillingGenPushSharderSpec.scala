package ru.auto.salesman.tasks

import org.scalatest.Ignore
import ru.auto.salesman.push.DefaultPushPathResolver

@Ignore
class OfferBillingGenPushSharderSpec extends OfferBillingGenPushSpec {

  val pushPathResolver =
    DefaultPushPathResolver("http://shard-01-sas.test.vertis.yandex.net:34330")
}
