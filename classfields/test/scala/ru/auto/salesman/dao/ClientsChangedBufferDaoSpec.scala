package ru.auto.salesman.dao

import ru.auto.salesman.dao.ClientsChangedBufferDao.DataSourceFilter
import ru.auto.salesman.model.ClientChangedBufferRecord
import ru.auto.salesman.test.BaseSpec

trait ClientsChangedBufferDaoSpec extends BaseSpec {

  val clientId = 1L

  val initialItems = List(
    record(1, 1, "data_source_1"),
    record(2, 1, "data_source_2"),
    record(3, 1, "data_source_3"),
    record(4, 2, "data_source_4")
  )

  val allFilter1 = DataSourceFilter(
    Set("data_source_1", "data_source_2", "data_source_3", "data_source_4")
  )

  val allFilter2 = DataSourceFilter(
    Set(
      "data_source_1",
      "data_source_2",
      "data_source_3",
      "data_source_4",
      "data_source_5",
      "data_source_6",
      "data_source_7"
    )
  )

  def clientsChangedBufferDao: ClientsChangedBufferDao

  private def record(id: Long, client: Long, ds: String) =
    ClientChangedBufferRecord(id, client, ds)

  private def inputRecord(client: Long, ds: String) =
    ClientsChangedBufferDao.InputRecord(client, ds)

  "ClientsBufferChangedDao" should {
    "list items by clientId and delete item by id" in {
      val (initialList, listAfterDelete) = {
        for {
          initialList <- clientsChangedBufferDao.get(allFilter1)
          _ <- clientsChangedBufferDao.delete(2)
          listAfterDelete <- clientsChangedBufferDao.get(allFilter1)
        } yield (initialList, listAfterDelete)
      }.success.value

      initialList should contain theSameElementsAs initialItems
      listAfterDelete should contain theSameElementsAs initialItems.filterNot(
        _.id == 2
      )
    }

    "insert items" in {
      val toInsert = List(
        inputRecord(4, "data_source_5"),
        inputRecord(4, "data_source_6"),
        inputRecord(6, "data_source_6")
      )
      val result = for {
        _ <- clientsChangedBufferDao.insert(toInsert)
        got <- clientsChangedBufferDao.get(allFilter2)
      } yield got

      result.success.value
        .map(r => r.clientId -> r.dataSource) should contain allElementsOf toInsert
        .map(r => r.clientId -> r.dataSource)
    }

    "get filtered item" in {
      val filteredResult = List(
        inputRecord(1, "data_source_1")
      )

      clientsChangedBufferDao
        .get(DataSourceFilter(Set("data_source_1")))
        .success
        .value
        .map(r => r.clientId -> r.dataSource) should contain allElementsOf filteredResult
        .map(r => r.clientId -> r.dataSource)
    }
  }
}
