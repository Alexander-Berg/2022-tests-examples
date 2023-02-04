package ru.yandex.realty.consumer.impl

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.features.SimpleFeatures
import ru.yandex.realty.rent.proto.model.diffevent.{DiffEvent, FlatDiffEvent, UserDiffEvent, UserProtoView}
import ru.yandex.realty.rent.proto.model.user.PassportData.{DocumentEntry, DocumentType}
import ru.yandex.realty.rent.proto.model.user.{PassportData, UserData}
import ru.yandex.realty.util.StubTestDataSettings
import ru.yandex.realty.yankee.consumer.impl.RentDiffEventProcessorImpl
import ru.yandex.realty.yankee.dao.BaseYankeeDaoSpec
import ru.yandex.realty.yankee.model.{YangTask, YangTaskType}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class RentDiffEventProcessorImplSpec extends BaseYankeeDaoSpec {

  private val Uid = 654321L
  private val PassportMainPageId = "passport_main_page_id"
  private val ChangedPassportMainPageId = "changed_passport_main_page_id"
  private val PassportRegistrationPageId = "passport_registration_page_id"
  private val SelfieWithPassportId = "selfie_with_passport_id"
  private val SelfieId = "selfie_id"

  private val DocumentIdsMap = Map(
    DocumentType.PASSPORT_MAIN_PAGE -> PassportMainPageId,
    DocumentType.REGISTRATION_PAGE -> PassportRegistrationPageId,
    DocumentType.SELFIE_WITH_PASSPORT -> SelfieWithPassportId,
    DocumentType.SELFIE -> SelfieId
  )

  "RentDiffEventProcessorImpl" should {
    "not create tasks for other than user event" in {
      val event = buildEvent(Seq.empty, Seq.empty).toBuilder
        .setFlatEvent(FlatDiffEvent.getDefaultInstance)
        .build()

      invokeProcessor(event)

      val storedTasks = getAllYangTasks()
      storedTasks.size shouldEqual 0
    }

    "not create tasks if not all documents were uploaded" in {
      val newDocuments = buildDocuments(DocumentType.PASSPORT_MAIN_PAGE, DocumentType.REGISTRATION_PAGE)
      val event = buildEvent(Seq.empty, newDocuments)

      invokeProcessor(event)

      val storedTasks = getAllYangTasks()
      storedTasks.size shouldEqual 0
    }

    "not create tasks if documents were not changed" in {
      val documents =
        buildDocuments(DocumentType.PASSPORT_MAIN_PAGE, DocumentType.REGISTRATION_PAGE, DocumentType.SELFIE)
      val event = buildEvent(documents, documents)

      invokeProcessor(event)

      val storedTasks = getAllYangTasks()
      storedTasks.size shouldEqual 0
    }

    "create tasks for combined selfie and passport photos" in {
      val documents = buildDocuments(
        DocumentType.PASSPORT_MAIN_PAGE,
        DocumentType.REGISTRATION_PAGE,
        DocumentType.SELFIE_WITH_PASSPORT
      )
      val event = buildEvent(Seq.empty, documents)

      invokeProcessor(event)

      val storedTasks = getAllYangTasks()
      storedTasks.size shouldEqual 3
      assertPassportMainPageTask(storedTasks.find(_.id == 1).get)
      assertPassportRegistrationPageTask(storedTasks.find(_.id == 2).get)
      assertCombinedSelfieTask(storedTasks.find(_.id == 3).get)
    }

    "create tasks for separate selfie and passport photos" in {
      val documents = buildDocuments(
        DocumentType.PASSPORT_MAIN_PAGE,
        DocumentType.REGISTRATION_PAGE,
        DocumentType.SELFIE
      )
      val event = buildEvent(Seq.empty, documents)

      invokeProcessor(event)

      val storedTasks = getAllYangTasks()
      storedTasks.size shouldEqual 3
      assertPassportMainPageTask(storedTasks.find(_.id == 1).get)
      assertPassportRegistrationPageTask(storedTasks.find(_.id == 2).get)
      assertSeparateSelfieTask(storedTasks.find(_.id == 3).get)
    }

    "recreate only passport markup task" in {
      val passportMainPageDocument = buildDocuments(DocumentType.PASSPORT_MAIN_PAGE).head
      val changedPassportMainPageDocument = passportMainPageDocument.toBuilder.setId(ChangedPassportMainPageId).build()
      val otherDocuments = buildDocuments(DocumentType.REGISTRATION_PAGE, DocumentType.SELFIE_WITH_PASSPORT)
      val oldDocuments = otherDocuments ++ Seq(passportMainPageDocument)
      val newDocuments = otherDocuments ++ Seq(changedPassportMainPageDocument)
      val firstEvent = buildEvent(Seq.empty, oldDocuments)
      val secondEvent = buildEvent(oldDocuments, newDocuments)

      invokeProcessor(firstEvent)
      invokeProcessor(secondEvent)

      val storedTasks = getAllYangTasks()
      storedTasks.size shouldEqual 4
      assertPassportMainPageTask(storedTasks.find(_.id == 4).get, ChangedPassportMainPageId)
    }

    "recreate passport markup and selfie reconciliation tasks" in {
      val passportMainPageDocument = buildDocuments(DocumentType.PASSPORT_MAIN_PAGE).head
      val changedPassportMainPageDocument = passportMainPageDocument.toBuilder.setId(ChangedPassportMainPageId).build()
      val otherDocuments = buildDocuments(DocumentType.REGISTRATION_PAGE, DocumentType.SELFIE)
      val oldDocuments = otherDocuments ++ Seq(passportMainPageDocument)
      val newDocuments = otherDocuments ++ Seq(changedPassportMainPageDocument)
      val firstEvent = buildEvent(Seq.empty, oldDocuments)
      val secondEvent = buildEvent(oldDocuments, newDocuments)

      invokeProcessor(firstEvent)
      invokeProcessor(secondEvent)

      val storedTasks = getAllYangTasks()
      storedTasks.size shouldEqual 5
      assertPassportMainPageTask(storedTasks.find(_.id == 4).get, ChangedPassportMainPageId)
      assertSeparateSelfieTask(storedTasks.find(_.id == 6).get, ChangedPassportMainPageId)
    }
  }

  private def invokeProcessor(event: DiffEvent): Unit = {
    val features = new SimpleFeatures
    features.RentPassportVerification.setNewState(true)
    val processor = new RentDiffEventProcessorImpl(doobieDatabase, yangTaskDao, features, StubTestDataSettings)
    processor.process(event).futureValue
  }

  private def buildEvent(oldDocuments: Seq[DocumentEntry], newDocuments: Seq[DocumentEntry]): DiffEvent =
    DiffEvent
      .newBuilder()
      .setUserEvent {
        UserDiffEvent
          .newBuilder()
          .setOld(buildUserProtoView(oldDocuments))
          .setNew(buildUserProtoView(newDocuments))
      }
      .build()

  private def buildUserProtoView(documents: Seq[DocumentEntry]): UserProtoView =
    UserProtoView
      .newBuilder()
      .setUid(Uid)
      .setData {
        UserData
          .newBuilder()
          .setPassportData {
            PassportData
              .newBuilder()
              .addAllDocuments(documents.asJava)
          }
      }
      .build()

  private def buildDocuments(documentTypes: DocumentType*): Seq[DocumentEntry] =
    documentTypes.map { documentType =>
      DocumentEntry
        .newBuilder()
        .setId(DocumentIdsMap(documentType))
        .setType(documentType)
        .build()
    }

  private def assertPassportMainPageTask(task: YangTask, documentId: String = PassportMainPageId): Unit = {
    task.idempotencyKey shouldEqual s"$Uid/$documentId"
    task.taskType shouldEqual YangTaskType.PassportMainPageMarkup
    val payload = task.payload.getPassportVerification
    payload.uid shouldEqual Uid
    payload.getPassportMainPageDocuments.passportMainPageDocumentId shouldEqual documentId
  }

  private def assertPassportRegistrationPageTask(task: YangTask): Unit = {
    task.idempotencyKey shouldEqual s"$Uid/$PassportRegistrationPageId"
    task.taskType shouldEqual YangTaskType.PassportRegistrationPageMarkup
    val payload = task.payload.getPassportVerification
    payload.uid shouldEqual Uid
    payload.getPassportRegistrationPageDocuments.passportRegistrationPageDocumentId
      .shouldEqual(PassportRegistrationPageId)
  }

  private def assertCombinedSelfieTask(task: YangTask): Unit = {
    task.idempotencyKey shouldEqual s"$Uid/$SelfieWithPassportId"
    task.taskType shouldEqual YangTaskType.SelfieReconciliation
    val payload = task.payload.getPassportVerification
    payload.uid shouldEqual Uid
    payload.getCombinedSelfieAndPassportDocuments.selfieWithPassportDocumentId shouldEqual SelfieWithPassportId
  }

  private def assertSeparateSelfieTask(task: YangTask, passportDocumentId: String = PassportMainPageId): Unit = {
    task.idempotencyKey shouldEqual s"$Uid/$SelfieId/$passportDocumentId"
    task.taskType shouldEqual YangTaskType.SelfieReconciliation
    val payload = task.payload.getPassportVerification
    payload.uid shouldEqual Uid
    payload.getSeparateSelfieAndPassportDocuments.passportMainPageDocumentId shouldEqual passportDocumentId
    payload.getSeparateSelfieAndPassportDocuments.selfieDocumentId shouldEqual SelfieId
  }
}
