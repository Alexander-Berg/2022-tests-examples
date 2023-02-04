package ru.auto.catalog

import com.google.protobuf.ByteString
import ru.auto.catalog.core.ext.DataTypes
import ru.auto.catalog.core.util.ProtoUtils
import ru.yandex.auto.message.CatalogSchema
import ru.yandex.auto.message.CatalogSchema.{CatalogCardMessage, CatalogDataMessage}
import ru.yandex.extdata.core.DataType
import ru.yandex.vertis.baker.util.extdata.DataDef
import ru.yandex.vertis.protobuf.ProtobufUtils

import java.io._
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

class CarsCatalogGenerator(cards: Seq[CatalogCardMessage], options: CatalogSchema.CatalogOptionHolderMessage) {

  private val actualCards = cards.sliding(2, cards.size / 10).toSeq.flatten ++
    cards.filter(c => c.getMark.getCode == "BMW" && c.getModel.getCode == "X1" && c.getSuperGeneration.getId == 5017453)

  def write(file: File): Unit = {
    val data = ProtobufUtils.writeDelimited(actualCards)
    val cardsStream = new ByteArrayOutputStream(data.length)
    val cardsGzip = new GZIPOutputStream(cardsStream)
    cardsGzip.write(data)
    cardsGzip.close()
    cardsStream.close()

    val result = CatalogDataMessage
      .newBuilder()
      .setCards(ByteString.copyFrom(cardsStream.toByteArray))
      .setOptions(options)
      .setVersion(1)
      .build()
      .toByteArray
    val outStream = new FileOutputStream(file)
    val dataStream = new GZIPOutputStream(outStream)
    dataStream.write(result)
    dataStream.close()
    outStream.close()
  }
}

object CarsCatalogGenerator extends DataDef[CarsCatalogGenerator] {
  override def dataType: DataType = DataTypes.CarsCatalog

  /** parse data object from input stream */
  override def parse(is: InputStream): CarsCatalogGenerator = {
    val originalStream = new GZIPInputStream(is)
    val data = CatalogDataMessage.parseFrom(originalStream)
    val cardsStream = new GZIPInputStream(data.getCards.newInput())
    val cards = ProtoUtils.parseDelimited(CatalogCardMessage.getDefaultInstance, cardsStream)
    val options = data.getOptions
    new CarsCatalogGenerator(cards, options)
  }
}
