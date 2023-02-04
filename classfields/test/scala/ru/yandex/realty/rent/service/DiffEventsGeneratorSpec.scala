package ru.yandex.realty.rent.service

import akka.actor.ActorSystem
import akka.kafka.testkit.ConsumerResultFactory
import akka.stream.ActorMaterializer
import com.google.protobuf.Timestamp
import com.google.protobuf.util.Timestamps
import org.apache.kafka.clients.producer.RecordMetadata
import org.junit.runner.RunWith
import org.scalamock.handlers.CallHandler2
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.application.ng.kafka.producer.SimpleKafkaProducerWithKeys
import ru.yandex.realty.model.transfer
import ru.yandex.realty.model.transfer.DataTransferChangeItem
import ru.yandex.realty.model.transfer.DataTransferChangeItem.{
  ColumnValue,
  Kind,
  LongColumnValue,
  NullColumnValue,
  StringColumnValue
}
import ru.yandex.realty.model.transfer.DataTransferChangeItem.Kind.Kind
import ru.yandex.realty.rent.application.DefaultYdbDaoSupplier
import ru.yandex.realty.rent.dao.TestYdbSupplier
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.proto.model.diffevent.{DiffEvent, FlatDiffEvent, FlatProtoView}
import ru.yandex.realty.rent.service.impl.DiffEventsGeneratorImpl
import ru.yandex.realty.util.Mappings._
import ru.yandex.vertis.ops.test.TestOperationalSupport

