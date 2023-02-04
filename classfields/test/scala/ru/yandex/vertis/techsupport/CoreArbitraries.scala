package ru.yandex.vertis.vsquality.techsupport

import org.scalacheck.derive.MkArbitrary
import org.scalacheck.{Arbitrary, Gen}
import ru.yandex.vertis.vsquality.techsupport.dao.AppealDao
import ru.yandex.vertis.vsquality.techsupport.service.{AppealFactory, ChatService}
import ru.yandex.vertis.vsquality.techsupport.service.AppealPatchService
import ru.yandex.vertis.vsquality.techsupport.service.bot.impl.ExternalGraphScenario
import ru.yandex.vertis.vsquality.techsupport.service.bot.{BotScenario, CompositeScenario}

object CoreArbitraries {
  import ru.yandex.vertis.vsquality.techsupport.Arbitraries._

  implicit lazy val AppealDaoConversationPatchArb: Arbitrary[AppealDao.ConversationPatch] =
    MkArbitrary[AppealDao.ConversationPatch].arbitrary

  implicit lazy val AppealDaoAppealPatchArb: Arbitrary[AppealDao.AppealPatch] =
    MkArbitrary[AppealDao.AppealPatch].arbitrary
  implicit lazy val AppealDaoSortArb: Arbitrary[AppealDao.Sort] = MkArbitrary[AppealDao.Sort].arbitrary

  implicit lazy val PatchResultArb: Arbitrary[AppealPatchService.PatchResult] =
    MkArbitrary[AppealPatchService.PatchResult].arbitrary

  implicit lazy val AppealFactoryResultArb: Arbitrary[AppealFactory.Result] =
    MkArbitrary[AppealFactory.Result].arbitrary

  implicit lazy val ChatServicesEnvelopeArb: Arbitrary[ChatService.Envelope] =
    MkArbitrary[ChatService.Envelope].arbitrary

  implicit lazy val BotScenarioBasicActionArb: Arbitrary[BotScenario.BasicAction] =
    MkArbitrary[BotScenario.BasicAction].arbitrary

  implicit lazy val CompositeScenarioCompositeStateArb: Arbitrary[CompositeScenario.CompositeState] =
    MkArbitrary[CompositeScenario.CompositeState].arbitrary

  implicit lazy val ExternalGraphScenarioNodeArb: Arbitrary[ExternalGraphScenario.Node] =
    MkArbitrary[ExternalGraphScenario.Node].arbitrary
}
