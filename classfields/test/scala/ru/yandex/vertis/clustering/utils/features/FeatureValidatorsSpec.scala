package ru.yandex.vertis.clustering.utils.features

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop.TableDrivenPropertyChecks._
import ru.yandex.vertis.clustering.BaseSpec
import ru.yandex.vertis.clustering.utils.features.FeatureValidators._

/**
  * @author Anton Tsyganov (jenkl)
  */
@RunWith(classOf[JUnitRunner])
class FeatureValidatorsSpec extends BaseSpec {

  "FeatureValidators" should {
    "validateSuid" in {
      import FeatureValidatorsSpec.SuidTestCases
      forAll(SuidTestCases) { (suid: String, valid: Boolean) =>
        validateSuid(suid) should equal(valid)
      }
    }

    "validateGaid" in {
      import FeatureValidatorsSpec.GaidTestCases
      forAll(GaidTestCases) { (gaid: String, valid: Boolean) =>
        validateGaid(gaid) should equal(valid)
      }
    }

    "validateIdfa" in {
      import FeatureValidatorsSpec.IdfaTestCases
      forAll(IdfaTestCases) { (idfa: String, valid: Boolean) =>
        validateIdfa(idfa) should equal(valid)
      }
    }

    "validateYandexUid" in {
      import FeatureValidatorsSpec.YandexUidCases
      forAll(YandexUidCases) { (yandexUid: String, valid: Boolean) =>
        validateYandexUid(yandexUid) should equal(valid)
      }
    }

    "validateMetricaDeviceId" in {
      import FeatureValidatorsSpec.MetricaDeviceIdCases
      forAll(MetricaDeviceIdCases) { (metricaDeviceId: String, valid: Boolean) =>
        validateMetricaDeviceId(metricaDeviceId) should equal(valid)
      }
    }
  }
}

object FeatureValidatorsSpec {

  val SuidTestCases = Table(
    ("suid", "valid"),
    ("0b73bc3545431348d592ce77be1469e4.75c1506398b5b9f0246c8dfa127e054a", true),
    ("28ac42eb09cd729ccacc758eaabd7fa3.5525a1e8f97065a584b598c062773563", true),
    ("430e24fcd0bfb6f0518f32fe9cb2f9ac.40e59cacbb4b2d23bdaff6226f8e6afa", true),
    ("430e24fcd0bfb6f0518f32fe9cb2f9ac.75c1506398b5b9f0246c8dfa127e054a", false),
    ("", false),
    ("5525a1e8f97065a584b598c0627735635525a1e8f97065a584b598c062773563", false)
  )

  val GaidTestCases = Table(
    ("gaid", "valid"),
    ("86122f06-82e6-4a9b-aa01-f0d224920903", true),
    ("9ebd0874-e9d7-4832-b62a-423cbd31210e", true),
    ("a3c7414a-287e-4443-8592-73558527483a", true),
    ("f1d2aee4-fde7-4e18-a612-4eab70dc2fcf", true),
    ("f1d2aee4-0000-4e18-a612-4eab70dc2fcf", true),
    ("", false),
    ("a3c7414a--44438592-73558527483a", false),
    ("f1d2aee4-fde7-4e18-a612-4eab70dc2Qcf", false),
    ("f1d2aee4-fde7-4e18-a612-4ea2fcff1d2-0dc2fcf", false),
    ("f1d2aee4-fde7-4e18-a612-4eab70dc2fcfs", false)
  )

  val IdfaTestCases = Table(
    ("idfa", "valid"),
    ("0B78F73C-D9AD-4306-AC1B-5ACF34D371AE", true),
    ("91764198-2093-4B9B-A608-D4FC4FF20BBB", true),
    ("64B45334-D476-4D88-8CBA-7E618F284AF9", true),
    ("F1D2AEE4-FDE7-4E18-A612-4EAB70DC2FCF", true),
    ("F1D2AEE4-0000-4E18-A612-4EAB70DC2FCF", true),
    ("", false),
    ("F1D2AEE4-00-4E18A612-4EAB70DC2FCF", false),
    ("F1D2AEE4-fDE7-4E18-A612-4EAB70DC2FCF", false),
    ("F1D2AEE4-FDE7-4E18-A612-4EAB70DC2FCFS", false),
    ("F1D2AEE4-FDE7-4E18-A612-4EA2FCFF1D2-0DC2FCF", false)
  )

  val YandexUidCases = Table(
    ("yandexUid", "valid"),
    ("4839277111555107652", true),
    ("5010024021518279131", true),
    ("5598893381518257229", true),
    ("10113701529442803", true),
    ("0011188541530035229", true),
    ("", false),
    ("0", false),
    ("-1", false),
    ("50100240215182791315010024021518279131", false),
    ("10113701529442803hl", false)
  )

  val MetricaDeviceIdCases = Table(
    ("metricaDeviceId", "valid"),
    //iOS
    ("8650CD15-83AD-4D78-B6EE-DA0626E40AAE", true),
    ("1D113B22-7D09-4B26-9470-D86D948A5716", true),
    ("CF8BABF8-7F77-44FE-98E8-DBC409C6A491", true),
    //Android
    ("a2d732c9d7d91870a4f53d5026900796", true),
    ("4fd4443eda620c6467acd01907de1cb5", true),
    ("4c4768b79f4aa4e172aee41a5b840ae4", true),
    //others
    ("f1d2aee4-fde7-4e18-a612-4eab70dc2fcf", true),
    ("f1d2aee4-fde7-4e18-a612-4eab70dc2Fcf", true),
    ("f1d2aee4-0000-4e18-a612-4eab70dc2fcf", true),
    ("", false),
    ("f1d2aee4-fde7-4e18-a612-4eab70dc2fcfs", false),
    ("f1d2aee4-fde7-4e18-a612-4ea2fcff1d2-0dc2fcf", false),
    ("f1d2aee4-fde7-4e18-a612-4ed2-0d2fcf", false),
    ("8650CD15-83AD4D78B6EE-DA0626E40AAE", false),
    ("4c4768b79f4aa4e172aee41a5b840ae42b34hhj", false)
  )
}
