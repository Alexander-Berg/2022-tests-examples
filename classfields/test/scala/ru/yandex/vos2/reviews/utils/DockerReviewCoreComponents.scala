package ru.yandex.vos2.reviews.utils

import ru.yandex.vos2.reviews.components.DefaultReviewsCoreComponents
import ru.yandex.vos2.reviews.env.ReviewsEnv
import ru.yandex.vos2.util.environment.Env

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 05/10/2017.
  */
trait DockerReviewCoreComponents extends DefaultReviewsCoreComponents {

  override lazy val env: ReviewsEnv = new DockerReviewEnvironment

}
