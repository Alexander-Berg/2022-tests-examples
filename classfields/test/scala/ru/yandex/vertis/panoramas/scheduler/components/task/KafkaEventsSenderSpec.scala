package ru.yandex.vertis.panoramas.scheduler.components.task

import play.api.libs.json.Json
import ru.yandex.vertis.panoramas.core.models.kafka.{KafkaEventBase, KafkaEventProto, KafkaJson}
import ru.yandex.vertis.panoramas.util.BaseSpec

class KafkaEventsSenderSpec extends BaseSpec with KafkaJson with KafkaEventProto {

  "check events serialize - deserialize" should {

    "just work for now" in {
      val s =
        "{\"panorama_id\":\"1425416555-1593810243958-wOd5e\",\"panorama_type\":\"ExteriorPanorama\",\"event_time\":\"2021-03-15T16:13:10.980914Z\",\"kafka_event_type\":\"PanoramaPoiChangedEvent\",\"payload\":{\"poi_count\":6}}"
      val evt: KafkaEventBase = Json.parse(s).validate[KafkaEventBase].get
      val proto = evt.asProto
      proto

    }
  }

}
