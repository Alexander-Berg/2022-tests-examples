package ru.yandex.realty.mail.sender.model.push

import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.model.offer.CategoryType
import ru.yandex.realty.pushnoy.model.{MetrikaPushId, PushActionType, PushTestUtils}

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class UserInactivityPushInfoSpec extends FlatSpec with Matchers {

  classOf[UserInactivityPushInfo].getName should "serialize to json correctly" in {
    PushTestUtils.checkV4Push(
      PushActionType.Open,
      MetrikaPushId.SimilarSearch,
      url = None,
      customData = Map.empty
    )(UserInactivityPushInfo("not used", CategoryType.HOUSE))
  }

}
