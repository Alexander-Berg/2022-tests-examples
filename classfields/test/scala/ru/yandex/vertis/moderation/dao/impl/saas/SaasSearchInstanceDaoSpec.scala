package ru.yandex.vertis.moderation.dao.impl.saas

import org.joda.time.DateTime
import org.junit.Ignore
import org.junit.runner.RunWith
import org.scalatest.exceptions.TestFailedException
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.feature.impl.BasicFeatureTypes.BooleanFeatureType
import ru.yandex.vertis.feature.impl.{BasicFeatureTypes, InMemoryFeatureRegistry}
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.InstanceGen
import ru.yandex.vertis.moderation.model.generators.Producer._
import ru.yandex.vertis.moderation.model.instance.{Instance, RealtyEssentials}
import ru.yandex.vertis.moderation.model.realty.PriceInfo
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.proto.RealtyLight.PriceInfo.Currency
import ru.yandex.vertis.moderation.searcher.core.model.Sort
import ru.yandex.vertis.moderation.searcher.core.saas.client.{HttpSearchClient, SaasOptions, SaasPrefixes}
import ru.yandex.vertis.moderation.searcher.core.saas.document.{Document, DocumentBuilder}
import ru.yandex.vertis.moderation.searcher.core.saas.search.{AutoruSearchQuery, SaasSearcher}
import ru.yandex.vertis.moderation.service.impl.TvmTicketProvidersHolder
import ru.yandex.vertis.moderation.settings.HttpClientConfig
import ru.yandex.vertis.moderation.util.Page
import ru.yandex.vertis.moderation.{Globals, SpecBase}

import java.io.IOException
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

/**
  * Specs for [[SaasSearchInstanceDao]]
  *
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
@Ignore
class SaasSearchInstanceDaoSpec extends SpecBase {

  implicit private val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
  implicit private val featureRegistry: InMemoryFeatureRegistry = new InMemoryFeatureRegistry(BasicFeatureTypes)

  private val clientConfig =
    HttpClientConfig(
      maxConnections = Some(100),
      requestTimeout = Some(1.second),
      readTimeout = Some(1.second)
    )

  private val saasOptions =
    new SaasOptions {
      def saasPrefixes: SaasPrefixes = SaasPrefixes(1, Seq(1), Seq(1))

      def realtime = true

      def usePruning = true
    }

  private val moderationClientId = 2016679
  private val saasTvmClientId = 2010902
  private val secret = "PUT YOUR TVM SECRET HERE"

  private val tvmTicketProvidersHolder =
    new TvmTicketProvidersHolder(
      selfClientId = moderationClientId,
      secret = secret,
      destinationClientIds = Seq(saasTvmClientId)
    )

  private val tvmTicketProvider = tvmTicketProvidersHolder.providersMap(saasTvmClientId)

  private lazy val searchClient =
    new HttpSearchClient(
      indexBaseUrl = "http://saas-indexerproxy-prestable.yandex.net:80/service/",
      clientConfig,
      searchBaseUrl = "http://saas-searchproxy-prestable.yandex.net:17000",
      clientConfig,
      saasOptions,
      ttl = Some(5.minutes),
      tvmTicketProvider
    ) {

      override def update(doc: Document, updateTime: DateTime, kps: Int)(implicit ec: ExecutionContext): Future[Unit] =
        Future {
          Thread.sleep(1000L)
          throw new IOException("Bang!")
        }

      override def flush()(implicit ec: ExecutionContext): Future[Unit] =
        Future {
          Thread.sleep(10L)
        }
    }

  val service = Service.AUTORU

  lazy val searcher = new SaasSearcher(searchClient, "vs_moderation_offers", saasOptions)
  lazy val documentBuilder = new DocumentBuilder(Globals.opinionCalculator(service))
  lazy val dao = new SaasSearchInstanceDao(searchClient, searcher, documentBuilder)

  "SaasSearchInstanceDao" should {

    def withoutUnknownCurrency(instance: Instance) = {
      val a =
        instance.essentials.asInstanceOf[RealtyEssentials].price match {
          case Some(PriceInfo(Some(Currency.CURRENCY_UNKNOWN), _, _, _)) => false
          case _                                                         => true
        }
      val b =
        instance.essentials.asInstanceOf[RealtyEssentials].pricePerM2 match {
          case Some(PriceInfo(Some(Currency.CURRENCY_UNKNOWN), _, _, _)) => false
          case _                                                         => true
        }
      a && b
    }

    "submit smthing" in {
      val instances =
        InstanceGen
          .suchThat(i => i.service == service && withoutUnknownCurrency(i))
          .next(500)

      intercept[TestFailedException] {
        dao.submit(instances, Seq.empty).futureValue
      }
    }

    "searchIds" in {
      featureRegistry.register("use-tvm-for-saas", true)
      val query =
        AutoruSearchQuery(
          contextVisibility = List("VISIBLE"),
          cvHash = Some("MBC0914E79C930F30")
        )

      val res = dao.searchIds(query, Page(0, 10), Sort.CreateDate(asc = true)).futureValue
      println(res)
    }
  }

}
