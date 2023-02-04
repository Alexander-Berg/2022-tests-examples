package ru.yandex.realty.flatplan

import java.io.ByteArrayOutputStream

import com.yandex.yoctodb.immutable.IndexedDatabase
import com.yandex.yoctodb.util.buf.Buffer
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.context.v2.FlatPlansIndex
import ru.yandex.realty.context.v2.index.ParsedYoctoIndexProvider
import ru.yandex.realty.model.message.ExtDataSchema.FlatPlan
import ru.yandex.realty.util.Mappings._

import scala.collection.JavaConverters.seqAsJavaListConverter

object FlatPlanSearcherTestUtils {

  case class Plan(
    series: Long,
    parent: Option[Long],
    variant: String,
    totalArea: Float,
    livingArea: Float,
    roomAreas: Seq[Float],
    kitchenArea: Float,
    link: String
  ) {

    def toProto: FlatPlan = {
      FlatPlan
        .newBuilder()
        .setBuildingSeriesCode(series.toString)
        .setParentSeriesCode(parent.map(_.toString).getOrElse(""))
        .setBuildingSeriesVariantCode(variant)
        .setTotalArea(totalArea)
        .setLivingArea(livingArea)
        .addAllRoomArea(roomAreas.map(Float.box).asJava)
        .setKitchenArea(kitchenArea)
        .setImageLinkSvg(link)
        .build()
    }
  }

  def createSearcherOverPlans(plans: Plan*): FlatPlanSearcher = {
    val bytes = {
      val baos = new ByteArrayOutputStream()
      FlatPlansIndex.YoctoFormat
        .newDatabaseBuilder()
        .applyTransforms[Plan](plans, (b, p) => b.merge(FlatPlansIndex.buildYoctoDocument(p.toProto)))
        .buildWritable()
        .writeTo(baos)
      baos.toByteArray
    }
    val plansIndex = FlatPlansIndex.YoctoFormat.getDatabaseReader.from(Buffer.from(bytes))
    new FlatPlanSearcher(
      indexProvider = new ParsedYoctoIndexProvider[FlatPlansIndex.Condition, FlatPlan](
        new Provider[IndexedDatabase] {
          override def get(): IndexedDatabase = plansIndex
        },
        FlatPlan.parseFrom
      )
    )
  }

}
