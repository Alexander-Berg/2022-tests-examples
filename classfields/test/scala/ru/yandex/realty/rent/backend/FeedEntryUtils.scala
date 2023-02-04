package ru.yandex.realty.rent.backend

import ru.yandex.realty.rent.model.feed.FeedEntry
import ru.yandex.realty.rent.proto.api.moderation.FlatQuestionnaire
import ru.yandex.realty.rent.proto.api.moderation.FlatQuestionnaire.Furniture
import ru.yandex.realty.rent.proto.api.moderation.FlatQuestionnaire.Furniture.Internet.InternetTypeNamespace

object FeedEntryUtils {

  implicit class FeedEntryOps(e: FeedEntry) {

    def setInternetType(internetType: InternetTypeNamespace.InternetType): FeedEntry = {
      e.copy(
        questionnaire = e.questionnaire.copy(
          data = e.questionnaire.data.toBuilder
            .setFurniture(
              e.questionnaire.data.getFurniture.toBuilder
                .setInternet(
                  Furniture.Internet
                    .newBuilder()
                    .setInternetType(internetType)
                    .build()
                )
                .build()
            )
            .build()
        )
      )
    }

    def setPayments(payments: FlatQuestionnaire.Payments): FeedEntry = {
      e.copy(
        questionnaire = e.questionnaire.copy(
          data = e.questionnaire.data.toBuilder
            .setPayments(payments)
            .build()
        )
      )
    }
  }
}
