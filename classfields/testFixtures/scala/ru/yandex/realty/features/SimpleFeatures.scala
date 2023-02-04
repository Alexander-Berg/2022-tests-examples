package ru.yandex.realty.features

import java.util.concurrent.ConcurrentHashMap

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 08.09.16
  */
class SimpleFeatures extends Features {

  private val states = new ConcurrentHashMap[String, State]()

  override protected def getState(feature: Feature): State = {
    states.getOrDefault(
      feature.name,
      State(feature.enabledByDefault, System.currentTimeMillis(), DefaultGeneration)
    )
  }

  override protected def setState(feature: Feature, state: State): Unit = {
    states.put(feature.name, state)
  }
}
