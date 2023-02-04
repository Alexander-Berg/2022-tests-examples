package vertis.palma.service

import com.google.protobuf.{DynamicMessage, Message}
import ru.yandex.vertis.palma.images.Image
import vertis.palma.test._
import vertis.palma.utils.ProtoReflectionUtils

import scala.jdk.CollectionConverters.{IterableHasAsJava, IterableHasAsScala}

/** @author ruslansd
  */
trait DictionaryBaseSpec {

  def toDynamicMessage(msg: Message): DynamicMessage =
    DynamicMessage.parseFrom(msg.getDescriptorForType, msg.toByteArray)

  def parse[M <: Message](msg: DynamicMessage, instance: M): M = {
    val parser = instance.getParserForType
    parser.parseFrom(msg.toByteArray).asInstanceOf[M]
  }

  def cleanRelations[M <: Message](message: M): M = {
    ProtoReflectionUtils.cleanNonKeyFields(message).asInstanceOf[M]
  }

  val WithEmptyDictionaryKey =
    Mark
      .newBuilder()
      .setRussianAlias("БМВ")
      .build()

  val RelatedToEmptyDictionaryKey =
    Model
      .newBuilder()
      .setCode("X5")
      .setMark(WithEmptyDictionaryKey)
      .build()

  val WithEmptyLink =
    Model
      .newBuilder()
      .setCode("X5")
      .build()

  val DictionaryWithoutNameOption =
    DictionaryWithoutName
      .newBuilder()
      .setAlias("alal")
      .build()

  val mark =
    Mark
      .newBuilder()
      .setCode("BMW")
      .setRussianAlias("БМВ")
      .build()

  val anotherMark =
    Mark
      .newBuilder()
      .setCode("BMW2")
      .setRussianAlias("БМВ2")
      .build()

  val red =
    Color
      .newBuilder()
      .setAlias("red")
      .setCode("x_red")
      .build()

  val black =
    Color
      .newBuilder()
      .setAlias("black")
      .setCode("x_black")
      .build()

  val model =
    Model
      .newBuilder()
      .setCode("X5")
      .setMark(mark)
      .addAllColors(Iterable(black, red).asJava)
      .build()

  val modelCleaned =
    Model
      .newBuilder()
      .setCode("X5")
      .setMark(cleanRelations(mark))
      .addAllColors(Iterable(cleanRelations(black), cleanRelations(red)).asJava)
      .build()

  val colorLinkRed =
    ColorLink
      .newBuilder()
      .setAbc("abc")
      .setColor(red)
      .build()

  val colorLinkBlack =
    ColorLink
      .newBuilder()
      .setAbc("abc")
      .setColor(black)
      .build()

  val colorLinkRedCleaned =
    colorLinkRed.toBuilder.setColor(cleanRelations(red)).build()

  val colorLinkBlackCleaned =
    colorLinkBlack.toBuilder.setColor(cleanRelations(black)).build()

  val Option1 =
    Option
      .newBuilder()
      .setOptionName("option_1")
      .setCode("option_1")
      .build()

  val Option2 =
    Option
      .newBuilder()
      .setOptionName("option_2")
      .setCode("option_2")
      .build()

  val OptionMissingCode =
    Option
      .newBuilder()
      .setOptionName("option_2")
      .build()

  val EmptyOption = Option.getDefaultInstance

  val OptionLink1 =
    OptionLink
      .newBuilder()
      .setOption(Option1)
      .setPrice(10000)
      .build()

  val OptionLink2 =
    OptionLink
      .newBuilder()
      .setOption(Option2)
      .setPrice(199999)
      .build()

  val OptionLink1Cleaned =
    OptionLink
      .newBuilder()
      .setOption(cleanRelations(Option1))
      .setPrice(10000)
      .build()

  val OptionLink2Cleaned =
    OptionLink
      .newBuilder()
      .setOption(cleanRelations(Option2))
      .setPrice(199999)
      .build()

  val optionPackage =
    OptionPackage
      .newBuilder()
      .addAllLinks(Iterable(OptionLink1, OptionLink2).asJava)
      .setName("main_package")
      .setColor(black)
      .build()

  val optionPackageCleaned =
    OptionPackage
      .newBuilder()
      .addAllLinks(Iterable(OptionLink1Cleaned, OptionLink2Cleaned).asJava)
      .setName("main_package")
      .setColor(cleanRelations(black))
      .build()

