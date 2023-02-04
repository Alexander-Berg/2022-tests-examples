package ru.auto.salesman.service.client

import org.scalacheck.Gen
import ru.auto.salesman.dao.ClientDao
import ru.auto.salesman.dao.ClientDao.{ForId, ForIdWithDeleted}
import ru.auto.salesman.service.client.ClientService.ClientServiceError
import ru.auto.salesman.service.client.ClientService.ClientServiceError.ClientNotFound
import ru.auto.salesman.test.model.gens._
import ru.auto.salesman.test.{BaseSpec, TestException}

class ClientServiceImplSpec extends BaseSpec {

  private val clientDao = mock[ClientDao]

  private val clientService = new ClientServiceImpl(clientDao)

  "getOrFail" should {

    "return not deleted client" in {
      forAll(Gen.posNum[Long], ClientRecordGen) { (clientId, client) =>
        (clientDao.get _).expects(ForId(clientId)).returningZ(List(client))
        clientService
          .getByIdOrFail(clientId, withDeleted = false)
          .success
          .value shouldBe client
      }
    }

    "return either deleted or not deleted client" in {
      forAll(Gen.posNum[Long], ClientRecordGen) { (clientId, client) =>
        (clientDao.get _)
          .expects(ForIdWithDeleted(clientId))
          .returningZ(List(client))
        clientService
          .getByIdOrFail(clientId, withDeleted = true)
          .success
          .value shouldBe client
      }
    }

    "return no such element" in {
      forAll(Gen.posNum[Long]) { clientId =>
        (clientDao.get _).expects(ForId(clientId)).returningZ(Nil)
        val result =
          clientService.getByIdOrFail(clientId, withDeleted = false).failure.exception
        result shouldBe a[ClientNotFound]
        result.getStackTrace shouldBe empty
      }
    }

    "return failure" in {
      forAll(Gen.posNum[Long]) { clientId =>
        val e = new TestException
        (clientDao.get _).expects(ForId(clientId)).throwingZ(e)
        clientService
          .getByIdOrFail(clientId, withDeleted = false)
          .failure
          .exception shouldBe ClientServiceError.TransportError(e)
      }
    }
  }

  "get" should {

    "return not deleted client" in {
      forAll(Gen.posNum[Long], ClientRecordGen) { (clientId, client) =>
        (clientDao.get _).expects(ForId(clientId)).returningZ(List(client))
        clientService
          .getById(clientId, withDeleted = false)
          .success
          .value
          .value shouldBe client
      }
    }

    "return either deleted or not deleted client" in {
      forAll(Gen.posNum[Long], ClientRecordGen) { (clientId, client) =>
        (clientDao.get _)
          .expects(ForIdWithDeleted(clientId))
          .returningZ(List(client))
        clientService
          .getById(clientId, withDeleted = true)
          .success
          .value
          .value shouldBe client
      }
    }

    "return None" in {
      forAll(Gen.posNum[Long]) { clientId =>
        (clientDao.get _).expects(ForId(clientId)).returningZ(Nil)
        clientService.getById(clientId, withDeleted = false).success.value shouldBe None
      }
    }

    "return failure" in {
      forAll(Gen.posNum[Long]) { clientId =>
        val e = new TestException
        (clientDao.get _).expects(ForId(clientId)).throwingZ(e)
        clientService
          .getById(clientId, withDeleted = false)
          .failure
          .exception shouldBe ClientServiceError.TransportError(e)
      }
    }
  }
}
