package ru.yandex.vertis.general.amonition.storage.test

import common.zio.doobie.syntax._
import common.zio.doobie.testkit.TestPostgresql
import ru.yandex.vertis.general.amonition.model.Company
import ru.yandex.vertis.general.amonition.storage.CompaniesDao
import ru.yandex.vertis.general.amonition.storage.postgresql.PgCompaniesDao
import ru.yandex.vertis.general.common.model.user.SellerId
import zio.ZIO
import zio.test.Assertion._
import zio.test._

object PgCompaniesDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("PgProfilesDao")(
      testM("Insert companies") {
        for {
          dao <- ZIO.service[CompaniesDao.Service]
          _ <- dao.insertOrIgnore(Seq(Company(SellerId.UserId(100L), 1000L))).transactIO
        } yield assertCompletes
      },
      testM("Insert companies and get by sellerIds") {
        for {
          dao <- ZIO.service[CompaniesDao.Service]
          company1 = Company(SellerId.UserId(7L), 2000L)
          company2 = Company(SellerId.UserId(8L), 3000L)
          _ <- dao.insertOrIgnore(Seq(company1, company2)).transactIO
          inserted <- dao.getCompanies(List(company1.sellerId, company2.sellerId)).transactIO
        } yield assert(inserted)(contains(company1)) && assert(inserted)(contains(company2))
      },
      testM("Insert companies and get by companyId") {
        for {
          dao <- ZIO.service[CompaniesDao.Service]
          company1 = Company(SellerId.UserId(9L), 4000L)
          company2 = Company(SellerId.UserId(10L), 5000L)
          _ <- dao.insertOrIgnore(Seq(company1, company2)).transactIO
          inserted <- dao.getCompaniesByIds(List(company1.companyId, company2.companyId)).transactIO
        } yield assert(inserted)(contains(company1)) && assert(inserted)(contains(company2))
      }
    ).provideCustomLayerShared(
      TestPostgresql.managedTransactor >+> PgCompaniesDao.live
    )
}
