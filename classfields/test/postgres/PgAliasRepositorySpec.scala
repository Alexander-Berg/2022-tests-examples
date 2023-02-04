package auto.dealers.dealer_aliases.storage.postgres.test

import common.zio.doobie.schema.InitSchema
import common.zio.doobie.testkit.TestPostgresql
import doobie._
import doobie.implicits._
import doobie.implicits.toSqlInterpolator
import auto.dealers.dealer_aliases.model._
import auto.dealers.dealer_aliases.storage.{AliasRepository, DoobieAliasRepository}
import zio.{Has, Task, URIO, ZIO}
import zio.interop.catz._
import zio.test.Assertion._
import zio.test.TestAspect.{after, beforeAll, failing, sequential}
import zio.test.{DefaultRunnableSpec, ZSpec, _}

object PgAliasRepositorySpec extends DefaultRunnableSpec {

  private val alias1 = Alias(DealerId(1234L), ExternalId("1234ext"), Source.CMExpert, AliasType.External)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    val suit = suite("PgAliasRepositorySpec")(
      addAliasSimple,
      addAliasDouble,
      addAndDelete,
      addAndDeleteAndRestore
    ) @@
      beforeAll(dbInit) @@
      after(dbClean) @@
      sequential

    suit.provideCustomLayerShared(TestPostgresql.managedTransactor >+> DoobieAliasRepository.test)
  }

  private val dbInit: URIO[Has[doobie.Transactor[Task]], Unit] =
    ZIO
      .service[Transactor[Task]]
      .flatMap(InitSchema("/schema.sql", _))
      .orDie

  private val dbClean = ZIO.serviceWith[Transactor[Task]](xa => sql"DELETE FROM relations".update.run.transact(xa))

  private val addAliasSimple = testM("add single alias") {
    for {
      repo <- ZIO.service[AliasRepository]
      len <- repo.listAll.map(_.length)
      ra <- repo.addAlias(alias1)
      len2 <- repo.listAll.map(_.length)
    } yield assert(ra.value)(equalTo(1)) && assert(len2 - len)(equalTo(1))
  }

  private val addAliasDouble = testM("add the same alias two times should throw an error") {
    for {
      repo <- ZIO.service[AliasRepository]
      _ <- repo.addAlias(alias1)
      ra2 <- repo.addAlias(alias1).run
    } yield assert(ra2)(fails(isSubtype[AliasAlreadyExistsStorageException](anything)))
  }

  private val addAndDelete = testM("add and delete the alias should be seen with listAll and checkHasDeleted") {
    for {
      repo <- ZIO.service[AliasRepository]
      lenBefore <- repo.listAll.map(_.length)
      ra1 <- repo.addAlias(alias1)
      ra2 <- repo.deleteAlias(alias1)
      lenAfter <- repo.listAll.map(_.length)
      isDel <- repo.checkHasDeleted(alias1)
    } yield assert(isDel)(equalTo(true)) &&
      assert(lenAfter - lenBefore)(equalTo(1)) &&
      assert((ra1.value, ra2.value))(equalTo((1, 1)))
  }

  private val addAndDeleteAndRestore = testM("adding, deleting and then adding back should be transparent")(
    for {
      repo <- ZIO.service[AliasRepository]
      ra1 <- repo.addAlias(alias1)
      qry1 <- repo.getByAutoruId(alias1.dealer)
      ra2 <- repo.deleteAlias(alias1)
      isDel <- repo.checkHasDeleted(alias1)
      ra3 <- repo.undeleteAlias(alias1)
      qry2 <- repo.getByAutoruId(alias1.dealer)
    } yield assert(qry1)(hasSameElements(qry2)) &&
      assert(isDel)(equalTo(true)) &&
      assert((ra1.value, ra2.value, ra3.value))(equalTo((1, 1, 1)))
  )

}
