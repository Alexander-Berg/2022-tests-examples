package ru.yandex.realty.rent.backend

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.application.ng.s3.S3Client
import ru.yandex.realty.features.{Features, SimpleFeatures}
import ru.yandex.realty.mds.s3.S3ClientConfig
import ru.yandex.realty.picapica.MdsUrlBuilder
import ru.yandex.realty.rent.backend.XmlFeedManager.FeedStreamDSL
import ru.yandex.realty.rent.dao.{FlatDao, FlatQuestionnaireDao, OwnerRequestDao}
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.feed.FeedEntry
import ru.yandex.realty.rent.proto.api.moderation.ClassifiedTypeNamespace
import ru.yandex.realty.rent.proto.model.flat.ClassifiedsPubStatusInternal
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.Mappings.MapAny
import ru.yandex.realty.AsyncSpecBase

import scala.collection.JavaConverters._
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

@RunWith(classOf[JUnitRunner])
class XmlFeedManagerSpec extends WordSpec with AsyncSpecBase with Matchers with RentModelsGen with MockFactory {
  import FeedStreamDSL._

  implicit val traced: Traced = Traced.empty

  "FeedStreamDSL.buildByteStringFlow" should {
    "return flow of bytes" in new Wiring with Data {
      val flow: Flow[FeedEntry, ByteString, NotUsed] = buildByteStringFlow(yandexBuilder)
      val simpleEntries = Seq(
        feedEntryGen.next
          .applyTransform { e =>
            e.copy(
              flat = e.flat.copy(
                flatId = "F1",
                data = e.flat.data.toBuilder
                  .applySideEffect(
                    _.getLocationBuilder
                      .setSubjectFederationGeoid(101)
                      .setSubjectFederationRgid(101)
                  )
                  .addAllClassifiedsPubStatuses(statuses.asJava)
                  .build()
              )
            )
          },
        feedEntryGen.next
          .applyTransform { e =>
            e.copy(
              flat = e.flat.copy(
                flatId = "F2",
                data = e.flat.data.toBuilder
                  .applySideEffect(
                    _.getLocationBuilder
                      .setSubjectFederationGeoid(101)
                      .setSubjectFederationRgid(101)
                  )
                  .addAllClassifiedsPubStatuses(statuses.asJava)
                  .build()
              )
            )
          },
        feedEntryGen.next
          .applyTransform { e =>
            e.copy(
              flat = e.flat.copy(
                flatId = "F3",
                data = e.flat.data.toBuilder
                  .applySideEffect(
                    _.getLocationBuilder
                      .setSubjectFederationGeoid(101)
                      .setSubjectFederationRgid(101)
                  )
                  .addAllClassifiedsPubStatuses(statuses.asJava)
                  .build()
              )
            )
          },
        feedEntryGen.next
          .applyTransform(
            e =>
              e.copy(
                flat = e.flat.copy(
                  flatId = "F4",
                  data = e.flat.data.toBuilder
                    .applySideEffect(
                      _.getLocationBuilder
                        .setSubjectFederationGeoid(101)
                        .setSubjectFederationRgid(101)
                    )
                    .build()
                )
              )
          )
      )

      val source: Source[FeedEntry, NotUsed] = Source(simpleEntries.toList)
      val bytes: Seq[ByteString] = source.via(flow).runWith(Sink.seq).futureValue
      val str = new String(bytes.flatMap(_.toList).toArray)

      val expected: String =
        """
          |YANDEX_REALTY {
          |  YANDEX_REALTY_entry { geoId: "101", flatId: "F1" }
          |  YANDEX_REALTY_entry { geoId: "101", flatId: "F2" }
          |  YANDEX_REALTY_entry { geoId: "101", flatId: "F3" }
          |}
          |""".stripMargin.trim

      str shouldEqual expected
    }
  }

