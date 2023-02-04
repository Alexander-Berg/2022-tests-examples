package ru.auto.salesman.service.impl

import org.scalacheck.Gen
import ru.auto.salesman.dao.ClientDao.{ForId, ForIdWithDeleted}
import ru.auto.salesman.dao.{BalanceClientDao, ClientDao}
import ru.auto.salesman.model.DetailedClient
import ru.auto.salesman.service.client.ClientService.ClientServiceError
import ru.auto.salesman.service.client.ClientServiceImpl
import ru.auto.salesman.test.model.gens._
import ru.auto.salesman.test.{BaseSpec, TestException}

class DetailedClientServiceImplSpec extends BaseSpec {

  private val clientDao = mock[ClientDao]
  private val balanceClientDao = mock[BalanceClientDao]

  private val clientSource =
    new DetailedClientSourceImpl(
      new ClientServiceImpl(clientDao),
      balanceClientDao
    )

  "unsafeResolve" should {

    "return not deleted detailed client" in {
      forAll(Gen.posNum[Long], ClientRecordGen, balanceRecordGen) {
        (clientId, client, balanceClient) =>
          (clientDao.get _).expects(ForId(clientId)).returningZ(List(client))
          (balanceClientDao.get _)
            .expects(clientId)
            .returningZ(Some(balanceClient))
          clientSource
            .unsafeResolve(clientId)
            .success
            .value shouldBe DetailedClient(client, balanceClient)
      }
    }

    "return either deleted or not deleted detailed client" in {
      forAll(Gen.posNum[Long], ClientRecordGen, balanceRecordGen) {
        (clientId, client, balanceClient) =>
          (clientDao.get _)
            .expects(ForIdWithDeleted(clientId))
            .returningZ(List(client))
          (balanceClientDao.get _)
            .expects(clientId)
            .returningZ(Some(balanceClient))
          clientSource
            .unsafeResolve(clientId, withDeleted = true)
            .success
            .value shouldBe DetailedClient(client, balanceClient)
      }
    }

    "return no such element on missing client" in {
      forAll(Gen.posNum[Long]) { clientId =>
        (clientDao.get _).expects(ForId(clientId)).returningZ(Nil)
        val result = clientSource.unsafeResolve(clientId).failure.exception
        result shouldBe a[ClientServiceError.ClientNotFound]
        result.getStackTrace shouldBe empty
      }
    }

    "return no such element on missing balance client" in {
      forAll(Gen.posNum[Long], ClientRecordGen) { (clientId, client) =>
        (clientDao.get _).expects(ForId(clientId)).returningZ(List(client))
        (balanceClientDao.get _)
          .expects(clientId)
          .returningZ(None)
        val result = clientSource.unsafeResolve(clientId).failure.exception
        result shouldBe a[NoSuchElementException]
        result.getStackTrace shouldBe empty
        result.getMessage should startWith("Balance info is not found")
      }
    }

    "return failure on client dao failure" in {
      forAll(Gen.posNum[Long]) { clientId =>
        val e = new TestException
        (clientDao.get _).expects(ForId(clientId)).throwingZ(e)
        clientSource
          .unsafeResolve(clientId)
          .failure
          .exception
          .shouldBe(ClientServiceError.TransportError(e))
      }
    }

    "return failure on balance dao failure" in {
      forAll(Gen.posNum[Long], ClientRecordGen) { (clientId, client) =>
        val e = new TestException
        (clientDao.get _).expects(ForId(clientId)).returningZ(List(client))
        (balanceClientDao.get _)
          .expects(clientId)
          .throwingZ(e)
        clientSource.unsafeResolve(clientId).failure.exception shouldBe e
      }
    }
  }

  "resolve" should {

    "return not deleted detailed client" in {
      forAll(Gen.posNum[Long], ClientRecordGen, balanceRecordGen) {
        (clientId, client, balanceClient) =>
          (clientDao.get _).expects(ForId(clientId)).returningZ(List(client))
          (balanceClientDao.get _)
            .expects(clientId)
            .returningZ(Some(balanceClient))
          clientSource
            .resolve(clientId)
            .success
            .value
            .value shouldBe DetailedClient(client, balanceClient)
      }
    }

    "return either deleted or not deleted detailed client" in {
      forAll(Gen.posNum[Long], ClientRecordGen, balanceRecordGen) {
        (clientId, client, balanceClient) =>
          (clientDao.get _)
            .expects(ForIdWithDeleted(clientId))
            .returningZ(List(client))
          (balanceClientDao.get _)
            .expects(clientId)
            .returningZ(Some(balanceClient))
          clientSource
            .resolve(clientId, withDeleted = true)
            .success
            .value
            .value shouldBe DetailedClient(client, balanceClient)
      }
    }

    "return None on missing client" in {
      forAll(Gen.posNum[Long]) { clientId =>
        (clientDao.get _).expects(ForId(clientId)).returningZ(Nil)
        clientSource.resolve(clientId).success.value shouldBe None
      }
    }

    "return None on missing balance client" in {
      forAll(Gen.posNum[Long], ClientRecordGen) { (clientId, client) =>
        (clientDao.get _).expects(ForId(clientId)).returningZ(List(client))
        (balanceClientDao.get _)
          .expects(clientId)
          .returningZ(None)
        clientSource.resolve(clientId).success.value shouldBe None
      }
    }

    "return failure on client dao failure" in {
      forAll(Gen.posNum[Long]) { clientId =>
        val e = new TestException
        (clientDao.get _).expects(ForId(clientId)).throwingZ(e)
        clientSource
          .resolve(clientId)
          .failure
          .exception
          .shouldBe(ClientServiceError.TransportError(e))
      }
    }

    "return failure on balance dao failure" in {
      forAll(Gen.posNum[Long], ClientRecordGen) { (clientId, client) =>
        val e = new TestException
        (clientDao.get _).expects(ForId(clientId)).returningZ(List(client))
        (balanceClientDao.get _)
          .expects(clientId)
          .throwingZ(e)
        clientSource.resolve(clientId).failure.exception shouldBe e
      }
    }
  }
}
