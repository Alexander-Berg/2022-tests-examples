package ru.yandex.vertis.billing

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
  * Base for all specs for avoid 'extends' same things in each spec.
  *
  * @author dimas
  */
trait SpecBase extends AnyWordSpec with Matchers
