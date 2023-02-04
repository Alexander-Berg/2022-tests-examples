package ru.yandex.vertis.billing.events.model

import org.joda.time.DateTimeZone
import org.joda.time.format.ISODateTimeFormat
import org.scalatest.Ignore
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.xerial.snappy.SnappyOutputStream
import ru.yandex.vertis.billing.events.model.EventGenSpec.nextIndexingEvent
import ru.yandex.vertis.billing.events.model.gens.{event, EventMessageGen}
import ru.yandex.vertis.billing.model_core.Division.Components
import ru.yandex.vertis.billing.model_core.SupportedDivisions
import ru.yandex.vertis.billing.model_core.gens.Producer
import ru.yandex.vertis.billing.util.DateTimeUtils.now
import ru.yandex.vertis.billing.util.ProtoDelimitedUtil

import java.io.FileOutputStream
import java.util.Locale

/**
  * Specs on event genetration
  *
  * @author alesavin
  */
@Ignore
class EventGenSpec extends AnyWordSpec with Matchers {

  "EventMessage" should {
    "dump 1" in {
      val bytes = ProtoDelimitedUtil.write(EventMessageGen.next(1))
      val file = new FileOutputStream("hydra-m.bin")
      file.write(bytes)
      file.close()
    }
    "dump 100" in {
      val bytes = ProtoDelimitedUtil.write(EventMessageGen.next(100))
      val file = new FileOutputStream("hydra-ms.bin")
      file.write(bytes)
      file.close()
    }
    "dump 1000 snappy compressed" in {
      val file = new FileOutputStream("hydra-ms-1000-snappy.bin")
      val snappy = new SnappyOutputStream(file)
      ProtoDelimitedUtil.write(EventMessageGen.next(1000), snappy)
      snappy.close()
    }
    "dump 10000 snappy uncompressed" in {
      val file = new FileOutputStream("hydra-ms-10000.bin")
      ProtoDelimitedUtil.write(EventMessageGen.next(10000), file)
      file.close()
    }
    "dump 10000 snappy compressed" in {
      val file = new FileOutputStream("hydra-ms-10000-snappy.bin")
      val snappy = new SnappyOutputStream(file)
      ProtoDelimitedUtil.write(EventMessageGen.next(10000), snappy)
      snappy.close()
    }
    "dump indexing events" in {
      val events = Iterator.continually(nextIndexingEvent("2")).take(2).to(Iterable)
      val bytes = ProtoDelimitedUtil.write(events)
      val file = new FileOutputStream("hydra-indexing-ms.bin")
      file.write(bytes)
      file.close()
    }
  }
}

object EventGenSpec {

  val UTCFormatter =
    ISODateTimeFormat.dateTime().withLocale(Locale.ENGLISH).withZone(DateTimeZone.UTC)

  def nextIndexingEvent(oid: String) = {
    val division = SupportedDivisions.RealtyCommercialRuIndexing
    val map = Map(
      "billing.header.id" -> "2f743be6-6f7e-4c8e-85d7-2113d7f3d3be",
      "billing.header.name" -> "Test",
      "billing.header.order.approximate_amount" -> "9900000",
      "billing.header.order.commit_amount" -> "9900000",
      "billing.header.order.id" -> "1006",
      "billing.header.order.memo" -> "-",
      "billing.header.order.owner.client_id" -> "9706698",
      "billing.header.order.owner.version" -> "1",
      "billing.header.order.product_key" -> "default",
      "billing.header.order.text" -> "Test",
      "billing.header.order.total_income" -> "9900000",
      "billing.header.order.total_spent" -> "0",
      "billing.header.order.version" -> "1",
      "billing.header.owner.id.client_id" -> "9706698",
      "billing.header.owner.id.version" -> "1",
      "billing.header.owner.resource_ref@0.developer_id" -> "187858",
      "billing.header.owner.resource_ref@0.version" -> "1",
      "billing.header.owner.version" -> "1",
      "billing.header.product.goods@0.placement.cost.per_indexing.units" -> "100",
      "billing.header.product.goods@0.placement.cost.version" -> "1",
      "billing.header.product.goods@0.version" -> "1",
      "billing.header.product.version" -> "1",
      "billing.header.settings.is_enabled" -> "true",
      "billing.header.settings.version" -> "1",
      "billing.header.version" -> "1",
      "billing.active.deadline" -> UTCFormatter.print(now()),
      "billing.instance.id" -> "f6dbc2f6fcb91ddd308cf491115571dc",
      "billing.offer.id" -> oid,
      "offer_campaign_id" -> "2f743be6-6f7e-4c8e-85d7-2113d7f3d3be",
      "offer_id" -> oid,
      "offer_partner_id" -> "222",
      "offer_url" -> s"//$oid.ru"
    )
    event(
      division.project,
      division.locale,
      division.component,
      now().getMillis,
      map
    )
  }

}
