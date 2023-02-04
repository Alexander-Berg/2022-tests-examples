package ru.yandex.vertis.parsing.realty.components.bunkerconfig

import play.api.libs.json.{JsError, JsSuccess, Json}
import ru.yandex.vertis.parsing.components.extdata.regions.RegionsAware
import ru.yandex.vertis.parsing.realty.bunkerconfig.{BunkerConfig, InnerBunkerConfig}

trait TestBunkerConfigSupport extends BunkerConfigAware with RegionsAware {

  val bunkerConfig: BunkerConfig = {
    val str = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/bunker_config.json")).mkString
    val json = Json.parse(str)
    json.validate[InnerBunkerConfig] match {
      case JsSuccess(result, _) => BunkerConfig.fromInner(result, regionTree)
      case e: JsError =>
        throw new IllegalArgumentException(s"Got unexpected JSON ${JsError.toJson(e)}")
    }
  }
}
