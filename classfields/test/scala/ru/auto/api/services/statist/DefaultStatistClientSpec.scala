package ru.auto.api.services.statist

import akka.http.scaladsl.model.HttpMethods.{GET, POST}
import akka.http.scaladsl.model.StatusCodes
import org.apache.http.client.utils.URIBuilder
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.managers.TestRequest
import ru.auto.api.model.gen.DateTimeGenerators
import ru.auto.api.model.gen.StatistModelGenerators._
import ru.auto.api.services.{HttpClientSpec, MockedHttpClient}
import java.time.format.DateTimeFormatter

import ru.auto.api.services.statist.Domain.AutoruPublicDomain
import ru.yandex.vertis.statist.model.api.ApiModel.FieldFilter.FieldValue
import ru.yandex.vertis.statist.model.api.ApiModel.{FieldFilter, FieldFilters}

import scala.jdk.CollectionConverters._

/**
  *
  * @author zvez
  */
class DefaultStatistClientSpec
  extends HttpClientSpec
  with MockedHttpClient
  with ScalaCheckPropertyChecks
  with TestRequest {

  val client = new DefaultStatistClient(http)

  val domain = AutoruPublicDomain
  val counter = "offer"
  val component = "card_show"

  "StatistClient.getCounterCompositeValues" should {
    "make request and return response" in {
      forAll(CounterCompositeValuesGen, DateTimeGenerators.dateTimeInPast) { (result, from) =>
        val ids = result.getValuesMap.keySet().asScala

        val uri = new URIBuilder(s"/api/1.x/${domain.name}/counters/$counter/components/$component/multiple/composite")
        ids.foreach(uri.addParameter("id", _))
        uri.addParameter("from", from.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))

        http.expectUrl(GET, uri.build().toString)
        http.respondWith(StatusCodes.OK, result)

        val callResult = client.getCounterCompositeValues(domain, counter, component, ids, from).futureValue
        callResult shouldBe result
      }
    }
  }

  "StatistClient.getCounterMultiComponentValues" should {
    "make request and return response" in {
      forAll(MultipleCompositeValuesGen, DateTimeGenerators.dateTimeInPast) { (result, from) =>
        val ids = result.getObjectsMap.keySet().asScala.toSet
        val components = result.getObjectsMap.values().asScala.head.getComponentsMap.keySet().asScala.toSet

        val uri = new URIBuilder(s"/api/1.x/${domain.name}/counters/$counter/components/multiple/composite")
        components.foreach(uri.addParameter("component", _))
        ids.foreach(uri.addParameter("id", _))
        uri.addParameter("from", from.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))

        http.expectUrl(GET, uri.build().toString)
        http.respondWith(StatusCodes.OK, result)

        val callResult = client.getCounterMultiComponentValues(domain, counter, components, ids, from).futureValue
        callResult shouldBe result
      }
    }
  }

  "StatistClient.getCounterMultiComponentValuesWithFilters" should {
    "make request and return response" in {
      forAll(MultipleCompositeValuesGen, localDateInPast(), Gen.option(localDateInPast())) { (result, from, until) =>
        val ids = result.getObjectsMap.keySet().asScala.toSet
        val components = result.getObjectsMap.values().asScala.head.getComponentsMap.keySet().asScala.toSet

        val uri = new URIBuilder(s"/api/1.x/autoru_public/counters/$counter/components/multiple/composite")
        components.foreach(uri.addParameter("component", _))
        ids.foreach(uri.addParameter("id", _))
        uri.addParameter("from", from.toString)
        until.foreach(v => uri.addParameter("until", v.toString))
        val filters = FieldFilters
          .newBuilder()
          .addFilters(
            FieldFilter
              .newBuilder()
              .setField("field")
              .setEquals(
                FieldFilter.Equals
                  .newBuilder()
                  .setValue(FieldValue.newBuilder().setStringValue("value"))
              )
          )
          .build()

        http.expectUrl(POST, uri.build().toString)
        http.expectProto(filters)
        http.respondWith(StatusCodes.OK, result)

        val callResult = client
          .getCounterMultiComponentValuesWithFilters(domain, counter, components, ids, filters, from, until)
          .futureValue
        callResult shouldBe result
      }
    }
  }

  "StatistClient.getCounterValuesByDay" should {
    "make request and return response" in {
      forAll(DailyCounterValuesGen, localDateInPast(), Gen.option(localDateInPast())) { (result, from, until) =>
        val ids = result.getDays(0).getValuesMap.keySet().asScala

        val uri = new URIBuilder(s"/api/1.x/${domain.name}/counters/$counter/components/$component/multiple/by-day")
        ids.foreach(uri.addParameter("id", _))
        uri.addParameter("from", from.toString)
        until.foreach(v => uri.addParameter("until", v.toString))

        http.expectUrl(GET, uri.build().toString)
        http.respondWith(StatusCodes.OK, result)

        val callResult = client.getCounterValuesByDay(domain, counter, component, ids, from, until).futureValue

        callResult shouldBe result
      }
    }
  }

  "StatistClient.getCounterMultiComponentValuesByDay" should {
    "make request and return response" in {
      forAll(MultipleDailyValuesGen, localDateInPast(), Gen.option(localDateInPast())) { (result, from, until) =>
        val ids = result.getObjectsMap.keySet().asScala.toSet
        val components =
          result.getObjectsMap.values().asScala.head.getDaysList.get(0).getComponentsMap.keySet().asScala.toSet

        val uri = new URIBuilder(s"/api/1.x/${domain.name}/counters/$counter/components/multiple/by-day")
        components.foreach(uri.addParameter("component", _))
        ids.foreach(uri.addParameter("id", _))
        uri.addParameter("from", from.toString)
        until.foreach(v => uri.addParameter("until", v.toString))

        http.expectUrl(GET, uri.build().toString)
        http.respondWith(StatusCodes.OK, result)

        val callResult =
          client.getCounterMultiComponentValuesByDay(domain, counter, components, ids, from, until).futureValue

        callResult shouldBe result
      }
    }
  }

  "StatistClient.getCounterMultiComponentValuesByDayWithFilters" should {
    "make request and return response" in {
      forAll(MultipleDailyValuesGen, localDateInPast(), Gen.option(localDateInPast())) { (result, from, until) =>
        val ids = result.getObjectsMap.keySet().asScala.toSet
        val components =
          result.getObjectsMap.values().asScala.head.getDaysList.get(0).getComponentsMap.keySet().asScala.toSet

        val uri = new URIBuilder(s"/api/1.x/${domain.name}/counters/$counter/components/multiple/by-day")
        components.foreach(uri.addParameter("component", _))
        ids.foreach(uri.addParameter("id", _))
        uri.addParameter("from", from.toString)
        until.foreach(v => uri.addParameter("until", v.toString))
        val filters = FieldFilters
          .newBuilder()
          .addFilters(
            FieldFilter
              .newBuilder()
              .setField("field")
              .setEquals(
                FieldFilter.Equals
                  .newBuilder()
                  .setValue(FieldValue.newBuilder().setStringValue("value"))
              )
          )
          .build()

        http.expectUrl(POST, uri.build().toString)
        http.expectProto(filters)
        http.respondWith(StatusCodes.OK, result)

        val callResult =
          client
            .getCounterMultiComponentValuesByDayWithFilters(domain, counter, components, ids, filters, from, until)
            .futureValue

        callResult shouldBe result
      }
    }
  }

  "StatistClient.getCounterValuesByDayWithFilters" should {
    "make request and return response" in {
      forAll(DailyCounterValuesGen, localDateInPast(), Gen.option(localDateInPast())) { (result, from, until) =>
        val ids = result.getDays(0).getValuesMap.keySet().asScala

        val uri = new URIBuilder(s"/api/1.x/${domain.name}/counters/$counter/components/$component/multiple/by-day")
        ids.foreach(uri.addParameter("id", _))
        uri.addParameter("from", from.toString)
        until.foreach(v => uri.addParameter("until", v.toString))
        val filters = FieldFilters
          .newBuilder()
          .addFilters(
            FieldFilter
              .newBuilder()
              .setField("field")
              .setEquals(
                FieldFilter.Equals
                  .newBuilder()
                  .setValue(FieldValue.newBuilder().setStringValue("value"))
              )
          )
          .build()

        http.expectUrl(POST, uri.build().toString)
        http.expectProto(filters)
        http.respondWith(StatusCodes.OK, result)

        val callResult =
          client.getCounterValuesByDayWithFilters(domain, counter, component, ids, filters, from, until).futureValue

        callResult shouldBe result
      }
    }
  }

}
