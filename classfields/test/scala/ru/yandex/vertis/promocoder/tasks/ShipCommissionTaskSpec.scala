package ru.yandex.vertis.promocoder.tasks

import org.mockito.Mockito
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike
import org.scalatest.{AsyncTestSuite, BeforeAndAfterEach, GivenWhenThen}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.promocoder.model.PromocodeInstance.ReferringStatuses
import ru.yandex.vertis.promocoder.model.gens.ModelGenerators
import ru.yandex.vertis.promocoder.model.{PromocodeId, PromocodeInstance}
import ru.yandex.vertis.promocoder.service.PromocodeInstanceService.Filter.ByReferringStatus
import ru.yandex.vertis.promocoder.service.PromocodeService.Filter.ByCode
import ru.yandex.vertis.promocoder.service.PromocodeService.Options
import ru.yandex.vertis.promocoder.service.{
  CommissionController,
  CommissionDecider,
  FeatureInstanceService,
  PromocodeInstanceService,
  PromocodeService
}
import ru.yandex.vertis.promocoder.tasks.ShipCommissionTaskSpec.MockSetup
import ru.yandex.vertis.promocoder.util.RequestContext

import scala.annotation.nowarn
import scala.concurrent.Future

/** Runnable specs on [[ShipCommissionTask]]
  *
  * @author alex-kovalenko
  */
class ShipCommissionTaskSpec
  extends Matchers
  with AsyncWordSpecLike
  with GivenWhenThen
  with MockSetup
  with ModelGenerators {

  "ShipCommissionTask" should {
    "not ship commission" when {
      "there is no waiting promocode instances" in {
        when(promocodeInstanceService.get(eq(ByReferringStatus(ReferringStatuses.Waiting)))(?))
          .thenReturn(Future.successful(Iterable.empty[PromocodeInstance]))

        task.execute().map(_ => succeed)
      }
    }

    "ship commission" when {
      "got waiting instances" in {
        val instance = PromocodeInstanceGen.next
          .copy(referringStatus = Some(ReferringStatuses.Waiting))
        when(promocodeInstanceService.get(eq(ByReferringStatus(ReferringStatuses.Waiting)))(?))
          .thenReturn(Future.successful(Iterable(instance)))
        when(controller.ship(eq(instance))(?))
          .thenReturn(Future.successful(()))

        for {
          _ <- task.execute()
          _ = Mockito.verify(controller).ship(eq(instance))(?)
        } yield succeed
      }
    }
  }
}

object ShipCommissionTaskSpec extends ModelGenerators {

  case class Setup(
      promocodes: PromocodeService,
      promocodeInstances: PromocodeInstanceService,
      featureInstances: FeatureInstanceService,
      commissionController: CommissionController,
      commissionDecider: CommissionDecider,
      task: ShipCommissionTask)

  trait MockSetup extends BeforeAndAfterEach with MockitoSupport {
    this: AsyncTestSuite =>

    val promocodeService = mock[PromocodeService]
    val promocodeInstanceService = mock[PromocodeInstanceService]
    val controller = mock[CommissionController]
    val task = new ShipCommissionTask(promocodeInstanceService, controller)

    override protected def beforeEach(): Unit = {
      Mockito.reset[Any](promocodeService, promocodeInstanceService, controller)
      stub(promocodeService.get(_: PromocodeService.Filter, _: Options)(_: RequestContext)) {
        case (ByCode(code), Options(false), _) =>
          Future.successful(PromocodeGen.next.copy(code = code, aliases = Set.empty[PromocodeId]))
      }
      super.beforeEach()
    }
  }
}
