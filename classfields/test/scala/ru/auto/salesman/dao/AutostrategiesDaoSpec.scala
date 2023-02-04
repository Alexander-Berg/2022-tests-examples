package ru.auto.salesman.dao

import org.scalatest._
import org.scalatest.concurrent.IntegrationPatience
import ru.auto.salesman.test.RandomUtil.chooseRandom
import ru.auto.salesman.dao.impl.jdbc.test.JdbcAutostrategiesDaoForTests
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens._
import ru.yandex.vertis.generators.ProducerProvider.asProducer

import scala.concurrent.Future

trait AutostrategiesDaoSpec
    extends BaseSpec
    with IntegrationPatience
    with BeforeAndAfter {

  def autostrategiesDao: JdbcAutostrategiesDaoForTests

  before {
    autostrategiesDao.clean()
  }

  "AutostrategiesDao" should {

    "put, then get; delete and then unable to get autostrategy" in {
      val autostrategies = AutostrategyListGen.next
      autostrategiesDao.put(autostrategies).success
      val offerIds = autostrategies.map(_.offerId).distinct
      autostrategiesDao
        .get(offerIds)
        .success
        .value should contain theSameElementsAs autostrategies
      val ids = autostrategies.map(_.id)
      autostrategiesDao.delete(ids).success
      autostrategiesDao.get(offerIds).success.value shouldBe empty
    }

    "create and then update autostrategy" in {
      val autostrategy = AutostrategyGen.next
      autostrategiesDao.put(Seq(autostrategy)).success
      val updated =
        autostrategyGen(autostrategy.id).suchThat(_ != autostrategy).next
      autostrategiesDao.put(Seq(updated)).success
      val gotAutostrategies =
        autostrategiesDao.get(Seq(autostrategy.offerId)).success.value
      gotAutostrategies should contain(updated)
      gotAutostrategies should not contain autostrategy
    }

    "not get old autostrategy" in {
      val autostrategy = OldAutostrategyGen.next
      autostrategiesDao.put(Seq(autostrategy)).success
      autostrategiesDao
        .get(Seq(autostrategy.offerId))
        .success
        .value shouldBe empty
    }

    "get all autostrategies" in {
      val autostrategies = AutostrategyListGen.next
      autostrategiesDao.put(autostrategies).success
      val gotAutostrategies = autostrategiesDao.all().success.value
      gotAutostrategies.map(
        _.props
      ) should contain theSameElementsAs autostrategies
    }

    "get all autostrategies including deleted" in {
      val autostrategies = AutostrategyListGen.next
      autostrategiesDao.put(autostrategies).success
      val forDeletion = chooseRandom(autostrategies)
      autostrategiesDao.delete(forDeletion.map(_.id)).success
      val gotAutostrategies = autostrategiesDao.all().success.value
      gotAutostrategies.map(
        _.props
      ) should contain theSameElementsAs autostrategies
    }

    "get autostrategy since its creation" in {
      val autostrategy = AutostrategyGen.next
      autostrategiesDao.put(Seq(autostrategy)).success
      val epoch = autostrategiesDao.all().success.value.head.epoch
      autostrategiesDao
        .since(epoch - 1)
        .success
        .value
        .head
        .props shouldBe autostrategy
    }

    "not get autostrategy later then its creation" in {
      val autostrategy = AutostrategyGen.next
      autostrategiesDao.put(Seq(autostrategy)).success
      val epoch = autostrategiesDao.all().success.value.head.epoch
      autostrategiesDao.since(epoch + 1).success.value shouldBe empty
    }

    "get autostrategy since its creation in sinceOrAll()" in {
      val autostrategy = AutostrategyGen.next
      autostrategiesDao.put(Seq(autostrategy)).success
      val epoch = autostrategiesDao.all().success.value.head.epoch
      autostrategiesDao
        .sinceOrAll(Some(epoch - 1))
        .success
        .value
        .head
        .props shouldBe autostrategy
    }

    "not get autostrategy later then its creation in sinceOrAll()" in {
      val autostrategy = AutostrategyGen.next
      autostrategiesDao.put(Seq(autostrategy)).success
      val epoch = autostrategiesDao.all().success.value.head.epoch
      autostrategiesDao.sinceOrAll(Some(epoch + 1)).success.value shouldBe empty
    }

    "get all autostrategies in sinceOrAll()" in {
      val autostrategies = AutostrategyListGen.next
      autostrategiesDao.put(autostrategies).success
      val gotAutostrategies = autostrategiesDao.sinceOrAll(None).success.value
      gotAutostrategies.map(
        _.props
      ) should contain theSameElementsAs autostrategies
    }

    "get all autostrategies including deleted in sinceOrAll()" in {
      val autostrategies = AutostrategyListGen.next
      autostrategiesDao.put(autostrategies).success
      val forDeletion = chooseRandom(autostrategies)
      autostrategiesDao.delete(forDeletion.map(_.id)).success
      val gotAutostrategies = autostrategiesDao.sinceOrAll(None).success.value
      gotAutostrategies.map(
        _.props
      ) should contain theSameElementsAs autostrategies
    }

    "serve several put requests simultaneously without errors" in {
      val batches = AutostrategyListGen.next(10).toList
      Future
        .sequence {
          batches.map { batch =>
            Future(autostrategiesDao.put(batch))
          }
        }
        .map(_ => ())
        .futureValue shouldBe (())
    }

    "succeed on get() if Nil passed" in {
      autostrategiesDao.get(Nil).success.value shouldBe Nil
    }

    "succeed on put() if Nil passed" in {
      autostrategiesDao.put(Nil).success.value shouldBe (())
    }

    "succeed on delete() if Nil passed" in {
      autostrategiesDao.delete(Nil).success.value shouldBe (())
    }
  }
}
