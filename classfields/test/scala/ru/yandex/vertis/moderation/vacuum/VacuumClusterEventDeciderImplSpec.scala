package ru.yandex.vertis.moderation.vacuum

import ru.yandex.vertis.moderation.SpecBase
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.model.ModerationRequest.AppendSignals
import ru.yandex.vertis.moderation.model.{DetailedReason, Domain, ModerationRequest}
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer._
import ru.yandex.vertis.moderation.model.vacuum.ClusterMarkup.Violation
import ru.yandex.vertis.moderation.model.vacuum._
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.service.PhoneIndexService

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@RunWith(classOf[JUnitRunner])
class VacuumClusterEventDeciderImplSpec extends SpecBase {
  "VacuumClusterEventDeciderImpl" should {
    "create ban request" in {
      val event =
        ClusterChangeEventGen.next.copy(
          dialog = DialogGen.next.copy(callPayload = Some(CallPayload("111", "realty"))),
          newCluster = ClusterGen.next.copy(markup = Some(ClusterMarkup(Some(Violation))))
        )

      val dao = mock[PhoneIndexService]
      val id = ExternalIdGen.next
      val id2 = ExternalIdGen.next
      doReturn(Future.successful(Seq(id, id2))).when(dao).getInstancesIds("111")

      val decider = new VacuumClusterEventDeciderImpl(dao, Domain.default(Service.REALTY))

      val res = decider.decide(event).futureValue
      res.size shouldBe 2

      val AppendSignals(_, Seq(source), _, _, _) = res.head
      val reasons = source.getDetailedReasons

      reasons.contains(DetailedReason.Violation) shouldBe true
    }
  }

}
