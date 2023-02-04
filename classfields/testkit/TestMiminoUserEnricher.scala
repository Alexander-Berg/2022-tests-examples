package ru.yandex.vertis.general.users.logic.testkit

import ru.yandex.vertis.general.users.logic.MiminoUserEnricher
import ru.yandex.vertis.general.users.logic.MiminoUserEnricher.MiminoUserEnricher
import ru.yandex.vertis.general.users.model.{LimitedUser, User, UserError}
import zio.{IO, ZIO, ZLayer}

object TestMiminoUserEnricher extends MiminoUserEnricher.Service {
  override def enrichUser(user: User): IO[UserError, User] = ZIO.succeed(user)

  override def enrichLimitedUser(limitedUser: LimitedUser): IO[UserError, LimitedUser] = ZIO.succeed(limitedUser)

  val layer: ZLayer[Any, Nothing, MiminoUserEnricher] = ZIO.succeed(this).toLayer
}
