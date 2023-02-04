package ru.yandex.vertis.passport.test

import com.google.common.io.BaseEncoding
import com.google.common.net.InetAddresses
import org.joda.time.{DateTime, DateTimeZone, Instant, LocalDate, Duration => JDuration}
import org.scalacheck.Gen.{alphaNumStr, Choose}
import org.scalacheck.{Arbitrary, Gen}
import ru.yandex.passport.model.api.ApiModel.{ApiTokenCreateParams, ApiTokenPayload, ApiTokenResult, MosRuPayload, SocialUserPayload}
import ru.yandex.vertis.generators.DateTimeGenerators
import ru.yandex.vertis.moderation.proto.Model.DetailedReason.Details.UserReseller.ResellerType
import ru.yandex.vertis.passport.model.FieldPatch.OptField
import ru.yandex.vertis.passport.model._
import ru.yandex.vertis.passport.model.api._
import ru.yandex.vertis.passport.model.tokens._
import ru.yandex.vertis.passport.test.Producer._
import ru.yandex.vertis.passport.util.DateTimeUtils.toJodaDuration
import ru.yandex.vertis.passport.util.lang.BooleanOption
import ru.yandex.vertis.passport.{model, Domain, Domains}
import ru.yandex.vertis.protobuf.BasicProtoFormats
import ru.yandex.vertis.tracing.Traced

import java.net.InetAddress
import scala.collection.JavaConverters._
import scala.concurrent.duration.{DurationLong, FiniteDuration}

/**
  * Generators for model classes
  * todo move it somewhere
  *
  * @author zvez
  */
//noinspection TypeAnnotation
object ModelGenerators extends BasicProtoFormats {

  implicit val chooseDuration: Choose[FiniteDuration] =
    Choose.xmap[Long, FiniteDuration](_.millis, _.toMillis)

  val userId = Gen.choose(1, 20000000).map(_.toString)

  val clientId = Gen.choose(1, 20000000).map(_.toString)

  val readableString: Gen[String] = for {
    length <- Gen.choose(5, 10)
  } yield Gen.alphaNumChar.next(length).mkString

  val resellerTypeGen: Gen[ResellerType] = Gen.oneOf(ResellerType.values.toSeq)

  // https://en.wikipedia.org/wiki/UTF-16#Code_points_U.2B10000..U.2B10FFFF
  val surrogatePairs: Gen[String] =
    Gen.choose(0x10000, 0x10ffff).map { code =>
      val n = code - 0x10000
      val builder = new StringBuilder
      builder += (0xd800 + (n >> 10)).toChar
      builder += (0xdc00 + (n & 0x3ff)).toChar
      builder.toString()
    }

  val stringWithSurrogates: Gen[String] = for {
    length <- Gen.choose(5, 10)
  } yield Gen.oneOf(Gen.alphaChar.map(_.toString), surrogatePairs).next(length).mkString

  val bool: Gen[Boolean] = Gen.oneOf(true, false)

  def seq[T](elementGen: Gen[T], min: Int = 0, max: Int = 5): Gen[Seq[T]] =
    for {
      count <- Gen.choose(min, max)
      res <- Gen.listOfN(count, elementGen)
    } yield res

  val deviceUid = readableString

  val sessionOwner = Gen.oneOf(
    userId.map(SessionUser),
    deviceUid.map(SessionAnonym)
  )

  val mailServer = for {
    host <- readableString.map(_.toLowerCase)
    domain <- Gen.oneOf("ru", "kz", "com")
  } yield host + "." + domain

  val emailAddress: Gen[String] = for {
    server <- mailServer
    address <- readableString.map(_.toLowerCase)
  } yield s"$address@$server"

  val emailYandexNonNormalizedAddress: Gen[String] = for {
    domain <- Gen.oneOf("kz", "com")
    address <- readableString.map(_.toLowerCase)
  } yield s"$address@yandex.$domain"

  val ipv4Address =
    Gen
      .choose(Int.MinValue, Int.MaxValue)
      .map(InetAddresses.fromInteger)
      .map(InetAddresses.toAddrString)

  val ipv6Address =
    Gen
      .listOfN(16, Gen.choose(Byte.MinValue, Byte.MaxValue))
      .map(bytes => InetAddress.getByAddress(bytes.toArray))
      .map(InetAddresses.toAddrString)

  val ipAddress = Gen.oneOf(ipv4Address, ipv6Address)

  val domain = Gen.oneOf(Domains.values.toSeq)

