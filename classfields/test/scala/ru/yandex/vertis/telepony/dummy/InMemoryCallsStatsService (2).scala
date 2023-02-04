package ru.yandex.vertis.telepony.dummy

import org.joda.time.DateTime
import ru.yandex.vertis.telepony.model.{CallV2, CallsStats, ObjectId, Tag}
import ru.yandex.vertis.telepony.service.CallsStatsService

import scala.collection.mutable
import scala.concurrent.Future

class InMemoryCallsStatsService extends CallsStatsService {
  private val history = mutable.Map.empty[String, mutable.Buffer[DateTime]]

  override def registerCall(call: CallV2): Future[Unit] = {
    val key = keyFrom(call.redirect.objectId, call.redirect.key.tag)
    val buffer = history.getOrElse(key, mutable.Buffer.empty[DateTime])
    buffer += call.time
    history.put(key, buffer)
    Future.unit
  }

  override def callsStats(objectId: ObjectId, tag: Tag): Future[CallsStats] = {
    val stats = history.getOrElse(keyFrom(objectId, tag), mutable.Buffer.empty)
    Future.successful(CallsStats(stats.size, stats.lastOption))
  }

  private def keyFrom(objectId: ObjectId, tag: Tag) = s"$objectId:$tag"
}
