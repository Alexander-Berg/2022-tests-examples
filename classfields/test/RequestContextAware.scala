package ru.yandex.vertis.chat.util.test

import ru.yandex.vertis.chat.SlagGenerators._
import ru.yandex.vertis.chat.model.ModelGenerators._
import ru.yandex.vertis.chat.model.{IdempotencyKey, Room, UserId}
import ru.yandex.vertis.chat.{RequestContext, SlagGenerators, UserRequestContext}

/**
  * Introduces implicit [[RequestContext]] into scope.
  * Suppose to be convenient for testing purposes.
  *
  * @author dimas
  */
trait RequestContextAware {

  implicit def requestContext: RequestContext =
    SlagGenerators.requestContext.next

  def withUserContext[A](user: UserId)(f: UserRequestContext => A): A = {
    f(
      SlagGenerators.userRequestContext.next
        .withUser(user)
    )
  }

  def withUserContext[A](user: UserId, ik: Option[IdempotencyKey])(f: UserRequestContext => A): A = {
    f(
      SlagGenerators.userRequestContext.next
        .withUser(user)
        .withIdempotencyKey(ik)
    )
  }

  def withUserContextFromPlatform[A](user: UserId, platform: Option[String])(f: UserRequestContext => A): A = {
    f(
      SlagGenerators
        .userRequestContext(platform)
        .next
        .withUser(user)
    )
  }

  def withUserContext[A](room: Room)(f: UserRequestContext => A): A = {
    f(
      SlagGenerators.userRequestContext.next
        .withUser(anyParticipant(room).next)
    )
  }

}