  val userEssentials = for {
    id <- userId
    email <- Gen.option(emailAddress)
    phones <- seq(phoneNumber)
    alias <- Gen.option(readableString)
    fullName <- Gen.option(readableString)
    userpic <- Gen.option(Gen.numStr)
    pwdHash <- Gen.option(Gen.numStr)
    ip <- Gen.option(ipAddress)
    clientId <- Gen.option(userId)
    lastSeen <- Gen.option(DateTime.now())
    allowOffersShow <- Gen.option(bool)
  } yield UserEssentials(
    id = id,
    email = email,
    alias = alias,
    fullName = fullName,
    userpic = userpic,
    pwdHash = pwdHash,
    lastSessionsDropTs = None,
    registrationDate = None,
    registrationIp = ip,
    clientId = clientId,
    clientGroup = None,
    lastSeen = lastSeen,
    yandexStaffLogin = None,
    yandexSocial = None,
    moderationStatus = None,
    socialProfiles = Nil,
    phones = Some(phones),
    allowOffersShow = allowOffersShow
  )

  val instantInPast = for {
    v <- Gen.choose(60.seconds, 7.days)
  } yield Instant.now().minus(v)

  val instantInFuture = for {
    v <- Gen.choose(60.seconds, 7.days)
  } yield Instant.now().plus(v)

  val dateInPast = for {
    v <- Gen.choose(0, System.currentTimeMillis())
  } yield new DateTime(v).withMillisOfSecond(0).withZone(DateTimeZone.getDefault)

  val ttl = for {
    v <- Gen.choose(1.days, 14.days)
  } yield toJodaDuration(v)

  val randomPartLegacy = for {
    length <- Gen.choose(10, 20)
    bytes = Gen.Choose.chooseByte.choose(Byte.MinValue, Byte.MaxValue).next(length)
  } yield BaseEncoding.base32Hex().omitPadding().encode(bytes.toArray)

  val randomPart = for {
    length <- Gen.choose(10, 20)
    bytes = Gen.Choose.chooseByte.choose(Byte.MinValue, Byte.MaxValue).next(length)
  } yield BaseEncoding.base64Url().omitPadding().encode(bytes.toArray)

  val legacySessionId = for {
    userId <- userId
    randomPart <- randomPartLegacy
  } yield LegacySessionId(userId, randomPart)

  val simpleSessionId = for {
    userId <- userId
    randomPart <- randomPart
  } yield SimpleSessionId(userId, randomPart)

  val sessionTtl: Gen[JDuration] = Gen.choose(1.minute, 90.days).map(_.toSeconds.seconds)

  val richSessionId = for {
    userId <- userId
    creationDate <- instantInPast
    expireDate <- instantInFuture
    rndPart <- randomPart
    signature <- randomPart
    ttl = JDuration.standardSeconds((expireDate.getMillis - creationDate.getMillis) / 1000)
  } yield RichSessionId(SessionUser(userId), creationDate, ttl, rndPart, signature)

  val fakeSessionId = Gen.oneOf(legacySessionId, simpleSessionId, richSessionId)

  val sessionData = for {
    size <- Gen.choose(0, 5)
    map <- Gen.option(Gen.mapOfN(size, Gen.zip(readableString, readableString)))
  } yield map

  val someSessionData = for {
    size <- Gen.choose(0, 5)
    map <- Gen.some(Gen.mapOfN(size, Gen.zip(readableString, readableString)))
  } yield map

  val parentSession = for {
    sid <- fakeSessionId
    userId <- userId
    creationDate <- instantInPast
    expireDate <- instantInFuture
    ttl = JDuration.standardSeconds((expireDate.getMillis - creationDate.getMillis) / 1000)
  } yield ParentSession(
    id = sid,
    userId = userId,
    creationDate = creationDate,
    ttl = ttl
  )

  val yaSession = for {
    sessionId <- alphaNumStr
    userId <- userId
    creationTimestamp <- instantInPast
    expireTimestamp <- instantInFuture
  } yield YandexSession(
    id = sessionId,
    userId = userId,
    creationTimestamp = creationTimestamp,
    expireTimestamp = expireTimestamp,
    ttl = YandexSessionTtl.Long,
    status = YandexSessionStatus.Valid
  )

  val yaSessionOpt = Gen.option(yaSession)

  val yaTokenAuth = for {
    token <- alphaNumStr
  } yield YandexTokenAuth(token)

  val yaTokenAuthOpt = Gen.option(yaTokenAuth)

  val session = for {
    userId <- Gen.option(userId)
    creationDate <- instantInPast
    expireDate <- instantInFuture
    data <- sessionData
    ttl = JDuration.standardSeconds((expireDate.getMillis - creationDate.getMillis) / 1000)
    sid <- fakeSessionId
    uid <- deviceUid
    parentSession <- Gen.option(parentSession)
    returnPath <- Gen.option(readableString)
    yandexSession <- Gen.option(yaSession)
  } yield Session(
    id = sid,
    userId = userId,
    deviceUid = uid,
    creationDate = creationDate,
    ttl = ttl,
    data = data,
    parentSession = parentSession,
    returnPath = returnPath,
    yandexSession = yandexSession
  )

