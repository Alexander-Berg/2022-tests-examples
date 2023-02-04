package ru.yandex.vertis.general.gost.model.test.attributes.validation

import general.bonsai.restriction_model.DictionaryRestriction.DictionaryRestrictionValue
import general.bonsai.restriction_model.NumberRestriction.NumberRestrictionValue
import general.bonsai.restriction_model.Restriction.TypeRestriction
import general.bonsai.restriction_model.{
  DictionaryRestriction,
  DictionaryValueSet,
  NumberRestriction,
  Restriction => ApiRestriction
}
import ru.yandex.vertis.general.gost.model.attributes.AttributeValue.{DictionaryValue, StringValue}
import ru.yandex.vertis.general.gost.model.attributes.Attribute
import ru.yandex.vertis.general.gost.model.validation.attributes._
import zio.ZIO
import zio.test.Assertion._
import zio.test._

object DictionaryRestrictionSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("DictionaryRestriction")(
      testM("validates value by set") {
        val restriction = ApiRestriction(
          "attr",
          TypeRestriction.DictionaryRestriction(
            DictionaryRestriction(DictionaryRestrictionValue.MustBeInSet(DictionaryValueSet(Seq("1", "2"))))
          )
        )

        for {
          compiled <- Restriction.compile(restriction)
          check <- compiled.check(Attribute("attr", 0, DictionaryValue("1")))
        } yield assert(check)(isNone)
      },
      testM("invalidates wrong value by set") {
        val restriction = ApiRestriction(
          "attr",
          TypeRestriction.DictionaryRestriction(
            DictionaryRestriction(DictionaryRestrictionValue.MustBeInSet(DictionaryValueSet(Seq("1", "2"))))
          )
        )

        for {
          compiled <- Restriction.compile(restriction)
          check <- compiled.check(Attribute("attr", 0, DictionaryValue("a")))
        } yield assert(check)(isSome(isSubtype[DictionaryKeysNotAllowed](anything)))
      },
      testM("validates value by equality") {
        val restriction = ApiRestriction(
          "attr",
          TypeRestriction.DictionaryRestriction(
            DictionaryRestriction(DictionaryRestrictionValue.MustEqual("1"))
          )
        )

        for {
          compiled <- Restriction.compile(restriction)
          check <- compiled.check(Attribute("attr", 0, DictionaryValue("1")))
        } yield assert(check)(isNone)
      },
      testM("invalidates wrong value by equality") {
        val restriction = ApiRestriction(
          "attr",
          TypeRestriction.DictionaryRestriction(
            DictionaryRestriction(DictionaryRestrictionValue.MustEqual("1"))
          )
        )

        for {
          compiled <- Restriction.compile(restriction)
          check <- compiled.check(Attribute("attr", 0, DictionaryValue("a")))
        } yield assert(check)(isSome(isSubtype[DictionaryKeysNotAllowed](anything)))
      },
      testM("builds correct limit") {
        val restriction = ApiRestriction(
          "attr",
          TypeRestriction.DictionaryRestriction(
            DictionaryRestriction(DictionaryRestrictionValue.MustBeInSet(DictionaryValueSet(Seq("1", "2"))))
          )
        )

        for {
          compiled <- Restriction.compile(restriction)
          limit = compiled.limitation
        } yield assert(limit.getDictionaryLimitation.allowedKeys)(equalTo(Seq("1", "2")))
      },
      testM("correctly merges set and equality") {
        val restriction1 = ApiRestriction(
          "attr",
          TypeRestriction.DictionaryRestriction(
            DictionaryRestriction(DictionaryRestrictionValue.MustBeInSet(DictionaryValueSet(Seq("1", "2"))))
          )
        )
        val restriction2 = ApiRestriction(
          "attr",
          TypeRestriction.DictionaryRestriction(
            DictionaryRestriction(DictionaryRestrictionValue.MustEqual("1"))
          )
        )

        for {
          restrictions <- ZIO.foreach(Seq(restriction1, restriction2))(Restriction.compile)
          merged <- Restriction.mergeRestrictions(restrictions)
          limit = merged.limitation
          check <- merged.check(Attribute("attr", 0, DictionaryValue("1")))
        } yield assert(limit.getDictionaryLimitation.allowedKeys)(equalTo(Seq("1"))) && assert(check)(isNone)
      },
      testM("correctly merges two sets") {
        val restriction1 = ApiRestriction(
          "attr",
          TypeRestriction.DictionaryRestriction(
            DictionaryRestriction(DictionaryRestrictionValue.MustBeInSet(DictionaryValueSet(Seq("1", "2"))))
          )
        )
        val restriction2 = ApiRestriction(
          "attr",
          TypeRestriction.DictionaryRestriction(
            DictionaryRestriction(DictionaryRestrictionValue.MustBeInSet(DictionaryValueSet(Seq("2", "3"))))
          )
        )

        for {
          restrictions <- ZIO.foreach(Seq(restriction1, restriction2))(Restriction.compile)
          merged <- Restriction.mergeRestrictions(restrictions)
          limit = merged.limitation
          check <- merged.check(Attribute("attr", 0, DictionaryValue("2")))
        } yield assert(limit.getDictionaryLimitation.allowedKeys)(equalTo(Seq("2"))) && assert(check)(isNone)
      },
      testM("fails to merge with number restriction") {
        val restriction1 = ApiRestriction(
          "attr",
          TypeRestriction.DictionaryRestriction(
            DictionaryRestriction(DictionaryRestrictionValue.MustBeInSet(DictionaryValueSet(Seq("1", "2"))))
          )
        )
        val restriction2 = ApiRestriction(
          "attr",
          TypeRestriction.NumberRestriction(NumberRestriction(NumberRestrictionValue.MustEqual(1)))
        )

        for {

          restrictions <- ZIO.foreach(Seq(restriction1, restriction2))(Restriction.compile)
          merged <- Restriction.mergeRestrictions(restrictions).flip
        } yield assert(merged)(isSubtype[DifferentRestrictionTypesMerge](anything))
      },
      testM("fails to test attribute with wrong id") {
        val restriction1 = ApiRestriction(
          "attr",
          TypeRestriction.DictionaryRestriction(
            DictionaryRestriction(DictionaryRestrictionValue.MustBeInSet(DictionaryValueSet(Seq("1", "2"))))
          )
        )

        for {
          compiled <- Restriction.compile(restriction1)
          result <- compiled.check(Attribute("attr2", 0, DictionaryValue("1"))).flip
        } yield assert(result)(isSubtype[AttemptToCheckAttributeWithWrongId](anything))
      },
      testM("fails to test attribute with wrong value type") {
        val restriction1 = ApiRestriction(
          "attr",
          TypeRestriction.DictionaryRestriction(
            DictionaryRestriction(DictionaryRestrictionValue.MustBeInSet(DictionaryValueSet(Seq("1", "2"))))
          )
        )

        for {
          compiled <- Restriction.compile(restriction1)
          result <- compiled.check(Attribute("attr", 0, StringValue("1"))).flip
        } yield assert(result)(isSubtype[AttemptToCheckAttributeWithWrongType](anything))
      }
    )
}
