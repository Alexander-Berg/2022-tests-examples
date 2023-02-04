package ru.yandex.realty.storage.pinned

import com.google.protobuf.Int32Value
import com.google.protobuf.util.Timestamps
import org.joda.time.LocalDate
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.model.message.ExtDataSchema.{PinnedSpecialProject, PinnedSpecialProjectData}
import ru.yandex.realty.CommonConstants.SAMOLET_DEVELOPER_ID

import java.util

trait PinnedSpecialProjectsTestComponents {

  val pinnedSpecialProjectsProvider: Provider[PinnedSpecialProjectsStorage] =
    PinnedSpecialProjectsTestComponents.pinnedSpecialProjectsProvider
}

object PinnedSpecialProjectsTestComponents {

  val MskSamoletProject = PinnedSpecialProject
    .newBuilder()
    .addGeoId(1)
    .setStartDate(Timestamps.fromMillis(LocalDate.parse("2020-12-01").toDateTimeAtStartOfDay.getMillis))
    .setEndDate(Timestamps.fromMillis(LocalDate.parse("3021-12-31").toDateTimeAtStartOfDay.getMillis))
    .setData(
      PinnedSpecialProjectData
        .newBuilder()
        .setDeveloperId(SAMOLET_DEVELOPER_ID)
        .setDeveloperName("Группа «Самолет»")
        .addAllSiteId(util.Arrays.asList(2890148))
        .setSpecialCallsRatio(Int32Value.of(5))
    )
    .build()

  val SpbSamoletProject = PinnedSpecialProject
    .newBuilder()
    .addGeoId(10174)
    .setStartDate(Timestamps.fromMillis(LocalDate.parse("2020-12-01").toDateTimeAtStartOfDay.getMillis))
    .setEndDate(Timestamps.fromMillis(LocalDate.parse("3021-12-31").toDateTimeAtStartOfDay.getMillis))
    .setData(
      PinnedSpecialProjectData
        .newBuilder()
        .setDeveloperId(SAMOLET_DEVELOPER_ID)
        .setDeveloperName("Группа «Самолет»")
        .addAllSiteId(util.Arrays.asList(57547))
    )
    .build()

  val VertoletProject =
    PinnedSpecialProject
      .newBuilder()
      .addGeoId(51)
      .addGeoId(65)
      .setStartDate(Timestamps.fromMillis(LocalDate.parse("2020-12-01").toDateTimeAtStartOfDay.getMillis))
      .setEndDate(Timestamps.fromMillis(LocalDate.parse("3021-12-31").toDateTimeAtStartOfDay.getMillis))
      .setData(
        PinnedSpecialProjectData
          .newBuilder()
          .setDeveloperId(102322L)
          .setDeveloperName("Группа «Вертолет»")
          .addAllSiteId(util.Arrays.asList(57548, 280522))
      )
      .build()

  val AlreadyEnded =
    PinnedSpecialProject
      .newBuilder()
      .addGeoId(1)
      .addGeoId(10174)
      .setStartDate(Timestamps.fromMillis(LocalDate.parse("2020-12-01").toDateTimeAtStartOfDay.getMillis))
      .setEndDate(Timestamps.fromMillis(LocalDate.parse("2020-12-31").toDateTimeAtStartOfDay.getMillis))
      .setData(
        PinnedSpecialProjectData
          .newBuilder()
          .setDeveloperId(102320L)
          .setDeveloperName("Группа «Самолет»")
          .addAllSiteId(util.Arrays.asList(57547, 280521))
      )
      .build()

  val NotYetStarted =
    PinnedSpecialProject
      .newBuilder()
      .addGeoId(1)
      .addGeoId(10174)
      .setStartDate(Timestamps.fromMillis(LocalDate.parse("2020-12-01").toDateTimeAtStartOfDay.getMillis))
      .setEndDate(Timestamps.fromMillis(LocalDate.parse("2020-12-31").toDateTimeAtStartOfDay.getMillis))
      .setData(
        PinnedSpecialProjectData
          .newBuilder()
          .setDeveloperId(102320L)
          .setDeveloperName("Группа «Самолет»")
          .addAllSiteId(util.Arrays.asList(57547, 280521))
      )
      .build()

  val pinnedSpecialProjectsStorage = new PinnedSpecialProjectsStorage(
    Seq(NotYetStarted, AlreadyEnded, VertoletProject, MskSamoletProject)
  )

  val pinnedSpecialProjectsProvider: Provider[PinnedSpecialProjectsStorage] =
    () => pinnedSpecialProjectsStorage
}