  val userSession: Gen[Session] = session.filter(_.userId.isDefined)

  val basicSessionData: Gen[BasicSessionData] = session

  val userProfile: Gen[AutoruUserProfile] = for {
    name <- Gen.option(readableString)
    userpic <- Gen.option(readableString)
    about <- Gen.option(readableString)
    age <- Gen.option(Gen.choose(10, 100))
    showCard <- bool
    showMail <- bool
    allowMessages <- bool
    fullName <- Gen.option(readableString)
    drivingYear <- Gen.option(Gen.chooseNum(1, 2100))
    countryId <- Gen.option(Gen.choose(1, 10000L))
    regionId <- Gen.option(Gen.choose(1, 10000L))
    cityId <- Gen.option(Gen.choose(1, 10000L))
    passwordLogin <- Gen.option(bool)
    allowOffersShow <- Gen.option(bool)
  } yield AutoruUserProfile(
    alias = name,
    userpic = userpic,
    about = about,
    birthday = age.map(age => LocalDate.now().minusYears(age)),
    showCard = showCard,
    showMail = showMail,
    allowMessages = allowMessages,
    fullName = fullName,
    drivingYear = drivingYear,
    countryId = countryId,
    regionId = regionId,
    cityId = cityId,
    passwordLogin = passwordLogin,
    allowOffersShow = allowOffersShow
  )

  val phoneNumber: Gen[Phone] =
    Gen.choose(70000000000L, 79999999999L).map(v => v.toString)

  val userEmail: Gen[UserEmail] = for {
    email <- emailAddress
    confirmed <- bool
  } yield UserEmail(email, confirmed)

  val userPhone: Gen[UserPhone] = for {
    phone <- phoneNumber
    added <- Gen.option(dateInPast)
  } yield UserPhone(phone, added = added)

  val socialProviderUser = for {
    id <- readableString
    qt <- Gen.choose(0, 2)
    email <- Gen.listOfN(qt, emailAddress)
    //   qt2 <- Gen.choose(0, 2)
    //   phones <- Gen.listOfN(qt2, phoneNumber)
    nickname <- Gen.option(readableString)
    firstName <- Gen.option(readableString)
    lastName <- Gen.option(readableString)
    avatar <- Gen.option(readableString)
    country <- Gen.option(readableString)
    city <- Gen.option(readableString)
    birthday <- Gen.option(dateInPast.map(_.toLocalDate))
    trusted <- bool
  } yield SocialUser(
    id = id,
    emails = email,
    phones = Nil,
    nickname = nickname,
    firstName = firstName,
    lastName = lastName,
    avatar = avatar,
    country = country,
    city = city,
    birthday = birthday,
    trusted = trusted
  )

  val socialUserPhone: Gen[SocialUserPhone] = for {
    phone <- phoneNumber
    added <- Gen.option(dateInPast)
  } yield SocialUserPhone(phone, added)

  val socialUserSource = for {
    id <- readableString.suchThat(_.nonEmpty)
    email <- Gen.option(emailAddress)
    phones <- seq(socialUserPhone)
    nickname <- Gen.option(readableString)
    firstName <- Gen.option(readableString)
    lastName <- Gen.option(readableString)
    avatar <- Gen.option(readableString)
    country <- Gen.option(readableString)
    city <- Gen.option(readableString)
    birthday <- Gen.option(dateInPast.map(_.toLocalDate))
  } yield SocialUserSource(
    id = id,
    emails = email.toSeq,
    phones = phones,
    nickname = nickname,
    firstName = firstName,
    lastName = lastName,
    avatar = avatar,
    country = country,
    city = city,
    birthday = birthday
  )

  val socialProvider = Gen.oneOf(SocialProviders.values.toSeq)

  val userSocialProfile = for {
    provider <- socialProvider
    user <- socialProviderUser
    added <- dateInPast
    activated <- bool
    activateDate <- Gen.option(dateInPast)
  } yield UserSocialProfile(
    provider = provider,
    socialUser = user,
    added = added,
    activated = activated,
    activateDate = activateDate
  )

  val fullUser: Gen[FullUser] = for {
    userId <- userId
    profile <- userProfile
    pwdHash <- readableString
    hashingStrategy <- Gen.oneOf(PasswordHashingStrategies.values.toSeq)
    passwordDate <- Gen.option(instantInPast.map(_.toDateTime))
    active <- bool
    emails <- Gen.option(userEmail).map(_.toSeq)
    phonesCount <- Gen.choose(0, 5)
    phones <- Gen.listOfN(phonesCount, userPhone)
    socialCount <- Gen.choose(0, 5)
    ip <- Gen.option(ipAddress)
    socialProfiles <- Gen.listOfN(socialCount, userSocialProfile)
  } yield FullUser(
    id = userId,
    pwdHash = Some(pwdHash),
    hashingStrategy = hashingStrategy,
    passwordDate = passwordDate,
    profile = profile,
    emails = emails,
    phones = phones,
    socialProfiles = socialProfiles,
    registrationDate = DateTime.now(),
    registrationIp = ip,
    updated = DateTime.now(),
    active = active
  )

