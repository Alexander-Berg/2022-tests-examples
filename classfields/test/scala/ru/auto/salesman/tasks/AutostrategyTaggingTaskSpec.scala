package ru.auto.salesman.tasks

import org.scalamock.function.MockFunction3
import org.scalatest.Inspectors
import ru.auto.salesman.client.VosClient
import ru.auto.salesman.dao.AutostrategiesDao
import ru.auto.salesman.model.autostrategies.StoredAutostrategy
import ru.auto.salesman.model.offer.OfferIdentity
import ru.auto.salesman.model.{Epoch, OfferTag}
import ru.auto.salesman.service.EpochService
import ru.auto.salesman.tasks.AutostrategyTaggingTask.{getTag, TaskName}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens._
import ru.auto.salesman.util.RequestContext

import scala.language.existentials
import scala.util.{Random, Try}

class AutostrategyTaggingTaskSpec extends BaseSpec {

  import AutostrategyTaggingTaskSpec._

  private val autostrategiesDao = mock[AutostrategiesDao]

  private val vosClient = mock[VosClient]

  private val epochService = mock[EpochService]

  private val task =
    new AutostrategyTaggingTask(autostrategiesDao, vosClient, epochService)

  "Autostrategy tagging task" should {

    "put tags for active autostrategies" in {
      forAll(ActiveStoredAutostrategyListGen, OptEpochGen) { (autostrategies, since) =>
        expectGetEpochAndAutostrategies(autostrategies, since)
        Inspectors.forEvery(autostrategies) { autostrategy =>
          expectPutTag(autostrategy)
        }
        expectSetEpoch(autostrategies)
        taskShouldSucceed()
      }
    }

    "put tags for not active autostrategies" in {
      forAll(NotActiveStoredAutostrategyListGen, OptEpochGen) { (autostrategies, since) =>
        expectGetEpochAndAutostrategies(autostrategies, since)
        Inspectors.forEvery(autostrategies) { autostrategy =>
          expectDeleteTag(autostrategy)
        }
        expectSetEpoch(autostrategies)
        taskShouldSucceed()
      }
    }

    "put tag if both active and inactive autostrategy exist" in {
      forAll(
        ActiveStoredAutostrategyGen,
        NotActiveStoredAutostrategyListGen,
        OptEpochGen
      ) { (active, notActiveList, since) =>
        val sameOfferNotActive = notActiveList.map(_.withSameIdAs(active))
        val autostrategies = Random.shuffle(active :: sameOfferNotActive)
        expectGetEpochAndAutostrategies(autostrategies, since)
        expectPutTag(active)
        expectSetEpoch(autostrategies)
        taskShouldSucceed()
      }
    }

    "delete tag if just inactive autostrategies exist" in {
      forAll(NotActiveStoredAutostrategyListGen, OptEpochGen) { (autostrategies, since) =>
        val base = autostrategies.head
        val withSameId = autostrategies.map(_.withSameIdAs(base))
        expectGetEpochAndAutostrategies(withSameId, since)
        expectDeleteTag(base)
        expectSetEpoch(autostrategies)
        taskShouldSucceed()
      }
    }

    "do nothing if no autostrategies found" in {
      forAll(OptEpochGen) { since =>
        expectGetEpochAndAutostrategies(Nil, since)
        taskShouldSucceed()
      }
    }

    "fail on get epoch failure" in {
      expectGetEpochFailure()
      taskShouldFail()
    }

    "fail on get autostrategies failure" in {
      forAll(OptEpochGen) { since =>
        expectGetEpoch(since)
        expectGetAutostrategiesFailure(since)
        taskShouldFail()
      }
    }

    "fail on put tag failure" in {
      forAll(ActiveStoredAutostrategyGen, OptEpochGen) { (autostrategy, since) =>
        expectGetEpochAndAutostrategies(List(autostrategy), since)
        expectPutTagFailure(autostrategy)
        taskShouldFail()
      }
    }

    "fail on delete tag failure" in {
      forAll(NotActiveStoredAutostrategyGen, OptEpochGen) { (autostrategy, since) =>
        expectGetEpochAndAutostrategies(List(autostrategy), since)
        expectDeleteTagFailure(autostrategy)
        taskShouldFail()
      }
    }

    "fail on set epoch failure" in {
      forAll(ActiveStoredAutostrategyGen, OptEpochGen) { (autostrategy, since) =>
        expectGetEpochAndAutostrategies(List(autostrategy), since)
        expectPutTag(autostrategy)
        expectSetEpochFailure(List(autostrategy))
        taskShouldFail()
      }
    }
  }

