package vertis.palma.service.schema

import ru.yandex.vertis.palma.palma_options.DictionaryOptions
import ru.yandex.vertis.palma.services.dictionary_scheme_service.DictionarySchemeApiModel.Enum.EnumValue
import ru.yandex.vertis.palma.services.dictionary_scheme_service.DictionarySchemeApiModel.PrimitiveTypeEnumMsg.PrimitiveType
import ru.yandex.vertis.palma.services.dictionary_scheme_service.DictionarySchemeApiModel.Type.TypeValue
import ru.yandex.vertis.palma.services.dictionary_scheme_service.DictionarySchemeApiModel.TypeEnumMsg.TypeEnum
import ru.yandex.vertis.palma.services.dictionary_scheme_service.DictionarySchemeApiModel.{
  DictionaryScheme,
  Enum,
  Field,
  FieldOptions,
  Link,
  OneOf,
  Primitive,
  StructuredType,
  Type
}
import vertis.palma.service.model.DictionaryException.RecursiveMessageException
import vertis.palma.service.schema.DictionarySchemeHelperSpec._
import vertis.palma.test.dictionary_scheme_samples.{Bucket, BucketSize, Fruit, HasRecursion, OneOfBucket}
import vertis.zio.test.ZioSpecBase

/** @author ruslansd
  */
class DictionarySchemeHelperSpec extends ZioSpecBase {

  "DictionarySchemeHelper" should {
    "parse Bucket" in {
      ioTest {
        for {
          scheme <- DictionarySchemeHelper.parse(Bucket.javaDescriptor)
          _ <- check(scheme shouldBe BucketScheme)
        } yield ()
      }
    }

    "parse Fruits" in {
      ioTest {
        for {
          scheme <- DictionarySchemeHelper.parse(Fruit.javaDescriptor)
          _ <- check(scheme shouldBe FruitScheme)
        } yield ()
      }
    }

    "fail on recursion messages" in {
      intercept[RecursiveMessageException] {
        ioTest {
          DictionarySchemeHelper.parse(HasRecursion.javaDescriptor)
        }
      }
    }
    "one of" in {
      ioTest {
        for {
          scheme <- DictionarySchemeHelper.parse(OneOfBucket.javaDescriptor)
          _ <- check(scheme shouldBe OneOfScheme)
        } yield ()
      }
    }
  }
}

object DictionarySchemeHelperSpec {

  private val BucketOptions =
    DictionaryOptions(
      name = "test/bucket",
      title = "Корзина"
    )

  private val BucketSizeMessage =
    StructuredType(
      BucketSize.javaDescriptor.getFullName,
      Seq(
        Field(
          "size",
          Some(primitive(PrimitiveType.INT32)),
          isRepeated = false
        )
      )
    )

  private val BucketScheme =
    DictionaryScheme(
      Some(BucketOptions),
      Some(
        StructuredType(
          Bucket.javaDescriptor.getFullName,
          Seq(
            Field(
              "id",
              Some(primitive(PrimitiveType.STRING)),
              isRepeated = false,
              Some(FieldOptions(title = "Идентификатор"))
            ),
            Field(
              "fruits",
              Some(link(Link("test/fruit", "name", "alias"))),
              isRepeated = true
            ),
            Field(
              "size",
              Some(structuredType(BucketSizeMessage)),
              isRepeated = false
            ),
            Field(
              "create_time",
              Some(Type(TypeEnum.TIMESTAMP, TypeValue.Prim(Primitive(PrimitiveType.STRING)))),
              isRepeated = false
            ),
            Field(
              "life_time",
              Some(primitive(PrimitiveType.STRING)),
              isRepeated = false,
              Some(FieldOptions(title = "", required = true, asTitle = false))
            )
          )
        )
      ),
      "id"
    )

  private val FruitOptions =
    DictionaryOptions(name = "test/fruit", title = "Фрукт", hidden = true, listingFields = Seq("name", "type"))

  private val FruitScheme =
    DictionaryScheme(
      Some(FruitOptions),
      Some(
        StructuredType(
          Fruit.javaDescriptor.getFullName,
          Seq(
            Field(
              "name",
              Some(primitive(PrimitiveType.STRING)),
              isRepeated = false,
              Some(FieldOptions(title = "Название"))
            ),
            Field(
              "type",
              Some(
                Type(
                  TypeEnum.ENUM,
                  TypeValue.Enum(
                    Enum(
                      Seq(EnumValue(0, "UNKNOWN"), EnumValue(1, "APPLE", "Яблоко"), EnumValue(2, "ORANGE"))
                    )
                  )
                )
              ),
              isRepeated = false
            ),
            Field(
              "alias",
              Some(primitive(PrimitiveType.STRING)),
              isRepeated = false,
              Some(FieldOptions(title = "", required = false, asTitle = true))
            )
          )
        )
      ),
      "name"
    )

  private val OneOfScheme =
    DictionaryScheme(
      Some(DictionaryOptions(name = "test/one_of", title = "OneOf")),
      Some(
        StructuredType(
          OneOfBucket.javaDescriptor.getFullName,
          Seq(
            Field(
              "name",
              Some(primitive(PrimitiveType.STRING)),
              isRepeated = false,
              Some(FieldOptions(title = "Название"))
            )
          ),
          Seq(
            OneOf(
              "oneof_field_1",
              Seq(
                Field(
                  "first",
                  Some(primitive(PrimitiveType.STRING)),
                  isRepeated = false
                ),
                Field(
                  "second",
                  Some(primitive(PrimitiveType.STRING)),
                  isRepeated = false
                )
              )
            ),
            OneOf(
              "oneof_field_2",
              Seq(
                Field(
                  "first_int",
                  Some(primitive(PrimitiveType.INT32)),
                  isRepeated = false
                ),
                Field(
                  "second_int",
                  Some(primitive(PrimitiveType.INT32)),
                  isRepeated = false
                )
              )
            )
          )
        )
      ),
      "name"
    )

  private def primitive(pt: PrimitiveType) =
    Type(TypeEnum.PRIMITIVE, TypeValue.Prim(Primitive(pt)))

  private def link(l: Link) =
    Type(TypeEnum.LINK, TypeValue.Link(l))

  private def structuredType(m: StructuredType) =
    Type(TypeEnum.MESSAGE, TypeValue.StructuredType(m))
}