  val fullUserWithAllCredentials: Gen[FullUser] = for {
    userId <- userId
    profile <- userProfile
    pwdHash <- readableString
    hashingStrategy <- Gen.oneOf(PasswordHashingStrategies.values.toSeq)
    passwordDate <- Gen.option(instantInPast.map(_.toDateTime))
    active <- bool
    email <- userEmail.filter(_.confirmed == true)
    phonesCount <- Gen.choose(5, 10)
    phones <- Gen.listOfN(phonesCount, userPhone)
    socialCount <- Gen.choose(5, 10)
    ip <- Gen.option(ipAddress)
    socialProfiles <- Gen.listOfN(socialCount, userSocialProfile)
  } yield FullUser(
    id = userId,
    pwdHash = Some(pwdHash),
    hashingStrategy = hashingStrategy,
    passwordDate = passwordDate,
    profile = profile,
    emails = Seq(email),
    phones = phones,
    socialProfiles = socialProfiles,
    registrationDate = DateTime.now(),
    registrationIp = ip,
    updated = DateTime.now(),
    active = active
  )

  val userAuthToken: Gen[UserAuthToken] = for {
    id <- Gen.identifier.filter(_.nonEmpty)
    userId <- userId
    ttl <- chooseDuration.choose(1.hour, 2.days)
    payload <- Gen.option(Gen.identifier.filter(_.nonEmpty))
  } yield {
    UserAuthToken(
      id = id.take(30),
      userId,
      ttl = JDuration.standardSeconds(ttl.toSeconds),
      payload = payload.map(p => Map("test_field" -> p)).getOrElse(Map.empty),
      created = DateTime.now().withMillisOfSecond(0),
      used = None
    )
  }

  val legacyUser: Gen[FullUser] = for {
    id <- userId
    profile <- userProfile
    pwdHash <- readableString
    passwordDate <- Gen.option(instantInPast.map(_.toDateTime))
    active <- bool
    emails <- Gen.option(userEmail.filter(_.confirmed)).map(_.toSeq)
    phonesCount <- Gen.choose(0, 5)
    phones <- Gen.listOfN(phonesCount, userPhone)
    socialCount <- Gen.choose(0, 5)
    ip <- Gen.option(ipAddress)
    socialProfiles <- Gen.listOfN(socialCount, userSocialProfile)
  } yield FullUser(
    id = id,
    pwdHash = Some(pwdHash),
    hashingStrategy = PasswordHashingStrategies.Legacy,
    passwordDate = passwordDate.map(_.withMillisOfSecond(0)),
    profile = profile,
    emails = emails,
    phones = phones,
    socialProfiles = socialProfiles,
    registrationDate = DateTime.now().withMillisOfSecond(0),
    registrationIp = ip,
    updated = DateTime.now().withMillisOfSecond(0),
    active = active
  )

  val sessionResult: Gen[SessionResult] = for {
    session <- session
    newSession <- Gen.option(session)
    user <- Gen.option(userEssentials)
  } yield SessionResult(session, newSession, user, user.isDefined, None)

  val userSessionSource: Gen[UserSessionSource] = for {
    userId <- userId
    ttl <- Gen.option(sessionTtl)
  } yield UserSessionSource(userId, ttl)

  val anonymousSessionSource: Gen[AnonymousSessionSource] = for {
    deviceUid <- Gen.option(deviceUid)
    ttl <- Gen.option(sessionTtl)
  } yield AnonymousSessionSource(deviceUid, ttl)

  val socialAuthorization: Gen[SocialAuthorization] = Gen.oneOf(
    readableString.map(SocialAuthorization.Code(_, "state")),
    readableString.map(SocialAuthorization.Token)
  )

  val emailIdentity: Gen[Identity.Email] = for {
    email <- emailAddress
  } yield Identity.Email(email)

  val emailYandexNonNormalizedIdentity: Gen[Identity.Email] = for {
    email <- emailYandexNonNormalizedAddress
  } yield Identity.Email(email)

  val phoneIdentity: Gen[Identity.Phone] = for {
    phone <- phoneNumber
  } yield Identity.Phone(phone)

  val tokenIdentity: Gen[model.IdentityOrToken.Token] = for {
    token <- Gen.alphaNumStr
  } yield IdentityOrToken.Token(token)

  val identity: Gen[Identity] = Gen.oneOf(emailIdentity, phoneIdentity)

  val realIdentity: Gen[model.IdentityOrToken.RealIdentity] =
    identity.map(model.IdentityOrToken.RealIdentity(_))

