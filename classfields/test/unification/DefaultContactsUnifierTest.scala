package ru.yandex.vertis.general.feed.processor.pipeline.test.unification

import general.feed.transformer.{RawContacts, RawOffer, RawSeller}
import general.users.model.{LimitedUserView, User}
import ru.yandex.vertis.general.feed.processor.model.IncorrectPhone
import ru.yandex.vertis.general.feed.processor.pipeline.unification.ContactsUnifier
import zio.test.Assertion._
import zio.test._

object DefaultContactsUnifierTest extends DefaultRunnableSpec {

  val rawGoodNumber = "+ 7 (903) 49 - 312-22"
  val rawBadNumber = "7 lol 123"
  val normalizedNumber = "+79034931222"

  def getRawOffer(pnone: String) = RawOffer(seller = Some(RawSeller(contacts = Some(RawContacts(phone = pnone)))))
  def getUser(phone: String) = LimitedUserView(user = Some(User(ymlPhone = Some(phone))))

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("DefaultContactsUnifier")(
      testM("успешная унификация номера телефона из объявления") {
        ContactsUnifier
          .unifyContacts(getRawOffer(rawGoodNumber))
          .map { result =>
            assert(result.field.map(_.phone))(isSome(equalTo(normalizedNumber)))
          }
      },
      testM("успешная унификация номера телефона из информации о пользователе") {
        ContactsUnifier
          .unifyContacts(getUser(rawGoodNumber))
          .map { result =>
            assert(result.field.map(_.phone))(isSome(equalTo(normalizedNumber)))
          }
      },
      testM("ошибка унификации номера телефона из объявления") {
        ContactsUnifier
          .unifyContacts(getRawOffer(rawBadNumber))
          .map { result =>
            assert(result.errors.headOption)(isSome(isSubtype[IncorrectPhone](anything)))
          }
      },
      testM("ошибка унификации номера телефона из информации о пользователе") {
        for {
          result <- ContactsUnifier
            .unifyContacts(getUser(rawBadNumber))
            .run
        } yield assert(result)(
          dies(hasMessage(containsString("The string supplied did not seem to be a phone number")))
        )
      }
    )
  }.provideCustomLayerShared {
    ContactsUnifier.live
  }

}
