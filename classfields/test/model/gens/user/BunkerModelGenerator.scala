package ru.auto.salesman.test.model.gens.user

import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel.Category
import ru.auto.salesman.model.DeprecatedDomains
import ru.auto.salesman.model.user._
import ru.auto.salesman.model.user.product.AutoruProduct

trait BunkerModelGenerator extends UserModelGenerators {

  lazy val DescriptionGenerator: Gen[ProductDescription] = for {
    product <- productGen[AutoruProduct]
    alias <- Gen.option(product.alias)
    title <- Gen.option(Gen.alphaNumStr)
    description <- Gen.option(Gen.alphaNumStr)
    multiplier <- Gen.option(Gen.posNum[Int])
    aliases <- Gen.option(Gen.listOf(Gen.alphaNumStr))
  } yield ProductDescription(alias, title, description, multiplier, aliases)

  lazy val DescriptionGeneratorWithName = for {
    name <- Gen.alphaNumStr
    vas <- DescriptionGenerator
  } yield vas.copy(name = Some(name))

  private def generateDescriptionMap() =
    Gen.mapOfN(1, Gen.zip(Gen.oneOf(EndUserType.values), DescriptionGenerator))

  private def generateCategoryToDescriptionMapMap() =
    Gen.mapOfN(
      1,
      Gen.zip(
        Gen.oneOf(Seq(Category.CARS, Category.MOTO, Category.TRUCKS)),
        generateDescriptionMap()
      )
    )

  lazy val DescriptionsGenerator = for {
    autoruProductsDescriptions <- AutoruProductsDescriptionsGenerator
  } yield ProductsDescriptions(autoruProductsDescriptions)

  lazy val AutoruProductsDescriptionsGenerator = for {
    autoruOfferTypeProductsDescriptions <-
      AutoruOfferTypeProductDescriptionsGenerator
    autoruUserTypeProductDescriptions <-
      AutoruUserTypeProductDescriptionsGenerator
  } yield
    AutoruProductsDescriptions(
      autoruOfferTypeProductsDescriptions,
      autoruUserTypeProductDescriptions
    )

  lazy val ProductDescriptionsGenerator = Gen.oneOf(
    AutoruOfferTypeProductDescriptionsGenerator,
    AutoruUserTypeProductDescriptionsGenerator
  )

  lazy val AutoruOfferTypeProductDescriptionsGenerator = for {
    map <- Gen.mapOfN(
      1,
      Gen.zip(OfferProductGen, generateCategoryToDescriptionMapMap())
    )
  } yield map

  lazy val AutoruUserTypeProductDescriptionsGenerator = for {
    map <- Gen.mapOfN(1, Gen.zip(UserProductGen, generateDescriptionMap()))
  } yield map

  lazy val StoTypeProductDescriptionsGenerator = for {
    map <- Gen.mapOfN(1, Gen.zip(ProductGen, generateDescriptionMap()))
  } yield map

  lazy val NotEmptyDescriptionsGenerator = for {
    domainGenerated <- Gen.oneOf(DeprecatedDomains.values.toSeq)
  } yield domainGenerated

}