  val identityOrToken: Gen[model.IdentityOrToken] =
    Gen.oneOf(
      realIdentity,
      tokenIdentity
    )

  val socialLoginParameters: Gen[SocialLoginParameters] = for {
    provider <- socialProvider
    useAuth <- bool
    auth <- socialAuthorization
    socialUser <- socialUserSource
    ttl <- Gen.option(sessionTtl)
  } yield SocialLoginParameters(
    provider = provider,
    authOrUser = if (useAuth) Left(auth) else Right(socialUser),
    ttl = ttl
  )

  val emailTemplateSettings: Gen[EmailTemplateSettings] = for {
    templateName <- Gen.option(readableString)
    additionalParams <- Gen.mapOf(
      readableString.flatMap(x => readableString.map(_ -> x))
    )
    redirectPath <- Gen.option(readableString)
  } yield EmailTemplateSettings(templateName, additionalParams, redirectPath)

  val loginOrRegisterParams: Gen[LoginOrRegisterParameters] = for {
    identity <- identity
    suppressNotifications <- bool
    emailSettings <- emailTemplateSettings
  } yield LoginOrRegisterParameters(IdentityOrToken.RealIdentity(identity), suppressNotifications, emailSettings)

  val loginOrRegisterParamsForToken: Gen[LoginOrRegisterParameters] = for {
    token <- readableString
    suppressNotifications <- bool
    emailSettings <- emailTemplateSettings
  } yield LoginOrRegisterParameters(IdentityOrToken.Token(token), suppressNotifications, emailSettings)

  val linkedSocializedUser: Gen[SocializedUser.Linked] = for {
    user <- legacyUser
    linkedBy <- identity
  } yield SocializedUser.Linked(user, linkedBy)

  val socializedUser: Gen[SocializedUser] = Gen.oneOf(
    legacyUser.map(SocializedUser.Created),
    linkedSocializedUser,
    legacyUser.map(SocializedUser.Found)
  )

  val unlinkedUser: Gen[SocializedUser.UnLinked] = for {
    qt <- Gen.choose(1, 5)
    users <- Gen.listOfN(qt, fullUser)
  } yield SocializedUser.UnLinked(users)

  val socialLoginResult: Gen[SocialLoginResult] = for {
    user <- socializedUser
    session <- userSession
    redirectPath <- Gen.option(readableString)
  } yield SocialLoginResult(user, session, redirectPath, None)

  val socialLoginResultYandex: Gen[SocialLoginResult] = for {
    user <- legacyUser.map(SocializedUser.Found)
    session <- userSession
    redirectPath <- Gen.option(readableString)
  } yield SocialLoginResult(user, session, redirectPath, None)

  val yandexAuthResultLinked: Gen[YandexAuthResult] = for {
    user <- Gen.option(socialLoginResultYandex)
  } yield YandexAuthResult(user, Seq())

  val yandexAuthResultUnlinked: Gen[YandexAuthResult] = for {
    qt <- Gen.choose(1, 5)
    identities <- Gen.listOfN(qt, identity)
  } yield YandexAuthResult(None, identities)

  val yandexAuthResult: Gen[YandexAuthResult] = Gen.oneOf(yandexAuthResultLinked, yandexAuthResultUnlinked)

  val loginResult: Gen[LoginResult] = for {
    user <- fullUser
    session <- userSession.map(_.copy(userId = Some(user.id)))
  } yield LoginResult(user, session, None)

  val userSource: Gen[UserSource] = for {
    profile <- userProfile
    email <- emailAddress
    phone <- phoneNumber
    useEmail <- bool
    password <- readableString
  } yield UserSource(
    profile = profile,
    email = useEmail.option(email),
    phone = (!useEmail).option(phone),
    password = password
  )

  val createUserResult: Gen[CreateUserResult] = for {
    user <- legacyUser
    session <- userSession
  } yield CreateUserResult(user, session)

  def patch[T](valueGen: Gen[T]): Gen[FieldPatch[T]] =
    for {
      clear <- bool
      value <- valueGen
    } yield
      if (clear) FieldPatch.Ignore
      else FieldPatch.SetValue(value)

  def patchOpt[T](valueGen: Gen[T]): Gen[OptField[T]] = patch(Gen.option(valueGen))

  val authActionGen: Gen[AuthMethod] = {
    import AuthMethod._
    Gen.oneOf(
      Gen.const(Password),
      Gen.const(Code),
      Gen.const(ByInternalService),
      socialProvider.map(Social),
      userId.map(ImpersonatedBy)
    )
  }

  val userLoggedIn: Gen[UserLoggedIn] = for {
    userId <- userId
    identity <- Gen.option(identity)
    sessionId <- fakeSessionId
    action <- authActionGen
  } yield UserLoggedIn(
    userId = userId,
    identity = identity,
    sessionId = sessionId,
    authMethod = action
  )

