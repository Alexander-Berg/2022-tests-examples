package vsquality.migration.featureregistry

import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.feature.impl.{BasicFeatureTypes, CompositeFeatureTypes, InMemoryFeatureRegistry}
import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vertis.vsquality.utils.cats_utils.{ApplicativeErr, FutureUtil, MonadErr}
import ru.yandex.vertis.vsquality.utils.test_utils.SpecBase
import vsquality.migration.featureregistry.CustomTypes.CustomTypeFeatureType

class FeatureRegistryMigratorSpec extends SpecBase {

  private val featureRegistry: FeatureRegistry = new InMemoryFeatureRegistry(
    new CompositeFeatureTypes(Iterable(BasicFeatureTypes, CustomTypes))
  )

  private val featureRegistryNew: FeatureRegistry = new InMemoryFeatureRegistry(
    new CompositeFeatureTypes(Iterable(BasicFeatureTypes, CustomTypes))
  )

  "Migrator" should {
    val migrator = new FeatureRegistryMigratorImpl[F](
      featureRegistry,
      featureRegistryNew,
      Map(
        "custom_CustomType" -> FeatureRegistryMigratorImpl.transfer[CustomType, F]
      )
    ) with LoggedFeatureRegistryMigrator[F] {
      override protected def component: String = "FeatureRegistryMigrator"
      implicit override protected def ae: ApplicativeErr[F] = MonadErr[F]
    }

    val s: Short = 4
    val b: Byte = 32
    val d: Double = 65d
    val f: Float = 12f
    val i: Int = 12
    val l: Long = 15L
    val str: String = "Hello"
    featureRegistry.register("bool", true)
    featureRegistry.register("short", s)
    featureRegistry.register("byte", b)
    featureRegistry.register("double", d)
    featureRegistry.register("float", f)
    featureRegistry.register("int", i)
    featureRegistry.register("long", l)
    featureRegistry.register("string", str)
    featureRegistry.register("custom", CustomType(1, "John Doe"))

    "migrate successfully" in {
      val result = for {
        oldFeatures <- FutureUtil.toF(featureRegistry.getFeatures)
        newFeatures <- migrator.migrate()
      } yield (oldFeatures, newFeatures)
      val (oldFeatures, newFeatures) = result.unsafeRunSync()
      newFeatures should contain theSameElementsAs oldFeatures
    }
  }
}
