package ru.yandex.vertis.feedprocessor.util

import ru.yandex.common.tokenization._

/**
  * For tests
  */
class DummyTokensDistribution(tokensNumber: Int) extends TokensDistribution {
  private val tokens = new IntTokens(tokensNumber)

  override def ofTokens: Tokens = tokens
  override def getTokens: Set[Token] = tokens.toSet
  override def getOwnerships: Ownerships = ???
  override def isDistributed: Boolean = ???
  override def set(tokens: Set[Token], ownerships: Ownerships): Unit = ???
  override def take(token: Token, ownerships: Ownerships): Unit = ???
  override def `return`(token: Token, ownerships: Ownerships): Unit = ???
  override def returnAll(): Unit = ???
  override def setDistributed(value: Boolean): Unit = ???
}
