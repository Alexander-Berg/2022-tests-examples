package ru.yandex.realty.statprocessor.hbase.buildinginfo.yt

import org.apache.log4j.{Level, Logger}
import org.joda.time.Duration.standardMinutes
import ru.yandex.common.util.IOUtils
import ru.yandex.inside.yt.kosher.Yt
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.inside.yt.kosher.impl.YtUtils
import ru.yandex.inside.yt.kosher.impl.transactions.utils.pinger.TransactionPinger
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes
import ru.yandex.realty.application.LogConfigurerImpl
import ru.yandex.realty.building.model.BuildingResidentialStatus
import ru.yandex.realty.buildinginfo.converter.ConvertProtoFromYtNode.Caster
import ru.yandex.realty.buildinginfo.converter.{ConvertImportBuildingFromYtNode, ConvertRawBuildingFromImport}
import ru.yandex.realty.buildinginfo.model.internal.AddressInfo
import ru.yandex.realty.buildinginfo.model.{BuildingSource, CompositeBuilding}
import ru.yandex.realty.buildinginfo.unification.BuildingUnifier
import ru.yandex.realty.context.ExtDataLoaders
import ru.yandex.realty.geocoder.cache.RegionGraphDistrictCache
import ru.yandex.realty.graph.RegionGraph
import ru.yandex.realty.graph.serialize.RegionGraphProtoConverter
import ru.yandex.realty.model.location.GeoPoint
import ru.yandex.realty.proto.offer.{BuildingType, HeatingType}

import java.io.{File, FileInputStream}
import java.time.Duration
import java.util.{Collections, Optional}
import scala.collection.JavaConverters.{asScalaIteratorConverter, mapAsJavaMapConverter, seqAsJavaListConverter}

object BuildingYtImporterTool extends App {

  Logger.getRootLogger.setLevel(Level.OFF)
  new LogConfigurerImpl(new File("dev/logback-local.xml")).configure()

  private val yt: Yt = YtUtils.http("hahn.yt.yandex.net", System.getenv("YT_TOKEN"))
  private val ytPinger: TransactionPinger = new TransactionPinger(1)

  private val verbaStorage =
    ExtDataLoaders.createVerbaStorage(new FileInputStream("verba2-3-test.xml.gz"))
  private val regionGraph: RegionGraph =
    RegionGraphProtoConverter.deserialize(
      IOUtils.gunzip(
        getClass.getClassLoader.getResourceAsStream("region_graph-8-2.data")
      )
    )

  val tx = ytPinger.enablePinging(
    yt.transactions.startAndGet(standardMinutes(20)),
    true,
    Duration.ofMinutes(10),
    Duration.ofSeconds(30)
  )
  val txId = Optional.of(tx.getId)

  try {
    val itr = yt
      .tables()
      .read(
        txId,
        false,
        YPath
          .simple("//home/verticals/realty/housebase/providers/reformazkh/data-2019-08-08")
//          .withRange(68, 69),
          .withRange(1257, 1258),
        YTableEntryTypes.YSON
      )

    for (row <- itr.asScala) {
      val address = row.getString("unified_address")
      println(address)
      val importBuilding = ConvertImportBuildingFromYtNode(
        row,
        "building_type" -> Caster.enumByName(BuildingType.valueOf),
        "residential_status" -> Caster.enumByName(BuildingResidentialStatus.valueOf),
        "heating_type" -> Caster.enumByName(HeatingType.valueOf)
      )
      println(importBuilding)
      val unifiedBuilding = BuildingUnifier.unify(
        new CompositeBuilding(
          true,
          address,
          GeoPoint.getPoint(1, 1),
          AddressInfo.getDefaultInstance,
          Seq.empty.asJava,
          Map(BuildingSource.REFORMA_ZHKH_2 -> ConvertRawBuildingFromImport(importBuilding)).asJava,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          Collections.emptyList(),
          null,
          null,
          null,
          null
        ),
        verbaStorage,
        regionGraph,
        new RegionGraphDistrictCache()
      )
      println(unifiedBuilding)
    }

    itr.close()
  } finally {
    tx.commit()
  }

}
