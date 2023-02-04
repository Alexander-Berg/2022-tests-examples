package ru.yandex.vertis.moderation.view.json

import org.junit.runner.RunWith
import org.scalacheck.Prop.forAll
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop.Checkers.check
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.signal.HoboSignalSourceInternal
import ru.yandex.vertis.moderation.view.ViewCompanion.MarshallingContext
import ru.yandex.vertis.moderation.view.{View, ViewCompanion}

/**
  * Specs on json view classes
  *
  * @author sunlight
  */
@RunWith(classOf[JUnitRunner])
class ViewSpec extends SpecBase {

  implicit private val marshallingContext: MarshallingContext = MarshallingContext(ServiceGen.next)

  "Instance" should {
    "correctly converts" in {
      check(forAll(InstanceGen)(checking(_, InstanceView)))
    }
  }

  "SignalSource" should {

    "correctly converts" in {
      check(forAll(SignalSourceGen.suchThat(!_.isInstanceOf[HoboSignalSourceInternal])) {
        checking(_, SignalSourceView)
      })
    }
  }

  "Metadata" should {

    "correctly converts" in {
      check(forAll(MetadataGen)(checking(_, MetadataView)))
    }
  }

  "MetadataSource" should {

    "correctly converts" in {
      check(forAll(MetadataSourceGen)(checking(_, MetadataSourceView)))
    }
  }

  private def checking[M, V <: View[M]](modelObject: M, viewCompanion: ViewCompanion[V, M]): Boolean =
    modelObject == viewCompanion.asView(modelObject).asModel
}
