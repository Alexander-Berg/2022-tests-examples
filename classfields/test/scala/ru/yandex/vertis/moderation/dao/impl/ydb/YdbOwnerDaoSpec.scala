package ru.yandex.vertis.moderation.dao.impl.ydb

import ru.yandex.vertis.moderation.YdbSpecBase
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.dao.OwnerDao
import ru.yandex.vertis.moderation.dao.impl.ydb.serde.InstanceDaoSerDe
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.OwnerSignalSetGen
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.User
import ru.yandex.vertis.moderation.model.instance.user.OwnerSignals
import ru.yandex.vertis.moderation.model.signal.{
  AutomaticSource,
  Inherited,
  SignalInfo,
  SignalInfoSet,
  SignalSet,
  SignalSwitchOff,
  WarnSignal
}
import ru.yandex.vertis.moderation.model.{DetailedReason, Domain}
import ru.yandex.vertis.moderation.proto.Model.AutomaticSource.Application
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.util.DateTimeUtil
import ru.yandex.vertis.quality.cats_utils.Awaitable.AwaitableSyntax
import ru.yandex.vertis.quality.ydb_utils.WithTransaction

import scala.concurrent.duration.DurationInt
import scala.util.Try

@RunWith(classOf[JUnitRunner])
class YdbOwnerDaoSpec extends YdbSpecBase {
  override val resourceSchemaFileName: String = "/owners.sql"

  val serDe = new InstanceDaoSerDe(Service.AUTORU)
  lazy val dao: OwnerDao[F, User] = new YdbOwnerDao[F, WithTransaction[F, *]](ydbWrapper, serDe, None)

  before {
    Try(ydbWrapper.runTx(ydbWrapper.execute("DELETE FROM owners;")).await)
  }

  val user1 = User.Yandex("1")
  val user2 = User.Yandex("2")

  val warnSignal: WarnSignal =
    WarnSignal(
      Domain.Autoru.default,
      AutomaticSource(Application.MODERATION, marker = Inherited(Service.USERS_AUTORU)),
      DateTimeUtil.now(),
      Some("info"),
      DetailedReason.Stopword,
      1,
      Some(SignalSwitchOff(AutomaticSource(Application.MODERATION), DateTimeUtil.now(), Some("lol"), Some(30.minutes))),
      Some(30.minutes),
      Some("o comm"),
      SignalInfoSet(SignalInfo.ModerationRules(Set(1, 2, 3)))
    )
  val ownerSignals = OwnerSignals(SignalSet(warnSignal))
  val newOwnerSignals = OwnerSignals(OwnerSignalSetGen.next)

  "YdbOwnerDao" should {
    "upsert signal set" in {
      dao.getSignals(user1).await shouldBe None
      dao.upsertSignals(user1, ownerSignals).await
      dao.getSignals(user1).await shouldBe Some(ownerSignals)
    }

    "rewrite signal set" in {
      dao.upsertSignals(user1, ownerSignals).await
      dao.getSignals(user1).await shouldBe Some(ownerSignals)
      dao.upsertSignals(user1, newOwnerSignals).await
      dao.getSignals(user1).await shouldBe Some(newOwnerSignals)
    }

    "separate two users" in {
      dao.upsertSignals(user1, ownerSignals).await
      dao.upsertSignals(user2, newOwnerSignals).await
      dao.getSignals(user1).await shouldBe Some(ownerSignals)
      dao.getSignals(user2).await shouldBe Some(newOwnerSignals)
    }

    "get owners batch" in {
      dao.upsertSignals(user1, ownerSignals).await
      dao.upsertSignals(user2, newOwnerSignals).await
      dao.getSignals(List(user1, user2)).await.values should contain theSameElementsAs List(
        ownerSignals,
        newOwnerSignals
      )
    }
  }

}
