package ru.yandex.hydra.profile.dao

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

/** @author @logab
  */
trait SpecBase extends DomainSpec with AnyWordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEach
