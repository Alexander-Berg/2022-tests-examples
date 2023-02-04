package ru.yandex.vertis.general.gost.logic.test.validation.validators

import common.zio.grpc.client.GrpcClient
import general.users.api.UserServiceGrpc.UserService
import general.users.model.UserView
import ru.yandex.vertis.general.common.cache.{Cache, RequestCacher}
import ru.yandex.vertis.general.gost.logic.testkit.ValidatorTestkit._
import ru.yandex.vertis.general.gost.logic.validation.Validator
import ru.yandex.vertis.general.gost.logic.validation.validators.ContactsValidator
import ru.yandex.vertis.general.gost.model.Offer.Contact
import ru.yandex.vertis.general.gost.model.{OfferOrigin, WayToContact}
import ru.yandex.vertis.general.gost.model.validation.fields.{
  ContactsEmpty,
  IllegalPhoneFormat,
  IllegalPhones,
  PhoneEmpty
}
import ru.yandex.vertis.general.users.testkit.TestUserService
import common.zio.logging.Logging
import zio.ZLayer
import zio.test.Assertion._
import zio.test._

object ContactsValidatorTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("ContactsValidator")(
      validatorTest("Нельзя не указывать контакты")(
        _.copy(contacts = Seq.empty)
      )(contains(ContactsEmpty)),
      validatorTest("Fail when chat is not preferred and phone is empty")(
        _.copy(preferredWayToContact = WayToContact.Any, contacts = Seq(Contact(phone = None)))
      )(contains(PhoneEmpty)),
      validatorTest("Work fine with preferred chat and empty phone")(
        _.copy(preferredWayToContact = WayToContact.Chat, contacts = Seq(Contact(phone = None)))
      )(isEmpty),
      validatorTest("Work fine with unknown phone when chat is preferred")(
        _.copy(preferredWayToContact = WayToContact.Chat, contacts = Seq(Contact(phone = Some("+79455552336"))))
      )(isEmpty),
      validatorTest("Можно использовать подтвержденные телефоны через апи")(
        _.copy(
          origin = OfferOrigin.Form,
          preferredWayToContact = WayToContact.PhoneCall,
          contacts = Seq(Contact(phone = Some(UserPhone)))
        )
      )(isEmpty),
      validatorTest("Нельзя использовать неподтвержденные телефоны через апи")(
        _.copy(
          origin = OfferOrigin.Form,
          preferredWayToContact = WayToContact.PhoneCall,
          contacts = Seq(Contact(phone = Some("+79455552336")))
        )
      )(contains(IllegalPhones(Seq("+79455552336")))),
      validatorTest("Нельзя использовать подтвержденные, но зарубежные телефоны через апи")(
        _.copy(
          origin = OfferOrigin.Form,
          preferredWayToContact = WayToContact.PhoneCall,
          contacts = Seq(Contact(phone = Some(ForeignUserPhone)))
        )
      )(contains(IllegalPhoneFormat)),
      validatorTest("Можно использовать неподтверждённые телефоны в фидах")(
        _.copy(
          origin = OfferOrigin.Feed,
          preferredWayToContact = WayToContact.PhoneCall,
          contacts = Seq(Contact(phone = Some("+79455552336")))
        )
      )(isEmpty),
      validatorTest("Нельзя использовать зарубежные телефоны в фидах")(
        _.copy(
          origin = OfferOrigin.Feed,
          preferredWayToContact = WayToContact.PhoneCall,
          contacts = Seq(Contact(phone = Some("+48862151115")))
        )
      )(contains(IllegalPhoneFormat)),
      validatorTest("Обработка невалидного телефона")(
        _.copy(
          origin = OfferOrigin.Feed,
          preferredWayToContact = WayToContact.PhoneCall,
          contacts = Seq(Contact(phone = Some("невалидный телефон")))
        )
      )(contains(IllegalPhoneFormat))
    ).provideCustomLayerShared {
      val user = UserView(UserId, None, None, None, Seq(UserPhone, ForeignUserPhone), None)

      val validationGrpcRequestCacher = Logging.live ++ Cache.noop >>> RequestCacher.live

      (TestUserService.withUsers(Seq(user)) ++ validationGrpcRequestCacher) >>>
        ZLayer
          .fromServices[RequestCacher.Service, GrpcClient.Service[UserService], Validator](
            new ContactsValidator(_, _)
          )

    }
}
