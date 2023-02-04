package ru.yandex.vertis.general.search.model.testkit

import general.search.api.SearchOffersRequest
import general.search.model.SearchArea.{Coordinates, Toponyms}
import general.search.model.{
  Between,
  Equal,
  GreaterThan,
  In,
  LessThan,
  RawValue,
  SearchArea,
  SearchFilter,
  SearchSortEnum
}
import zio.random.Random
import zio.test.{Gen, Sized}

object SearchOffersRequestGen {

  val anyText: Gen[Random with Sized, String] = {
    Gen.alphaNumericStringBounded(10, 15).noShrink
  }

  val anyCategory: Gen[Random with Sized, String] = {
    Gen.alphaNumericStringBounded(10, 15).noShrink
  }

  val anySort: Gen[Random, SearchSortEnum.SearchSort] =
    Gen.elements(
      SearchSortEnum.SearchSort.BY_PRICE_ASC,
      SearchSortEnum.SearchSort.BY_PRICE_DESC,
      SearchSortEnum.SearchSort.BY_PUBLISH_DATE_DESC,
      SearchSortEnum.SearchSort.BY_RELEVANCE
    )

  def anyToponymsSearchArea(
      region: Gen[Random, Long] = Gen.anyLong,
      metro: Gen[Random, Seq[Long]] = Gen.listOfN(10)(Gen.anyLong),
      district: Gen[Random, Seq[Long]] = Gen.listOfN(10)(Gen.anyLong)): Gen[Random, SearchArea] =
    for {
      region <- region
      metro <- metro
      district <- district
    } yield SearchArea(SearchArea.Area.Toponyms(Toponyms(region = region, metro = metro, district = district)))

  val anyToponymsSearchArea: Gen[Random, SearchArea] = anyToponymsSearchArea()

  def anyCoordinatesArea(
      latitude: Gen[Random, Double] = Gen.anyDouble,
      longitude: Gen[Random, Double] = Gen.anyDouble,
      radius: Gen[Random, Int] = Gen.anyInt): Gen[Random, SearchArea] =
    for {
      latitude <- latitude
      longitude <- longitude
      radius <- radius
    } yield SearchArea(
      SearchArea.Area.Coordinates(Coordinates(latitude = latitude, longitude = longitude, radius = radius))
    )

  val anyCoordinatesArea: Gen[Random, SearchArea] = anyCoordinatesArea()

  def anySearchArea(
      coordinatesArea: Gen[Random, SearchArea] = anyCoordinatesArea,
      toponymsSearchArea: Gen[Random, SearchArea] = anyToponymsSearchArea): Gen[Random, SearchArea] =
    Gen.oneOf(coordinatesArea, toponymsSearchArea)

  val anySearchArea: Gen[Random, SearchArea] = anySearchArea()

  val doubleRawValue: Gen[Random, RawValue] =
    for {
      double <- Gen.anyDouble
    } yield RawValue(RawValue.Value.Double(double))

  val booleanRawValue: Gen[Random, RawValue] =
    for {
      boolean <- Gen.boolean
    } yield RawValue(RawValue.Value.Boolean(boolean))

  val stringRawValue: Gen[Random, RawValue] =
    for {
      string <- Gen.stringN(12)(Gen.alphaNumericChar)
    } yield RawValue(RawValue.Value.String(string))

  def anyRawValue(
      doubleValue: Gen[Random, RawValue] = doubleRawValue,
      booleanValue: Gen[Random, RawValue] = booleanRawValue,
      stringValue: Gen[Random, RawValue] = stringRawValue): Gen[Random, RawValue] =
    Gen.oneOf(doubleValue, booleanValue, stringValue)

  val anyRawValue: Gen[Random, RawValue] = anyRawValue()

  val anyEqual: Gen[Random, SearchFilter.Operation] =
    for {
      value <- anyRawValue
    } yield SearchFilter.Operation.Equal(Equal(Some(value)))

  val anyIn: Gen[Random, SearchFilter.Operation] =
    for {
      value <- Gen.listOfN(10)(anyRawValue)
    } yield SearchFilter.Operation.In(In(value))

  val anyLessThan: Gen[Random, SearchFilter.Operation] =
    for {
      value <- anyRawValue
      orEqual <- Gen.boolean
    } yield SearchFilter.Operation.LessThan(LessThan(Some(value), orEqual))

  val anyGreaterThan: Gen[Random, SearchFilter.Operation] =
    for {
      value <- anyRawValue
      orEqual <- Gen.boolean
    } yield SearchFilter.Operation.GreaterThan(GreaterThan(Some(value), orEqual))

  val anyBetween: Gen[Random, SearchFilter.Operation] =
    for {
      fromValue <- anyRawValue
      orEqualFrom <- Gen.boolean
      toValue <- anyRawValue
      orEqualTo <- Gen.boolean
    } yield SearchFilter.Operation.Between(
      Between(from = Some(fromValue), orEqualsFrom = orEqualFrom, to = Some(toValue), orEqualsTo = orEqualTo)
    )

  def anyOperation(
      equal: Gen[Random, SearchFilter.Operation] = anyEqual,
      in: Gen[Random, SearchFilter.Operation] = anyIn,
      lessThan: Gen[Random, SearchFilter.Operation] = anyLessThan,
      greaterThan: Gen[Random, SearchFilter.Operation] = anyGreaterThan,
      between: Gen[Random, SearchFilter.Operation] = anyBetween): Gen[Random, SearchFilter.Operation] =
    Gen.oneOf(equal, in, lessThan, greaterThan, between)

  val anyOperation: Gen[Random, SearchFilter.Operation] = anyOperation()

  def anySearchFilter(
      key: Gen[Random, String] = Gen.stringN(10)(Gen.alphaNumericChar),
      operation: Gen[Random, SearchFilter.Operation] = anyOperation): Gen[Random, SearchFilter] =
    for {
      key <- key
      operation <- operation
    } yield SearchFilter(key, operation)

  val anySearchFilter: Gen[Random, SearchFilter] = anySearchFilter()

  def anySearchFilters(count: Int): Gen[Random, List[SearchFilter]] = Gen.listOfN(count)(anySearchFilter)

  def anySearchOffersRequest(
      text: Gen[Random with Sized, String] = anyText,
      filter: Gen[Random, List[SearchFilter]] = anySearchFilters(10),
      category: Gen[Random with Sized, String] = anyCategory,
      area: Gen[Random, SearchArea] = anySearchArea,
      sort: Gen[Random, SearchSortEnum.SearchSort] = anySort): Gen[Random with Sized, SearchOffersRequest] =
    for {
      text <- text
      filter <- filter
      category <- category
      area <- area
      sort <- sort
    } yield SearchOffersRequest(
      text = text,
      parameter = filter,
      categoryIds = Seq(category),
      area = Some(area),
      sort = sort
    )

  val anySearchOffersRequest: Gen[Random with Sized, SearchOffersRequest] = anySearchOffersRequest().noShrink

  def anySearchOffersRequests(count: Int): Gen[Random with Sized, List[SearchOffersRequest]] =
    Gen.listOfN(count)(anySearchOffersRequest).noShrink

}
