package ru.yandex.vertis.feedprocessor

import org.scalamock.scalatest.MockFactory
import org.scalatest.OneInstancePerTest

trait BaseSpec
  extends WordSpecBase
  with PropSpecBase
  with MockFactory
  with OneInstancePerTest
  with ScalamockCallHandlers
