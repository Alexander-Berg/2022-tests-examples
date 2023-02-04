package ru.yandex.vertis.vsquality.techsupport.conversion.jivosite

import com.softwaremill.tagging._
import ru.yandex.vertis.vsquality.techsupport.model.Appeal.{Mark, Marks}
import ru.yandex.vertis.vsquality.techsupport.model.Request.TechsupportAppeal.AddMarks
import ru.yandex.vertis.vsquality.techsupport.model.api.RequestMeta
import ru.yandex.vertis.vsquality.techsupport.model.{ChatProvider, Domain, RequestId}
import ru.yandex.vertis.vsquality.techsupport.model.external.jivosite.{ChatUser, Request, UserMessage}
import ru.yandex.vertis.vsquality.techsupport.util.SpecBase

import java.time.Instant

class JivositeFormatInstancesSpec extends SpecBase {

  private val recep = ChatUser(
    Some("user:10245464"),
    Some("Name"),
    Some("+79097777755"),
    Some("a@ya.ru"),
    None,
    None,
    None,
    None
  )

  private val sender = ChatUser(
    Some("user:66245464"),
    Some("Name2"),
    Some("+79097777755"),
    Some("b@ya.ru"),
    None,
    None,
    None,
    None
  )
  private val ts = Instant.now()

  private val meta = RequestMeta(
    ts,
    "reqId".taggedWith,
    Some("deviceId")
  )

  "jivositeReads" should {
    "parse marks" in {
      val message = UserMessage(
        "text",
        text = Some("###tag ffsdfsd ###tag2 ###Метка:mark ###Метка:mark ddfdf ###Метка:mark2"),
        id = Some("id"),
        file = None,
        thumb = None,
        suggests = None
      )

      val req = Request(Some(sender), Some(message), Some(recep))

      val res = JivositeFormatInstances.jivositeReads
        .deserialize(
          req,
          Domain.Autoru,
          ChatProvider.VertisChats
        )(meta)
        .getOrElse(throw new RuntimeException("should not be empty"))
        .collect { case AddMarks(_, _, marks) =>
          marks
        }
        .head

      res shouldBe Marks(
        Set(
          Mark("mark", ts, "id"),
          Mark("mark2", ts, "id")
        )
      )
    }
  }
}
