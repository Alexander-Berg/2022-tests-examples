package ru.yandex.vertis.picture.support

import org.mockito
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FlatSpecLike, Matchers}
import ru.yandex.vertis.picture.client.MdsClientImpl
import ru.yandex.vertis.picture.{MdsClient, PictureSettings}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class MdsPictureSupportSpec
    extends FlatSpecLike
        with BeforeAndAfter
        with Matchers
        with MockitoSugar {

  implicit val ex = new ExecutionContext {
    override def reportFailure(cause: Throwable): Unit = {}

    override def execute(runnable: Runnable): Unit = runnable.run()
  }

  private var client: MdsClient = _
  private var support: MdsPictureSupport = _
  private val Name = "add.2"
  private val GroupId = "1234"

  before {
    client = {
      val settings = PictureSettings(
        uploadUrl = "http://avatars-int.mdst.yandex.net:13000 ",
        getUrl = "http://avatars.mdst.yandex.net:80",
        namespace = "realty"
      )
      val m = mock[MdsClientImpl]
      when(m.settings).thenReturn(settings)
      when(m.delete(mockito.Matchers.anyString(), mockito.Matchers.anyString()))
          .thenReturn(Future.successful(true))
      m
    }
    support = new MdsPictureSupport(client)
  }

  def await[T](f: Future[T]): T = Await.result(f, 10.second)

  it should "accept prod https url" in {
    await {
      support.delete(
        "https://avatars.mdst.yandex.net:80/get-realty/1234/add.2/orig"
      )
    }
    verify(client).delete(GroupId, Name)
  }

  it should "accept prod url without port" in {
    await {
      support.delete(
        "https://avatars.mdst.yandex.net/get-realty/1234/add.2/orig"
      )
    }
    verify(client).delete(GroupId, Name)
  }

  it should "accept prod http url" in {
    await {
      support.delete(
        "http://avatars.mdst.yandex.net/get-realty/1234/add.2/orig"
      )
    }
    verify(client).delete(GroupId, Name)
  }

  it should "reject wrong port" in {
    await {
      val delete: Future[Unit] = support.delete(
        "http://avatars.mdst.yandex.net:12002/get-realty/1234/add.2/orig"
      )
      delete.failed
    }
    verify(client, never()).delete(mockito.Matchers.anyString(), mockito.Matchers.anyString())
  }

  it should "reject wrong host" in {
    await {
      support.delete(
        "http://avatars2.mdst.yandex.net/get-realty/1234/add.2/orig"
      ).failed
    }
    verify(client, never()).delete(mockito.Matchers.anyString(), mockito.Matchers.anyString())
  }

  it should "reject wrong service" in {
    await {
      support.delete(
        "http://avatars.mdst.yandex.net/get-realty2/1234/add.2/orig"
      ).failed
    }
    verify(client, never()).delete(mockito.Matchers.anyString(), mockito.Matchers.anyString())
  }
}