  "FeedStreamDSL.buildValidatorsFlow" should {
    "return flow with validator which pass all entries" in new Wiring with Data {
      val flow: Flow[FeedEntry, FeedEntry, NotUsed] =
        buildValidatorsFlow(Seq(mockSampleValidator, mockGeneralValidator))

      (mockSampleValidator.validate _)
        .expects(*)
        .repeated(sampleEntries.size)
        .returns(Nil)

      (mockGeneralValidator.validate _)
        .expects(*)
        .repeated(sampleEntries.size)
        .returns(Nil)

      val result: Seq[FeedEntry] = sampleSource.via(flow).runWith(Sink.seq).futureValue

      result should contain theSameElementsAs sampleEntries
    }

    "return flow with validator which skip all entries" in new Wiring with Data {
      val flow: Flow[FeedEntry, FeedEntry, NotUsed] =
        buildValidatorsFlow(Seq(mockSampleValidator, mockGeneralValidator))

      // validator #1
      (mockSampleValidator.validate _)
        .expects(where((e: FeedEntry) => e.flat.subjectFederationId.isDefined))
        .repeated(0 to sampleEntries.size)
        .returns(Nil)
      (mockSampleValidator.validate _)
        .expects(where((e: FeedEntry) => e.flat.subjectFederationId.isEmpty))
        .repeated(0 to sampleEntries.size)
        .returns(Seq(XmlGeneralFeedValidatorCode.AreaNotFound))

      // validator #2
      (mockGeneralValidator.validate _)
        .expects(where((e: FeedEntry) => e.flat.subjectFederationId.isEmpty))
        .repeated(0 to sampleEntries.size)
        .returns(Nil)
      (mockGeneralValidator.validate _)
        .expects(where((e: FeedEntry) => e.flat.subjectFederationId.isDefined))
        .repeated(0 to sampleEntries.size)
        .returns(Seq(XmlGeneralFeedValidatorCode.DescriptionNotFound))

      // entries which passed by validator #1 by not passed by validator #2
      val entries1: Seq[FeedEntry] = sampleEntriesWithGeo.flatten
      // entries which passed by validator #2 by not passed by validator #1
      val entries2: Seq[FeedEntry] = sampleEntriesWithoutGeo
      val src: Source[FeedEntry, NotUsed] = Source(entries1.toList ::: entries2.toList)

      val result: Seq[FeedEntry] = src.via(flow).runWith(Sink.seq).futureValue
      result shouldBe empty
    }

    "return flow with validator which skip entries without geo and data" in new Wiring with Data {
      val flow: Flow[FeedEntry, FeedEntry, NotUsed] =
        buildValidatorsFlow(Seq(mockSampleValidator, mockGeneralValidator))

      // validator #1
      (mockSampleValidator.validate _)
        .expects(where((e: FeedEntry) => e.flat.subjectFederationId.isDefined))
        .repeated(0 to sampleEntries.size)
        .returns(Nil)
      (mockSampleValidator.validate _)
        .expects(where((e: FeedEntry) => e.flat.subjectFederationId.isEmpty))
        .repeated(0 to sampleEntries.size)
        .returns(Seq(XmlGeneralFeedValidatorCode.AreaNotFound))

      // validator #2
      (mockGeneralValidator.validate _)
        .expects(where((e: FeedEntry) => e.flat.address.nonEmpty))
        .repeated(0 to sampleEntries.size)
        .returns(Nil)
      (mockGeneralValidator.validate _)
        .expects(where((e: FeedEntry) => e.flat.address.isEmpty))
        .repeated(0 to sampleEntries.size)
        .returns(Seq(XmlGeneralFeedValidatorCode.DescriptionNotFound))

      def clearAddress(e: FeedEntry): FeedEntry = e.copy(flat = e.flat.copy(address = ""))

      val (entries11, tmpEntries) = sampleEntriesWithGeo.flatten.splitAt(sampleEntriesWithGeo.size / 2)
      val entries10 = tmpEntries.map(clearAddress)
      val entries01 = sampleEntriesWithoutGeo
      val entries00 = sampleEntriesWithoutGeo.map(clearAddress)

      val src: Source[FeedEntry, NotUsed] = Source(entries11.toList ++ entries10 ++ entries01 ++ entries00)

      val result: Seq[FeedEntry] = src.via(flow).runWith(Sink.seq).futureValue
      result should contain theSameElementsAs entries11
    }
  }

