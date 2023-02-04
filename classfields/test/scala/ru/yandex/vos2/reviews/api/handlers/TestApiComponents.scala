package ru.yandex.vos2.reviews.api.handlers

import ru.yandex.vos2.reviews.app.AkkaSupport
import ru.yandex.vos2.reviews.app.components.DefaultReviewsApiComponents
import ru.yandex.vos2.reviews.utils.DockerReviewCoreComponents

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 10/11/2017.
  */
trait TestApiComponents extends DefaultReviewsApiComponents
  with AkkaSupport with DockerReviewCoreComponents
