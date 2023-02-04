package vertis.palma.gen

import org.scalacheck.Gen
import vertis.palma.test.{Color, Mark, Model}
import vertis.palma.gen.ModelGenerators.{DictionaryItemGen, StrGen}
import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.proto.util.RandomProtobufGenerator
import vertis.palma.DictionaryId
import vertis.palma.dao.model.DictionaryItem
import vertis.palma.dao.model.DictionaryItem.Id

/** @author kusaeva
  */
object ModelProtoGenerators extends ProducerProvider {

  val markGen: Gen[Mark] =
    for {
      model <- RandomProtobufGenerator.genForAuto[Mark]
    } yield model.toBuilder
      .setCode(StrGen.next)
      .build

  def modelGen(mark: Mark): Gen[Model] = {
    for {
      model <- RandomProtobufGenerator.genForAuto[Model]
    } yield model.toBuilder
      .setMark(mark)
      .setCode(StrGen.next)
      .build
  }

  def colorGen(alias: String): Gen[Color] = {
    for {
      color <- RandomProtobufGenerator.genForAuto[Color]
    } yield color.toBuilder
      .setAlias(alias)
      .setCode(StrGen.next)
      .build
  }

  def dictionaryItemGen(dictionaryId: DictionaryId): Gen[DictionaryItem] =
    for {
      item <- DictionaryItemGen
    } yield item.copy(id = Id(dictionaryId = dictionaryId, key = item.id.key))
}
