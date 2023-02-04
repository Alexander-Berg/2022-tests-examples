package ru.yandex.vos2.realty.model.offer

import org.scalacheck.Gen
import ru.yandex.vos2.OfferModel.{OfferStatusHistoryItem, OfferStatusSource}
import ru.yandex.vos2.model.CommonGen._

/**
  * @author roose
  */
object OfferStatusHistoryGenerator {

  val OshItemGen: Gen[OfferStatusHistoryItem] = for {
    timestamp ← CreateDateGen
    status ← CompositeStatusGen
    userTl ← TrustLevelGen
    source ← Gen.oneOf(OfferStatusSource.values)
    login ← limitedStr()
    comment ← limitedStr()
    code ← Gen.choose(1, 99)
  } yield OfferStatusHistoryItem
    .newBuilder()
    .setTimestamp(timestamp)
    .setOfferStatus(status)
    .setUserTl(userTl)
    .setSource(source)
    .setAdminLogin(login)
    .setComment(comment)
    .setIdxCode(code)
    .build()

  val OshListGen: Gen[List[OfferStatusHistoryItem]] =
    Gen
      .choose(0, 5)
      .flatMap(n ⇒ Gen.listOfN(n, OshItemGen))
      .map(_.sortBy(_.getTimestamp))
}
