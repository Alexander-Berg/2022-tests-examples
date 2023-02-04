package ru.yandex.vertis.parsing.auto.components.bunkerconfig

import ru.auto.api.ApiOfferModel.Category

trait TestBunkerConfigSupport extends BunkerConfigAware {

  val carsConfig = Seq(
    CallCenterConfig(
      "cc1",
      Seq("autoru-parsing@yandex-team.ru"),
      Seq(Percents("", 35, 0, 35)),
      0,
      7,
      0,
      8,
      0,
      19,
      30,
      0,
      500,
      30,
      Seq.empty,
      Category.CARS
    ),
    CallCenterConfig(
      "cc2",
      Seq("autoru-parsing@yandex-team.ru"),
      Seq(Percents("", 65, 100, 65)),
      0,
      7,
      0,
      8,
      0,
      19,
      30,
      0,
      500,
      30,
      Seq.empty,
      Category.CARS
    )
  )

  val trucksConfig = Seq(
    CallCenterConfig(
      "cc1",
      Seq("autoru-parsing@yandex-team.ru"),
      Seq(Percents("", 100, 100, 100)),
      0,
      7,
      0,
      8,
      0,
      19,
      30,
      0,
      500,
      30,
      Seq.empty,
      Category.TRUCKS
    )
  )

  val bunkerConfig = BunkerConfig(
    Set(),
    Set(),
    Set(),
    Set(),
    Set(),
    Set(),
    carsConfig,
    trucksConfig
  )
}