  val userBadLogin: Gen[UserBadLogin] = for {
    userId <- Gen.option(userId)
    login <- readableString
    hash <- readableString
  } yield UserBadLogin(userId, login, hash)

  val autoruUserProfilePatch: Gen[AutoruUserProfilePatch] = for {
    alias <- patchOpt(readableString)
    userpic <- patchOpt(Gen.numStr)
    birthday <- patchOpt(dateInPast.map(_.toLocalDate))
    about <- patchOpt(readableString)
    showCard <- patch(bool)
    showMail <- patch(bool)
    allowMessages <- patch(bool)
    fullName <- patchOpt(readableString)
    drivingYear <- patchOpt(Gen.choose(1, 100))
    countryId <- patchOpt(Gen.choose(1L, 100000L))
    regionId <- patchOpt(Gen.choose(1L, 100000L))
    cityId <- patchOpt(Gen.choose(1L, 100000L))
    passwordLogin <- patch(bool)
    geoId <- patchOpt(Gen.choose(1L, 100000L))
    allowOffersShow <- patch(bool)
  } yield AutoruUserProfilePatch(
    alias = alias,
    userpic = userpic,
    birthday = birthday,
    about = about,
    showCard = showCard,
    showMail = showMail,
    allowMessages = allowMessages,
    fullName = fullName,
    drivingYear = drivingYear,
    countryId = countryId,
    regionId = regionId,
    cityId = cityId,
    passwordLogin = passwordLogin,
    geoId = geoId,
    allowOffersShow = allowOffersShow
  )

  val confirmRegistration: Gen[ConfirmRegistration] = for {
    userId <- userId
    identity <- identity
  } yield ConfirmRegistration(userId, identity)

  val confirmAddIdentity: Gen[ConfirmAddIdentity] = for {
    userId <- userId
    identity <- identity
    steal <- bool
  } yield ConfirmAddIdentity(userId, identity, steal)

  val confirmPasswordReset: Gen[ConfirmPasswordReset] = for {
    userId <- userId
  } yield ConfirmPasswordReset(userId)

  val confirmDelayedRegistration: Gen[ConfirmDelayedRegistration] = for {
    identity <- identity
  } yield ConfirmDelayedRegistration(identity)

  val confirmationPayload: Gen[ConfirmationPayload] = Gen.oneOf(
    confirmRegistration,
    confirmAddIdentity,
    confirmPasswordReset,
    confirmDelayedRegistration
  )

  val confirmationCode: Gen[ConfirmationCode] = for {
    optIdentity <- Gen.option(identity)
    code <- readableString
  } yield ConfirmationCode(optIdentity.map(i => IdentityOrToken.RealIdentity(i)), code)

  val confirmationCodeForToken: Gen[ConfirmationCode] = for {
    token <- readableString
    code <- readableString
  } yield ConfirmationCode(Some(IdentityOrToken.Token(token)), code)

  val identityConfirmationCode: Gen[ConfirmationCode] =
    confirmationCode.filter(_.identity.isDefined)

  val resetPasswordParameters: Gen[ResetPasswordParameters] = for {
    code <- confirmationCode
    newPassword <- readableString
  } yield ResetPasswordParameters(code, newPassword)

  val confirmIdentityParams: Gen[ConfirmIdentityParameters] = for {
    code <- identityConfirmationCode
    createSession <- bool
  } yield ConfirmIdentityParameters(code, createSession)

  val confirmIdentityResult: Gen[ConfirmIdentityResult] = for {
    i <- identity
    user <- legacyUser
    session <- Gen.option(session)
    newUser <- bool
  } yield ConfirmIdentityResult(Some(i), user, session, newUser, None)

  val addPhoneParams: Gen[AddPhoneParameters] = for {
    phone <- phoneNumber
    steal <- bool
    confirm <- bool
    suppressNotifications <- bool
  } yield AddPhoneParameters(phone, steal, confirm, suppressNotifications)

  val addSocialProfileParams: Gen[AddSocialProfileParameters] = for {
    provider <- socialProvider
    auth <- socialAuthorization
  } yield AddSocialProfileParameters(provider, auth)

  val addSocialProfileResult: Gen[AddSocialProfileResult] = for {
    added <- bool
    redirectPath <- Gen.option(readableString)
  } yield AddSocialProfileResult(added, redirectPath)

  val removeSocialProfileParams: Gen[RemoveSocialProfileParameters] = for {
    provider <- socialProvider
    socialUserId <- readableString
  } yield RemoveSocialProfileParameters(provider, socialUserId)

  def userCredentialsF[T](identity: Gen[T]): Gen[UserCredentialsF[T]] =
    for {
      identity <- identity
      password <- readableString
    } yield UserCredentials(identity, password)

  val userCredentials: Gen[UserCredentials] = userCredentialsF(identityOrToken)

