package ru.yandex.vertis.moisha.impl.autoru.example.httpclient

import org.apache.http.util.EntityUtils
import ru.yandex.vertis.moisha.impl.autoru.gens.RequestGen
import ru.yandex.vertis.moisha.impl.autoru.view.{AutoRuRequestView, AutoRuResponseView}
import ru.yandex.vertis.moisha.model.gens.Producer
import spray.json.{JsonParser, ParserInput}

/**
  * Uses AutoRu [[ru.yandex.vertis.moisha.api.view.View]] implementations
  * from [[ru.yandex.vertis.moisha.impl.autoru.view]]
  * for marshalling and unmarshalling.
  *
  * @author Alexander Kovalenko (alex-kovalenko@yandex-team.ru)
  */
object MoishaModelExample extends App {

  val request = RequestGen.next

  val json = AutoRuRequestView.jsonFormat
    .write(AutoRuRequestView(request))
    .compactPrint

  val response = executeWithApache(json, r => {
    val responseBody = EntityUtils.toString(r.getEntity)
    AutoRuResponseView.jsonFormat
      .read(JsonParser(ParserInput(responseBody)))
      .asModel
  })
  println(s"request: $request\nresponse: $response")
}
