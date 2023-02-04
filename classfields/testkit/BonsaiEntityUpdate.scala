package ru.yandex.vertis.general.bonsai.model.testkit

import general.bonsai.attribute_model.AttributeDefinition
import general.bonsai.category_model.Category

trait BonsaiEntityUpdate[T] {
  def withId(entity: T, id: String): T
  def withVersion(entity: T, version: Long): T
}

object BonsaiEntityUpdate {
  def apply[A](implicit sh: BonsaiEntityUpdate[A]): BonsaiEntityUpdate[A] = sh

  implicit class BonsaiEntityUpdateOps[T: BonsaiEntityUpdate](val entity: T) {
    def withId(id: String): T = BonsaiEntityUpdate[T].withId(entity, id)
    def withVersion(version: Long): T = BonsaiEntityUpdate[T].withVersion(entity, version)
  }

  implicit val categoryEntityUpdate: BonsaiEntityUpdate[Category] = new BonsaiEntityUpdate[Category] {
    override def withId(entity: Category, id: String): Category = entity.copy(id = id)

    override def withVersion(entity: Category, version: Long): Category = entity.copy(version = version)
  }

  implicit val attributeEntityUpdate: BonsaiEntityUpdate[AttributeDefinition] =
    new BonsaiEntityUpdate[AttributeDefinition] {
      override def withId(entity: AttributeDefinition, id: String): AttributeDefinition = entity.copy(id = id)

      override def withVersion(entity: AttributeDefinition, version: Long): AttributeDefinition =
        entity.copy(version = version)
    }
}
