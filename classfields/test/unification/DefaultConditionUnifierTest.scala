package ru.yandex.vertis.general.feed.processor.pipeline.test.unification

import general.feed.transformer.{FeedFormat, RawCategory, RawCondition, RawOffer}
import ru.yandex.vertis.general.feed.model.FeedSource
import ru.yandex.vertis.general.feed.processor.model._
import ru.yandex.vertis.general.feed.processor.pipeline.unification.ConditionUnifier
import zio.test.Assertion._
import zio.test._

object DefaultConditionUnifierTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("DefaultConditionUnifier")(
      testM("Использует состояние, если оно было передано, для фидовых объявлений") {
        ConditionUnifier
          .unifyCondition(
            FeedSource.Feed,
            testOffer.copy(format = FeedFormat.GENERAL, condition = RawCondition.Condition.USED),
            category
          )
          .map { result =>
            assert(result.field)(isSome(equalTo(RawCondition.Condition.USED)))
          }
      },
      testM("Использует состояние, если оно было передано, для спаршенных объявлений") {
        ConditionUnifier
          .unifyCondition(
            FeedSource.Parsing,
            testOffer.copy(format = FeedFormat.GENERAL, condition = RawCondition.Condition.USED),
            category
          )
          .map { result =>
            assert(result.field)(isSome(equalTo(RawCondition.Condition.USED)))
          }
      },
      testM("Использует состояние, если оно было передано, для авитовских фидов") {
        ConditionUnifier
          .unifyCondition(
            FeedSource.Feed,
            testOffer.copy(format = FeedFormat.AVITO, condition = RawCondition.Condition.USED),
            category
          )
          .map { result =>
            assert(result.field)(isSome(equalTo(RawCondition.Condition.USED)))
          }
      },
      testM("Возвращает фатальную ошибку, если состояние не было передано, для фидовых объявлений") {
        ConditionUnifier
          .unifyCondition(
            FeedSource.Feed,
            testOffer.copy(format = FeedFormat.GENERAL, condition = RawCondition.Condition.UNSET),
            category
          )
          .map { result =>
            assert(result.field)(isNone) && assert(result.errors)(
              equalTo(Seq(MandatoryFieldNotFound("Состояние"): UnificationError))
            )
          }
      },
      testM(
        "Использует дефолтное значение б/у, если состояние не было передано, для спаршенных объявлений в категориях с состоянием"
      ) {
        ConditionUnifier
          .unifyCondition(
            FeedSource.Parsing,
            testOffer.copy(format = FeedFormat.GENERAL, condition = RawCondition.Condition.UNSET),
            category.copy(ignoreCondition = false)
          )
          .map { result =>
            assert(result.field)(isSome(equalTo(RawCondition.Condition.USED)))
          }
      },
      testM(
        "Использует дефолтное значение б/у, если состояние не было передано, для авитовских фидов в категориях с состоянием"
      ) {
        ConditionUnifier
          .unifyCondition(
            FeedSource.Feed,
            testOffer.copy(format = FeedFormat.AVITO, condition = RawCondition.Condition.UNSET),
            category.copy(ignoreCondition = false)
          )
          .map { result =>
            assert(result.field)(isSome(equalTo(RawCondition.Condition.USED)))
          }
      },
      testM(
        "Использует дефолтное значение неприменимо, если состояние не было передано, для спаршенных объявлений в категориях без состояния"
      ) {
        ConditionUnifier
          .unifyCondition(
            FeedSource.Parsing,
            testOffer.copy(format = FeedFormat.GENERAL, condition = RawCondition.Condition.UNSET),
            category.copy(ignoreCondition = true)
          )
          .map { result =>
            assert(result.field)(isSome(equalTo(RawCondition.Condition.INAPPLICABLE)))
          }
      },
      testM(
        "Использует дефолтное значение неприменимо, если состояние не было передано, для авитовских фидов в категориях без состояния"
      ) {
        ConditionUnifier
          .unifyCondition(
            FeedSource.Feed,
            testOffer.copy(format = FeedFormat.AVITO, condition = RawCondition.Condition.UNSET),
            categoryWithoutCondition
          )
          .map { result =>
            assert(result.field)(isSome(equalTo(RawCondition.Condition.INAPPLICABLE)))
          }
      }
    )
  }.provideCustomLayerShared {
    ConditionUnifier.live
  }

  val category = Category("category-id", "some-category-name", 1, Map.empty, false, true, true)

  val categoryWithoutCondition =
    category.copy(ignoreCondition = true)

  val testOffer: RawOffer =
    RawOffer(
      externalId = "offer-1",
      title = "Test offer",
      description = "продаю мопед (не мой)",
      condition = RawCondition.Condition.USED,
      category = Some(RawCategory("category"))
    )
}
