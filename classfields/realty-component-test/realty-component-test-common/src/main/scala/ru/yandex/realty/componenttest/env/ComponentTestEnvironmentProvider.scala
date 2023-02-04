package ru.yandex.realty.componenttest.env

trait ComponentTestEnvironmentProvider[T <: ComponentTestEnvironment[_]] {

  def env: T

}
