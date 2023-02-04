package ru.yandex.vertis.subscriptions.storage

import com.google.common.base.Charsets
import com.google.protobuf.ByteString
import ru.yandex.vertis.subscriptions.Model
import org.scalacheck.Gen
import ru.yandex.vertis.subscriptions.Model.LiteDocument
import ru.yandex.vertis.subscriptions.model.Dsl
import ru.yandex.vertis.subscriptions.util.test.CoreGenerators._

import scala.collection.JavaConversions._

/**
  * Generators for test data
  */
object Generators {

  val documentSummaryGen: Gen[DocumentSummary] = for {
    n <- Gen.choose(0, 10)
    documents <- Gen.containerOfN(n, tinyDocumentGen)
  } yield documents.foldLeft(
    DocumentSummary.emptySinceNow(5, System.currentTimeMillis() - 100)
  ) { (ns, d) =>
    ns.updated(d)
  }

  val tinyDocumentGen: Gen[DocumentView] = for {
    id <- idGen
    timestamp = System.currentTimeMillis()
  } yield TinyDocument(
    Model.LiteDocument
      .newBuilder()
      .setId(id)
      .setCreateTimestamp(timestamp)
      .setUpdateTimestamp(timestamp)
      .build()
  )

  val activeSubscriptionGen: Gen[ActiveSubscription] = for {
    subscription <- emailSubscriptionGen
    summary <- documentSummaryGen
    key = Notification.Key(subscription, Model.Delivery.Type.EMAIL)
  } yield ActiveSubscription(key, summary, System.currentTimeMillis())
}