  "FeedStreamDSL.buildValidatorFlow" should {
    "return flow with validator which pass all entries" in new Wiring with Data {
      val flow: Flow[FeedEntry, FeedEntry, NotUsed] = buildValidatorFlow(mockSampleValidator)

      (mockSampleValidator.validate _)
        .expects(*)
        .repeated(sampleEntries.size)
        .returns(Nil)

      val result: Seq[FeedEntry] = sampleSource.via(flow).runWith(Sink.seq).futureValue

      result should contain theSameElementsAs sampleEntries
    }

    "return flow with validator which skip all entries without geo" in new Wiring with Data {
      val flow: Flow[FeedEntry, FeedEntry, NotUsed] = buildValidatorFlow(mockSampleValidator)

      (mockSampleValidator.validate _)
        .expects(where((e: FeedEntry) => e.flat.subjectFederationId.isDefined))
        .repeated(1 to sampleEntries.size)
        .returns(Nil)

      (mockSampleValidator.validate _)
        .expects(where((e: FeedEntry) => e.flat.subjectFederationId.isEmpty))
        .repeated(1 to sampleEntries.size)
        .returns(Seq(XmlGeneralFeedValidatorCode.AreaNotFound))

      val result: Seq[FeedEntry] = sampleSource.via(flow).runWith(Sink.seq).futureValue
      result should contain theSameElementsAs sampleEntriesWithGeo.flatten
    }

    "return flow with validator which skip all entries" in new Wiring with Data {
      val flow: Flow[FeedEntry, FeedEntry, NotUsed] = buildValidatorFlow(mockSampleValidator)

      (mockSampleValidator.validate _)
        .expects(*)
        .repeated(sampleEntries.size)
        .returns(Seq(XmlGeneralFeedValidatorCode.AreaNotFound))

      val result: Seq[FeedEntry] = sampleSource.via(flow).runWith(Sink.seq).futureValue
      result shouldBe empty
    }
  }

  "FeedStreamDSL.GroupByDefaultFlow" should {
    "return a flow with single group which contains all geos" in new Wiring with Data {
      val result: Seq[Seq[(GeoId, FeedEntry)]] = sampleSource
        .via(FeedStreamDSL.GroupByDefaultFlow)
        .mapAsync(4) {
          case (geoId, src) =>
            src
              .map(e => geoId -> e)
              .runWith(Sink.seq)
        }
        .runWith(Sink.seq)
        .futureValue

      result.size shouldEqual 1
      val group: Seq[(GeoId, FeedEntry)] = result.head
      group.forall(_._1 == XmlFeedManager.DefaultGeoId) shouldEqual true
      group.map(_._2) should contain theSameElementsAs sampleEntries
    }
  }

  "FeedStreamDSL.GroupByGeoFlow" should {
    "return a flow grouped by geo" in new Wiring with Data {
      val result: Seq[Seq[(GeoId, FeedEntry)]] = sampleSource
        .via(FeedStreamDSL.GroupByGeoFlow)
        .mapAsync(4) {
          case (geoId, src) =>
            src
              .map(e => geoId -> e)
              .runWith(Sink.seq)
        }
        .runWith(Sink.seq)
        .futureValue

      // check size of groups
      result.size shouldEqual (sampleGeoIds.size + 1)

      // check groups integrity
      val groupedByGeoResult: Map[GeoId, Seq[FeedEntry]] = result.flatten.groupBy(_._1).mapValues(_.map(_._2))

      def checkKeyAndValue(key: GeoId, group: Seq[FeedEntry]): Boolean =
        group forall (_.flat.subjectFederationId.contains(key))

      groupedByGeoResult.foreach {
        case (GeoId0, group) =>
          checkKeyAndValue(GeoId0, group) shouldEqual true
          group should contain theSameElementsAs geoId0Entries

        case (GeoId1, group) =>
          checkKeyAndValue(GeoId1, group) shouldEqual true
          group should contain theSameElementsAs geoId1Entries

        case (GeoId2, group) =>
          checkKeyAndValue(GeoId2, group) shouldEqual true
          group should contain theSameElementsAs geoId2Entries

        case (XmlFeedManager.UnknownGeoId, group) =>
          group forall (_.flat.subjectFederationId.isEmpty) shouldEqual true
          group should contain theSameElementsAs sampleEntriesWithoutGeo

        case unknownGroup => fail(s"wrong unknown group $unknownGroup")
      }
    }
  }