import java.nio.charset.StandardCharsets
import java.util.Base64
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class DiffEventsGeneratorSpec extends AsyncSpecBase with TestYdbSupplier with DefaultYdbDaoSupplier with RentModelsGen {

  "DataTransferChangeItem" should {

    "parse null columnnames" in {
      val s = DataTransferChangeItem.parseJson(
        """
          |{"id":2253124363,"nextlsn":4962000052291134,"commitTime":0,"txPosition":0,"kind":"init_load_table",
          |"schema":"realty_rent","table":"insurance_policy","columnnames":null,"oldkeys":{},
          |"tx_id":"9fb59c80-234d-11eb-9eeb-2492707b1701:1-20697958,\nd8018b87-234d-11eb-8096-553f3e779caa:1-35350678","query":""}
          """.stripMargin.getBytes
      )
      println(s)
    }
  }

  "DIffEventsGenerator.process" should {
    "send updates" in new Wiring {
      override val flatId = "test_1"
      val insertEntity =
        flatProtoViewGen.next.toBuilder.setFlatId(flatId).setUpdateTime(Timestamp.newBuilder().setSeconds(1)).build()
      val updateEntity =
        flatProtoViewGen.next.toBuilder.setFlatId(flatId).setUpdateTime(Timestamp.newBuilder().setSeconds(2)).build()

      inSequence {
        producerExpectation(newEntity = Some(insertEntity))
        producerExpectation(oldEntity = Some(insertEntity), newEntity = Some(updateEntity))
        producerExpectation(oldEntity = Some(updateEntity))
      }

      val items = List(
        convertEntityToChangeItem(insertEntity, Kind.Insert, 1, 1),
        convertEntityToChangeItem(updateEntity, Kind.Update, 1, 2),
        convertEntityToChangeItem(updateEntity, Kind.Delete, 2, 1)
      )
      runGenerator(items).futureValue
    }

    "not resend events from the past" in new Wiring {
      override val flatId = "test_2"
      val insertEntity =
        flatProtoViewGen.next.toBuilder.setFlatId(flatId).setUpdateTime(Timestamp.newBuilder().setSeconds(1)).build()
      val updateEntity =
        flatProtoViewGen.next.toBuilder.setFlatId(flatId).setUpdateTime(Timestamp.newBuilder().setSeconds(2)).build()

      inSequence {
        producerExpectation(newEntity = Some(insertEntity))
        producerExpectation(oldEntity = Some(insertEntity), newEntity = Some(updateEntity))
        producerExpectation(oldEntity = Some(updateEntity))
      }

      val insertVersion = (5L, 15)
      val updateVersion = (-1L, 10) // -1 is max unsigned value
      val deleteVersion = (-1L, 11)

      val items = List(
        convertEntityToChangeItem(insertEntity, Kind.Insert, insertVersion), // first insert
        convertEntityToChangeItem(insertEntity, Kind.Insert, insertVersion), // skip
        //---------
        convertEntityToChangeItem(updateEntity, Kind.Update, updateVersion), // first update
        convertEntityToChangeItem(updateEntity, Kind.Update, updateVersion), // skip
        convertEntityToChangeItem(insertEntity, Kind.Insert, insertVersion), // skip
        //---------
        convertEntityToChangeItem(updateEntity, Kind.Delete, deleteVersion), // first delete
        convertEntityToChangeItem(updateEntity, Kind.Delete, deleteVersion), // skip
        //---------
        convertEntityToChangeItem(insertEntity, Kind.Insert, insertVersion), // skip
        convertEntityToChangeItem(updateEntity, Kind.Update, updateVersion), // skip
        convertEntityToChangeItem(updateEntity, Kind.Delete, deleteVersion) // skip
      )
      runGenerator(items).futureValue
    }
  }

  private def convertEntityToChangeItem(
    entity: FlatProtoView,
    kind: Kind,
    version: (Long, Int)
  ): DataTransferChangeItem = convertEntityToChangeItem(entity, kind, lsn = version._1, txPosition = version._2)

  private def convertEntityToChangeItem(
    entity: FlatProtoView,
    kind: Kind,
    lsn: Long,
    txPosition: Int
  ): DataTransferChangeItem = {
    val fieldMap = convertEntityFieldMap(entity)
    transfer.DataTransferChangeItem(
      id = 0,
      nextlsn = lsn,
      commitTime = 0,
      txPosition = txPosition,
      kind = kind,
      schema = "realty_rent",
      table = "flat",
      columnnames = Some(fieldMap.keys.toList),
      columnvalues = Some(fieldMap.values.toList)
    )
  }

  private def convertEntityFieldMap(entity: FlatProtoView): Map[String, ColumnValue] =
    Map(
      "id" -> LongColumnValue(entity.getId),
      "flat_id" -> StringColumnValue(entity.getFlatId),
      "code" -> entity.getCode
        .wrapInOption()
        .filter(_ => entity.hasCode)
        .map(_.getValue)
        .fold[ColumnValue](NullColumnValue)(StringColumnValue),
      "data" -> StringColumnValue(Base64.getEncoder.encodeToString(entity.getData.toByteArray)),
      "data_json" -> StringColumnValue(
        Base64.getEncoder.encodeToString(entity.getDataJson.getBytes(StandardCharsets.UTF_8))
      ),
      "address" -> StringColumnValue(entity.getAddress),
      "unified_address" -> entity.getUnifiedAddress
        .wrapInOption()
        .filter(_ => entity.hasUnifiedAddress)
        .map(_.getValue)
        .fold[ColumnValue](NullColumnValue)(StringColumnValue),
      "flat_number" -> StringColumnValue(entity.getFlatNumber),
      "name_from_request" -> entity.getNameFromRequest
        .wrapInOption()
        .filter(_ => entity.hasNameFromRequest)
        .map(_.getValue)
        .fold[ColumnValue](NullColumnValue)(StringColumnValue),
      "phone_from_request" -> entity.getPhoneFromRequest
        .wrapInOption()
        .filter(_ => entity.hasPhoneFromRequest)
        .map(_.getValue)
        .fold[ColumnValue](NullColumnValue)(StringColumnValue),
      "create_time" -> StringColumnValue(Timestamps.toString(entity.getCreateTime)),
      "update_time" -> StringColumnValue(Timestamps.toString(entity.getUpdateTime)),
      "visit_time" -> entity.getVisitTime
        .wrapInOption()
        .filter(_ => entity.hasVisitTime)
        .map(Timestamps.toString)
        .fold[ColumnValue](NullColumnValue)(StringColumnValue),
      "shard_key" -> entity.getShardKey
        .wrapInOption()
        .filter(_ => entity.hasShardKey)
        .map(_.getValue.toLong)
        .fold[ColumnValue](NullColumnValue)(LongColumnValue),
      "is_rented" -> LongColumnValue(if (entity.getIsRented) 1 else 0),
      "key_code" -> entity.getKeyCode
        .wrapInOption()
        .filter(_ => entity.hasKeyCode)
        .map(_.getValue)
        .fold[ColumnValue](NullColumnValue)(StringColumnValue)
    )

  private trait Wiring {
    protected def flatId: String

    val producerMock = mock[SimpleKafkaProducerWithKeys[String, DiffEvent]]

    implicit private val system: ActorSystem = ActorSystem("test")
    implicit private val materializer: ActorMaterializer = ActorMaterializer()
    implicit private val ops = TestOperationalSupport

    private val generator = new DiffEventsGeneratorImpl(
      entityCacheDao,
      producerMock
    )

    private def createCommitableOffset(offset: Int) = ConsumerResultFactory.committableOffset(
      groupId = "group",
      topic = "topic",
      partition = 0,
      offset = offset,
      metadata = ""
    )

    def runGenerator(elements: List[DataTransferChangeItem]): Future[Unit] = {
      val batch = elements.zipWithIndex.map {
        case (item, index) => (createCommitableOffset(index).partitionOffset, item)
      }
      generator.processBatch(batch)
    }

    def producerExpectation(
      oldEntity: Option[FlatProtoView] = None,
      newEntity: Option[FlatProtoView] = None
    ): CallHandler2[String, DiffEvent, Future[RecordMetadata]] =
      (producerMock.send _)
        .expects(
          flatId,
          DiffEvent
            .newBuilder()
            .setFlatEvent(
              FlatDiffEvent
                .newBuilder()
                .applyTransformIf(oldEntity.isDefined, _.setOld(oldEntity.get))
                .applyTransformIf(newEntity.isDefined, _.setNew(newEntity.get))
            )
            .build()
        )
        .returning(Future.successful(null))
        .once()
  }
}
