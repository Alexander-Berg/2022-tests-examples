package ru.yandex.realty.rent.amohub.backend.manager

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.amohub.clients.amocrm.{AmocrmClient, PipelineConfig}
import ru.yandex.realty.amohub.dao.{ContactDao, ContactLeadDao}
import ru.yandex.realty.amohub.model.{CommonLeadStatus, ContactLead}
import ru.yandex.realty.amohub.proto.model.responsible_managers.{ResponsibleManager, ResponsibleManagers}
import ru.yandex.realty.rent.amohub.application.AmocrmPipelineConfig
import ru.yandex.realty.rent.amohub.backend.converters.LeadStatus
import ru.yandex.realty.rent.amohub.backend.manager.responsible.ResponsibleUserManagerImpl
import ru.yandex.realty.rent.amohub.backend.resource.ResponsibleManagersStorage
import ru.yandex.realty.rent.amohub.dao.LeadDao
import ru.yandex.realty.rent.amohub.gen.AmohubModelsGen
import ru.yandex.realty.rent.proto.api.showing.FlatShowingStatusNamespace.FlatShowingStatus
import ru.yandex.realty.rent.proto.api.showing.FlatShowingTypeNamespace.FlatShowingType
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future
import scala.jdk.CollectionConverters._

@RunWith(classOf[JUnitRunner])
class ResponsibleUserManagerSpec extends AsyncSpecBase with AmohubModelsGen {

  val leadDao = mock[LeadDao]
  val contactLeadDao = mock[ContactLeadDao]
  val contactDao = mock[ContactDao]
  val amocrmManager = mock[AmocrmManager]
  val amocrmClient = mock[AmocrmClient]
  val responsibleManagersProvider = mock[Provider[ResponsibleManagersStorage]]

  val defaultResponsible = posNum[Long].next

  val pipelinesConfig: AmocrmPipelineConfig = AmocrmPipelineConfig(
    requests = PipelineConfig(pipelineId = 1, responsibleUserId = None),
    offers = PipelineConfig(pipelineId = 2, responsibleUserId = None),
    showings = PipelineConfig(pipelineId = 3, responsibleUserId = Some(defaultResponsible)),
    moveOuts = PipelineConfig(pipelineId = 4, responsibleUserId = None)
  )

  implicit val traced: Traced = Traced.empty

  val responsibleUserManager =
    new ResponsibleUserManagerImpl(
      leadDao,
      contactLeadDao,
      contactDao,
      responsibleManagersProvider,
      amocrmManager,
      amocrmClient,
      pipelinesConfig
    )

  private def registerManagers(managers: ResponsibleManager*) = {
    (responsibleManagersProvider.get _)
      .expects()
      .returning(
        new ResponsibleManagersStorage(ResponsibleManagers.newBuilder().addAllManagers(managers.asJava).build())
      )
  }