  //TODO REALTYBACK-7529 tmp disabled
//  "XmlFeedManager.publishFeedXml" should {
//    "publish feeds successfully" in new Wiring with Data {
//      (mockFlatQuestionnaireDao
//        .questionnairesStream(_: Traced))
//        .expects(*)
//        .once()
//        .returning(sampleSource)
//
//      (mockGeneralValidator.validate _)
//        .expects(*)
//        .repeated(sampleEntries.size)
//        .returns(Nil)
//
//      val sinks: Iterable[StringSink] = expectedFeeds.keys map mockSink
//
//      manager.publishFeedXml.futureValue
//
//      val result: Map[String, String] =
//        Future.sequence(sinks.map(sink => sink.content.map(res => (sink.name, res)))).map(_.toMap).futureValue
//
//      result should contain theSameElementsAs expectedFeeds.mapValues(_.trim)
//    }
//
//    "REALTYBACK-6953 if some elements for YANDEX_REALTY are disabled then they are disabled for JCAT too" in new Wiring
//    with Data {
//      (mockFlatQuestionnaireDao
//        .questionnairesStream(_: Traced))
//        .expects(*)
//        .once()
//        .returning(sampleSource.map {
//          // disable publication in YANDEX_REALTY for flats with geo=101
//          case e if e.flat.subjectFederationId.contains(101) =>
//            val (Seq(yaPubStatus), pubStatuses) = e.flat.data.getClassifiedsPubStatusesList.asScala
//              .partition(_.getClassifiedType == ClassifiedTypeNamespace.ClassifiedType.YANDEX_REALTY)
//            val disabledYaPubStatus = yaPubStatus.toBuilder.setEnabled(false).build()
//            val newPubStatuses = pubStatuses :+ disabledYaPubStatus
//
//            val data = e.flat.data.toBuilder
//              .clearClassifiedsPubStatuses()
//              .addAllClassifiedsPubStatuses(newPubStatuses.asJava)
//              .build()
//
//            e.copy(flat = e.flat.copy(data = data))
//          case e => e
//        })
//
//      (mockGeneralValidator.validate _)
//        .expects(*)
//        .repeated(sampleEntries.size)
//        .returns(Nil)
//
//      val sinks: Iterable[StringSink] = expectedFeedsWithout101GeoForYANDEX_and_JCAT.keys map mockSink
//
//      manager.publishFeedXml.futureValue
//
//      val result: Map[String, String] =
//        Future.sequence(sinks.map(sink => sink.content.map(res => (sink.name, res)))).map(_.toMap).futureValue
//
//      result should contain theSameElementsAs expectedFeedsWithout101GeoForYANDEX_and_JCAT.mapValues(_.trim)
//    }
//  }

  trait Wiring {
    val mockFlatDao: FlatDao = mock[FlatDao]
    val mockFlatQuestionnaireDao: FlatQuestionnaireDao = mock[FlatQuestionnaireDao]
    val mockOwnerRequestDao: OwnerRequestDao = mock[OwnerRequestDao]
    val stubS3Client: S3Client = stub[S3Client]

