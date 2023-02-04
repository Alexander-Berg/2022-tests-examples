package ru.yandex.vertis.feature.impl

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, Matchers, WordSpec}
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.feature.impl.CustomFeatureTypes.ComplexType
import ru.yandex.vertis.feature.model._

import scala.concurrent.Future
import scala.concurrent.duration.{FiniteDuration, _}
import scala.reflect.ClassTag

/**
  * Base specs for [[TypedValueFeatureRegistry]]
  *
  * @author frenki
  */
trait TypedFeatureRegistrySpecBase
  extends WordSpec
    with Matchers
    with ScalaFutures {

  val DefaultFeatureTypes =
    new CompositeFeatureTypes(Iterable(
      BasicFeatureTypes,
      CustomFeatureTypes))

  def syncPeriod: FiniteDuration = 1.millis

  def registry(ft: FeatureTypes = DefaultFeatureTypes): TypedValueFeatureRegistry

  "FeatureRegistry" should {
    "fail if construct registry with duplicate types" in {
      intercept[IllegalArgumentException] {
        registry(new CompositeFeatureTypes(Iterable(BasicFeatureTypes, BasicFeatureTypes)))
      }
    }
    "register basic features with BasicFeatureTypes" in {
      val featureRegistry = registry()
      featureRegistry.register("str_feature", "value")
      featureRegistry.register("bool_feature", false)
    }
    "register all features with CompositeFeatureTypes" in {
      val featureRegistry = registry()
      featureRegistry.register("bool_feature", false) shouldBeInRegistryAs("bool_feature", false)
      featureRegistry.register("byte_feature", 0x1) shouldBeInRegistryAs("byte_feature", 0x1)
      featureRegistry.register("short_feature", Short.MaxValue) shouldBeInRegistryAs("short_feature", Short.MaxValue)
      featureRegistry.register("int_feature", 1) shouldBeInRegistryAs("int_feature", 1)
      featureRegistry.register("float_feature", 6.0f) shouldBeInRegistryAs("float_feature", 6.0f)
      featureRegistry.register("double_feature", 5.0) shouldBeInRegistryAs("double_feature", 5.0)
      featureRegistry.register("str_feature", "value") shouldBeInRegistryAs("str_feature", "value")
      featureRegistry.register("custom_feature", ComplexType(0, 0)) shouldBeInRegistryAs("custom_feature", ComplexType(0, 0))
    }
    "register feature with meta" in {
      val featureRegistry = registry()
      featureRegistry.register("int_feature", 1, Set("int", "decimal"), Some("one"))

      val expected = TypedValue("integer", "1", Set("int", "decimal"), Some("one"))
      extractFromRegistry(featureRegistry, "int_feature") shouldBe expected
    }
    "return all registered features" in {
      val featureRegistry = registry()
      featureRegistry.register("str_feature", "value")
      featureRegistry.register("bool_feature", false)
      featureRegistry.register("int_feature", 1)
      val features = featureRegistry.getFeatures.futureValue
        .map(f => f.name -> f.value.value).toMap
      features("str_feature") shouldBe "value"
      features("bool_feature") shouldBe "false"
      features("int_feature") shouldBe "1"
    }
    "update feature value with correct type" in {
      val featureRegistry = registry()
      val name = "ct"
      val value = ComplexType(0, 0)

      val f = featureRegistry.register(name, value)
      f shouldBeInRegistryAs(name, value)

      val newValue = ComplexType(2, 5)
      featureRegistry.updateFeature(name, newValue.toString).futureValue
      Thread.sleep(syncPeriod.toMillis)

      f shouldBeInRegistryAs(name, newValue)
    }
    "update feature value with correct type using typed update method" in {
      val featureRegistry = registry()
      val name = "ct"
      val value = ComplexType(0, 0)

      val f = featureRegistry.register(name, value)
      f shouldBeInRegistryAs(name, value)

      val newValue = ComplexType(2, 5)
      featureRegistry.updateFeature(name, newValue).futureValue
      Thread.sleep(syncPeriod.toMillis)

      f shouldBeInRegistryAs(name, newValue)
    }
    "fail to update feature value with wrong type" in {
      val featureRegistry = registry()
      val name = "int_feature"

      featureRegistry.register(name, 122)
      featureRegistry.updateFeature(name, "new_value")
        .shouldCompleteWithException[IllegalArgumentException]
    }
    "fail to update feature value with wrong type using typed update method" in {
      val featureRegistry = registry()
      val name = "int_feature"

      featureRegistry.register(name, 122)
      featureRegistry.updateFeature(name, false)
        .shouldCompleteWithException[IllegalArgumentException]
    }
    "delete registered feature and return true" in {
      val featureRegistry = registry()
      val name = "int_feature"

      featureRegistry.register(name, 122)
      featureRegistry.deleteFeature(name).futureValue shouldBe true
      featureRegistry.getFeatures.futureValue.isEmpty shouldBe true
    }
    "return false on calling delete for nonexistent feature" in {
      val featureRegistry = registry()

      featureRegistry.deleteFeature("new_feature").futureValue shouldBe false
    }
    "update feature tags" in {
      val featureRegistry = registry()
      val name = "int"
      val value = 1
      val tags = Set("tag")
      val info = Some("info")

      val expected = TypedValue("integer", value.toString, tags, info)

      featureRegistry.register(name, value, tags, info)
      extractFromRegistry(featureRegistry, name) shouldBe expected

      val newTags = Set("new tag")
      featureRegistry.updateFeatureTags(name, newTags).futureValue
      Thread.sleep(syncPeriod.toMillis)

      extractFromRegistry(featureRegistry, name) shouldBe expected.copy(tags = newTags)
    }
    "update feature info" in {
      val featureRegistry = registry()
      val name = "int"
      val value = 1
      val tags = Set("tag")
      val info = Some("info")

      val expected = TypedValue("integer", value.toString, tags, info)

      featureRegistry.register(name, value, tags, info)
      extractFromRegistry(featureRegistry, name) shouldBe expected

      val newInfo = Some("new info")
      featureRegistry.updateFeatureInfo(name, newInfo).futureValue
      Thread.sleep(syncPeriod.toMillis)

      extractFromRegistry(featureRegistry, name) shouldBe expected.copy(info = newInfo)
    }
    "update only tags and info on second registration " in {
      val featureRegistry = registry()
      val name = "int"
      val value = 1
      val tags = Set("tag")
      val info = Some("info")

      val expected = TypedValue("integer", value.toString, tags, info)

      featureRegistry.register(name, value, tags, info)
      extractFromRegistry(featureRegistry, name) shouldBe expected

      val newTags = Set("new tag")
      val newInfo = Some("new info")
      val newValue = 111
      featureRegistry.register(name, newValue, newTags, newInfo)
      Thread.sleep(syncPeriod.toMillis)

      extractFromRegistry(featureRegistry, name) shouldBe expected.copy(tags = newTags, info = newInfo)
    }
  }

  private def extractFromRegistry(registry: FeatureRegistry, name: String): TypedValue =
    registry.getFeatures.futureValue.find(_.name == name).get.value

  implicit class TestFuture[T](val future: Future[T]) {
    def shouldCompleteWithException[A <: Throwable: ClassTag]: Assertion =
      whenReady(future.failed) { e =>
        e shouldBe a [A]
      }
  }

  implicit class TestFeature[T](val feature: Feature[T]) {
    def shouldBeInRegistryAs(name: String, value: T): Assertion = {
      feature.name shouldBe name
      feature.value shouldBe value
    }
  }

}
