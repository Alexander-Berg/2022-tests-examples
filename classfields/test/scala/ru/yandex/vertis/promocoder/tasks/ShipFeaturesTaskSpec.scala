package ru.yandex.vertis.promocoder.tasks

import org.joda.time.DateTime
import org.scalatest.GivenWhenThen
import ru.yandex.vertis.promocoder.{model, FeatureSpecBase}
import ru.yandex.vertis.promocoder.dao.{PromocodeDao, PromocodeInstanceDao}
import ru.yandex.vertis.promocoder.dao.impl.jvm.{JvmPromocodeDao, JvmPromocodeInstanceDao}
import ru.yandex.vertis.promocoder.model.gens.ModelGenerators
import ru.yandex.vertis.promocoder.model.{GeneratedBatch, Promocode, PromocodeId, User}
import ru.yandex.vertis.promocoder.service.FeaturesController.{InstanceSource, Source}
import ru.yandex.vertis.promocoder.service.impl.PromocodeInstanceServiceImpl
import ru.yandex.vertis.promocoder.service.{FeaturesController, PromocodeInstanceService, PromocodeService}
import ru.yandex.vertis.promocoder.tasks.ShipFeaturesTaskSpec.{ControllerImpl, Setup}
import ru.yandex.vertis.promocoder.util.RequestContext

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

/** Runnable specs on [[ShipFeaturesTask]]
  *
  * @author alex-kovalenko
  */
class ShipFeaturesTaskSpec extends FeatureSpecBase with GivenWhenThen with ModelGenerators {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  type Init = (User, PromocodeId)

  def initialize(instances: Iterable[Init]): Setup = {
    val promocodeDao = new JvmPromocodeDao
    val promocodeInstanceDao = new JvmPromocodeInstanceDao(promocodeDao)
    instances.foreach { case (user, code) =>
      val p = PromocodeGen.next.copy(code = code)
      promocodeDao
        .upsert(p)
        .flatMap(_ => promocodeInstanceDao.insert(PromocodeInstanceDao.Source(user, p)))
        .futureValue
    }

    val promocodeInstances =
      new PromocodeInstanceServiceImpl(promocodeInstanceDao, new NoOpPromocodeService)

    val controller = new ControllerImpl
    val task = new ShipFeaturesTask(promocodeInstances, controller)
    Setup(promocodeInstances, controller, task)
  }

  Feature("CompletePromocodeActivationTask") {
    Scenario("nothing to activate") {
      Given("state without not shipped instances")
      val Setup(_, controller, task) = initialize(Iterable.empty)

      When("task is executed")
      task.execute().futureValue

      Then("nothing should happen")
      controller.sources.size shouldBe 0
    }

    Scenario("has instances need to be Shipped") {
      Given("state with not shipped instances")
      val count = 3
      val users = listNUnique(count, UserGen)(identity).next
      val codes = listNUnique(count, PromocodeIdGen)(identity).next
      val Setup(_, controller, task) = initialize(users.zip(codes))

      When("task is executed")
      task.execute().futureValue

      Then("activation should become completed")
      val sources = controller.sources.collect { case InstanceSource(i) =>
        i
      }.toSet
      sources.size shouldBe count
      sources.map(_.user).toList shouldBe users
      sources.map(_.code).toList shouldBe codes
    }
  }
}

object ShipFeaturesTaskSpec {

  case class Setup(
      promocodeInstances: PromocodeInstanceService,
      featuresController: ControllerImpl,
      task: ShipFeaturesTask)

  class ControllerImpl extends FeaturesController {

    val sources: mutable.ArrayBuffer[Source] = mutable.ArrayBuffer()

    def ship(source: Source)(implicit rc: RequestContext): Future[Unit] = {
      sources += source
      Future.successful(())
    }
  }

}

class NoOpPromocodeService extends PromocodeService with ModelGenerators {

  override def get(
      filter: PromocodeService.Filter,
      options: PromocodeService.Options
    )(implicit rc: RequestContext): Future[Promocode] =
    Future.successful {
      PromocodeGen.next
    }

  override def get(filter: Seq[PromocodeDao.Filter])(implicit rc: RequestContext): Future[Iterable[Promocode]] =
    Future.successful {
      List(PromocodeGen.next)
    }

  override def create(promocode: Promocode)(implicit rc: RequestContext): Future[Unit] =
    Future.unit

  override def generate(promocodeSource: model.PromocodeSource)(implicit rc: RequestContext): Future[GeneratedBatch] =
    Future.successful {
      GeneratedBatch(PromocodeGen.next)
    }

  override def updateDeadline(
      code: PromocodeId,
      newDeadline: DateTime,
      currentDeadline: DateTime
    )(implicit rc: RequestContext): Future[Unit] =
    Future.unit
}
