package ru.yandex.realty.searcher.response.builders.inexact.absent

import com.google.protobuf.util.JsonFormat
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.proto.search.inexact.{HasWashingMachineInexact, InexactMatching}
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.response.builders.inexact.{OfferBuilderContextFixture, ProtoHelper}
import ru.yandex.vertis.protobuf.ProtoInstanceProvider._

@RunWith(classOf[JUnitRunner])
class HasWashingMachineInexactEnricherSpec extends WordSpec with Matchers {

  val searchQuery = new SearchQuery()
  searchQuery.setHasWashingMachine(true)

  val hasWashingMachineEnricher = new HasWashingMachineInexactEnricher()

  val builder = InexactMatching.newBuilder()
  val parser = JsonFormat.parser()

  import ProtoHelper._

  "HasWashingMachineInexactEnricherTest" should {

    "checkAndEnrich" in new OfferBuilderContextFixture {
      val inexact =
        """{
          |  absent:{}
          |
          |  }""".stripMargin
          .toProto[HasWashingMachineInexact]

      hasWashingMachineEnricher.checkAndEnrich(builder, dummyOfferBuilderContext).getHasWashingMachine shouldBe inexact
    }
  }
}
