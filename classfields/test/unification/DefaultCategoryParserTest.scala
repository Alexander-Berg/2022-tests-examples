package ru.yandex.vertis.general.feed.processor.pipeline.test.unification

import common.zio.grpc.client.GrpcClient.GrpcClient
import common.zio.grpc.client.GrpcClientLive
import general.category_matcher.api.{Match, MatchWithNamespace, Namespace}
import general.classifiers.feed_category_classifier_api.FeedCategoryClassifierGrpc.FeedCategoryClassifier
import general.classifiers.feed_category_classifier_api.{
  LeafCategoryRequest,
  LeafCategoryResponse,
  RootCategoryRequest,
  RootCategoryResponse
}
import general.classifiers.title_classifier_api.TitleClassifierServiceGrpc.TitleClassifierService
import general.classifiers.title_classifier_api.{TitlePredictProbabilitiesRequest, TitlePredictProbabilitiesResponse}
import general.feed.transformer.{FeedFormat, RawCategory, RawCondition, RawOffer}
import ru.yandex.vertis.general.common.cache.{Cache, RequestCacher}
import ru.yandex.vertis.general.common.model.user.SellerId
import ru.yandex.vertis.general.feed.logic.testkit.TestPredictionEventsLogger
import ru.yandex.vertis.general.feed.processor.dictionary.BonsaiDictionaryService
import ru.yandex.vertis.general.feed.processor.dictionary.BonsaiDictionaryService.BonsaiDictionaryService
import ru.yandex.vertis.general.feed.processor.dictionary.testkit.BonsaiDictionaryTestService
import ru.yandex.vertis.general.feed.processor.model._
import ru.yandex.vertis.general.feed.processor.pipeline.unification.{CategoryMatchSnapshot, CategoryParser}
import common.zio.logging.Logging
import zio.random.Random
import zio.test.Assertion._
import zio.test._
import zio.{Has, Ref, ULayer, ZLayer}

import scala.concurrent.Future

