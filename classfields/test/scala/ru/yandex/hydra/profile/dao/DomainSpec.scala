package ru.yandex.hydra.profile.dao

import java.util.Random

/** @author @logab
  */
trait DomainSpec {
  def project: String = "p" + math.abs(new Random().nextInt())

  def locale: String = "l" + math.abs(new Random().nextInt())

  def component: String = "c" + math.abs(new Random().nextInt())
}
