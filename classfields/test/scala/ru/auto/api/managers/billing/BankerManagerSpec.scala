package ru.auto.api.managers.billing

import com.google.protobuf.util.Timestamps
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.{never, reset, verify}
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.exceptions.AccountNotFoundException
import ru.auto.api.managers.TestRequest
import ru.auto.api.managers.billing.BankerManagerSpec.{UserEmailOrdering, UserPhoneOrdering}
import ru.auto.api.model.AutoruUser
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.gen.BankerModelGenerators
import ru.auto.api.services.billing.BankerClient
import ru.auto.api.services.passport.PassportClient
import ru.auto.api.util.Request
import ru.yandex.passport.model.api.ApiModel.{LoadUserHint, UserResult}
import ru.yandex.passport.model.api.{ApiModel => Passport}
import ru.yandex.vertis.banker.model.ApiModel.{Account, AccountInfo}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.jdk.CollectionConverters.ListHasAsScala

class BankerManagerSpec
  extends BaseSpec
  with MockitoSupport
  with ScalaCheckPropertyChecks
  with BankerModelGenerators
  with TestRequest {

  private val bankerClient = mock[BankerClient]
  private val passportClient = mock[PassportClient]
  private val bankerManager = new BankerManager(bankerClient, passportClient)

  implicit override val request: Request = super.request

  before {
    reset(bankerClient, passportClient)
  }

  private val passportUserWithEmailGen: Gen[Passport.UserResult] =
    PassportUserResultGen.suchThat(!_.getUser.getEmailsList.isEmpty)

  private val passportUserWoEmailGen: Gen[Passport.UserResult] =
    PassportUserResultGen.map { userResult =>
      val builder = userResult.toBuilder
      builder.getUserBuilder.clearEmails()
      builder.build()
    }

  private val passportUserWoAnything: Gen[Passport.UserResult] = for {
    user <- PassportUserResultGen
    builder = user.toBuilder
    _ = builder.getUserBuilder.clearEmails()
    _ = builder.getUserBuilder.clearPhones()
  } yield builder.build()

  private val accountWithEmailAndPhoneGen: Gen[Account] = for {
    email <- readableString
    phone <- readableString
    account <- AccountGen
  } yield {
    val b = account.toBuilder
    b.getPropertiesBuilder.setEmail(email).setPhone(phone)
    b.build()
  }

  private val accountWoEmailGen: Gen[Account] =
    AccountGen.map { account =>
      val b = account.toBuilder
      b.getPropertiesBuilder.clearEmail()
      b.build()
    }

  private val accountWoPhoneGen: Gen[Account] =
    AccountGen.map { account =>
      val b = account.toBuilder
      b.getPropertiesBuilder.clearPhone()
      b.build()
    }

  private val accountWoEmailOrPhoneGen: Gen[Account] =
    Gen.oneOf(accountWoEmailGen, accountWoPhoneGen)

  private def emailAndPhone(passportUser: UserResult): (Option[String], Option[String]) = {
    val passportEmail = passportUser.getUser.getEmailsList.asScala.sorted(UserEmailOrdering).lastOption.map(_.getEmail)
    val passportPhone = passportUser.getUser.getPhonesList.asScala.sorted(UserPhoneOrdering).lastOption.map(_.getPhone)
    (passportEmail, passportPhone)
  }

  private def checkAccountSource(source: Account,
                                 expectedId: String,
                                 user: AutoruUser,
                                 passportUser: Passport.UserResult): Unit = {
    source.getId shouldBe expectedId
    source.getUser shouldBe user.toPlain
    val properties = source.getProperties
    val (expectedEmail, expectedPhone) = emailAndPhone(passportUser)
    properties.getEmail shouldBe expectedEmail.getOrElse("")
    properties.getPhone shouldBe expectedPhone.getOrElse("")
  }

  private def getFirstIfNotEqual(first: Option[String], second: String): Option[String] =
    for {
      p <- first
      if p != second
    } yield p

  private def checkPatch(patch: Account.Patch, account: Account, passportUser: UserResult): Unit = {
    val (passportEmail, passportPhone) = emailAndPhone(passportUser)
    val expectedEmail = getFirstIfNotEqual(passportEmail, account.getProperties.getEmail)
    val expectedPhone = getFirstIfNotEqual(passportPhone, account.getProperties.getPhone)
    val builder = Account.Patch.newBuilder()
    expectedEmail.foreach(builder.getEmailBuilder.setValue)
    expectedPhone.foreach(builder.getPhoneBuilder.setValue)
    val expectedPatch = builder.build()

    patch.getEmail shouldBe expectedPatch.getEmail
    patch.getPhone shouldBe expectedPatch.getPhone
  }

  "BankerManager.getOrCreateInfo()" should {
    "get info immediately" in {
      forAll(PrivateUserRefGen, ReadableStringGen, AccountInfoGen, BankerDomainGen) {
        (user, accountId, accountInfo, domain) =>
          when(bankerClient.getAccountInfo(user, accountId, domain)).thenReturnF(accountInfo)
          val result = bankerManager.getOrCreateInfo(user, accountId, domain).futureValue
          result shouldBe accountInfo
      }
    }

    "create account and return default info" in {
      forAll(PrivateUserRefGen, ReadableStringGen, AccountGen, passportUserWithEmailGen, BankerDomainGen) {
        (user, accountId, account, passportUser, domain) =>
          when(bankerClient.getAccountInfo(user, accountId, domain)).thenThrowF(new AccountNotFoundException)
          when(bankerClient.createAccount(eq(user), ?, eq(domain))(?)).thenReturnF(account)
          when(passportClient.getUserWithHints(user, Seq(LoadUserHint.SALES_EMAILS))).thenReturnF(passportUser)
          val result = bankerManager.getOrCreateInfo(user, accountId, domain).futureValue
          result shouldBe AccountInfo.getDefaultInstance
      }
    }

    "fail with getInfo() unexpected error" in {
      forAll(PrivateUserRefGen, ReadableStringGen, BankerDomainGen) { (user, accountId, domain) =>
        when(bankerClient.getAccountInfo(user, accountId, domain)).thenThrowF(new Exception)
        val result = bankerManager.getOrCreateInfo(user, accountId, domain).failed.futureValue
        result shouldBe an[Exception]
      }
    }

    "fail with createAccount() unexpected error" in {
      forAll(PrivateUserRefGen, ReadableStringGen, BankerDomainGen) { (user, accountId, domain) =>
        when(bankerClient.getAccountInfo(user, accountId, domain)).thenThrowF(new AccountNotFoundException)
        when(bankerClient.createAccount(eq(user), ?, eq(domain))(?)).thenThrowF(new Exception)
        val result = bankerManager.getOrCreateInfo(user, accountId, domain).failed.futureValue
        result shouldBe an[Exception]
      }
    }
  }

  "BankerManager.createAccount()" should {
    "create account with email from passport" in {
      forAll(PrivateUserRefGen, readableString, AccountGen, PassportUserResultGen, BankerDomainGen) {
        (user, accountId, account, passportUser, domain) =>
          when(passportClient.getUserWithHints(user, Seq(LoadUserHint.SALES_EMAILS))).thenReturnF(passportUser)
          reset(bankerClient)
          when(bankerClient.createAccount(?, ?, ?)(?)).thenReturnF(account)

          val created = bankerManager.createAccount(user, accountId, domain).futureValue
          created shouldBe account

          val accountSourceCaptor: ArgumentCaptor[Account] = ArgumentCaptor.forClass(classOf[Account])
          verify(bankerClient).createAccount(eq(user), accountSourceCaptor.capture(), eq(domain))(eq(request))
          checkAccountSource(
            accountSourceCaptor.getValue,
            accountId,
            user,
            passportUser
          )
      }
    }
    "do not fail if can't get email from passport" in {
      forAll(PrivateUserRefGen, readableString, passportUserWoEmailGen, BankerDomainGen) {
        (user, accountId, passportUser, domain) =>
          when(passportClient.getUserWithHints(user, Seq(LoadUserHint.SALES_EMAILS))).thenReturnF(passportUser)
          val expectedAccount = Account
            .newBuilder()
            .setId(accountId)
            .setUser(user.toPlain)
            .build()
          when(bankerClient.createAccount(?, ?, ?)(?)).thenReturnF(expectedAccount)
          bankerManager.createAccount(user, accountId, domain).futureValue shouldBe expectedAccount

      }
    }
  }

  "BankerManager.checkOrUpdateProperties()" should {
    "update account properties by passport data" in {
      forAll(
        PrivateUserRefGen,
        AccountGen,
        accountWithEmailAndPhoneGen,
        PassportUserResultGen,
        BankerDomainGen
      ) { (user, account, accountWithEmail, passportUser, domain) =>
        reset(bankerClient, passportClient)
        when(passportClient.getUserWithHints(user, Seq(LoadUserHint.SALES_EMAILS))).thenReturnF(passportUser)
        when(bankerClient.getAccounts(user, domain)).thenReturnF(Iterable(account))
        when(bankerClient.updateAccount(?, ?, ?)(?)).thenReturnF(accountWithEmail)

        bankerManager.checkOrUpdateProperties(user, account.getId, domain).futureValue
        val patchCaptor: ArgumentCaptor[Account.Patch] = ArgumentCaptor.forClass(classOf[Account.Patch])
        verify(bankerClient).getAccounts(user, domain)
        if (passportUser.getUser.getEmailsCount == 0 && passportUser.getUser.getPhonesCount == 0) {
          verify(bankerClient, never()).updateAccount(?, ?, ?)(?)
        } else {
          verify(bankerClient).updateAccount(eq(account), patchCaptor.capture(), eq(domain))(eq(request))
          val patch = patchCaptor.getValue
          checkPatch(patch, account, passportUser)
        }
      }
    }
    "create new account with passport data" in {
      forAll(PrivateUserRefGen, accountWithEmailAndPhoneGen, PassportUserResultGen, BankerDomainGen) {
        (user, accountWithEmail, passportUser, domain) =>
          reset(bankerClient, passportClient)
          when(passportClient.getUserWithHints(user, Seq(LoadUserHint.SALES_EMAILS))).thenReturnF(passportUser)
          when(bankerClient.getAccounts(user, domain)).thenReturnF(Iterable.empty)
          when(bankerClient.createAccount(?, ?, ?)(?)).thenReturnF(accountWithEmail)

          bankerManager.checkOrUpdateProperties(user, accountWithEmail.getId, domain).futureValue
          val accountSourceCaptor: ArgumentCaptor[Account] = ArgumentCaptor.forClass(classOf[Account])
          verify(bankerClient).getAccounts(user, domain)
          verify(bankerClient).createAccount(eq(user), accountSourceCaptor.capture(), eq(domain))(eq(request))
          checkAccountSource(
            accountSourceCaptor.getValue,
            accountWithEmail.getId,
            user,
            passportUser
          )
      }
    }
  }
}

object BankerManagerSpec {

  implicit private[BankerManagerSpec] object UserEmailOrdering extends Ordering[Passport.UserEmail] {

    override def compare(x: Passport.UserEmail, y: Passport.UserEmail): Int =
      x.getConfirmed.compare(y.getConfirmed) match {
        case 0 =>
          Timestamps.comparator().compare(x.getAdded, y.getAdded)
        case other => other
      }
  }

  implicit private[BankerManagerSpec] object UserPhoneOrdering extends Ordering[Passport.UserPhone] {

    override def compare(x: Passport.UserPhone, y: Passport.UserPhone): Int =
      Timestamps.comparator().compare(x.getAdded, y.getAdded)
  }

}
