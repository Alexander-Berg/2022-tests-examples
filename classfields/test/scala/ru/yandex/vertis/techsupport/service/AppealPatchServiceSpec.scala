package ru.yandex.vertis.vsquality.techsupport.service

import cats.Id
import ru.yandex.vertis.vsquality.techsupport.dao.AppealDao.AppealPatch
import ru.yandex.vertis.vsquality.techsupport.model.{Appeal, Request}
import ru.yandex.vertis.vsquality.techsupport.service.AppealPatchService.PatchError
import ru.yandex.vertis.vsquality.techsupport.service.impl.AppealPatchServiceImpl
import ru.yandex.vertis.vsquality.techsupport.util.SpecBase

/**
  * @author potseluev
  */
class AppealPatchServiceSpec extends SpecBase {

  import ru.yandex.vertis.vsquality.techsupport.Arbitraries._
  import ru.yandex.vertis.vsquality.techsupport.CoreArbitraries._

  private val patchCalculator: AppealPatchCalculator[Id] = mock[AppealPatchCalculator[Id]]
  private val patcher: AppealPatcher[Id] = mock[AppealPatcher[Id]]
  private val factory: AppealFactory[Id] = mock[AppealFactory[Id]]
  private val patchService: AppealPatchService[Id] = new AppealPatchServiceImpl(patchCalculator, patcher, factory)

  "AppealPatchService" should {
    "fail when request is not ProcessMessage and old appeal doesn't exist" in {
      val request = generate[Request.TechsupportAppeal](!_.isInstanceOf[Request.TechsupportAppeal.ProcessMessage])
      val result = patchService.patch(appeal = None, request)
      result shouldBe Left(PatchError.EmptyAppeal)
    }

    "create new appeal when if there is no old one and request is ProcessMessage" in {
      val request = generate[Request.TechsupportAppeal.ProcessMessage]()
      val factorySource = AppealFactory.AppealSource.fromRequest(request)
      val factoryResult = generate[AppealFactory.Result]()
      stub(factory.createAppeal _) { case `factorySource` => factoryResult }
      val expectedResult = AppealPatchService.PatchResult(
        patch = factoryResult.patch,
        patchedAppeal = factoryResult.appeal
      )
      val actualResult = patchService.patch(appeal = None, request)
      actualResult shouldBe Right(expectedResult)
    }

    "return correct result when old appeal existed" in {
      val request = generate[Request.TechsupportAppeal]()
      val oldAppeal = generate[Appeal]()
      val patch = generate[AppealPatch]()
      stub(patchCalculator.calculatePatch(_, _)) { case (`oldAppeal`, `request`) => patch }
      val patchedAppeal = generate[Appeal]()
      stub(patcher.applyPatch(_, _)) { case (`oldAppeal`, `patch`) => Right(patchedAppeal) }
      val expectedResult = AppealPatchService.PatchResult(patch, patchedAppeal)
      val actualResult = patchService.patch(Some(oldAppeal), request)
      actualResult shouldBe Right(expectedResult)
    }
  }
}
