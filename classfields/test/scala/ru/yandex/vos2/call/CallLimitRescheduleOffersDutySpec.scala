package ru.yandex.vos2.call

import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.model.message.ExtDataSchema.CallCountStat
import ru.yandex.realty.storage.CallCountStatStorage
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vos2.config.TestRealtySchedulerComponents
import ru.yandex.vos2.dao.call.{CallStatDao, StatRecord}

import scala.collection.JavaConverters._

class CallLimitRescheduleOffersDutySpec extends WordSpec with Matchers with MockitoSupport {

  trait Fixture {
    val components = new TestRealtySchedulerComponents
    val callCountStatProvider = mock[Provider[CallCountStatStorage]]
    val userCallStatDao = mock[CallStatDao[Long]]
    val clientCallStatDao = mock[CallStatDao[Long]]

    val duty =
      new CallLimitRescheduleOffersDuty(callCountStatProvider, userCallStatDao, clientCallStatDao, components)
  }

  classOf[CallLimitRescheduleOffersDuty].getSimpleName should {
    "successfully process empty current snapshot" in new Fixture {
      when(callCountStatProvider.get())
        .thenReturn(buildCallCountStatStorage())

      when(userCallStatDao.getSnapshots(0))
        .thenReturn(Seq.empty[Long])

      Mockito
        .doAnswer(new Answer[Void]() {
          override def answer(invocation: InvocationOnMock): Void = {
            val args = invocation.getArguments
            args(2) match {
              case s: Seq[_] => s should be(Seq(StatRecord[Long](0, 0)))
              case _ => fail("bad argument type")
            }
            null
          }
        })
        .when(userCallStatDao)
        .writeSnapshot(any(), any(), any())

      when(userCallStatDao.getSnapshots(1))
        .thenReturn(Seq.empty[Long])

      when(callCountStatProvider.get())
        .thenReturn(buildCallCountStatStorage())

      when(clientCallStatDao.getSnapshots(0))
        .thenReturn(Seq.empty[Long])
      Mockito
        .doAnswer(new Answer[Void]() {
          override def answer(invocation: InvocationOnMock): Void = {
            val args = invocation.getArguments
            args(2) match {
              case s: Seq[_] => s should be(Seq(StatRecord[Long](0, 0)))
              case _ => fail("bad argument type")
            }
            null
          }
        })
        .when(clientCallStatDao)
        .writeSnapshot(any(), any(), any())

      when(clientCallStatDao.getSnapshots(1))
        .thenReturn(Seq.empty[Long])

      duty.checkCallCountAndMaybeReschedule()

      Mockito.verify(callCountStatProvider, Mockito.times(2)).get()
      Mockito.verify(userCallStatDao, Mockito.times(2)).writeSnapshot(any(), any(), any())
      Mockito.verify(clientCallStatDao, Mockito.times(2)).writeSnapshot(any(), any(), any())
    }

    "successfully process non empty current snapshot" in new Fixture {
      val userCallCount = Map("uid_2" -> java.lang.Integer.valueOf(12))

      when(callCountStatProvider.get())
        .thenReturn(buildCallCountStatStorage(userCallCount))

      when(userCallStatDao.getSnapshots(0))
        .thenReturn(Seq.empty[Long])
      Mockito
        .doAnswer(new Answer[Void]() {
          override def answer(invocation: InvocationOnMock): Void = {
            val args = invocation.getArguments
            val shard: Int = args(1).asInstanceOf[Int]
            shard >= 0 && shard <= 1 should be(true)
            val expected = if (shard == 0) {
              Seq(StatRecord[Long](2L, 12L))
            } else {
              Seq(StatRecord[Long](0, 0))
            }
            args(2) match {
              case s: Seq[_] => s should be(expected)
              case _ => fail("bad argument type")
            }
            null
          }
        })
        .when(userCallStatDao)
        .writeSnapshot(any(), any(), any())

      when(userCallStatDao.getSnapshots(1))
        .thenReturn(Seq.empty[Long])

      when(callCountStatProvider.get())
        .thenReturn(buildCallCountStatStorage(userCallCount))

      when(clientCallStatDao.getSnapshots(0))
        .thenReturn(Seq.empty[Long])
      Mockito
        .doAnswer(new Answer[Void]() {
          override def answer(invocation: InvocationOnMock): Void = {
            val args = invocation.getArguments
            args(2) match {
              case s: Seq[_] => s should be(Seq(StatRecord[Long](0, 0)))
              case _ => fail("bad argument type")
            }
            null
          }
        })
        .when(clientCallStatDao)
        .writeSnapshot(any(), any(), any())

      when(clientCallStatDao.getSnapshots(1))
        .thenReturn(Seq.empty[Long])

      duty.checkCallCountAndMaybeReschedule()

      Mockito.verify(callCountStatProvider, Mockito.times(2)).get()
      Mockito.verify(userCallStatDao, Mockito.times(2)).writeSnapshot(any(), any(), any())
      Mockito.verify(clientCallStatDao, Mockito.times(2)).writeSnapshot(any(), any(), any())
    }
  }

  private def buildCallCountStatStorage(uidCallCountMap: Map[String, java.lang.Integer] = Map.empty) =
    new CallCountStatStorage(CallCountStat.newBuilder().putAllUidToCalls(uidCallCountMap.asJava).build())
}