  val modelWithNestedLink =
    ModelWithNestedLink
      .newBuilder()
      .setCode("X5")
      .setMark(mark)
      .addAllColors(Iterable(colorLinkBlack, colorLinkRed).asJava)
      .addAllReleaseYears(Iterable(Int.box(2010), Int.box(2005)).asJava)
      .setPackage(optionPackage)
      .build()

  val modelWithNestedLinkCleaned =
    ModelWithNestedLink
      .newBuilder()
      .setCode("X5")
      .setMark(cleanRelations(mark))
      .addAllColors(Iterable(colorLinkBlackCleaned, colorLinkRedCleaned).asJava)
      .addAllReleaseYears(Iterable(Int.box(2010), Int.box(2005)).asJava)
      .setPackage(optionPackageCleaned)
      .build()

  val modelMissingOptionCode = {
    val links = optionPackage.getLinksList.asScala.toSeq
    val incorrectLink = OptionLink.newBuilder().setOption(OptionMissingCode).build()
    val newPackage =
      optionPackage.toBuilder.clearLinks().addAllLinks((links :+ incorrectLink).asJava)
    modelWithNestedLink.toBuilder.setPackage(newPackage).build()
  }

  val modelWithEmptyOption = {
    val links = optionPackage.getLinksList.asScala.toSeq
    val incorrectLink = OptionLink.newBuilder().setOption(EmptyOption).build() // should failed
    val newPackage =
      optionPackage.toBuilder.clearLinks().addAllLinks((links :+ incorrectLink).asJava)
    modelWithNestedLink.toBuilder.setPackage(newPackage).build()
  }

  val modelWithUnsetOption = {
    val links = optionPackage.getLinksList.asScala.toSeq
    val incorrectLink = OptionLink.newBuilder().build() // should not failed
    val newPackage =
      optionPackage.toBuilder.clearLinks().addAllLinks((links :+ incorrectLink).asJava)
    modelWithNestedLink.toBuilder.setPackage(newPackage).build()
  }

  val Bar1 =
    Bar
      .newBuilder()
      .setCode("bar1")
      .setColor(black)
      .build()

  val Bar2 =
    Bar
      .newBuilder()
      .setCode("bar2")
      .setColor(red)
      .build()

  val foo =
    Foo
      .newBuilder()
      .setCode("foo")
      .setBar(Bar1)
      .setBarLink(Bar2)
      .build()

  val fooCleaned =
    Foo
      .newBuilder()
      .setCode("foo")
      .setBar(Bar1)
      .setBarLink(cleanRelations(Bar2))
      .build()

  val brokenIndex =
    BrokenIndex
      .newBuilder()
      .setCode("color")
      .setAlias("black")
      .setOtherAlias("schwarz")
      .build()

  val multiIndex =
    MultiIndex
      .newBuilder()
      .setCode("color")
      .setAlias("black")
      .setOtherAlias("schwarz")
      .build()

  val encryptedMark =
    EncryptedMark
      .newBuilder()
      .setCode("AUDI")
      .setRussianAlias("ауди")
      .build()

  val encryptedModel =
    EncryptedModel
      .newBuilder()
      .setCode("A6")
      .setMark(encryptedMark)
      .setReleaseYear(1996)
      .build()

  val encryptedModelCleaned =
    EncryptedModel
      .newBuilder()
      .setCode("A6")
      .setMark(encryptedMark.toBuilder.clearRussianAlias().build())
      .setReleaseYear(1996)
      .build()

  val withDefinedKey =
    WithAutoGenKey
      .newBuilder()
      .setCode("test")
      .build()

  val withUndefinedKey =
    WithAutoGenKey.newBuilder().build()

  val intKey =
    IntKey.newBuilder.setCode(1).build()

  val testImage = NeverbaPhoto
    .newBuilder()
    .setCode("test_image")
    .setImage(Image.newBuilder().setGroupId("1111").setName("test").setNamespace("neverba"))
    .build()

  def clean(model: Model): Model =
    model
      .toBuilder()
      .setMark(cleanRelations(model.getMark))
      .addAllColors(model.getColorsList.asScala.map(cleanRelations).asJava)
      .build()

}