    val mockS3Config: S3ClientConfig = S3ClientConfig(
      bucket = "sampleBucket",
      url = "localhost",
      keyId = "sampleKeyId",
      keySecret = "sampleKeySecret",
      connectionTimeout = 0,
      requestTimeout = 0,
      maxConnections = 0,
      numberOfRetries = 0
    )

    trait JsonRepresentationBuilder {
      this: XmlFeedBuilder =>
      val name: String = classifiedType.name()

      override def head: String = s"$name {"

      override def body(entry: FeedEntry): String =
        s"""\n  ${name}_entry { geoId: "${entry.flat.subjectFederationId
          .getOrElse("")}", flatId: "${entry.flat.flatId}" }"""

      override def tail: String = "\n}"

      override def validators: Seq[FeedEntry => Option[XmlFeedValidatorCode#Value]] = Nil
    }

    trait NoSplitByGeo {
      this: XmlFeedBuilder =>
      override def splitByGeo: Boolean = false
    }

    val features: Features = new SimpleFeatures()
    val mockMdsUrlBuilder = new MdsUrlBuilder("//localhost:8080")
    val yandexBuilder = new XmlYandexFeedBuilder(mockMdsUrlBuilder) with JsonRepresentationBuilder
    val cianBuilder = new XmlCianFeedBuilder(mockMdsUrlBuilder, features) with JsonRepresentationBuilder

    val avitoBuilder =
      new XmlAvitoFeedBuilder(mockMdsUrlBuilder, features) with JsonRepresentationBuilder with NoSplitByGeo
    val jcatBuilder = new XmlJcatFeedBuilder(mockMdsUrlBuilder) with JsonRepresentationBuilder with NoSplitByGeo

    val builders = Seq(
      yandexBuilder,
      cianBuilder,
      avitoBuilder,
      jcatBuilder
    )

    implicit val system: ActorSystem = ActorSystem("test")

    val mockSampleValidator: XmlFeedValidator = mock[XmlFeedValidator]
    (mockSampleValidator.validatorName _).expects().anyNumberOfTimes().returns("")

    val mockGeneralValidator: XmlFeedValidator = mock[XmlFeedValidator]
    (mockGeneralValidator.validatorName _).expects().anyNumberOfTimes().returns("")

    val manager = new XmlFeedManager(
      mockFlatDao,
      mockOwnerRequestDao,
      mockFlatQuestionnaireDao,
      stubS3Client,
      mockS3Config,
      builders,
      mockGeneralValidator
    )

    case class StringSink(name: String) {
      private val prom = Promise[String]

      def content: Future[String] =
        prom.future

      def sink: Sink[ByteString, NotUsed] = {
        Flow[ByteString]
          .alsoTo(Sink.foreach[ByteString] { b =>
            Thread.sleep(100)
            buffer.append(new String(b.toArray))
          })
          .to(
            Sink.onComplete {
              case Success(result) =>
                println(s"Success $result")
                prom.success(buffer.toString)
              case Failure(err) =>
                println("Failure", err)
                prom.failure(err)
            }
          )
      }

      private val buffer = new StringBuilder
    }

    def mockSink(key: String): StringSink = {
//      val sink = StringSink(key)
//      (stubS3Client
//        .writeByteSourceSink(_: String)(_: Materializer))
//        .when(key, *)
//        .returns(sink.sink)
//      sink
      //TODO REALTYBACK-7529 tmp disabled
      ???
    }
  }

  trait Data {
    this: Wiring =>

    val GeoId0 = 100
    val GeoId1 = 101
    val GeoId2 = 102
    val sampleGeoIds = Seq(GeoId0, GeoId1, GeoId2)

    val statuses: Seq[ClassifiedsPubStatusInternal] = Seq(
      ClassifiedTypeNamespace.ClassifiedType.CIAN,
      ClassifiedTypeNamespace.ClassifiedType.AVITO,
      ClassifiedTypeNamespace.ClassifiedType.YANDEX_REALTY,
      ClassifiedTypeNamespace.ClassifiedType.JCAT
    ).map { classifiedType =>
      ClassifiedsPubStatusInternal
        .newBuilder()
        .setEnabled(true)
        .setClassifiedType(classifiedType)
        .build()
    }

