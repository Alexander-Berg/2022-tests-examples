package ru.yandex.realty.rent.amohub.gen

import org.scalacheck.Gen
import ru.yandex.realty.amohub.model.{Contact, ContactPhone, ContactPhoneStatus}
import ru.yandex.realty.rent.amohub.model.{FlatShowingType, RentLead => Lead}
import ru.yandex.vertis.generators.BasicGenerators
import ru.yandex.vertis.util.time.DateTimeUtil

trait AmohubModelsGen extends BasicGenerators {

  private val phones = Seq(
    "+79521079770",
    "+79992134916",
    "+79042166212",
    "+79213910656",
    "89521079770",
    "89992134916",
    "89042166212",
    "89213910656"
  )

  def contactGen(shardKey: Option[Int] = None): Gen[Contact] =
    for {
      contactId <- posNum[Long]
      firstPhone <- Gen.oneOf(phones)
      secondPhone <- Gen.oneOf(phones).filter(_ != firstPhone)
      genShardKey <- Gen.oneOf(0, 1)
      name <- Gen.option(readableString)
      responsibleUserId <- posNum[Long]
    } yield Contact(
      contactId,
      Seq(
        ContactPhone(contactId, firstPhone, Some(firstPhone), ContactPhoneStatus.Unified),
        ContactPhone(contactId, secondPhone, None, ContactPhoneStatus.New)
      ),
      deleted = false,
      responsibleUserId = Some(responsibleUserId),
      name = name,
      DateTimeUtil.now,
      DateTimeUtil.now,
      Some(DateTimeUtil.now.minusMinutes(1)),
      shardKey.getOrElse(genShardKey),
      amoResponse = None
    )

  def contactPhoneGen(uid: Long): Gen[ContactPhone] =
    for {
      phone <- Gen.oneOf(phones)
    } yield ContactPhone(uid, phone, Some(phone), ContactPhoneStatus.Unified)

  def leadGen: Gen[Lead] =
    for {
      uid <- posNum[Long]
      flatId <- Gen.option(readableString)
      statusId <- posNum[Long]
      source <- Gen.option(readableString)
      pipeline <- posNum[Long]
      lossReasonId <- Gen.option(posNum[Long])
      showingId <- Gen.option(readableString)
      ownerRequestId <- Gen.option(readableString)
      genShardKey <- Gen.oneOf(0, 1)
      closeShowingCauseId <- Gen.option(posNum[Long])
    } yield Lead(
      leadId = uid,
      flatId = flatId,
      statusId = statusId,
      closeShowingCauseId = closeShowingCauseId,
      source = source,
      pipelineId = pipeline,
      lossReasonId = lossReasonId,
      isConfirmed = true,
      showingId = showingId,
      showingType = Some(FlatShowingType.Unknown),
      tenantStructure = None,
      deleted = false,
      createTime = DateTimeUtil.now,
      updateTime = DateTimeUtil.now,
      ytExportHash = None,
      kafkaEventHash = None,
      managerId = None,
      ownerRequestId = ownerRequestId,
      createdByBack = true,
      lastEventTimestamp = None,
      visitTime = Some(DateTimeUtil.now),
      shardKey = genShardKey,
      amoResponse = None
    )
}
