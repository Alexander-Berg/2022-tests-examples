package ru.yandex.realty.sitemap.service.entry

import eu.timepit.refined.auto._
import org.joda.time.DateTime
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.canonical.base.request.RelativeDate
import ru.yandex.realty.sitemap.model.entry.SubServicesUrlsEntry
import ru.yandex.realty.sitemap.model.{ChangeFrequency, FeedTarget, SitemapEntryRequest, SitemapUrl}
import ru.yandex.realty.sitemap.service.entry.live.SubServicesEntries
import ru.yandex.realty.sitemap.testkit.EntriesSpec
import ru.yandex.realty.storage.verba.{VerbaDictionary, VerbaStorage}
import ru.yandex.verba2.model.attribute.{Attribute, StringAttribute}
import ru.yandex.verba2.model.{Dictionary, Term}
import zio.ZLayer
import zio.magic._
import zio.test.ZSpec
import zio.test.junit.JUnitRunnableSpec

import scala.jdk.CollectionConverters._

class SubServicesEntriesSpec extends JUnitRunnableSpec {

  private def verbaStorage: Provider[VerbaStorage] = {
    val terms = Seq(term).asJava
    val dictionary = new Dictionary(2L, 2L, VerbaDictionary.SITEMAP_SUB_SERVICES.getCode, "name", "", terms)
    () => new VerbaStorage(List(dictionary).asJava)
  }

  private def term = {
    val attributes: Seq[Attribute] = Seq(
      new StringAttribute("code", Seq("verbaCode").asJava),
      new StringAttribute("url", Seq("/verbaTestUrl").asJava)
    )
    val date = DateTime.now

    val innerTerm = new Term(1, "", "shortDescription", 1, "", date, date)
    new Term(innerTerm, attributes.asJava, Seq.empty.asJava)
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("SubServicesEntries") {
      testM("should correctly produce entries") {
        EntriesSpec.specEffect[SitemapEntryRequest[SubServicesUrlsEntry]](
          Seq(
            SitemapEntryRequest[SubServicesUrlsEntry](
              SitemapUrl(
                path = "/verbaTestUrl",
                lastMod = RelativeDate.WeekAgo.calculateModificationDate(),
                changeFrequency = ChangeFrequency.Weekly,
                priority = 0.7,
                images = Seq.empty,
                target = FeedTarget.SitemapSubServices
              )
            )
          )
        )
      }.inject(ZLayer.succeed(verbaStorage), SubServicesEntries.layer)
    }
}
