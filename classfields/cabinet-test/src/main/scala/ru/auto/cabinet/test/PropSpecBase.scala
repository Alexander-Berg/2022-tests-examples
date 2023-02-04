package ru.auto.cabinet.test

import org.scalacheck.ShrinkLowPriority
import org.scalatestplus.scalacheck.{ScalaCheckPropertyChecks => PropertyChecks}

trait PropSpecBase extends PropertyChecks with ShrinkLowPriority
