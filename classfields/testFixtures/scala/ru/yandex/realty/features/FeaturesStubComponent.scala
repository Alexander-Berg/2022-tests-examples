package ru.yandex.realty.features

trait FeaturesStubComponent extends FeaturesComponents {
  override lazy val features: Features = new SimpleFeatures
}
