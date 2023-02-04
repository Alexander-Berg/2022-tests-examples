package ru.yandex.vertis.punisher.tasks.impl

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.Domain
import ru.yandex.vertis.feature.impl.InMemoryFeatureRegistry
import ru.yandex.vertis.punisher.AutoruStagesBuilder
import ru.yandex.vertis.punisher.feature.UserPunisherFeatureTypes
import ru.yandex.vertis.punisher.model.TaskDomain.Labels
import ru.yandex.vertis.punisher.model.{AutoruUser, TaskDomainImpl}
import ru.yandex.vertis.punisher.stages.FindAndPunish
import ru.yandex.vertis.punisher.tasks.settings.TaskSettings
import ru.yandex.vertis.punisher.tasks.{FindAndPunishTask, FindAndPunishTaskSpec}
import ru.yandex.vertis.quality.feature_registry_utils.FeatureRegistryF

import scala.concurrent.duration._

/**
  * Runnable spec for [[FindAndPunishTaskSpec]]
  *
  * @author devreggs
  */
@RunWith(classOf[JUnitRunner])
class AutoruFindAndPunishImplTaskSpec extends FindAndPunishTaskSpec {

  private val taskSettings: TaskSettings = TaskSettings(1.hours, 1.hours, 25.hours)

  val findAndPunish: FindAndPunish[F, AutoruUser] =
    new FindAndPunish[F, AutoruUser](
      AutoruStagesBuilder.clusterizer,
      AutoruStagesBuilder.baseEnricher,
      AutoruStagesBuilder.offersPunishPolicy,
      AutoruStagesBuilder.punisher
    )

  val inMemoryFeatureRegistry = new FeatureRegistryF[F](new InMemoryFeatureRegistry(UserPunisherFeatureTypes))

  override val findAndPunishTask =
    new FindAndPunishTask[F, AutoruUser](
      TaskDomainImpl(Domain.DOMAIN_AUTO, Labels.Offers),
      AutoruStagesBuilder.finder,
      taskSettings,
      findAndPunish,
      inMemoryFeatureRegistry
    )
}