    val sampleEntriesWithGeo @ Seq(geoId0Entries, geoId1Entries, geoId2Entries) = sampleGeoIds map { geoId =>
      feedEntryGen
        .next(3)
        .zipWithIndex
        .map {
          case (e, i) =>
            e.copy(
              flat = e.flat.copy(
                flatId = s"f-[$geoId]-$i",
                data = e.flat.data.toBuilder
                  .applySideEffect(
                    _.getLocationBuilder
                      .setSubjectFederationGeoid(geoId)
                      .setSubjectFederationRgid(geoId)
                  )
                  .addAllClassifiedsPubStatuses(statuses.asJava)
                  .build()
              )
            )
        }
        .toSeq
    }

    val sampleEntriesWithoutGeo: Seq[FeedEntry] =
      feedEntryGen
        .next(3)
        .zipWithIndex
        .map {
          case (e, i) =>
            e.copy(
              flat = e.flat.copy(
                flatId = s"f-[]-$i",
                data = e.flat.data.toBuilder
                  .clearLocation()
                  .clearNearestMetro()
                  .addAllClassifiedsPubStatuses(statuses.asJava)
                  .build()
              )
            )
        }
        .toSeq

    val sampleEntries: Seq[FeedEntry] = sampleEntriesWithGeo.flatten ++ sampleEntriesWithoutGeo
    val sampleSource: Source[FeedEntry, NotUsed] = Source(sampleEntries.toList)