  "ResponsibleUserManager" should {
    "choose default user if responsible managers do not exist" in {
      registerManagers()

      val result = responsibleUserManager
        .chooseShowingResponsibleUser(
          showingStatus = FlatShowingStatus.SENT_CONTRACT,
          showingType = FlatShowingType.OFFLINE,
          flatId = Some("1"),
          tenantContactId = None,
          None
        )
        .futureValue

      result shouldBe defaultResponsible
    }

    "Leave current responsible manager if it is on duty" in {

      val Array(managerId1, managerId2, managerId3) = posNum[Long].next(3).toArray

      registerManagers(
        ResponsibleManager.newBuilder().addStatuses(FlatShowingStatus.SENT_CONTRACT).setAmoUserId(managerId1).build(),
        ResponsibleManager.newBuilder().addStatuses(FlatShowingStatus.SENT_CONTRACT).setAmoUserId(managerId2).build(),
        ResponsibleManager.newBuilder().addStatuses(FlatShowingStatus.SENT_CONTRACT).setAmoUserId(managerId3).build()
      )

      (leadDao
        .findByFlatIdsAndPipeline(_: Set[String], _: Long, _: Set[Long], _: Boolean)(_: Traced))
        .expects(*, *, *, *, *)
        .never()

      val result = responsibleUserManager
        .chooseShowingResponsibleUser(
          showingStatus = FlatShowingStatus.SENT_CONTRACT,
          showingType = FlatShowingType.OFFLINE,
          flatId = Some("1"),
          tenantContactId = None,
          Some(managerId2)
        )
        .futureValue

      result shouldBe managerId2
    }

    "Choose responsible manager automatically if only one option exists" in {

      val Array(managerId1, managerId2, managerId3) = posNum[Long].next(3).toArray

      registerManagers(
        ResponsibleManager.newBuilder().addStatuses(FlatShowingStatus.ONLINE).setAmoUserId(managerId1).build(),
        ResponsibleManager
          .newBuilder()
          .addStatuses(FlatShowingStatus.SENT_CONTRACT)
          .addStatuses(FlatShowingStatus.PREPARE_ACCOUNT)
          .setAmoUserId(managerId2)
          .build(),
        ResponsibleManager
          .newBuilder()
          .addStatuses(FlatShowingStatus.PREPARE_ACCOUNT)
          .setAmoUserId(managerId3)
          .build()
      )

      (leadDao
        .findByFlatIdsAndPipeline(_: Set[String], _: Long, _: Set[Long], _: Boolean)(_: Traced))
        .expects(*, *, *, *, *)
        .never()

      val result = responsibleUserManager
        .chooseShowingResponsibleUser(
          showingStatus = FlatShowingStatus.SENT_CONTRACT,
          showingType = FlatShowingType.OFFLINE,
          flatId = Some("1"),
          tenantContactId = None,
          None
        )
        .futureValue

      result shouldBe managerId2
    }

    "Choose responsible manager that works with same flatId" in {

      val Array(managerId1, managerId2, managerId3, managerNotOnDuty) = posNum[Long].next(4).toArray
      val flatId = readableString.next
      val contactId = posNum[Long].next

      registerManagers(
        ResponsibleManager.newBuilder().addStatuses(FlatShowingStatus.SENT_CONTRACT).setAmoUserId(managerId1).build(),
        ResponsibleManager.newBuilder().addStatuses(FlatShowingStatus.SENT_CONTRACT).setAmoUserId(managerId2).build(),
        ResponsibleManager.newBuilder().addStatuses(FlatShowingStatus.SENT_CONTRACT).setAmoUserId(managerId3).build()
      )

      (leadDao
        .findByFlatIdsAndPipeline(_: Set[String], _: Long, _: Set[Long], _: Boolean)(_: Traced))
        .expects(Set(flatId), pipelinesConfig.showings.pipelineId, *, *, *)
        .returning(
          Future.successful(
            List(
              leadGen.next.copy(managerId = Some(managerNotOnDuty), statusId = LeadStatus.Testing.Showings.NewShowing),
              leadGen.next.copy(managerId = Some(managerNotOnDuty), statusId = LeadStatus.Testing.Showings.NewShowing),
              leadGen.next.copy(managerId = Some(managerId2), statusId = LeadStatus.Testing.Showings.NewShowing)
            )
          )
        )

      val result = responsibleUserManager
        .chooseShowingResponsibleUser(
          showingStatus = FlatShowingStatus.SENT_CONTRACT,
          showingType = FlatShowingType.OFFLINE,
          flatId = Some(flatId),
          tenantContactId = Some(contactId),
          None
        )
        .futureValue

      result shouldBe managerId2
    }

    "Choose responsible manager that works with same tenant" in {

      val Array(managerId1, managerId2, managerId3) = posNum[Long].next(3).toArray
      val flatId = readableString.next
      val contactId = posNum[Long].next
      val leadId = posNum[Long].next

      registerManagers(
        ResponsibleManager.newBuilder().addStatuses(FlatShowingStatus.SENT_CONTRACT).setAmoUserId(managerId1).build(),
        ResponsibleManager.newBuilder().addStatuses(FlatShowingStatus.SENT_CONTRACT).setAmoUserId(managerId2).build(),
        ResponsibleManager.newBuilder().addStatuses(FlatShowingStatus.SENT_CONTRACT).setAmoUserId(managerId3).build()
      )

      (leadDao
        .findByFlatIdsAndPipeline(_: Set[String], _: Long, _: Set[Long], _: Boolean)(_: Traced))
        .expects(Set(flatId), pipelinesConfig.showings.pipelineId, *, *, *)
        .returning(
          Future.successful(
            // should prefer manager with same contact
            List(leadGen.next.copy(managerId = Some(managerId1), statusId = CommonLeadStatus.Finalized))
          )
        )

      (contactLeadDao
        .getByContactIds(_: Set[Long])(_: Traced))
        .expects(Set(contactId), *)
        .returning(Future.successful(List(ContactLead(contactId, leadId, isMainContact = true))))

      (leadDao
        .findByIds(_: Set[Long], _: Boolean)(_: Traced))
        .expects(Set(leadId), true, *)
        .returning(
          Future.successful(
            List(
              leadGen.next.copy(
                managerId = Some(managerId2),
                statusId = LeadStatus.Testing.Showings.NewShowing,
                pipelineId = pipelinesConfig.showings.pipelineId
              )
            )
          )
        )

      val result = responsibleUserManager
        .chooseShowingResponsibleUser(
          showingStatus = FlatShowingStatus.SENT_CONTRACT,
          showingType = FlatShowingType.OFFLINE,
          flatId = Some(flatId),
          tenantContactId = Some(contactId),
          None
        )
        .futureValue

      result shouldBe managerId2
    }

    "Choose responsible with minimum active leads if flat and tenant are both new" in {

      val Array(managerId1, managerId2, managerId3) = posNum[Long].next(3).toArray
      val flatId = readableString.next
      val contactId = posNum[Long].next

      registerManagers(
        ResponsibleManager.newBuilder().addStatuses(FlatShowingStatus.SENT_CONTRACT).setAmoUserId(managerId1).build(),
        ResponsibleManager.newBuilder().addStatuses(FlatShowingStatus.SENT_CONTRACT).setAmoUserId(managerId2).build(),
        ResponsibleManager.newBuilder().addStatuses(FlatShowingStatus.SENT_CONTRACT).setAmoUserId(managerId3).build()
      )

      (leadDao
        .findByFlatIdsAndPipeline(_: Set[String], _: Long, _: Set[Long], _: Boolean)(_: Traced))
        .expects(Set(flatId), pipelinesConfig.showings.pipelineId, *, *, *)
        .returning(Future.successful(Nil))

      (contactLeadDao
        .getByContactIds(_: Set[Long])(_: Traced))
        .expects(Set(contactId), *)
        .returning(Future.successful(Nil))

      (leadDao
        .countByManagerId(_: Set[Long], _: Long, _: Set[Long], _: Boolean)(_: Traced))
        .expects(
          Set(managerId1, managerId2, managerId3),
          pipelinesConfig.showings.pipelineId,
          Set(CommonLeadStatus.Loss, CommonLeadStatus.Finalized),
          true,
          *
        )
        .returning(Future.successful(Map(managerId1 -> 3, managerId3 -> 5)))

      val result = responsibleUserManager
        .chooseShowingResponsibleUser(
          showingStatus = FlatShowingStatus.SENT_CONTRACT,
          showingType = FlatShowingType.OFFLINE,
          flatId = Some(flatId),
          tenantContactId = Some(contactId),
          None
        )
        .futureValue

      result shouldBe managerId2
    }

    "Choose responsible with minimum active leads if flat and tenant are not specified" in {

      val Array(managerId1, managerId2, managerId3) = posNum[Long].next(3).toArray
      val flatId = readableString.next
      val contactId = posNum[Long].next

      registerManagers(
        ResponsibleManager.newBuilder().addStatuses(FlatShowingStatus.SENT_CONTRACT).setAmoUserId(managerId1).build(),
        ResponsibleManager.newBuilder().addStatuses(FlatShowingStatus.SENT_CONTRACT).setAmoUserId(managerId2).build(),
        ResponsibleManager.newBuilder().addStatuses(FlatShowingStatus.SENT_CONTRACT).setAmoUserId(managerId3).build()
      )

      (leadDao
        .countByManagerId(_: Set[Long], _: Long, _: Set[Long], _: Boolean)(_: Traced))
        .expects(
          Set(managerId1, managerId2, managerId3),
          pipelinesConfig.showings.pipelineId,
          Set(CommonLeadStatus.Loss, CommonLeadStatus.Finalized),
          true,
          *
        )
        .returning(Future.successful(Map(managerId1 -> 3, managerId2 -> 4, managerId3 -> 5)))

      val result = responsibleUserManager
        .chooseShowingResponsibleUser(
          showingStatus = FlatShowingStatus.SENT_CONTRACT,
          showingType = FlatShowingType.OFFLINE,
          flatId = None,
          tenantContactId = None,
          None
        )
        .futureValue

      result shouldBe managerId1
    }

    "Not change responsible manager if status is showing_appointed and showing_type is online" in {

      val Array(managerId1, managerId2, managerId3) = posNum[Long].next(3).toArray
      val flatId = readableString.next
      val contactId = posNum[Long].next

      registerManagers(
        ResponsibleManager
          .newBuilder()
          .addStatuses(FlatShowingStatus.SHOWING_APPOINTED)
          .setAmoUserId(managerId1)
          .build(),
        ResponsibleManager
          .newBuilder()
          .addStatuses(FlatShowingStatus.SHOWING_APPOINTED)
          .setAmoUserId(managerId2)
          .build()
      )

      val result = responsibleUserManager
        .chooseShowingResponsibleUser(
          showingStatus = FlatShowingStatus.SHOWING_APPOINTED,
          showingType = FlatShowingType.ONLINE,
          flatId = Some(flatId),
          tenantContactId = Some(contactId),
          currentResponsibleUser = Some(managerId3)
        )
        .futureValue

      result shouldBe managerId3
    }
  }
}
