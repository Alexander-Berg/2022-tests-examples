package ru.yandex.vertis.moderation.scheduler.tasks.region

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.scheduler.task.region.RegionMismatchDecider
import ru.yandex.vertis.moderation.scheduler.task.region.RegionMismatchDecider.{Match, Mismatch, Source, Verdict}
import ru.yandex.vertis.moderation.scheduler.task.region.RegionMismatchDecider._

@RunWith(classOf[JUnitRunner])
class RegionMismatchDeciderSpec extends SpecBase {

  "Source.toTag" should {
    "be correct" in {
      Source(1, Some(2), Seq.empty).toTag should
        be("offer=1;ip=2")
      Source(1, Some(2), Seq(3, 4)).toTag should
        be("offer=1;ip=2;phones=3,4")
      Source(1, None, Seq.empty).toTag should
        be("offer=1")
    }
  }

  val Tests: Seq[(Source, Verdict)] =
    Seq(
      (Source(1, Some(2), Seq.empty), Match),
      (Source(1, None, Seq(2)), Match),
      (Source(1, None, Seq(2, 3)), Match),
      (Source(1, None, Seq.empty), Match),
      (Source(1, Some(1), Seq.empty), Match),
      (Source(1, None, Seq(1)), Match),
      (Source(1, None, Seq(1, 1)), Match),
      (Source(1, None, Seq(1, 1, 1)), Match),
      (Source(1, None, Seq(1, 2, 3)), Match),
      (Source(KrasnodarKrai, Some(KrasnodarKrai), Seq(KrasnodarKrai, 2, 3)), Match),
      (Source(KrasnodarKrai, Some(KrasnodarKrai), Seq(RepublicOfCrimea, 2, 3)), Match),
      (Source(KrasnodarKrai, Some(RepublicOfCrimea), Seq(RepublicOfCrimea, 2, 3)), Match),
      (Source(1, Some(2), Seq(3)), Mismatch("total_region_mismatch", Some(Double.MaxValue))),
      (Source(1, Some(2), Seq(3, 4)), Mismatch("total_region_mismatch", Some(Double.MaxValue))),
      (Source(1, Some(2), Seq(2, 4)), Mismatch("ip_and_phone_region_mismatch")),
      (Source(1, Some(2), Seq(2, 4, 1)), Mismatch("only_ip_region_mismatch")),
      (Source(1, Some(2), Seq(1, 4)), Mismatch("only_ip_region_mismatch")),
      (Source(1, Some(2), Seq(1)), Mismatch("only_ip_region_mismatch")),
      (Source(1, Some(1), Seq(3, 4, 2)), Mismatch("only_phone_region_mismatch", Some(10.0))),
      (Source(1, Some(1), Seq(3, 4)), Mismatch("only_phone_region_mismatch", Some(10.0))),
      (Source(1, Some(1), Seq(4)), Mismatch("only_phone_region_mismatch", Some(10.0))),
      (Source(1, Some(1), Seq(RepublicOfCrimea)), Mismatch("only_phone_region_mismatch", Some(10.0)))
    )

  // VSMODERATION-1912
  val AutoruSpecificTests: Seq[(Source, Verdict)] =
    Seq((Source(1, Some(RepublicOfAdygea), Seq(3)), Mismatch("total_region_mismatch")))

  "RegionMismatchDecider for Service.AUTORU" should {
    val decider = RegionMismatchDecider.forService(Service.AUTORU)
    (Tests ++ AutoruSpecificTests).foreach { case (source, verdict) =>
      s"decision on $source should be $verdict" in {
        decider(source) should be(verdict)
      }
    }
  }

  "RegionMismatchDecider for Service.REALTY" should {
    val decider = RegionMismatchDecider.forService(Service.REALTY)
    Tests.foreach { case (source, verdict) =>
      s"decision on $source should be $verdict" in {
        decider(source) should be(verdict)
      }
    }
  }
}
