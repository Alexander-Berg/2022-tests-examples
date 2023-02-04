package ru.yandex.complaints.dao.complaints

import java.sql.Timestamp
import org.joda.time.DateTime
import org.scalacheck.Gen
import ru.yandex.complaints.dao._
import ru.yandex.complaints.model.Complaint.{Application, Group}
import ru.yandex.complaints.model.User.UserId
import ru.yandex.complaints.model.UserType._
import ru.yandex.complaints.model._

import scala.collection.immutable
import scala.language.implicitConversions
import scala.util.{Random, Try}

/**
  * Generators for some domain objects
  *
  * @author potseluev
  */
object Generators {

  implicit class RichGen[T](gen: Gen[T]) {
    def next: T = stream.head

    def next(n: Int): immutable.Seq[T] = stream.take(n)

    def stream: Stream[T] = Stream.continually(gen.sample).flatten
  }

  def choose[T](src: Iterable[T]): T = src.toSeq.apply(Random.nextInt(src.size))

  def stringGen(minLength: Int = 6, maxLength: Int = 10): Gen[String] = for {
    length <- Gen.chooseNum(minLength, maxLength)
    result = Random.alphanumeric.take(length).mkString
  } yield result

  val BooleanGen: Gen[Boolean] = Gen.oneOf(true, false)

  val PlainOfferIdGen: Gen[Plain] = stringGen().map(Plain)

  val AutoruStoIdGen: Gen[AutoruSto] = stringGen().map(AutoruSto.apply)

  val OfferIdGen: Gen[OfferID] = Gen.oneOf(PlainOfferIdGen, AutoruStoIdGen)

  val AuthorIdGen: Gen[AuthorId] = stringGen()

  val UserTypeGen: Gen[UserType] = Gen.oneOf(UserType.values.toSeq)

  val UserIdGen: Gen[UserId] = stringGen().map(UserId.apply)

  val ComplaintIdGen: Gen[ComplaintID] = stringGen()

  val ModObjIdGen: Gen[ModObjID] = stringGen()

  val DateTimeGen: Gen[DateTime] = for {
    days <- Gen.chooseNum(-100, +100)
  } yield DateTime.now().plusDays(days)

  val TimestampGen: Gen[Timestamp] = DateTimeGen.map(date => new Timestamp(date.getMillis))

  val ComplaintTypeGen: Gen[ComplaintType] = Gen.chooseNum(1, 23)
    .map(_.toByte)
    .flatMap(code => Try(ComplaintType.apply(code)))
    .filter(_.isSuccess)
    .map(_.get)

  val GroupGen: Gen[Group] = Gen.oneOf(List(Group.MobileApp))

  val ApplicationGen: Gen[Application] = Gen.oneOf(Application.values)

  implicit class RichComplaintsGen(private var gen: Gen[Complaint]) {
    def forOffers(offers: Iterable[OfferID]): RichComplaintsGen = switch(_.copy(offerId = choose(offers)))

    def fromUsers(users: Iterable[UserId]): RichComplaintsGen = switch(_.copy(userId = choose(users)))

    private def switch(mapper: Complaint => Complaint): RichComplaintsGen = {
      gen = gen.map(mapper)
      this
    }
  }

  object RichComplaintsGen {
    implicit def toGen(richComplaintsGen: RichComplaintsGen): Gen[Complaint] = richComplaintsGen.gen

    implicit def toRichGen(richComplaintsGen: RichComplaintsGen): RichGen[Complaint] = RichGen(richComplaintsGen.gen)
  }

  val UserDataGen: Gen[Complaint.UserData] =
    for {
      group <- Gen.option(GroupGen)
      application <- Gen.option(ApplicationGen)
      placement <- Gen.option(stringGen())
      isAuthorizedUser <- Gen.option(BooleanGen)
    } yield Complaint.UserData(group, application, placement, isAuthorizedUser)

  val ComplaintsGen: Gen[Complaint] = for {
    userId <- UserIdGen
    userType <- UserTypeGen
    offerId <- OfferIdGen
    complaintId <- ComplaintIdGen
    modObjId <- Gen.option(stringGen())
    complaintType <- ComplaintTypeGen
    description <- stringGen()
    created <- TimestampGen
    scheduled <- Gen.option(TimestampGen)
    userData <- UserDataGen
    notified <- BooleanGen
    source <- Gen.option(stringGen())
  } yield Complaint(userId, userType, offerId, complaintId, modObjId, complaintType, description,
    created, scheduled, userData, notified, source)

  val OfferGen: Gen[Offer] = for {
    offerId <- OfferIdGen
    createTime <- TimestampGen
    scheduled <- Gen.option(TimestampGen)
    authorId <- AuthorIdGen
    hash <- stringGen()
  } yield Offer(offerId, createTime, scheduled, authorId, hash)
}
