package ru.yandex.common.tokenization

import akka.actor.Actor
import ru.yandex.common.actor.logging.Slf4jActorLogging

/**
 * Collects distributed tokens.
 */
class TokensCollector
  extends Actor
  with Slf4jActorLogging {

  @volatile
  var tokens = Set.empty[Token]

  override def receive = {
    case TokensDistributor.Command.Current(tokens, _) =>
      log.info(s"Current tokens is ${tokens.mkString(", ")}")
      this.tokens = tokens
    case TokensDistributor.Command.Take(token, _) =>
      tokens = tokens + token
      log.info(s"Take token <$token>. Already taken <${tokens.size}> tokens")
    case TokensDistributor.Command.Return(token, _) =>
      tokens = tokens - token
      log.info(s"Return token <$token>. Already taken <${tokens.size}> tokens")
    case TokensDistributor.Command.ReturnAll =>
      log.info("Return all tokens")
      tokens = Set.empty

    case TokensDistributor.Notification.Distributed =>
      log.info("All tokens are distributed")
    case TokensDistributor.Notification.DistributionViolation =>
      log.info("Distribution has been violated")
    case TokensDistributor.Notification.Redistributing =>
      log.info("Tokens are redistributing")
  }
}

