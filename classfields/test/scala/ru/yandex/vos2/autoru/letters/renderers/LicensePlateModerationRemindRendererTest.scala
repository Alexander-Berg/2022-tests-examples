package ru.yandex.vos2.autoru.letters.renderers

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport._
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.OfferModel.OfferFlag
import ru.yandex.vos2.autoru.catalog.cars.CarsCatalog
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.autoru.model.extdata.LicensePlateValidationDictionary
import ru.yandex.vos2.autoru.model.extdata.LicensePlateValidationDictionary.Settings
import ru.yandex.vos2.autoru.utils.geo.Tree

@RunWith(classOf[JUnitRunner])
class LicensePlateModerationRemindRendererTest extends AnyFunSuite {
  implicit val trace = Traced.empty

  private val carsCatalog = mock[CarsCatalog]
  private val regionTree = mock[Tree]
  private val validationDictionary = mock[LicensePlateValidationDictionary]
  when(validationDictionary.getSettings(?, ?))
    .thenReturn(Settings("", "", "", "", "", sendingActive = false, hidingVinReportActive = false))
  private val isSpamalotFeature: Feature[Boolean] = mock[Feature[Boolean]]
  when(isSpamalotFeature.value).thenReturn(true)

  test("Create notification with SMS template") {

    val notification = buildRenderer(LicensePlateModerationRemindRendererType.Sms)
      .render(TestUtils.createOffer().build())

    assert(notification.chatSupport === None)
    assert(notification.mail === None)
    assert(notification.push === None)
    assert(notification.sms !== None)
  }

  test("Create notification with PUSH template") {

    val notification = buildRenderer(LicensePlateModerationRemindRendererType.Push)
      .render(TestUtils.createOffer().build())

    assert(notification.chatSupport === None)
    assert(notification.mail === None)
    assert(notification.sms === None)
    assert(notification.push !== None)
  }

  test("Templates for notification are not created") {

    val renderTypes = List(LicensePlateModerationRemindRendererType.Push, LicensePlateModerationRemindRendererType.Sms)
    val flags = List(OfferFlag.OF_INACTIVE, OfferFlag.OF_BANNED, OfferFlag.OF_DELETED)

    for {
      renderType <- renderTypes
      renderer = buildRenderer(renderType)
      flag <- flags
    } {
      val notification = renderer.render(TestUtils.createOffer().addFlag(flag).build())

      assert(notification.chatSupport === None)
      assert(notification.sms === None)
      assert(notification.mail === None)
      assert(notification.push === None)
    }
  }

  private def buildRenderer(rendererType: LicensePlateModerationRemindRendererType.Value) =
    new LicensePlateModerationRemindRenderer(
      carsCatalog,
      regionTree,
      validationDictionary,
      rendererType,
      isSpamalotFeature
    )

}
