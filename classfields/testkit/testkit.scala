package ru.auto.catalog.core

import ru.auto.catalog.core.model.raw.layers.ResultBuilder
import ru.auto.catalog.core.model.raw.moto.{MotoCatalogWrapper, MotoCatalogWrapperBuilder}
import ru.auto.catalog.core.model.raw.trucks.{TrucksCatalogWrapper, TrucksCatalogWrapperBuilder}
import ru.auto.catalog.core.model.raw.cars.{CarsCatalogWrapper, CarsCatalogWrapperBuilder}
import ru.auto.catalog.core.model.verba.{VerbaCars, VerbaMoto, VerbaTrucks}
import ru.auto.catalog.core.util.ApiExceptions.FilterException
import ru.auto.catalog.model.api.ApiModel.RawCatalog

package object testkit {
  val DummyBuilder: ResultBuilder = identity
  val EmptyCatalog: Either[FilterException, RawCatalog] = Right(RawCatalog.getDefaultInstance)

  val verbaTrucks: VerbaTrucks = VerbaTrucks.from(TestDataEngine)
  val verbaMoto: VerbaMoto = VerbaMoto.from(TestDataEngine)
  val verbaCars: VerbaCars = VerbaCars.from(TestDataEngine)

  val TestCardCatalogWrapper: CarsCatalogWrapper =
    new CarsCatalogWrapperBuilder(EmptyCarsSearchTagsInheritanceDecider).from(TestDataEngine)

  val TestTruckCardCatalogWrapper: TrucksCatalogWrapper =
    new TrucksCatalogWrapperBuilder(verbaTrucks).from(TestDataEngine)

  val TestMotoCardCatalogWrapper: MotoCatalogWrapper =
    new MotoCatalogWrapperBuilder(verbaMoto).from(TestDataEngine)
}
