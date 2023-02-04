package ru.yandex.common.tokenization.equal

import ru.yandex.common.tokenization._

/**
  * @author evans
  */
class EqualDistributionBehaviourSpec
    extends DistributionBehaviourPropertySpec
        with DistributionBehaviourSpec {

  override def behaviourForOwnerConfig(ownerConfig: OwnerConfig): DistributionBehaviour =
    new EqualDistributionBehaviour(ownerConfig.owner)
}

