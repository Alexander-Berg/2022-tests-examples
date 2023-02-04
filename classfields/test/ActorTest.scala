package vertis.zio.actor

import vertis.zio.actor.Actor.Insides
import vertis.zio.actor.ZioActor.{create, ActorBody, ActorSettings}
import common.zio.logging.Logging
import zio.URIO

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
trait ActorTest {

  def createActor[R <: Logging.Logging, M, S](initialState: S)(body: Insides[S, R, M]): URIO[R, Actor[M]] =
    create(ActorSettings(initialState, "test-actor", ActorBody(body)))
}
