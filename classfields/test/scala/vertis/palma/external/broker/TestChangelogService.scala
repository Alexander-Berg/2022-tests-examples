package vertis.palma.external.broker

import com.google.protobuf.{Descriptors, Message}
import ru.yandex.vertis.palma.event.dictionary_event.MutationEvent.ActionEnum
import vertis.palma.dao.model.DictionaryItem
import vertis.palma.service.model.RequestContext
import vertis.zio.RTask
import zio.ZIO

import java.time.Instant

/** @author kusaeva
  */
class TestChangelogService extends ChangelogService {

  override protected def logChanges(
      dictDescriptor: Descriptors.Descriptor,
      id: DictionaryItem.Id,
      context: RequestContext,
      eventTime: Instant,
      action: ActionEnum.Action,
      previous: Option[Message],
      current: Option[Message]): RTask[Unit] = ZIO.unit
}