object DefaultCategoryParserTest extends DefaultRunnableSpec {
  val seller = SellerId.UserId(1L)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("DefaultCategoryParser")(
      testM("Парсит категорию по ее идентификатору") {
        val testOffer = offer(Some(leafCanCreateCategory.id))
        CategoryParser.parseCategory(testOffer, seller).map { result =>
          assert(result.field)(isSome(equalTo(leafCanCreateCategory)))
        }
      },
      testM("Парсит категорию по ее уникальному названию") {
        val testOffer = offer(Some(leafCanCreateCategory.uniqueName))
        CategoryParser.parseCategory(testOffer, seller).map { result =>
          assert(result.field)(isSome(equalTo(leafCanCreateCategory)))
        }
      },
//      testM(
//        "Пытается угадать категорию по тексту, если в спаршенной категории нельзя разместить объявление и она не конечная"
//      ) {
//        val testOffer = offer(Some(nonLeafCantCreateCategory.id), title = "text")
//        CategoryParser.parseCategory(testOffer, seller).map { result =>
//          assert(result.field)(isSome(equalTo(anotherLeafCanCreateCategory)))
//        }
//      },
      testM(
        "Не пытается угадать категорию по тексту, если в спаршенной категории нельзя разместить объявление и она конечная"
      ) {
        val testOffer = offer(Some(leafCantCreateCategory.id), title = "text")
        CategoryParser.parseCategory(testOffer, seller).map { result =>
          assert(result.field)(isSome(equalTo(leafCantCreateCategory)))
        }
      },
      testM("Использует матчинг для категорий Авито, если он есть") {
        val testOffer = offer(Some(avitoCategory), tags = Map("tag" -> "tag_value"), format = FeedFormat.AVITO)
        CategoryParser.parseCategory(testOffer, seller).map { result =>
          assert(result.field)(isSome(equalTo(anotherLeafCanCreateCategory)))
        }
      },
      testM("Фолбек на матчинг без тегов") {
        val testOffer = offer(Some(avitoCategory), format = FeedFormat.AVITO)
        CategoryParser.parseCategory(testOffer, seller).map { result =>
          assert(result.field)(isSome(equalTo(anotherLeafCanCreateCategory)))
        }
      },
//      testM("Пытается угадать категорию по тексту, если ее нет в матчинге нет") {
//        val testOffer = offer(Some(avitoNotMatchedCategory), format = FeedFormat.AVITO, title = "text")
//        CategoryParser.parseCategory(testOffer, seller).map { result =>
//          assert(result.field)(isSome(equalTo(leafCanCreateCategory)))
//        }
//      },
//      testM("Пытается угадать категорию по тайтлу для фидов маркета") {
//        val testOffer = offer(Some("category"), format = FeedFormat.MARKET, title = "text")
//        CategoryParser.parseCategory(testOffer, seller).map { result =>
//          assert(result.field)(isSome(equalTo(leafCanCreateCategory)))
//        }
//      },
      testM("Возвращает ошибку, если категория из маркета отсутствует") {
        val testOffer = offer(None, format = FeedFormat.MARKET, title = "text")
        CategoryParser.parseCategory(testOffer, seller).map { result =>
          assert(result.errors.headOption)(isSome(equalTo(MarketCategoryNotRecognized)))
        }
      }
    )
  }.provideCustomLayerShared {
    val logging = Logging.live
    val cacher = Cache.noop ++ logging >>> RequestCacher.live
    logging ++ Random.any ++ bonsai ++ categoryMatchRef ++ prepareFeedClassifier() ++ prepareClassifier() ++ cacher ++ TestPredictionEventsLogger.test >>> CategoryParser.live
  }

  val avitoCategory = "avito-category"
  val avitoNotMatchedCategory = "avito-not-matched-category"

  val leafCanCreateCategory = Category("leaf-can-create", "leaf-can-create-name", 1, Map.empty, false, true, true)

  val anotherLeafCanCreateCategory =
    Category("another-leaf-can-create", "another-leaf-can-create-name", 1, Map.empty, false, true, true)
  val leafCantCreateCategory = Category("leaf-can't-create", "leaf-can't-create-name", 1, Map.empty, false, false, true)

  val nonLeafCantCreateCategory =
    Category("non-leaf-can't-create", "non-leaf-can't-create-name", 1, Map.empty, false, false, false)

  private val bonsai: ULayer[Has[Ref[BonsaiDictionaryService.Service]]] = {
    Ref.make {
      val service: BonsaiDictionaryService.Service = new BonsaiDictionaryTestService(
        Map(
          leafCanCreateCategory.id -> leafCanCreateCategory,
          anotherLeafCanCreateCategory.id -> anotherLeafCanCreateCategory,
          leafCantCreateCategory.id -> leafCantCreateCategory,
          nonLeafCantCreateCategory.id -> nonLeafCantCreateCategory
        ),
        Map(
          leafCanCreateCategory.uniqueName -> leafCanCreateCategory,
          anotherLeafCanCreateCategory.uniqueName -> anotherLeafCanCreateCategory,
          leafCantCreateCategory.uniqueName -> leafCantCreateCategory,
          nonLeafCantCreateCategory.uniqueName -> nonLeafCantCreateCategory
        ),
        Map.empty,
        Map.empty,
        Map.empty,
        Map.empty
      )
      service
    }.toLayer
  }

  private val categoryMatchRef: ULayer[Has[Ref[CategoryMatchSnapshot]]] =
    Ref
      .make(
        new CategoryMatchSnapshot(
          Seq(
            MatchWithNamespace(
              Namespace.AVITO,
              Some(
                Match(Some(Match.Key(avitoCategory, Map("tag" -> "tag_value"))), List(anotherLeafCanCreateCategory.id))
              )
            )
          )
        )
      )
      .toLayer

  private def prepareClassifier(): ULayer[GrpcClient[TitleClassifierService]] = {
    ZLayer.succeed {
      new GrpcClientLive[TitleClassifierService](new TestTitleClassifierService, null)
    }
  }

  class TestTitleClassifierService extends TitleClassifierService {

    private def probabilities(request: TitlePredictProbabilitiesRequest) =
      if (request.title != "text" && request.title != "category text") {
        Map.empty[String, Double]
      } else {
        if (request.categoryIdHint.isEmpty) {
          Map(leafCanCreateCategory.id -> 0.6, anotherLeafCanCreateCategory.id -> 0.4)
        } else {
          Map(leafCanCreateCategory.id -> 0.4, anotherLeafCanCreateCategory.id -> 0.6)
        }
      }

    override def predictProbabilitiesBert(
        request: TitlePredictProbabilitiesRequest): Future[TitlePredictProbabilitiesResponse] =
      Future.successful(TitlePredictProbabilitiesResponse(probabilities(request)))

    override def predictBertHinted(
        request: TitlePredictProbabilitiesRequest): Future[TitlePredictProbabilitiesResponse] =
      Future.successful(TitlePredictProbabilitiesResponse(probabilities(request)))
  }

  private def prepareFeedClassifier(): ULayer[GrpcClient[FeedCategoryClassifier]] = {
    ZLayer.succeed {
      new GrpcClientLive[FeedCategoryClassifier](new TestFeedCategoryClassifier, null)
    }
  }

  class TestFeedCategoryClassifier extends FeedCategoryClassifier {

    override def predictRootCategoryProbabilities(request: RootCategoryRequest): Future[RootCategoryResponse] =
      Future.successful(RootCategoryResponse())

    override def predictLeafCategoryProbabilities(request: LeafCategoryRequest): Future[LeafCategoryResponse] =
      Future.successful(LeafCategoryResponse())
  }

  private def offer(
      categoryId: Option[String],
      tags: Map[String, String] = Map.empty,
      title: String = "Test offer",
      format: FeedFormat = FeedFormat.GENERAL): RawOffer =
    RawOffer(
      format = format,
      externalId = "offer-1",
      title = title,
      condition = RawCondition.Condition.USED,
      category = categoryId.map(categoryId => RawCategory(categoryId, tags))
    )
}
