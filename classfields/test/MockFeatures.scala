package ru.yandex.vertis.passport.test

import ru.yandex.vertis.feature.model.Feature

object MockFeatures {

  val featureOn: Feature[Boolean] = new Feature[Boolean] {
    override def name: String = "feature_on"
    override def value: Boolean = true
  }

  val featureOff: Feature[Boolean] = new Feature[Boolean] {
    override def name: String = "feature_off"
    override def value: Boolean = false
  }
}
