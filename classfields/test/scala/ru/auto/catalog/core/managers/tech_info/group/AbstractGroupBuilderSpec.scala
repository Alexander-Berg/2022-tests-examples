package ru.auto.catalog.core.managers.tech_info.group

import org.scalatest.Assertion
import ru.auto.catalog.BaseSpec
import ru.auto.catalog.model.api.ApiModel

import scala.jdk.CollectionConverters._

abstract class AbstractGroupBuilderSpec extends BaseSpec {

  private def findEntity(result: ApiModel.HumanTechInfoGroup, id: String) =
    result.getEntityList.asScala.find(_.getId == id)

  def checkEmpty(result: ApiModel.HumanTechInfoGroup, id: String): Assertion = {
    findEntity(result, id) shouldBe empty
  }

  def checkValue(result: ApiModel.HumanTechInfoGroup, id: String, value: String): Assertion = {
    val entity = findEntity(result, id)
    entity should not be empty
    entity.get.getValue shouldBe value
  }

}