    val expectedFeeds: Map[String, String] = Map(
      "realty-rent-feed.xml" ->
        """YANDEX_REALTY {
          |  YANDEX_REALTY_entry { geoId: "", flatId: "f-[]-0" }
          |  YANDEX_REALTY_entry { geoId: "", flatId: "f-[]-1" }
          |  YANDEX_REALTY_entry { geoId: "", flatId: "f-[]-2" }
          |}
          |""".stripMargin,
      "realty-rent-feed-100.xml" ->
        """YANDEX_REALTY {
          |  YANDEX_REALTY_entry { geoId: "100", flatId: "f-[100]-0" }
          |  YANDEX_REALTY_entry { geoId: "100", flatId: "f-[100]-1" }
          |  YANDEX_REALTY_entry { geoId: "100", flatId: "f-[100]-2" }
          |}
          |""".stripMargin,
      "realty-rent-feed-101.xml" ->
        """YANDEX_REALTY {
          |  YANDEX_REALTY_entry { geoId: "101", flatId: "f-[101]-0" }
          |  YANDEX_REALTY_entry { geoId: "101", flatId: "f-[101]-1" }
          |  YANDEX_REALTY_entry { geoId: "101", flatId: "f-[101]-2" }
          |}
          |""".stripMargin,
      "realty-rent-feed-102.xml" ->
        """YANDEX_REALTY {
          |  YANDEX_REALTY_entry { geoId: "102", flatId: "f-[102]-0" }
          |  YANDEX_REALTY_entry { geoId: "102", flatId: "f-[102]-1" }
          |  YANDEX_REALTY_entry { geoId: "102", flatId: "f-[102]-2" }
          |}
          |""".stripMargin,
      "realty-rent-feed-CIAN.xml" ->
        """CIAN {
          |  CIAN_entry { geoId: "", flatId: "f-[]-0" }
          |  CIAN_entry { geoId: "", flatId: "f-[]-1" }
          |  CIAN_entry { geoId: "", flatId: "f-[]-2" }
          |}
          |""".stripMargin,
      "realty-rent-feed-CIAN-100.xml" ->
        """CIAN {
          |  CIAN_entry { geoId: "100", flatId: "f-[100]-0" }
          |  CIAN_entry { geoId: "100", flatId: "f-[100]-1" }
          |  CIAN_entry { geoId: "100", flatId: "f-[100]-2" }
          |}
          |""".stripMargin,
      "realty-rent-feed-CIAN-101.xml" ->
        """CIAN {
          |  CIAN_entry { geoId: "101", flatId: "f-[101]-0" }
          |  CIAN_entry { geoId: "101", flatId: "f-[101]-1" }
          |  CIAN_entry { geoId: "101", flatId: "f-[101]-2" }
          |}
          |""".stripMargin,
      "realty-rent-feed-CIAN-102.xml" ->
        """CIAN {
          |  CIAN_entry { geoId: "102", flatId: "f-[102]-0" }
          |  CIAN_entry { geoId: "102", flatId: "f-[102]-1" }
          |  CIAN_entry { geoId: "102", flatId: "f-[102]-2" }
          |}
          |""".stripMargin,
      "realty-rent-feed-AVITO-0.xml" ->
        """AVITO {
          |  AVITO_entry { geoId: "100", flatId: "f-[100]-0" }
          |  AVITO_entry { geoId: "100", flatId: "f-[100]-1" }
          |  AVITO_entry { geoId: "100", flatId: "f-[100]-2" }
          |  AVITO_entry { geoId: "101", flatId: "f-[101]-0" }
          |  AVITO_entry { geoId: "101", flatId: "f-[101]-1" }
          |  AVITO_entry { geoId: "101", flatId: "f-[101]-2" }
          |  AVITO_entry { geoId: "102", flatId: "f-[102]-0" }
          |  AVITO_entry { geoId: "102", flatId: "f-[102]-1" }
          |  AVITO_entry { geoId: "102", flatId: "f-[102]-2" }
          |  AVITO_entry { geoId: "", flatId: "f-[]-0" }
          |  AVITO_entry { geoId: "", flatId: "f-[]-1" }
          |  AVITO_entry { geoId: "", flatId: "f-[]-2" }
          |}
          |""".stripMargin,
      "realty-rent-feed-JCAT-0.xml" ->
        """JCAT {
          |  JCAT_entry { geoId: "100", flatId: "f-[100]-0" }
          |  JCAT_entry { geoId: "100", flatId: "f-[100]-1" }
          |  JCAT_entry { geoId: "100", flatId: "f-[100]-2" }
          |  JCAT_entry { geoId: "101", flatId: "f-[101]-0" }
          |  JCAT_entry { geoId: "101", flatId: "f-[101]-1" }
          |  JCAT_entry { geoId: "101", flatId: "f-[101]-2" }
          |  JCAT_entry { geoId: "102", flatId: "f-[102]-0" }
          |  JCAT_entry { geoId: "102", flatId: "f-[102]-1" }
          |  JCAT_entry { geoId: "102", flatId: "f-[102]-2" }
          |  JCAT_entry { geoId: "", flatId: "f-[]-0" }
          |  JCAT_entry { geoId: "", flatId: "f-[]-1" }
          |  JCAT_entry { geoId: "", flatId: "f-[]-2" }
          |}
          |""".stripMargin
    )

