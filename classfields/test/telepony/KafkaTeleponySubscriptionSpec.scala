package auto.dealers.match_maker.scheduler.telepony

import auto.dealers.match_maker.logic.TestMatchApplicationDao
import auto.dealers.match_maker.logic.dao.MatchApplicationDao
import auto.dealers.match_maker.logic.dao.MatchApplicationDao.MatchApplicationDao
import auto.dealers.match_maker.model.{MatchApplicationState, TeleponyId}
import ru.auto.match_maker.model.api.ApiModel.{MatchApplication, Target, UserInfo}
import ru.yandex.vertis.telepony.model.proto.TeleponyCall
import zio.ZIO
import zio.random.Random
import zio.test.Assertion._
import zio.test.{Assertion, DefaultRunnableSpec, _}

import scala.jdk.CollectionConverters._
import scala.concurrent.duration._

object KafkaTeleponySubscriptionSpec extends DefaultRunnableSpec {

  override def spec = {
    suite("KafkaTeleponySubscriptionSpec")(
      testM("should ignore random call") {
        checkM(randomCall)(checkIfCall(not(recorded)))
      },
      testM("should ignore short call") {
        checkM(connectedCall(10))(checkIfCall(not(recorded)))
      },
      testM("record call") {
        checkM(connectedCall(40))(checkIfCall(recorded))
      }
    )
  }

  def createApplication(application: MatchApplication)(): ZIO[MatchApplicationDao, Throwable, Unit] = {
    for {
      _ <- MatchApplicationDao.create(application.getId, application, MatchApplicationState.Processed, 7.days)
    } yield ()
  }

  private def checkIfCall(
      check: Assertion[
        Iterable[Iterable[Any]]
      ]): ((TeleponyCall, MatchApplication)) => ZIO[zio.ZEnv, Throwable, TestResult] = { case (call, app) =>
    (for {
      _ <- createApplication(app)()
      _ <- KafkaTeleponySubscription.processCall(call)
      result <- MatchApplicationDao.get(app.getId)
    } yield assert(result.getTargetList.asScala.map(_.getTeleponyCallIdList.asScala))(check))
      .provideCustomLayer(TestMatchApplicationDao.make.toLayer)
  }

  val recorded: Assertion[Iterable[Iterable[Any]]] = exists(isNonEmpty)

  val idGen: Gen[Random with Sized, String] = Gen.stringN(32)(Gen.alphaNumericChar)
  val posInt: Gen[Random, Int] = Gen.int(0, Int.MaxValue)

  val teleponyId: Gen[Random with Sized, TeleponyId] = {
    for {
      appId <- idGen
      dealerId <- posInt
    } yield TeleponyId(appId, Some(dealerId))
  }

  val call: Gen[Random with Sized, TeleponyCall] = {
    for {
      id <- idGen
      objectId <- idGen
      duration <- posInt
    } yield TeleponyCall
      .newBuilder()
      .setCallId(id.toString)
      .setObjectId(objectId)
      .setTalkDurationSeconds(duration)
      .build()
  }

  val application: Gen[Random with Sized, MatchApplication] = {
    for {
      id <- idGen
      dealerId <- posInt
      userId <- posInt
    } yield MatchApplication
      .newBuilder()
      .setId(id)
      .setUserInfo(UserInfo.newBuilder().setUserId(userId))
      .addTarget(Target.newBuilder().setClientId(dealerId.toString))
      .build()
  }

  val randomCall: Gen[Random with Sized, (TeleponyCall, MatchApplication)] = call <*> application

  def connectedCall(duration: Int): Gen[Random with Sized, (TeleponyCall, MatchApplication)] =
    teleponyId.crossWith(randomCall) { (teleponyId, tuple) =>
      (
        tuple._1.toBuilder.setObjectId(teleponyId.render).setTalkDurationSeconds(duration).build(),
        tuple._2.toBuilder
          .addTarget(Target.newBuilder().setClientId(teleponyId.dealerId.fold("")(_.toString)))
          .setId(teleponyId.matchApplicationId)
          .build()
      )
    }

}
