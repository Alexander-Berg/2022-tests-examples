package ru.yandex.vertis.promocoder.service

import org.joda.time.DateTime
import org.junit.runner.RunWith
import ru.yandex.vertis.promocoder.{model, WordSpecBase}
import ru.yandex.vertis.promocoder.dao.PromocodeDao
import ru.yandex.vertis.promocoder.model.PromocodeInstance.{ReferringStatuses, Statuses}
import ru.yandex.vertis.promocoder.model.gens.ModelGenerators
import ru.yandex.vertis.promocoder.model.{
  GeneratedBatch,
  Promocode,
  PromocodeId,
  PromocodeInstance,
  PromocodeInstanceId,
  User
}
import ru.yandex.vertis.promocoder.service.FeaturesController.{PromocodeSource, Source}
import ru.yandex.vertis.promocoder.service.FeaturesShippingPromocodeInstanceServiceSpec.{
  ControllerImpl,
  NoOpPromocodeInstanceService,
  NoOpPromocodeService
}
import ru.yandex.vertis.promocoder.service.PromocodeInstanceService.Filter
import ru.yandex.vertis.promocoder.util.{AutomatedContext, RequestContext}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future, Promise}

/** Runnable specs on [[FeaturesShippingPromocodeInstanceService]]
  *
  * @author alex-kovalenko
  */
class FeaturesShippingPromocodeInstanceServiceSpec extends WordSpecBase with ModelGenerators {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val rc: RequestContext = AutomatedContext("FeaturesCreatingPromocodeInstanceServiceSpec")

  def newService(p: Promise[Source]): FeaturesShippingPromocodeInstanceService =
    new FeaturesShippingPromocodeInstanceService(
      new NoOpPromocodeInstanceService,
      new NoOpPromocodeService,
      new ControllerImpl(p)
    )

  "FeaturesCreatingPromocodeInstanceService" should {
    "use FeaturesController to create features" in {
      val promise: Promise[Source] = Promise()
      val service = newService(promise)
      val user = "user"
      val promocode = PromocodeGen.next
      service.activate(user, promocode).futureValue

      Await.result(promise.future, 1.second) match {
        case PromocodeSource(`promocode`, _) =>
        case other => fail(s"Unexpected $other")
      }
    }
  }
}

object FeaturesShippingPromocodeInstanceServiceSpec extends ModelGenerators {

  class NoOpPromocodeInstanceService extends PromocodeInstanceService {

    def get(filter: Filter)(implicit rc: RequestContext): Future[Iterable[PromocodeInstance]] =
      Future.successful(Iterable.empty)

    override def count(code: PromocodeId)(implicit rc: RequestContext): Future[Int] =
      Future.successful(0)

    def activate(user: User, promocode: Promocode)(implicit rc: RequestContext): Future[PromocodeInstance] =
      Future.successful {
        PromocodeInstanceGen.next.copy(code = promocode.code, user = user)
      }

    override def activate(user: User, code: String)(implicit rc: RequestContext): Future[PromocodeInstance] =
      Future.successful {
        PromocodeInstanceGen.next.copy(code = code, user = user)
      }

    def shipFeatures(instance: PromocodeInstance)(implicit rc: RequestContext): Future[PromocodeInstance] =
      Future.successful(instance.copy(status = Statuses.Shipped))

    def shipCommission(instance: PromocodeInstance)(implicit rc: RequestContext): Future[PromocodeInstance] =
      Future.successful(instance.copy(referringStatus = instance.referringStatus.map(_ => ReferringStatuses.Shipped)))

    def delete(id: PromocodeInstanceId)(implicit rc: RequestContext): Future[Unit] =
      Future.successful(())

  }

  class NoOpPromocodeService extends PromocodeService {

    override def get(
        filter: PromocodeService.Filter,
        options: PromocodeService.Options
      )(implicit rc: RequestContext): Future[Promocode] =
      Future.successful {
        PromocodeGen.next.copy()
      }

    override def get(filter: Seq[PromocodeDao.Filter])(implicit rc: RequestContext): Future[Iterable[Promocode]] =
      Future.successful {
        List(PromocodeGen.next.copy())
      }

    override def create(promocode: Promocode)(implicit rc: RequestContext): Future[Unit] =
      Future.unit

    override def generate(promocodeSource: model.PromocodeSource)(implicit rc: RequestContext): Future[GeneratedBatch] =
      Future.successful {
        GeneratedBatch(PromocodeGen.next.copy())
      }

    override def updateDeadline(
        code: PromocodeId,
        newDeadline: DateTime,
        currentDeadline: DateTime
      )(implicit rc: RequestContext): Future[Unit] =
      Future.unit
  }

  class ControllerImpl(p: Promise[Source]) extends FeaturesController {

    def ship(source: Source)(implicit rc: RequestContext): Future[Unit] = {
      p.success(source)
      Future.successful(())
    }
  }

  case class Setup(service: FeaturesShippingPromocodeInstanceService, promocodeDao: PromocodeDao)
}
