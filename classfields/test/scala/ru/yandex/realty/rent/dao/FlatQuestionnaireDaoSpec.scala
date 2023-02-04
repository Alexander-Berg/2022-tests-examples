package ru.yandex.realty.rent.dao

import org.junit.runner.RunWith
import org.scalatest.WordSpecLike
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.rent.dao.actions.impl.{
  FlatDbActionsImpl,
  FlatQuestionnaireDbActionsImpl,
  FlatShowingDbActionsImpl,
  KeysHandoverDbActionsImpl,
  PaymentDbActionsImpl,
  RentContractDbActionsImpl,
  UserDbActionsImpl
}
import ru.yandex.realty.rent.dao.impl.{FlatDaoImpl, FlatQuestionnaireDaoImpl}
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.{Flat, FlatQuestionnaire}
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class FlatQuestionnaireDaoSpec extends WordSpecLike with RentSpecBase with CleanSchemaBeforeEach {

  implicit val trace: Traced = Traced.empty

  "FlatQuestionnaireDao.findByFlatId" should {
    "returns successful result" in new Wiring with Data {
      val sampleFlatId: String = sampleFlats.head.flatId
      val result: Option[FlatQuestionnaire] = flatQuestionnaireDao.findByFlatId(sampleFlatId).futureValue
      result shouldBe defined
      result.map(_.flatId) shouldEqual questionnaires.find(_.flatId == sampleFlatId).map(_.flatId)
    }

    "returns None for unknown flatId" in new Wiring with Data {
      val sampleFlatId: String = "unknown"
      val result: Option[FlatQuestionnaire] = flatQuestionnaireDao.findByFlatId(sampleFlatId).futureValue
      result shouldBe None
    }
  }

  "FlatQuestionnaireDao.findByFlatIds" should {
    "returns successful result" in new Wiring with Data {
      val sampleFlatIds: Set[String] = sampleFlats.zipWithIndex.filter(_._2 % 2 == 0).map(_._1.flatId).toSet
      val result: Seq[FlatQuestionnaire] = flatQuestionnaireDao.findByFlatIds(sampleFlatIds).futureValue
      result.size shouldEqual sampleFlatIds.size
      result should contain theSameElementsAs questionnaires.filter(f => sampleFlatIds.contains(f.flatId))
    }

    "returns empty for unknown flatIds" in new Wiring with Data {
      val sampleFlatIds: Set[String] = Set("unknown-0", "unknown-1")
      val result: Seq[FlatQuestionnaire] = flatQuestionnaireDao.findByFlatIds(sampleFlatIds).futureValue
      result shouldEqual Nil
    }
  }

  trait Wiring {
    val flatDbActions = new FlatDbActionsImpl()
    val contractDbActions = new RentContractDbActionsImpl()
    val paymentDbActions = new PaymentDbActionsImpl()
    val userDbActions = new UserDbActionsImpl()
    val flatShowingDbActions = new FlatShowingDbActionsImpl()
    val keysHandoverDbActions = new KeysHandoverDbActionsImpl()
    val flatQuestionnaireDbActions = new FlatQuestionnaireDbActionsImpl()

    val flatDao = new FlatDaoImpl(
      flatDbActions,
      contractDbActions,
      paymentDbActions,
      ownerRequestDbActions,
      periodDbActions,
      meterReadingsDbActions,
      houseServiceDbActions,
      flatShowingDbActions,
      keysHandoverDbActions,
      flatQuestionnaireDbActions,
      masterSlaveDb2,
      daoMetrics
    )
    val flatQuestionnaireDao = new FlatQuestionnaireDaoImpl(flatQuestionnaireDbActions, masterSlaveDb2, daoMetrics)
  }

  trait Data extends RentModelsGen {
    this: Wiring =>

    val sampleFlats: Seq[Flat] = {
      val src = Seq(
        ("f-0", "addr-0"),
        ("f-1", "addr-1"),
        ("f-2", "addr-2"),
        ("f-3", "addr-3"),
        ("f-4", "addr-4"),
        ("f-5", "addr-5"),
        ("f-6", "addr-6"),
        ("f-7", "addr-7"),
        ("f-8", "addr-8"),
        ("f-9", "addr-9")
      )
      flatGen().next(src.length).toSeq.zip(src).map {
        case (f, (id, addr)) =>
          f.copy(flatId = id, address = addr, unifiedAddress = Some(addr))
      }
    }
    Future.sequence(sampleFlats.map(flatDao.create)).futureValue

    val questionnaires: Iterable[FlatQuestionnaire] = {
      flatQuestionnaireGen.next(sampleFlats.length).zip(sampleFlats).map {
        case (q, f) => q.copy(flatId = f.flatId)
      }
    }
    Future.sequence(questionnaires.map(flatQuestionnaireDao.insert)).futureValue

  }

}
