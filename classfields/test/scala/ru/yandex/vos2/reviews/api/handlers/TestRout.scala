package ru.yandex.vos2.reviews.api.handlers

import akka.http.scaladsl.server.Route
import ru.yandex.vos2.reviews.app.AkkaSupport
import ru.yandex.vos2.reviews.app.components.{ReviewsHandlerComponents, ReviewsManagerComponents}

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 10/11/2017.
  */
class TestRout extends TestApplication with AkkaSupport with TestApiComponents with ReviewsManagerComponents
  with ReviewsHandlerComponents {

  def rout: Route = rootHandler.route

  override def onStart(action: => Unit): Unit = Unit

  override def onStop(action: => Unit): Unit = Unit
}
