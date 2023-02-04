package ru.yandex.vertis.telepony.service.impl

import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.model.{AntiFraudOptions, AonBlockInfo, BlockInfo, BlockReasons, PrefixBlacklist, RefinedSource}
import ru.yandex.vertis.telepony.model.AntiFraudOptions._
import ru.yandex.vertis.telepony.service.{BlacklistService, DomainAonBlacklistProvider}

import scala.concurrent.Future

class ResolutionServiceImplSpec extends SpecBase with MockitoSupport {

  val source: RefinedSource = RefinedSourceGen.next

  def mockBlacklistService(blockInfo: Seq[BlockInfo]): BlacklistService = {
    val blackListService = mock[BlacklistService]
    when(blackListService.getList(?)).thenReturn(Future.successful(blockInfo))
    blackListService
  }

  def mockDomainAonBlacklistProvider(blockInfo: Option[AonBlockInfo]): DomainAonBlacklistProvider = {
    val domainAonBlackListService = mock[DomainAonBlacklistProvider]
    when(domainAonBlackListService.get(?)).thenReturn(Future.successful(blockInfo))
    domainAonBlackListService
  }

  def mockPrefixBlacklist(matched: Boolean): PrefixBlacklist = {
    val prefixBlacklist = mock[PrefixBlacklist]
    when(prefixBlacklist.hasMatch(?)).thenReturn(matched)
    prefixBlacklist
  }

  val passBlacklistService: BlacklistService = mockBlacklistService(Seq.empty)
  val passDomainAonBlacklistProvider: DomainAonBlacklistProvider = mockDomainAonBlacklistProvider(None)
  val passPrefixBlacklist: PrefixBlacklist = mockPrefixBlacklist(false)

  "ResolutionService" should {
    "pass when no block info " in {
      val resolutionService =
        new ResolutionServiceImpl(passBlacklistService, passDomainAonBlacklistProvider, passPrefixBlacklist)
      val resolutions = resolutionService.getBlockResolutions(source).futureValue
      resolutions shouldBe empty
    }

    "block by prefix blacklist" in {
      val prefixBlacklist = mockPrefixBlacklist(true)
      val resolutionService =
        new ResolutionServiceImpl(passBlacklistService, passDomainAonBlacklistProvider, prefixBlacklist)
      val resolutions = resolutionService.getBlockResolutions(source).futureValue
      resolutions should contain theSameElementsAs Set(Blacklist)
    }

    "block by blacklist block info" in {
      val blockInfo = BlockInfoGen.next.copy(source = source, antiFraud = AntiFraudOptions.Blacklist)
      val blacklistService = mockBlacklistService(Seq(blockInfo))
      val resolutionService =
        new ResolutionServiceImpl(blacklistService, passDomainAonBlacklistProvider, passPrefixBlacklist)
      val resolutions = resolutionService.getBlockResolutions(source).futureValue
      resolutions should contain theSameElementsAs Set(Blacklist)
    }

    "block by calls counter block info" in {
      val blockInfo = BlockInfoGen.next.copy(source = source, antiFraud = AntiFraudOptions.CallsCounter)
      val blacklistService = mockBlacklistService(Seq(blockInfo))
      val resolutionService =
        new ResolutionServiceImpl(blacklistService, passDomainAonBlacklistProvider, passPrefixBlacklist)
      val resolutions = resolutionService.getBlockResolutions(source).futureValue
      resolutions should contain theSameElementsAs Set(CallsCounter)
    }

    "block by aon block info" in {
      val blockInfo = AonBlockInfoGen.next.copy(source = source)
      val aonBlacklistService = mockDomainAonBlacklistProvider(Some(blockInfo))
      val resolutionService =
        new ResolutionServiceImpl(passBlacklistService, aonBlacklistService, passPrefixBlacklist)
      val resolutions = resolutionService.getBlockResolutions(source).futureValue
      resolutions should contain theSameElementsAs Set(AonBlacklist)
    }

    "block with all reasons" in {
      val blockInfo1 = BlockInfoGen.next.copy(source = source, antiFraud = AntiFraudOptions.Blacklist)
      val blockInfo2 = BlockInfoGen.next.copy(source = source, antiFraud = AntiFraudOptions.CallsCounter)
      val blacklistService = mockBlacklistService(Seq(blockInfo1, blockInfo2))

      val aonBlockInfo = AonBlockInfoGen.next.copy(source = source)
      val aonBlacklistService = mockDomainAonBlacklistProvider(Some(aonBlockInfo))

      val resolutionService =
        new ResolutionServiceImpl(blacklistService, aonBlacklistService, passPrefixBlacklist)
      val resolutions = resolutionService.getBlockResolutions(source).futureValue
      resolutions should contain theSameElementsAs Set(Blacklist, AonBlacklist, CallsCounter)
    }
  }
}
