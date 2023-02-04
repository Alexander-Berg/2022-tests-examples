package auto.c2b.reception.storage.test

import auto.c2b.common.model.BuyOutAlg
import auto.c2b.common.model.paging.PagingRequest
import auto.c2b.common.testkit.CommonGenerators
import auto.c2b.reception.model.testkit.Generators
import auto.c2b.reception.model.{ApplicationStatus, _}
import auto.c2b.reception.storage.postgresql.ApplicationsDao
import auto.c2b.reception.storage.postgresql.impl.ApplicationsDaoImpl
import common.zio.doobie.syntax._
import common.zio.doobie.testkit.TestPostgresql
import zio.ZIO
import zio.test.Assertion._
import zio.test.TestAspect.{sequential, shrinks}
import zio.test._

import java.time.Instant

object ApplicationsDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("ApplicationsDao")(
      testM("создаёт заявку") {
        checkNM(1)(Generators.Application.paramForNewApplicationAny) {
          case (
                vin,
                offer,
                userId,
                inspectDates,
                inspectTime,
                inspectPlace,
                pricePrediction,
                startingPrice,
                carInfo,
                alg
              ) =>
            for {
              dao <- ZIO.service[ApplicationsDao.Service]
              applicationOrError <- dao
                .create(
                  vin,
                  Some(offer),
                  Some(userId),
                  inspectDates,
                  inspectTime,
                  inspectPlace,
                  pricePrediction,
                  startingPrice,
                  carInfo,
                  alg,
                  Some(Instant.now())
                )
                .transactIO
              application <- ZIO.succeed(applicationOrError)
              saved <- dao.get(application.id).transactIO
            } yield {
              assert(saved)(isSome) &&
              assertTrue(saved.get.vin == vin) &&
              assertTrue(saved.get.status == ApplicationStatus.New) &&
              assertTrue(saved.get.offer.get == offer) &&
              assertTrue(saved.get.userId.get == userId) &&
              assertTrue(saved.get.inspectDates == inspectDates) &&
              assertTrue(saved.get.inspectTime == inspectTime) &&
              assertTrue(saved.get.inspectPlace == inspectPlace) &&
              assertTrue(saved.get.waitingData == Set.empty[DataSource]) &&
              assertTrue(saved.get.proAutoReport == ProAutoReport.WaitRequest) &&
              assertTrue(saved.get.cmeReport == CMEReport.WaitRequest) &&
              assertTrue(saved.get.buyOutAlg == alg)
            }
        }
      },
      testM("возвращает заявку c select for update") {
        checkNM(1)(Generators.Application.applicationAny.map(_.copy(statusFinishAt = None))) { application =>
          for {
            dao <- ZIO.service[ApplicationsDao.Service]
            _ <- dao.createOrUpdate(application).transactIO
            saved <- dao.getAndLock(application.id).transactIO
          } yield assert(saved)(isSome(equalTo(application)))
        }
      },
      testM("возвращает заявку") {
        checkNM(1)(Generators.Application.applicationAny.map(_.copy(statusFinishAt = None))) { application =>
          for {
            dao <- ZIO.service[ApplicationsDao.Service]
            _ <- dao.createOrUpdate(application).transactIO
            saved <- dao.get(application.id).transactIO
          } yield assert(saved)(isSome(equalTo(application)))
        }
      },
      testM("удаляет заявку") {
        checkNM(1)(Generators.Application.applicationAny) { application =>
          for {
            dao <- ZIO.service[ApplicationsDao.Service]
            _ <- dao.createOrUpdate(application).transactIO
            saved <- dao.get(application.id).transactIO
            _ <- dao.delete(application.id).transactIO
            afterDelete <- dao.get(application.id).transactIO
          } yield assert(saved)(isSome) && assert(afterDelete)(isNone)
        }
      },
      testM("возвращает заявки ожидающие данные") {
        checkNM(1)(
          Gen.setOfN(10)(
            Generators.Application.applicationAny.map(_.copy(status = ApplicationStatus.WaitingDocuments))
          )
        ) { applications =>
          for {
            dao <- ZIO.service[ApplicationsDao.Service]
            _ <- ZIO.foreach_(applications)(dao.createOrUpdate(_).transactIO)
            saved <- ZIO.foreach(applications)(a => dao.get(a.id).transactIO)
            applicationWaitingCme <- dao.getWaitingDataApplications(DataSource.Cme, 10).transactIO
            applicationWaitingCarfax <- dao.getWaitingDataApplications(DataSource.Carfax, 10).transactIO
            awCme = saved.flatMap(_.filter(_.waitingData.contains(DataSource.Cme))).toList
            awCarfax = saved.flatMap(_.filter(_.waitingData.contains(DataSource.Carfax))).toList
          } yield {
            assert(applicationWaitingCme)(hasSameElements(awCme)) &&
            assert(applicationWaitingCarfax)(hasSameElements(awCarfax))
          }
        }
      },
      testM("возвращает список заявок") {
        checkNM(1)(
          Gen.setOfN(10)(Generators.Application.applicationAny.map(_.copy(statusFinishAt = None))),
          CommonGenerators.userIdAny.derive
        ) { (applications, userId) =>
          val withUser = applications.map(_.copy(userId = Some(userId)))
          for {
            dao <- ZIO.service[ApplicationsDao.Service]
            _ <- ZIO.foreach_(withUser)(dao.createOrUpdate(_).transactIO)
            got <- dao.list(userId, PagingRequest(10, 1)).transactIO
          } yield assert(got.applications)(hasSameElements(withUser.toList)) && assertTrue(
            got.page.total == withUser.size
          )
        }
      }
    ) @@ sequential @@ shrinks(0))
      .provideCustomLayerShared {
        TestPostgresql.managedTransactor >+> ApplicationsDaoImpl.live
      }
  }
}
