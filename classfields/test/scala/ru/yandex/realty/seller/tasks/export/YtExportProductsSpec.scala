package ru.yandex.realty.seller.tasks.`export`

import org.joda.time.Duration
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.bolts.collection.MapF
import ru.yandex.inside.yt.kosher.Yt
import ru.yandex.inside.yt.kosher.common.GUID
import ru.yandex.inside.yt.kosher.cypress.{Cypress, CypressNodeType, YPath}
import ru.yandex.inside.yt.kosher.impl.common.http.Compressor
import ru.yandex.inside.yt.kosher.impl.transactions.utils.pinger.TransactionPinger
import ru.yandex.inside.yt.kosher.tables.YTableEntryType
import ru.yandex.inside.yt.kosher.transactions.{Transaction, YtTransactions}
import ru.yandex.inside.yt.kosher.ytree.{YTreeMapNode, YTreeNode}
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.application.ng.db.MasterSlaveJdbcDatabase
import ru.yandex.realty.mocks.yt.MockYtTable
import ru.yandex.realty.seller.dao.impl.PurchasedProductDaoImpl
import ru.yandex.realty.seller.dao.jdbc.PurchasedProductDbActionsImpl
import ru.yandex.realty.seller.db.mysql.SellerJdbcSpecBase
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import ru.yandex.realty.seller.tasks.`export`.dao.{DefaultYtProductsDao, PurchasedProductMapper, YtTableMapper}
import ru.yandex.vertis.util.time.DateTimeUtil
import ru.yandex.realty.mocks.yt.MockYtOperations
import ru.yandex.realty.pos.TestOperationalComponents
import ru.yandex.realty.seller.model.product.PurchasedProduct

import java.util.{Optional, Iterator => JIterator}

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class YtExportProductsSpec
  extends AsyncSpecBase
  with SellerJdbcSpecBase
  with TestOperationalComponents
  with SellerModelGenerators {

  trait Fixture {
    val productDbActions = new PurchasedProductDbActionsImpl()
    val db = MasterSlaveJdbcDatabase(database, database)
    val productsDao = new PurchasedProductDaoImpl(productDbActions, masterSlaveDatabase = db)

    val mapper = PurchasedProductMapper
    val ytPath = YPath.cypressRoot().child("products")
    val mockYt = mock[Yt]
    lazy val ytPinger: TransactionPinger = new TransactionPinger(1)

    val task = new YtExportProducts(
      ytProductsDao = new DefaultYtProductsDao(
        yt = mockYt,
        path = ytPath,
        ytPinger = ytPinger,
        schema = mapper.schema
      ),
      ytProductsMapper = mapper,
      productDbActions = productDbActions,
      productsDao = productsDao,
      db = db,
      prometheusRegistry = ops.prometheusRegistry
    )

    def mockCreateTransaction(txId: GUID): Transaction = {
      val mockTxn2 = mock[Transaction]
      val mockTxn = mock[YtTransactions]
      (mockYt.transactions _).expects().returning(mockTxn)
      (mockTxn.startAndGet(_: Duration)).expects(*).returning(mockTxn2)
      (mockTxn2.getTimeout _).expects().returning(Duration.standardMinutes(20))
      (mockTxn2.getId _).expects().returning(txId).anyNumberOfTimes()
      mockTxn2
    }

    def mockMerge: Unit =
      (mockYt.operations _)
        .expects()
        .returning(MockYtOperations)

    def mockWrite(expectedProducts: Seq[PurchasedProduct], mapper: YtTableMapper[PurchasedProduct]): Unit = {
      val expectedYtNodes = expectedProducts.map(mapper.buildYtRow)
      val mockYtTables = new MockYtTable[YTreeMapNode](expectedYtNodes) {
        override def write[T](
          transactionId: Optional[GUID],
          pingAncestorTransactions: Boolean,
          path: YPath,
          entryType: YTableEntryType[T],
          entries: JIterator[T],
          compressor: Compressor
        ): Unit = {
          for (p <- expectedYtNodes) {
            entries.hasNext shouldBe (true)
            val actualP = entries.next()
            actualP shouldEqual p
          }
          entries.hasNext shouldBe (false)
        }
      }
      (mockYt.tables _)
        .expects()
        .returning(mockYtTables)
    }

    def mockCreateTable(txId: GUID, schema: MapF[String, YTreeNode]): Unit = {
      val mockCypress = mock[Cypress]
      (mockYt.cypress _).expects().returning(mockCypress)
      (mockCypress
        .create(
          _: Optional[GUID],
          _: Boolean,
          _: YPath,
          _: CypressNodeType,
          _: Boolean,
          _: Boolean,
          _: MapF[String, YTreeNode]
        ))
        .expects(
          Optional.of(txId),
          true,
          ytPath,
          CypressNodeType.TABLE,
          false,
          true,
          schema
        )
        .returning(())
    }

    def mockCommit(txn: Transaction): Unit = (txn.commit(_: Boolean)).expects(false).returning(())
  }

  "YtExportProducts" should {
    "not fail if no staled products exist" in new Fixture {
      task.run()
    }

    "should move existing stale products to yt" in new Fixture {
      val maxCreateTimeToMove = DateTimeUtil.now().minusMonths(6)
      //prepare stale products
      val staleProducts = for (i <- 1 to 10) yield {
        purchasedProductGen.next.copy(id = i.toString, createTime = maxCreateTimeToMove.minusMinutes(i))
      }
      val staleProductsIds = staleProducts.map(_.id)
      //prepare actual products
      val actualProducts = for (i <- 11 to 20) yield {
        purchasedProductGen.next.copy(id = i.toString, createTime = maxCreateTimeToMove.plusMinutes(i))
      }
      val actualProductsIds = actualProducts.map(_.id)
      val allIds = staleProductsIds ++ actualProductsIds
      productsDao.create(staleProducts ++ actualProducts).futureValue

      val txId = GUID.create()
      val transaction = mockCreateTransaction(txId)
      mockCreateTable(txId, mapper.schema.toAttributesMap)
      mockWrite(staleProducts, mapper)
      mockMerge
      mockCommit(transaction)

      task.run()

      // actual products should not be move
      val products = productsDao.getProducts(allIds).futureValue
      products.toSet shouldEqual actualProducts.toSet
    }
  }
}
