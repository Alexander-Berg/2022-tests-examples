package ru.yandex.vos2.model.user

import org.scalacheck.Gen
import ru.yandex.vos2.UserModel.UserStatusHistoryItem
import ru.yandex.vos2.model.CommonGen.{CreateDateGen, TrustLevelGen, limitedStr}

/**
  * @author roose
  */
object UserStatusHistoryGenerator {

  val UshItemGen: Gen[UserStatusHistoryItem] = for {
    timestamp ← CreateDateGen
    tl ← TrustLevelGen
    login ← limitedStr()
    comment ← limitedStr()
  } yield UserStatusHistoryItem.newBuilder()
    .setTimestamp(timestamp)
    .setLogin(login)
    .setComment(comment)
    .build()

  val UshListGen: Gen[List[UserStatusHistoryItem]] =
    Gen.choose(0, 3).flatMap(n ⇒ Gen.listOfN(n, UshItemGen))
      .map(_.sortBy(_.getTimestamp))
}
