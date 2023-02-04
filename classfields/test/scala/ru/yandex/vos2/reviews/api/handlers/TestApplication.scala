package ru.yandex.vos2.reviews.api.handlers

import ru.yandex.vos2.reviews.app.DefaultApplication
import ru.yandex.vos2.reviews.env.ReviewsEnv
import ru.yandex.vos2.reviews.utils.{DockerReviewCoreComponents, DockerReviewEnvironment}

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 10/11/2017.
  */
trait TestApplication extends DefaultApplication with DockerReviewCoreComponents
