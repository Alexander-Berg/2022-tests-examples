package vertistraf.common.pushnoy.client.mocks

import vertistraf.common.pushnoy.client.ArbitraryTestBase
import vertistraf.common.pushnoy.client.service.TopicNameResolver
import zio._

/** @author kusaeva
  */
class TestTopicNameResolver extends TopicNameResolver.Service with ArbitraryTestBase {

  private val topics = Map(
    "personal_recommendations" -> "Персональные рекомендации"
  )

  override def resolve(id: String): Task[String] =
    UIO(topics.getOrElse(id, random[String]))
}
