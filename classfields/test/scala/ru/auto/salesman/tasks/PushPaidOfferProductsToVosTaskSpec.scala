package ru.auto.salesman.tasks

import org.scalacheck.Gen
import ru.auto.salesman.dao.user.VosProductSourceDao
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.service.VosPushApi
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.UserModelGenerators

import scala.util.Success

class PushPaidOfferProductsToVosTaskSpec extends BaseSpec with UserModelGenerators {
  private val vosPushApi = mock[VosPushApi]
  private val vosProductSource = mock[VosProductSourceDao]

  private val task =
    new PushPaidOfferProductsToVosTask(vosProductSource, vosPushApi)

  "PushPaidOfferProductsToVosTask" should {
    "push products" in {
      forAll(Gen.listOf(vosProductSourceGen)) { sources =>
        (vosProductSource.getWaitingForPush _)
          .expects()
          .returningZ(sources)

        sources.groupBy(p => (p.offer, p.user)).values.foreach { batch =>
          (vosPushApi.pushPaidOfferProducts _)
            .expects(batch)
            .returningZ(unit)

          (vosProductSource.markPushed _).expects(batch).returningZ(unit)
        }
        task.execute() shouldBe Success(unit)
      }
    }
  }

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}
