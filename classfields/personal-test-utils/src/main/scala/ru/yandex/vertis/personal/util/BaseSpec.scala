package ru.yandex.vertis.personal.util

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{Matchers, OptionValues, TryValues, WordSpecLike}

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 29.06.16
  */
trait BaseSpec
  extends WordSpecLike
  with Matchers
  with TryValues
  with OptionValues
  with ScalaFutures
  with IntegrationPatience