  def loginParametersF[T](identity: Gen[T]): Gen[LoginParametersF[T]] =
    userCredentialsF(identity).map(LoginParameters(_))

  val loginParameters: Gen[LoginParameters] = loginParametersF(identityOrToken)

  val resolvedLoginParameters: Gen[LoginParameters.Resolved] = loginParametersF(identity)

  val domainBan: Gen[(ModerationDomain, DomainBan)] = for {
    moderationDomain <- readableString
    reasons <- seq(readableString)
    resellerType <- Gen.option(resellerTypeGen)
  } yield moderationDomain -> DomainBan(reasons.toSet, resellerType)

  val userModerationStatus: Gen[UserModerationStatus] = for {
    bans <- seq(domainBan)
    reseller <- bool
  } yield UserModerationStatus(bans.toMap, reseller)

  val requestEmailChangeParams: Gen[RequestEmailChangeParameters] = for {
    idty <- identity
    suppressNotifications <- bool
    emailSettings <- emailTemplateSettings
  } yield RequestEmailChangeParameters(idty, suppressNotifications, emailSettings)

  val changeEmailConfirmationByCode: Gen[ChangeEmailConfirmation.ByCode] = for {
    idty <- identity
    code <- readableString
  } yield ChangeEmailConfirmation.ByCode(idty, code)

  val changeEmailConfirmationByPassword: Gen[ChangeEmailConfirmation.ByPassword] =
    readableString.map(ChangeEmailConfirmation.ByPassword)

  val changeEmailConfirmation: Gen[ChangeEmailConfirmation] =
    Gen.oneOf(changeEmailConfirmationByCode, changeEmailConfirmationByPassword)

  val changeEmailParams: Gen[ChangeEmailParameters] = for {
    newEmail <- emailAddress
    confirmaion <- changeEmailConfirmation
    emailSettings <- emailTemplateSettings
  } yield ChangeEmailParameters(newEmail, confirmaion, emailSettings = emailSettings)

  val addIdentityResult: Gen[AddIdentityResult] = for {
    needConfirm <- bool
    code <- Gen.option(readableString)
  } yield AddIdentityResult(needConfirm, code)

  val requestPasswordResetParams: Gen[RequestPasswordResetParameters] = for {
    identity <- identity
    emailSettings <- emailTemplateSettings
  } yield RequestPasswordResetParameters(identity, emailSettings)

  val requestContext: Gen[RequestContext] = for {
    id <- readableString
  } yield RequestContext(ApiPayload(id), Traced.empty)

  val phoneXorEmail: Gen[(Option[String], Option[String])] = {
    val onlyPhone = phoneNumber.map(p => (Some(p), None))
    val onlyEmail = emailAddress.map(e => (None, Some(e)))
    Gen.oneOf(onlyPhone, onlyEmail)
  }

  val userChanges: Gen[UserChanges] = for {
    emails <- Gen.listOf(emailAddress)
    phones <- Gen.listOf(phoneNumber)
  } yield UserChanges(newEmails = emails, newPhones = phones)

  val userCreated: Gen[UserCreated] = for {
    id <- userId
    identity <- Gen.option(identity)
    authMode <- authActionGen
    userChanges <- userChanges
  } yield UserCreated(id, identity, authMode, userChanges)

  val loginForbidden: Gen[LoginForbidden] = for {
    id <- Gen.option(userId)
    login <- readableString
    ipBlocked <- ipAddress.map(LoginForbiddenReason.IpBlocked)
    reason <- Gen.oneOf(ipBlocked, LoginForbiddenReason.LoginBlocked(login))
  } yield LoginForbidden(id, login, reason)

  val identityAdded: Gen[IdentityAdded] = for {
    id <- userId
    idty <- identity
    withoutConfirmation <- bool
  } yield IdentityAdded(id, idty, withoutConfirmation)

  val identityRemoved: Gen[IdentityRemoved] = for {
    id <- userId
    idty <- identity
  } yield IdentityRemoved(id, idty)

  val userFinishedRegistration: Gen[UserFinishedRegistration] = for {
    id <- userId
    (phone, email) <- phoneXorEmail
  } yield UserFinishedRegistration(
    id,
    phone,
    email
  )

  val smsSent: Gen[SmsSent] = for {
    id <- Gen.option(userId)
    phone <- phoneNumber
    text <- readableString
    result <- readableString
  } yield SmsSent(id, phone, text, result)

  val userWasSeen: Gen[UserWasSeen] = for {
    id <- userId
    sid <- simpleSessionId
    yandexLinked <- bool
  } yield UserWasSeen(id, sid, yandexLinked)

  val userStolePhone: Gen[UserStolePhone] = for {
    id <- userId
    fromUser <- userId
    phone <- phoneNumber
  } yield UserStolePhone(
    id,
    phone,
    fromUser
  )