    val expectedFeedsWithout101GeoForYANDEX_and_JCAT: Map[String, String] = Map(
      "realty-rent-feed.xml" ->
        """YANDEX_REALTY {
          |  YANDEX_REALTY_entry { geoId: "", flatId: "f-[]-0" }
          |  YANDEX_REALTY_entry { geoId: "", flatId: "f-[]-1" }
          |  YANDEX_REALTY_entry { geoId: "", flatId: "f-[]-2" }
          |}
          |""".stripMargin,
      "realty-rent-feed-100.xml" ->
        """YANDEX_REALTY {
          |  YANDEX_REALTY_entry { geoId: "100", flatId: "f-[100]-0" }
          |  YANDEX_REALTY_entry { geoId: "100", flatId: "f-[100]-1" }
          |  YANDEX_REALTY_entry { geoId: "100", flatId: "f-[100]-2" }
          |}
          |""".stripMargin,
      "realty-rent-feed-101.xml" ->
        """YANDEX_REALTY {
          |}
          |""".stripMargin,
      "realty-rent-feed-102.xml" ->
        """YANDEX_REALTY {
          |  YANDEX_REALTY_entry { geoId: "102", flatId: "f-[102]-0" }
          |  YANDEX_REALTY_entry { geoId: "102", flatId: "f-[102]-1" }
          |  YANDEX_REALTY_entry { geoId: "102", flatId: "f-[102]-2" }
          |}
          |""".stripMargin,
      "realty-rent-feed-CIAN.xml" ->
        """CIAN {
          |  CIAN_entry { geoId: "", flatId: "f-[]-0" }
          |  CIAN_entry { geoId: "", flatId: "f-[]-1" }
          |  CIAN_entry { geoId: "", flatId: "f-[]-2" }
          |}
          |""".stripMargin,
      "realty-rent-feed-CIAN-100.xml" ->
        """CIAN {
          |  CIAN_entry { geoId: "100", flatId: "f-[100]-0" }
          |  CIAN_entry { geoId: "100", flatId: "f-[100]-1" }
          |  CIAN_entry { geoId: "100", flatId: "f-[100]-2" }
          |}
          |""".stripMargin,
      "realty-rent-feed-CIAN-101.xml" ->
        """CIAN {
          |  CIAN_entry { geoId: "101", flatId: "f-[101]-0" }
          |  CIAN_entry { geoId: "101", flatId: "f-[101]-1" }
          |  CIAN_entry { geoId: "101", flatId: "f-[101]-2" }
          |}
          |""".stripMargin,
      "realty-rent-feed-CIAN-102.xml" ->
        """CIAN {
          |  CIAN_entry { geoId: "102", flatId: "f-[102]-0" }
          |  CIAN_entry { geoId: "102", flatId: "f-[102]-1" }
          |  CIAN_entry { geoId: "102", flatId: "f-[102]-2" }
          |}
          |""".stripMargin,
      "realty-rent-feed-AVITO-0.xml" ->
        """AVITO {
          |  AVITO_entry { geoId: "100", flatId: "f-[100]-0" }
          |  AVITO_entry { geoId: "100", flatId: "f-[100]-1" }
          |  AVITO_entry { geoId: "100", flatId: "f-[100]-2" }
          |  AVITO_entry { geoId: "101", flatId: "f-[101]-0" }
          |  AVITO_entry { geoId: "101", flatId: "f-[101]-1" }
          |  AVITO_entry { geoId: "101", flatId: "f-[101]-2" }
          |  AVITO_entry { geoId: "102", flatId: "f-[102]-0" }
          |  AVITO_entry { geoId: "102", flatId: "f-[102]-1" }
          |  AVITO_entry { geoId: "102", flatId: "f-[102]-2" }
          |  AVITO_entry { geoId: "", flatId: "f-[]-0" }
          |  AVITO_entry { geoId: "", flatId: "f-[]-1" }
          |  AVITO_entry { geoId: "", flatId: "f-[]-2" }
          |}
          |""".stripMargin,
      "realty-rent-feed-JCAT-0.xml" ->
        """JCAT {
          |  JCAT_entry { geoId: "100", flatId: "f-[100]-0" }
          |  JCAT_entry { geoId: "100", flatId: "f-[100]-1" }
          |  JCAT_entry { geoId: "100", flatId: "f-[100]-2" }
          |  JCAT_entry { geoId: "102", flatId: "f-[102]-0" }
          |  JCAT_entry { geoId: "102", flatId: "f-[102]-1" }
          |  JCAT_entry { geoId: "102", flatId: "f-[102]-2" }
          |  JCAT_entry { geoId: "", flatId: "f-[]-0" }
          |  JCAT_entry { geoId: "", flatId: "f-[]-1" }
          |  JCAT_entry { geoId: "", flatId: "f-[]-2" }
          |}
          |""".stripMargin
    )
  }
}
