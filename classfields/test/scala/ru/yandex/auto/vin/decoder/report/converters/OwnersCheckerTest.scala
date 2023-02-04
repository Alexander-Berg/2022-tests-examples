package ru.yandex.auto.vin.decoder.report.converters

import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.VinResolutionEnums
import ru.yandex.auto.vin.decoder.report.converters.OwnersChecker.ops._
import ru.yandex.vertis.mockito.MockitoSupport

class OwnersCheckerTest extends AnyFunSuite with MockitoSupport {

  test("When offer owners is 1 and gibdd owners is 1 then status should be ok") {
    assert(OwnersChecker(1, Some(1)).status[VinResolutionEnums.Status] == VinResolutionEnums.Status.OK)
  }

  test("When offer owners is 2 and gibdd owners is 2 then status should be ok") {
    assert(OwnersChecker(2, Some(2)).status[VinResolutionEnums.Status] == VinResolutionEnums.Status.OK)
  }

  test("When offer owners is 3 and gibdd owners is 1 then status should be unknown") {
    assert(OwnersChecker(3, Some(1)).status[VinResolutionEnums.Status] == VinResolutionEnums.Status.UNKNOWN)
  }

  test("When offer owners is 3 and gibdd owners is 2 then status should be unknown") {
    assert(OwnersChecker(3, Some(2)).status[VinResolutionEnums.Status] == VinResolutionEnums.Status.UNKNOWN)
  }

  test("When offer owners is 4 and gibdd owners is 1 then status should be unknown") {
    assert(OwnersChecker(4, Some(1)).status[VinResolutionEnums.Status] == VinResolutionEnums.Status.UNKNOWN)
  }

  test("When offer owners is 4 and gibdd owners is 2 then status should be unknown") {
    assert(OwnersChecker(4, Some(2)).status[VinResolutionEnums.Status] == VinResolutionEnums.Status.UNKNOWN)
  }

  test("When offer owners is 1 and gibdd owners is greater then 1 then status should be unknown") {
    assert(OwnersChecker(1, Some(2)).status[VinResolutionEnums.Status] == VinResolutionEnums.Status.UNKNOWN)
  }

  test("When offer owners is 2 and gibdd owners is greater then 2 then status should be unknown") {
    assert(OwnersChecker(2, Some(3)).status[VinResolutionEnums.Status] == VinResolutionEnums.Status.UNKNOWN)
  }

  test("When offer owners is 3 and gibdd owners is greater or equal 3 then status should be ok") {
    assert(OwnersChecker(3, Some(3)).status[VinResolutionEnums.Status] == VinResolutionEnums.Status.OK)
  }

  test("When offer owners is 4 and gibdd owners is greater or equal 4 then status should be ok") {
    assert(OwnersChecker(4, Some(4)).status[VinResolutionEnums.Status] == VinResolutionEnums.Status.OK)
  }

  test("When offer owners is greater then gibdd owners then status should be ok") {
    assert(OwnersChecker(6, Some(5)).status[VinResolutionEnums.Status] == VinResolutionEnums.Status.OK)
  }

}