  val socialUserLinked: Gen[SocialUserLinked] = for {
    id <- userId
    provider <- socialProvider
    source <- socialUserSource
    candidatesSize <- Gen.choose(0, 3)
    candidatesUserId <- Gen.listOfN(candidatesSize, userId)
    candidatesIdentity <- Gen.listOfN(candidatesSize, identity)
    changes <- userChanges
  } yield SocialUserLinked(
    userId = id,
    provider = provider,
    source = source,
    linkedBy = candidatesIdentity.headOption,
    candidates = candidatesUserId.zip(candidatesIdentity),
    changes = changes
  )

  val socialUserUpdated: Gen[SocialUserUpdated] = for {
    id <- userId
    provider <- socialProvider
    source <- socialUserSource
    changes <- userChanges
  } yield SocialUserUpdated(
    userId = id,
    provider = provider,
    source = source,
    changes = changes
  )

  val socialUserUnlinked: Gen[SocialUserUnlinked] = for {
    id <- userId
    provider <- socialProvider
    source <- socialUserSource
  } yield SocialUserUnlinked(
    userId = id,
    provider = provider,
    socialUserId = source.id
  )

  val event: Gen[Event] =
    Gen.oneOf(
      userCreated,
      userLoggedIn,
      userBadLogin,
      loginForbidden,
      identityAdded,
      identityRemoved,
      userFinishedRegistration,
      smsSent,
      userWasSeen,
      userStolePhone,
      socialUserLinked,
      socialUserUpdated,
      socialUserUnlinked
    )

  def eventEnvelope(domain: Domain): Gen[EventEnvelope] =
    for {
      ts <- dateInPast
      e <- event
      rc <- requestContext
    } yield EventEnvelope(domain, ts, e, rc)

  val ApitokenPayloadGen: Gen[ApiTokenPayload] = for {
    ratelimit <- Gen.choose(300, 350)
    grantsN <- Gen.choose(1, 5)
    grants <- Gen.listOfN(grantsN, readableString)
  } yield ApiTokenPayload.newBuilder().setRatelimitPerApplication(ratelimit).addAllGrants(grants.asJava).build()

  val ApiTokenCreateParamsGen: Gen[ApiTokenCreateParams] = for {
    payload <- ApitokenPayloadGen
    name <- readableString
    requester <- readableString
    comment <- readableString
  } yield ApiTokenCreateParams
    .newBuilder()
    .setName(name)
    .setPayload(payload)
    .setRequester(requester)
    .setComment(comment)
    .build()

  val ApiTokenGen: Gen[ApiTokenRow] = for {
    name <- Gen.identifier.filter(_.nonEmpty)
    rand <- Gen.identifier.filter(_.nonEmpty)
    token = name + "-" + rand
    payload <- ApitokenPayloadGen
  } yield {
    val moment = DateTime.now().withMillisOfSecond(0)
    ApiTokenRow(0L, token.take(50), moment, moment, payload, 1L)
  }

  val ApiTokenHistoryRowGen: Gen[ApiTokenHistoryRow] = for {
    id <- Arbitrary.arbLong.arbitrary
    token <- readableString
    moment <- DateTimeGenerators.dateTime()
    requester <- readableString
    comment <- readableString
    payload <- ApitokenPayloadGen
    version <- Arbitrary.arbLong.arbitrary
  } yield ApiTokenHistoryRow(id, token, moment, requester, comment, payload, version)

  val ApiTokenUpdateGen: Gen[ApiTokenUpdate] = for {
    name <- Gen.identifier.filter(_.nonEmpty)
    rand <- Gen.identifier.filter(_.nonEmpty)
    token = name + "-" + rand
    requester <- readableString
    comment <- readableString
    payload <- ApitokenPayloadGen
  } yield {
    ApiTokenUpdate(token, requester, comment, payload)
  }

  val ApiTokenRemoveGen: Gen[ApiTokenRemove] = for {
    name <- Gen.identifier.filter(_.nonEmpty)
    rand <- Gen.identifier.filter(_.nonEmpty)
    token = name + "-" + rand
    requester <- readableString
    comment <- readableString
  } yield {
    val moment = DateTime.now().withMillisOfSecond(0)
    ApiTokenRemove(token, requester, comment)
  }

  val ApiTokenResultGen: Gen[ApiTokenResult] = for {
    token <- ApiTokenGen
  } yield ApiTokenResult.newBuilder().setToken(ApiTokenFormats.ApiTokenWriter.write(token)).build()

  private val MosRuPayloadGen: Gen[MosRuPayload] = for {
    snilsExists: Boolean <- bool
  } yield MosRuPayload.newBuilder.setSnilsExists(BoolFormat.write(snilsExists)).build

  val SocialUserPayloadGen: Gen[SocialUserPayload] = for {
    mosru <- MosRuPayloadGen
  } yield SocialUserPayload.newBuilder.setMosru(mosru).build
}
