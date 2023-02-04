package ru.yandex.vertis.moderation.geobase.impl

import org.scalatest.Ignore
import ru.yandex.geobase6.Lookup
import ru.yandex.vertis.moderation.geobase.{GeobaseClient, GeobaseClientSpecBase}

import scala.concurrent.ExecutionContext.Implicits.global

@Ignore
class LibGeobaseClientSpec extends GeobaseClientSpecBase {

  private val lookup = new Lookup("/var/cache/geobase/geodata6.bin")
  override protected val geobaseClient: GeobaseClient = new LibGeobaseClient(lookup)
}
