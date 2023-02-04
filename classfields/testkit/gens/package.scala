package auto.dealers.amoyak.logic.gens

import com.google.protobuf.timestamp.Timestamp
import common.scalapb.ScalaProtobuf.instantToTimestamp
import ru.yandex.vertis.billing.billing_event.BillingOperation
import ru.yandex.vertis.billing.billing_event.CommonBillingInfo._
import ru.yandex.vertis.billing.billing_event.CommonBillingInfo.TransactionInfo.TransactionType
import ru.yandex.vertis.billing.model.CustomerId
import zio.random.Random
import zio.test.{Gen, Sized}
import zio.test.magnolia._

package object gens {
  type DerivedGen[A] = Gen[zio.random.Random with zio.test.Sized, A]

  implicit private val unknownFieldSetGen: DeriveGen[_root_.scalapb.UnknownFieldSet] =
    DeriveGen.instance(Gen.const(_root_.scalapb.UnknownFieldSet.empty))

  implicit def paramToGen[A](a: A): Gen[Any, A] = Gen.const(a)

  val customerIdGen: Gen[Random with Sized, CustomerId] = DeriveGen[CustomerId]
  val timestampGen: Gen[Random, Timestamp] = Gen.anyInstant.map(instantToTimestamp)

  val transactionTypeGen: Gen[Random, TransactionType] =
    Gen.elements(TransactionType.INCOMING, TransactionType.WITHDRAW)

  def billingOperationGen(
      defaultGen: DerivedGen[BillingOperation] = Gen.const(BillingOperation.defaultInstance),
      transactionTypeGen: DerivedGen[TransactionType] = transactionTypeGen,
      customerIdGen: DerivedGen[CustomerId] = customerIdGen,
      amountGen: DerivedGen[Long] = Gen.anyLong,
      timestampGen: DerivedGen[Timestamp] = timestampGen): Gen[Random with Sized, BillingOperation] =
    for {
      billingOperation <- defaultGen
      customerId <- customerIdGen
      transactionInfo <- DeriveGen[TransactionInfo]
      transactionType <- transactionTypeGen
      amount <- amountGen
      timestamp <- timestampGen
    } yield billingOperation
      .withTransactionInfo {
        transactionInfo
          .withAmount(amount)
          .withCustomerId(customerId)
          .withType(transactionType)
      }
      .withTimestamp(timestamp)
      .withDomain(BillingDomain.AUTORU)

}
