package ru.yandex.vertis

import ru.yandex.vertis.telepony.model.{ActualRedirect, AppBackCall, AppCall, CallV2, Callback, ObjectId, Tag, TypedDomain}

package object telepony {

  implicit class ActualRedirectOps(redirect: ActualRedirect) {

    import ru.yandex.vertis.telepony.model.Phone

    def withObjectId(objectId: ObjectId): ActualRedirect = redirect.copy(key = redirect.key.copy(objectId = objectId))

    def withTag(tag: Tag): ActualRedirect = redirect.copy(key = redirect.key.copy(tag = tag))

    def withTarget(target: Phone): ActualRedirect = redirect.copy(key = redirect.key.copy(target = target))
  }

  implicit class AppCallOps(call: AppCall) {

    import ru.yandex.vertis.telepony.model.{Phone, RefinedSource, Username}

    def withRedirect(redirect: ActualRedirect): AppCall = call.copy(redirect = redirect.asHistoryRedirect)
    def withSource(source: Phone): AppCall = call.copy(source = Some(RefinedSource(source)))
    def withSourceUsername(username: Username): AppCall = call.copy(sourceUsername = username)
    def withTargetUsername(username: Username): AppCall = call.copy(targetUsername = Some(username))
    def withUuid(uuid: Option[String]): AppCall = call.copy(uuid = uuid)
    def withPayloadJson(payloadJson: Option[String]): AppCall = call.copy(payloadJson = payloadJson)
  }

  implicit class AppBackCallOps(call: AppBackCall) {

    import ru.yandex.vertis.telepony.model.Username

    def withRedirect(redirect: ActualRedirect): AppBackCall = call.copy(redirect = redirect.asHistoryRedirect)
    def withSourceUsername(username: Username): AppBackCall = call.copy(sourceUsername = username)
    def withTargetUsername(username: Username): AppBackCall = call.copy(targetUsername = username)
    def withUuid(uuid: Option[String]): AppBackCall = call.copy(uuid = uuid)
    def withPayloadJson(payloadJson: Option[String]): AppBackCall = call.copy(payloadJson = payloadJson)
  }

  implicit class CallOps(call: CallV2) {

    import ru.yandex.vertis.telepony.model.{Phone, RefinedSource}

    def withRedirect(redirect: ActualRedirect): CallV2 = call.copy(redirect = redirect.asHistoryRedirect)
    def withSource(source: Phone): CallV2 = call.copy(source = Some(RefinedSource(source)))
  }

  implicit class CallbackOps(call: Callback) {
    import ru.yandex.vertis.telepony.model.Phone

    def withObjectId(objectId: ObjectId): Callback = call.copy(order = call.order.copy(objectId = objectId))

    def withDomain(domain: TypedDomain): Callback = call.copy(order = call.order.copy(domain = domain))

    def withTag(tag: Tag): Callback = call.copy(order = call.order.copy(tag = tag))

    def withSource(source: Phone): Callback =
      call.copy(order = call.order.copy(source = call.order.source.copy(number = source)))

    def withTarget(target: Phone): Callback =
      call.copy(order = call.order.copy(target = call.order.target.copy(number = target)))

  }

}
