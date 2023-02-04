package ru.yandex.complaints.api.directives

import akka.actor.{Actor, ActorSystem, Props}
import spray.http.{HttpRequest, Uri}
import spray.routing.directives.ParameterDirectives
import spray.routing.{Directive1, Rejection, RequestContext}

/**
  * Created by s-reznick on 20.03.17.
  */
trait DirectiveSpec extends ParameterDirectives {
  class EmptyActor extends Actor {
    override def receive = {
      case request@_ =>
    }
  }

  val actorSystem = ActorSystem.create("actors")

  val emptyActor = actorSystem.actorOf(Props(new EmptyActor))

  val GoodFlags = Seq("true", "false")
  val BadFlags = Seq("gewgr", " ")

  def normaizeMap(pairs: Seq[(String, Option[String])]): Map[String, String] = {
    pairs.filter(_._2.isDefined).toMap.mapValues(_.get)
  }

  def withNone[T](elems: Seq[T]) = {
    elems.map(Some(_)) ++ Seq(None)
  }

  def withParam(requests: Seq[Map[String, String]],
                key: String,
                value: String): Seq[Map[String, String]] =
    requests.map(_ + (key -> value))

  def withParam(requests: Seq[Map[String, String]],
                key: String,
                vals: Seq[String]): Seq[Map[String, String]] =
    vals.map(withParam(requests, key, _)).flatten

  def withoutParam(requests: Seq[Map[String, String]],
                   key: String): Seq[Map[String, String]] =
    requests.map(_ - key)

  case class CheckStatus(isAccepted: Boolean, rejectReasons: List[Rejection])

  def check[T](directive: Directive1[T],
               params: Map[String, String] = Map(),
               path: Uri.Path = Uri.Path.Empty
              ): CheckStatus = {
    var accepted = false
    var rejected: List[Rejection] = List()

    val Context = RequestContext(
      request = HttpRequest(uri = Uri./.withQuery(params)),
      responder = emptyActor, // кажется, спрей не умеет совсем без актора
      unmatchedPath = path
    ).withRejectionHandling(e => {
      rejected = e
    })

    directive {
      v =>
        http ⇒ {
          accepted = true
        }
    }(Context)

    CheckStatus(accepted, rejected)
  }
}