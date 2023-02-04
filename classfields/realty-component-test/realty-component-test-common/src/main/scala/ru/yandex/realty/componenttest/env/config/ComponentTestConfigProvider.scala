package ru.yandex.realty.componenttest.env.config

trait ComponentTestConfigProvider[T] {

  def config: T

}
