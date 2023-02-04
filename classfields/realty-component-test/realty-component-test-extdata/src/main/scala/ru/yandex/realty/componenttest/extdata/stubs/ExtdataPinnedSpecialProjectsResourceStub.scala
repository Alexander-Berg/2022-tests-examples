package ru.yandex.realty.componenttest.extdata.stubs

import com.google.protobuf.util.Timestamps
import org.joda.time.LocalDate
import ru.yandex.realty.componenttest.extdata.core.ExtdataResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType
import ru.yandex.realty.model.message.ExtDataSchema.{PinnedSpecialProject, PinnedSpecialProjectData, TagMessage}

import java.util

trait ExtdataPinnedSpecialProjectsResourceStub extends ExtdataResourceStub {

  private val projects: Seq[PinnedSpecialProject] =
    Seq(
      PinnedSpecialProject
        .newBuilder()
        .addGeoId(1)
        .addGeoId(10174)
        .setStartDate(Timestamps.fromMillis(LocalDate.parse("2020-12-01").toDateTimeAtStartOfDay.getMillis))
        .setEndDate(Timestamps.fromMillis(LocalDate.parse("3021-12-31").toDateTimeAtStartOfDay.getMillis))
        .setData(
          PinnedSpecialProjectData
            .newBuilder()
            .setDeveloperId(102320L)
            .setDeveloperName("Группа «Самолет»")
            .addAllSiteId(util.Arrays.asList(57547, 280521))
        )
        .build()
    )

  stubGzipped(RealtyDataType.PinnedSpecialProjects, projects)

}
