package vertis.broker.api.validate

import ru.yandex.vertis.validation.model.{Invalid, MissingRequiredField, Valid}
import vertis.broker.api.validate.ValidatorSupport._
import zio.test.Assertion._
import zio.test._

/** @author kusaeva
  */
object ValidatorSupport {
  val validator = new ValidatorImpl(Some(Seq()))
}

object ValidatorSpec extends DefaultRunnableSpec {

  override def spec = suite("ValidatorSpec")(
    testM("successfully validates valid message") {
      val bar = Bar
        .newBuilder()
        .setNum(2)
        .setStr("bar")
        .setBool(true)
        .setEnum(Enum.BAR)
        .build()
      val message = Foo
        .newBuilder()
        .setNum(1)
        .setStr("bar")
        .setBool(true)
        .setEnum(Enum.BAZ)
        .setBar(bar)
        .build()
      for {
        res <- validator.validate(message)
      } yield assert(res)(equalTo(Valid))
    } @@ TestAspect.ignore,
    testM("throw InvalidError for invalid message") {
      val noBarMessage = Foo
        .newBuilder()
        .setNum(1)
        .setStr("bar")
        .setBool(true)
        .setEnum(Enum.BAZ)
        .build()
      val reasons = List(MissingRequiredField("vertis.broker.api.validate.Foo.bar"))
      for {
        res <- validator.validate(noBarMessage).run
      } yield assert(res)(fails(equalTo(Validator.InvalidError(Invalid(reasons)))))
    } @@ TestAspect.ignore
  )
}