  private def expectGetEpochAndAutostrategies(
      autostrategies: List[StoredAutostrategy],
      since: Option[Epoch]
  ) = {
    expectGetEpoch(since)
    expectGetAutostrategies(autostrategies, since)
  }

  private def expectGetEpoch(since: Option[Epoch]) =
    (epochService.getOptional _).expects(TaskName).returningT(since)

  private def expectGetEpochFailure() =
    (epochService.getOptional _)
      .expects(TaskName)
      .throwingT(new RuntimeException)

  private def expectGetAutostrategies(
      autostrategies: List[StoredAutostrategy],
      since: Option[Epoch]
  ) =
    (autostrategiesDao.sinceOrAll _).expects(since).returningZ(autostrategies)

  private def expectGetAutostrategiesFailure(since: Option[Epoch]) =
    (autostrategiesDao.sinceOrAll _)
      .expects(since)
      .throwingZ(new RuntimeException)

  private def expectPutTag(autostrategy: StoredAutostrategy) =
    (vosClient
      .putTag(_: OfferIdentity, _: OfferTag))
      .expects(
        autostrategy.props.offerId,
        getTag(autostrategy.props.id.autostrategyType)
      )
      .returningZ(())

  private def expectDeleteTag(autostrategy: StoredAutostrategy) =
    expectTagAction(
      vosClient.deleteTag(_: OfferIdentity, _: OfferTag)(_: RequestContext),
      autostrategy
    )

  private def expectTagAction(
      f: MockFunction3[OfferIdentity, OfferTag, RequestContext, Try[Unit]],
      autostrategy: StoredAutostrategy
  ) =
    f.expects(
      autostrategy.props.offerId,
      getTag(autostrategy.props.id.autostrategyType),
      *
    ).returningT(())

  private def expectPutTagFailure(autostrategy: StoredAutostrategy) =
    (vosClient
      .putTag(_: OfferIdentity, _: OfferTag))
      .expects(
        autostrategy.props.offerId,
        getTag(autostrategy.props.id.autostrategyType)
      )
      .throwingZ(new RuntimeException)

  private def expectDeleteTagFailure(autostrategy: StoredAutostrategy) =
    expectTagActionFailure(
      vosClient.deleteTag(_: OfferIdentity, _: OfferTag)(_: RequestContext),
      autostrategy
    )

  private def expectTagActionFailure(
      f: MockFunction3[OfferIdentity, OfferTag, RequestContext, Try[Unit]],
      autostrategy: StoredAutostrategy
  ) =
    f.expects(
      autostrategy.props.offerId,
      getTag(autostrategy.props.id.autostrategyType),
      *
    ).throwingT(new RuntimeException)

  private def expectSetEpoch(autostrategies: List[StoredAutostrategy]) =
    (epochService.set _)
      .expects(TaskName, autostrategies.maxBy(_.epoch).epoch)
      .returningT(())

  private def expectSetEpochFailure(autostrategies: List[StoredAutostrategy]) =
    (epochService.set _)
      .expects(TaskName, autostrategies.maxBy(_.epoch).epoch)
      .throwingT(new RuntimeException)

  private def taskShouldSucceed() =
    task.execute().success.value shouldBe (())

  private def taskShouldFail() =
    task.execute().failure.exception shouldBe a[RuntimeException]
}

object AutostrategyTaggingTaskSpec {

  implicit private class RichStoredAutostrategy(
      val autostrategy: StoredAutostrategy
  ) extends AnyVal {

    def withSameIdAs(other: StoredAutostrategy): StoredAutostrategy =
      autostrategy.copy(
        props = autostrategy.props.copy(
          offerId = other.props.offerId,
          // so that both of autostrategies would have the same type
          payload = other.props.payload
        )
      )
  }
}
