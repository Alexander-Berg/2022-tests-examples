package ru.yandex.realty.rent.backend.manager

import cats.implicits.catsSyntaxOptionId
import org.junit.runner.RunWith
import org.scalatest.AsyncFunSuite
import org.scalatestplus.junit.JUnitRunner
import realty.palma.rent_user.{RentUser => PalmaUser}
import ru.yandex.realty.application.ng.palma.client.PalmaClient
import ru.yandex.realty.clients.dochub.DocumentServiceClient
import ru.yandex.realty.dochub.DocumentResponse
import ru.yandex.realty.files.GetDownloadUrlResponse
import ru.yandex.realty.rent.backend.ExtendedUserManager
import ru.yandex.realty.rent.dao.{
  FlatDao,
  FlatQuestionnaireDao,
  FlatShowingDao,
  RentContractDao,
  StatusAuditLogDao,
  UserDao,
  UserShowingDao
}
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.enums.ContractStatus
import ru.yandex.realty.serialization.json.ProtoJsonFormats
import ru.yandex.realty.tracing.Traced
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class ContractPdfManagerTest extends AsyncFunSuite with MockitoSupport with RentModelsGen with ProtoJsonFormats {
  implicit val traced: Traced = Traced.empty

  val rentContractDao: RentContractDao = mock[RentContractDao]
  val flatDao: FlatDao = mock[FlatDao]
  val palmaUserClient: PalmaClient[PalmaUser] = mock[PalmaClient[PalmaUser]]
  val userDao: UserDao = mock[UserDao]
  val flatQuestionnaireDao: FlatQuestionnaireDao = mock[FlatQuestionnaireDao]
  val flatShowingDao: FlatShowingDao = mock[FlatShowingDao]
  val userShowingDao: UserShowingDao = mock[UserShowingDao]
  val documentClient: DocumentServiceClient = mock[DocumentServiceClient]
  val statusAuditLogDao: StatusAuditLogDao = mock[StatusAuditLogDao]
  val extendedUserManager: ExtendedUserManager = new ExtendedUserManager(userDao, palmaUserClient)

  val contractPdfManager = new ContractPdfManager(
    rentContractDao,
    flatDao,
    userDao,
    flatQuestionnaireDao,
    flatShowingDao,
    userShowingDao,
    documentClient,
    extendedUserManager
  )

  test("getContractUrl") {
    when(rentContractDao.findByIdOpt(?)(?))
      .thenReturn(Future.successful(rentContractGen(ContractStatus.Active).next.some))
    when(flatDao.findByIdOpt(?)(?)).thenReturn(Future.successful(flatGen().next.some))
    when(flatQuestionnaireDao.findByFlatId(?)(?)).thenReturn(Future.successful(flatQuestionnaireGen.next.some))
    when(userDao.findByUidOpt(?, ?)(?)).thenReturn(Future.successful(userGen().next.some))
    when(palmaUserClient.get(?)(?)).thenReturn(Future.successful(palmaUserGen.next.some))
    when(flatShowingDao.findApprovedByOwnerRequest(?)(?)).thenReturn(Future.successful(flatShowingGen.next.some))
    when(userShowingDao.findByShowing(?)(?)).thenReturn(Future.successful(userShowingGen.next(5).toSeq))
    when(userDao.findByUserIds(?)(?)).thenReturn(Future.successful(userGen().next(2)))
    when(documentClient.createDocument(?, ?)(?)).thenReturn(Future.successful(DocumentResponse.getDefaultInstance))
    when(documentClient.getDocumentUrl(?)(?)).thenReturn(Future.successful(GetDownloadUrlResponse.getDefaultInstance))

    contractPdfManager.getContractUrl("1").map { _ =>
      succeed
    }
  }
}
