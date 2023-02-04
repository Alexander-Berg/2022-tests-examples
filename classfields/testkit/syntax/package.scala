package ru.auto.catalog.core.testkit

import ru.auto.catalog.core.model.raw._
import ru.auto.catalog.core.util.tagging._
import com.softwaremill.tagging._
import ru.auto.catalog.model.api.ApiModel.RawCatalog
import scala.jdk.CollectionConverters._

package object syntax {

  implicit class LiteralIds(val sc: StringContext) extends AnyVal {
    def complectation(): ComplectationId = sc.s().taggedWith[Tag.ComplectationId]
    def configuration(): ConfigurationId = sc.s().taggedWith[Tag.ConfigurationId]
    def mark(): MarkId = sc.s().taggedWith[Tag.MarkId]
    def model(): ModelId = sc.s().taggedWith[Tag.ModelId]
    def searchTag(): SearchTag = sc.s().taggedWith[Tag.SearchTag]
    def subcategory(): SubcategoryId = sc.s().taggedWith[Tag.SubcategoryId]
    def superGeneration(): SuperGenerationId = sc.s().taggedWith[Tag.SuperGenerationId]
    def techParam(): TechParamId = sc.s().taggedWith[Tag.TechParamId]
    def nameplateName(): NameplateName = sc.s().taggedWith[Tag.NameplateName]
    def nameplateSemanticUrl(): NameplateSemanticUrl = sc.s().taggedWith[Tag.NameplateSemanticUrl]
  }

  implicit class RawCatalogOps(val rc: RawCatalog) extends AnyVal {

    def techParamIds: Set[TechParamId] = taggedKeys[String, Tag.TechParamId](rc.getTechParamMap())
    def configurationIds: Set[ConfigurationId] = taggedKeys[String, Tag.ConfigurationId](rc.getConfigurationMap())
  }

  private def taggedKeys[A, T](m: java.util.Map[A, _]): Set[A @@ T] =
    m.taggedWithFirstF[T].keySet.asScala.toSet
}
