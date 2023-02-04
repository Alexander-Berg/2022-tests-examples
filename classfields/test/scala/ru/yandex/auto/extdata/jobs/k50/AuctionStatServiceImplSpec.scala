package ru.yandex.auto.extdata.jobs.k50

import org.scalatest.{FlatSpecLike, Matchers}
import org.springframework.jdbc.datasource.AbstractDataSource
import ru.yandex.auto.extdata.jobs.k50.model.AuctionStat
import ru.yandex.auto.extdata.jobs.k50.model.AuctionStat.{AuctionInfo, AuctionKey}
import ru.yandex.auto.extdata.jobs.k50.services.impl.AuctionStatServiceImpl
import ru.yandex.auto.extdata.service.util.MockitoSyntax._
import ru.yandex.vertis.mockito.MockitoSupport

import java.sql.{Connection, PreparedStatement, ResultSet}

class AuctionStatServiceImplSpec extends FlatSpecLike with MockitoSupport with Matchers {

  private val result = Seq(
    "user_id=dealer:10328&region_id=1&mark=LEXUS&model=LX" -> "50",
    "user_id=dealer:10328&region_id=1&mark=LEXUS&model=LX" -> "500",
    "user_id=dealer:10325&region_id=1&mark=LEXUS&model=LX" -> "6",
    "user_id=dealer:10323&region_id=1&mark=LEXUS&model=RX" -> "100",
    "user_id=dealer:10322&region_id=213&mark=LEXUS&model=R" -> "100",
    "user_id=dealer:10321&region_id=213&mark=LEXUS&model=R" -> "5"
  )

  private val iterator = result.iterator
  private var current: Option[(String, String)] = None

  private val resultSet = mock[ResultSet]
  when(resultSet.next()).answer { _ =>
    if (iterator.hasNext) {
      current = Some(iterator.next())
      true
    } else {
      false
    }
  }

  when(resultSet.getString("context")).answer { _ =>
    current.get._1
  }

  when(resultSet.getString("product")).answer { _ =>
    "call"
  }

  when(resultSet.getString("bid")).answer { _ =>
    current.get._2
  }

  private val preparedStatement = mock[PreparedStatement]
  when(preparedStatement.executeQuery()).answer { _ =>
    resultSet
  }

  private val mockConnection = mock[Connection]
  when(mockConnection.prepareStatement(?)).answer { _ =>
    preparedStatement
  }

  when(mockConnection.close()).answer { _ =>
    preparedStatement
  }

  private val mockDataSource = mock[AbstractDataSource]
  when(mockDataSource.getConnection()).answer { _ =>
    mockConnection
  }

  private val auctionStatService = new AuctionStatServiceImpl(mockDataSource, "mockedTable")

  "AuctionStatServiceImpl" should "return correct stat" in {
    auctionStatService.stat shouldBe AuctionStat(
      Map(
        AuctionKey(1, "LEXUS", "LX", true) -> AuctionInfo(6, 500, 2),
        AuctionKey(1, "LEXUS", "RX", true) -> AuctionInfo(100, 100, 1),
        AuctionKey(213, "LEXUS", "R", true) -> AuctionInfo(5, 100, 2),
        AuctionKey(213, "LEXUS", "R", false) -> AuctionInfo(0, 0, 0)
      )
    )
  }

}
