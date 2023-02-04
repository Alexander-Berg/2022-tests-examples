package ru.auto.salesman.mocks

import cats.data.NonEmptyList
import ru.auto.salesman.dao.MatchApplicationDao
import ru.auto.salesman.dao.MatchApplicationDao.{Filter, Update}
import ru.auto.salesman.dao.impl.jdbc.StaticQueryBuilderHelper.LimitOffset
import ru.auto.salesman.model.match_applications.MatchApplicationCreateRequest

import scala.util.Try

class MatchApplicationDaoMock extends BaseMock[MatchApplicationDao] {
  override protected val mocked: MatchApplicationDao = mock[MatchApplicationDao]

  val createIfNotExistsMethod = toMockFunction1 {
    mocked.createIfNotExists(_: MatchApplicationCreateRequest)
  }

  val findMethod = toMockFunction2 {
    mocked.find(_: NonEmptyList[Filter], _: Option[LimitOffset])
  }

  val totalCostMethod = toMockFunction1 {
    mocked.totalCost(_: NonEmptyList[Filter])
  }

  val countMethod = toMockFunction1 {
    mocked.count(_: NonEmptyList[Filter])
  }

  val updateMethod = toMockFunction2 {
    mocked.update(_: NonEmptyList[Filter], _: NonEmptyList[Update])
  }

  val updateIfPreconditionMethod = toMockFunction3 {
    mocked.updateIfPrecondition(
      _: NonEmptyList[Filter],
      _: NonEmptyList[Update],
      _: Seq[MatchApplicationCreateRequest] => Try[Unit]
    )
  }
}
