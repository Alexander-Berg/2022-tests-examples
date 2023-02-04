package ru.yandex.vertis.feedprocessor

import org.scalacheck.ShrinkLowPriority
import org.scalatestplus.scalacheck.{Checkers, ScalaCheckPropertyChecks}

trait PropSpecBase extends Checkers with ShrinkLowPriority with ScalaCheckPropertyChecks
