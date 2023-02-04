package ru.yandex.realty.main

import ru.yandex.realty.geo.RegionGraphTestComponents

import scala.concurrent.ExecutionContext

trait ComponentsTest extends RegionGraphTestComponents {
  implicit val ec: ExecutionContext = ExecutionContext.global
}
