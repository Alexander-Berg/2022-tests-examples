package ru.yandex.auto.vin.decoder.scheduler.workers.partners.autocode

import auto.carfax.common.utils.tracing.Traced
import io.opentracing.noop.NoopTracerFactory
import org.mockito.Mockito.{reset, verify}
import org.mockito.internal.verification.AtLeast
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.Json
import ru.yandex.auto.vin.decoder.manager.vin.autocode.VinAutocodeManager
import ru.yandex.auto.vin.decoder.manager.vin.{VinHistoryManager, VinUpdateManager}
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.model.scheduler.cs
import ru.yandex.auto.vin.decoder.partners.autocode.model.{
  AutocodeReportResponse,
  Source,
  SourceExtendedState,
  SourceState
}
import ru.yandex.auto.vin.decoder.partners.autocode.{AutocodeHttpManager, AutocodeReportType, AutocodeRequest}
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState
import ru.yandex.auto.vin.decoder.raw.autocode.AutocodeReportResponseRaw
import ru.yandex.auto.vin.decoder.scheduler.models.WatchingStateHolder
import ru.yandex.auto.vin.decoder.scheduler.workers.queue.WorkersQueue
import ru.yandex.auto.vin.decoder.scheduler.workers.{MixedRateLimiter, WorkResult}
import ru.yandex.auto.vin.decoder.storage.vin.VinWatchingDao
import ru.yandex.auto.vin.decoder.utils.scheduler.PartnerUtils._
import ru.yandex.auto.vin.decoder.vos.VosNotifier
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class VinAutocodeWorkerTest extends AnyWordSpecLike with MockitoSupport with BeforeAndAfterAll {

  private val autocodeHttpManager = mock[AutocodeHttpManager]
  private val vinAutocodeManager = mock[VinAutocodeManager]
  private val vosNotifier = mock[VosNotifier]
  private val rateLimiter = MixedRateLimiter(100)
  private val vinHistoryManager = mock[VinHistoryManager]
  private val vinUpdateDao = mock[VinWatchingDao]
  private val queue = mock[WorkersQueue[VinCode, CompoundState]]
  private val feature = mock[Feature[Boolean]]
  private val vinUpdateManager = mock[VinUpdateManager]

  implicit val tracer = NoopTracerFactory.create()
  implicit val t = Traced.empty
  private val vin = VinCode("X4X3D59430PS96744")

  implicit private val metrics = TestOperationalSupport

  override def beforeAll(): Unit = {
    reset(autocodeHttpManager)
    reset(vinAutocodeManager)
    reset(vosNotifier)
    reset(vinHistoryManager)
  }

  val reportTypes = Set(
    AutocodeReportType.Main,
    AutocodeReportType.MainUpdate,
    AutocodeReportType.OldTaxiByVin,
    AutocodeReportType.TechInspections,
    AutocodeReportType.Tech,
    AutocodeReportType.Identifiers
  )

  val mainReportTypes = Set(
    AutocodeReportType.Main,
    AutocodeReportType.MainUpdate
  )

  reportTypes.foreach { reportType =>
    val worker = new VinAutocodeWorker(
      reportType,
      autocodeHttpManager,
      vinAutocodeManager,
      Some(vosNotifier),
      rateLimiter,
      None,
      vinUpdateDao,
      queue,
      Some(vinUpdateManager),
      feature
    )

    s"AutocodeWorker($reportType)" should {

      "ignore if no autocode" in {
        val b = CompoundState.newBuilder.build()
        intercept[IllegalArgumentException] {
          worker.action(WatchingStateHolder(vin, b, 1))
        }
      }

      "ignore if autocode completed with no flags" in {
        val b = CompoundState.newBuilder
        b.getAutocodeStateBuilder
          .getReportBuilder(reportType)
          .setReportId("dsaf")
          .setRequestSent(324)
          .setReportArrived(54643)

        val res = worker.action(WatchingStateHolder(vin, b.build(), 1))
        assert(res.updater.isEmpty)
        assert(!res.reschedule)
      }

      "finish report if invalid" in {
        val b = CompoundState.newBuilder
        b.getAutocodeStateBuilder.setInvalid(true)
        b.getAutocodeStateBuilder.getReportBuilder(reportType).setShouldProcess(true)
        val state = WatchingStateHolder(vin, b.build(), 1)
        val res = worker.action(state)

        assert(!res.reschedule)

        val updated = res.updater.get(state.toUpdate)
        val report = updated.state.getAutocodeState.findReport(reportType.toString).get

        assert(!report.getShouldProcess)
        assert(!report.getForceUpdate)
        assert(report.getStateUpdateHistoryCount == 0)
      }

      "send request if should process" in {
        val b = CompoundState.newBuilder()
        b.getAutocodeStateBuilder.getReportBuilder(reportType).setShouldProcess(true)
        val state = WatchingStateHolder(vin, b.build(), 1)

        when(autocodeHttpManager.makeCreateRequest(?, ?, ?)(?, ?))
          .thenReturn(Future.successful(AutocodeRequest("5463", vin, AutocodeReportType.Main)))

        val res = worker.action(WatchingStateHolder(vin, b.build(), 1))

        assert(res.updater.nonEmpty)
        assert(res.updater.get.delay().toDuration == 1.minutes)
        assert(!res.reschedule)
        val updated = res.updater.get(state.toUpdate)
        val reportOpt = updated.state.getAutocodeState.findReport(reportType.toString)

        assert(reportOpt.exists(_.getRequestSent != 0))
        assert(reportOpt.exists(_.getReportId == "5463"))
      }

      "reschedule if send failed" in {
        val b = CompoundState.newBuilder()
        b.getAutocodeStateBuilder.getReportBuilder(reportType).setShouldProcess(true)
        val state = WatchingStateHolder(vin, b.build(), 1)
        when(autocodeHttpManager.makeCreateRequest(?, ?, ?)(?, ?))
          .thenReturn(Future.failed(new RuntimeException))

        val res: WorkResult[CompoundState] = worker.action(state)

        assert(res.updater.nonEmpty)
        assert(res.updater.get.delay().toDuration >= 5.minute)
        assert(res.updater.get.delay().toDuration <= 10.minute)
        assert(!res.reschedule)
      }

      "send regenerate request if force update" in {
        val b = CompoundState.newBuilder()
        b.getAutocodeStateBuilder
          .getReportBuilder(reportType)
          .setForceUpdate(true)
          .setRequestSent(345)
          .setReportArrived(452354)
          .setReportId("asdf")
        val state = WatchingStateHolder(vin, b.build(), 1)
        when(autocodeHttpManager.makeRegenerateRequest(?, ?, ?)(?, ?))
          .thenReturn(Future.successful(AutocodeRequest("asdf", vin, AutocodeReportType.Main)))

        val res: WorkResult[CompoundState] = worker.action(state)

        assert(!res.reschedule)

        val updated = res.updater.get(state.toUpdate)
        val reportOpt = updated.state.getAutocodeState.findReport(reportType.toString)

        assert(reportOpt.exists(_.getRequestSent != 0))
        assert(reportOpt.exists(_.getReportId == "asdf"))
      }

      "reschedule if regenerate request failed" in {
        val b = CompoundState.newBuilder()
        b.getAutocodeStateBuilder
          .getReportBuilder(reportType)
          .setForceUpdate(true)
          .setRequestSent(345)
          .setReportArrived(452354)
          .setReportId("asdf")
        val state = WatchingStateHolder(vin, b.build(), 1)
        when(autocodeHttpManager.makeRegenerateRequest(?, ?, ?)(?, ?))
          .thenReturn(Future.failed(new RuntimeException))

        val res: WorkResult[CompoundState] = worker.action(state)

        assert(res.updater.nonEmpty)
        assert(res.updater.get.delay().toDuration >= 5.minute)
        assert(res.updater.get.delay().toDuration <= 10.minute)
        assert(!res.reschedule)
      }

      "get response if unfinished" in {
        val b = CompoundState.newBuilder()
        b.getAutocodeStateBuilder
          .getReportBuilder(reportType)
          .setForceUpdate(true)
          .setRequestSent(34234545)
          .setReportId("5463")
        val state = WatchingStateHolder(vin, b.build(), 1)
        when(autocodeHttpManager.getResult(?)(?, ?))
          .thenReturn(Future.successful(VinAutocodeWorkerTest.ready))
        when(vinAutocodeManager.update(?, ?, ?, ?)(?, ?)).thenReturn(Future.unit)
        when(vosNotifier.asyncNotify(?)(?)).thenReturn(Future.successful(()))
        when(vinAutocodeManager.shouldFallbackGibdd(?)).thenReturn(None)

        val res: WorkResult[CompoundState] = worker.action(state)

        val updated = res.updater.get(state.toUpdate)
        val reportOpt = updated.state.getAutocodeState.findReport(reportType.toString)
        assert(res.updater.nonEmpty)
        assert(reportOpt.exists(_.getRequestSent != 0))
        assert(reportOpt.exists(_.getReportArrived != 0))
        assert(reportOpt.exists(_.getReportId == "5463"))
      }

      "reschedule if not ready" in {
        val b = CompoundState.newBuilder()
        b.getAutocodeStateBuilder
          .getReportBuilder(reportType)
          .setForceUpdate(true)
          .setRequestSent(34234545)
          .setReportId("asdf")
        val state = WatchingStateHolder(vin, b.build(), 1)
        when(autocodeHttpManager.getResult(?)(?, ?))
          .thenReturn(Future.successful(VinAutocodeWorkerTest.notReady))

        val res: WorkResult[CompoundState] = worker.action(state)

        assert(res.updater.nonEmpty)
        assert(res.updater.get.delay().toDuration == 1.minute)
        assert(!res.reschedule)
      }

      "reschedule if not ready with regards to counter" in {
        val b = CompoundState.newBuilder()
        b.getAutocodeStateBuilder
          .getReportBuilder(reportType)
          .setForceUpdate(true)
          .setRequestSent(34234545)
          .setReportId("asdf")
          .setCounter(10)
        val state = WatchingStateHolder(vin, b.build(), 1)
        when(autocodeHttpManager.getResult(?)(?, ?))
          .thenReturn(Future.successful(VinAutocodeWorkerTest.notReady))

        val res: WorkResult[CompoundState] = worker.action(state)

        val updated = res.updater.get(state.toUpdate)
        val reportOpt = updated.state.getAutocodeState.findReport(reportType.toString)
        assert(res.updater.nonEmpty)
        assert(reportOpt.exists(_.getCounter == 11))
        assert(res.updater.get.delay().toDuration == 4.minutes)
        assert(!res.reschedule)
      }

      "reschedule if not ready with regards to counter but no more then 3 hours" in {
        val b = CompoundState.newBuilder()
        b.getAutocodeStateBuilder
          .getReportBuilder(reportType)
          .setForceUpdate(true)
          .setRequestSent(34234545)
          .setReportId("asdf")
          .setCounter(3000)

        val state = WatchingStateHolder(vin, b.build(), 1)

        when(autocodeHttpManager.getResult(?)(?, ?))
          .thenReturn(Future.successful(VinAutocodeWorkerTest.notReady))

        val res: WorkResult[CompoundState] = worker.action(state)

        assert(res.updater.nonEmpty)
        assert(res.updater.get.delay().toDuration == 180.minute)
        assert(!res.reschedule)
      }

      "reschedule if update db failed" in {
        val b = CompoundState.newBuilder()
        b.getAutocodeStateBuilder
          .getReportBuilder(reportType)
          .setForceUpdate(true)
          .setRequestSent(34234545)
          .setReportId("asdf")
        val state = WatchingStateHolder(vin, b.build(), 1)

        when(autocodeHttpManager.getResult(?)(?, ?))
          .thenReturn(Future.successful(VinAutocodeWorkerTest.notReady))
        when(vinAutocodeManager.update(?, ?, ?, ?)(?, ?)).thenReturn(Future.failed(new RuntimeException))

        val res: WorkResult[CompoundState] = worker.action(state)

        assert(res.updater.nonEmpty)
        assert(res.updater.get.delay().toDuration == 1.minute)
        assert(!res.reschedule)
      }

      "get and save response if vos notify failed" in {
        val b = CompoundState.newBuilder()
        b.getAutocodeStateBuilder
          .getReportBuilder(reportType)
          .setForceUpdate(true)
          .setRequestSent(34234545)
          .setReportId("asdf")
        val state = WatchingStateHolder(vin, b.build(), 1)
        when(autocodeHttpManager.getResult(?)(?, ?))
          .thenReturn(Future.successful(VinAutocodeWorkerTest.ready))
        when(vinAutocodeManager.update(?, ?, ?, ?)(?, ?)).thenReturn(Future.unit)
        when(vosNotifier.asyncNotify(?)(?)).thenReturn(Future.failed(new RuntimeException))
        when(vinAutocodeManager.shouldFallbackGibdd(?)).thenReturn(None)

        val res: WorkResult[CompoundState] = worker.action(state)

        val updated = res.updater.get(state.toUpdate)
        val reportOpt = updated.state.getAutocodeState.findReport(reportType.toString)
        assert(res.updater.nonEmpty)
        assert(reportOpt.exists(_.getRequestSent != 0))
        assert(reportOpt.exists(_.getReportArrived != 0))
        assert(reportOpt.exists(_.getReportId == "asdf"))
      }

      "get response if force update, but unfinished" in {
        val b = CompoundState.newBuilder()
        b.getAutocodeStateBuilder
          .getReportBuilder(reportType)
          .setForceUpdate(true)
          .setRequestSent(34234545)
          .setReportArrived(452354)
          .setReportId("asdf")
        val state = WatchingStateHolder(vin, b.build(), 1)
        when(autocodeHttpManager.getResult(?)(?, ?))
          .thenReturn(Future.successful(VinAutocodeWorkerTest.ready))
        when(vinAutocodeManager.update(?, ?, ?, ?)(?, ?)).thenReturn(Future.unit)
        when(vosNotifier.asyncNotify(?)(?)).thenReturn(Future.successful(()))
        when(vinAutocodeManager.shouldFallbackGibdd(?)).thenReturn(None)

        val res: WorkResult[CompoundState] = worker.action(state)

        val updated = res.updater.get(state.toUpdate)
        val reportOpt = updated.state.getAutocodeState.findReport(reportType.toString)
        assert(res.updater.nonEmpty)
        assert(reportOpt.exists(_.getRequestSent != 0))
        assert(reportOpt.exists(_.getReportArrived != 0))
        assert(reportOpt.exists(_.getReportArrived != 452354))
        assert(reportOpt.exists(_.getReportId == "asdf"))
      }

      "finish if identifier rejected" in {
        val b = CompoundState.newBuilder()
        b.getAutocodeStateBuilder
          .getReportBuilder(reportType)
          .setRequestSent(123)
          .setReportId("asdf")
        val state = WatchingStateHolder(vin, b.build, 1)
        when(autocodeHttpManager.getResult(?)(?, ?))
          .thenReturn(Future.successful(VinAutocodeWorkerTest.rejected))
        when(vinAutocodeManager.update(?, ?, ?, ?)(?, ?)).thenReturn(Future.unit)

        val res: WorkResult[CompoundState] = worker.action(state)

        val updated = res.updater.get(state.toUpdate)
        val reportOpt = updated.state.getAutocodeState.findReport(reportType.toString)
        assert(res.updater.nonEmpty)
        assert(reportOpt.exists(_.getReportArrived != 0))
        assert(reportOpt.exists(_.getReportId == "asdf"))
        assert(reportOpt.exists(_.getNoInfo == true))
      }
    }
  }

  mainReportTypes.foreach { reportType =>
    val worker = new VinAutocodeWorker(
      reportType,
      autocodeHttpManager,
      vinAutocodeManager,
      Some(vosNotifier),
      rateLimiter,
      None,
      vinUpdateDao,
      queue,
      Some(vinUpdateManager),
      feature
    )

    s"VinAutoCodeWorker($reportType)" should {
      "fallback to another providers" in {
        val b = CompoundState.newBuilder()
        b.getAutocodeStateBuilder
          .getReportBuilder(reportType)
          .setRequestSent(123)
          .setReportId("asdf")
        val state = WatchingStateHolder(vin, b.build, 1)
        when(autocodeHttpManager.getResult(?)(?, ?))
          .thenReturn(Future.successful(VinAutocodeWorkerTest.gibddFailedSources))
        when(vinAutocodeManager.update(?, ?, ?, ?)(?, ?)).thenReturn(Future.unit)
        when(vinUpdateManager.forceUpdateCustomGibddBlocks(?, ?)(?, ?)).thenReturn(Future.unit)
        when(vinAutocodeManager.shouldFallbackGibdd(?))
          .thenReturn(Some(List(Source("gibdd.wanted", SourceState.ERROR, Some(SourceExtendedState.ERROR)))))

        val res: WorkResult[CompoundState] = worker.action(state)

        val updated = res.updater.get(state.toUpdate)
        val reportOpt = updated.state.getAutocodeState.findReport(reportType.toString)
        verify(vinUpdateManager, new AtLeast(1)).forceUpdateCustomGibddBlocks(?, ?)(?, ?)
        assert(res.updater.nonEmpty)
        assert(reportOpt.exists(_.getRequestSent != 0))
        assert(reportOpt.exists(_.getReportArrived != 0))
        assert(reportOpt.exists(_.getReportId == "asdf"))
        assert(reportOpt.exists(_.getNoInfo == true))
      }
    }
  }
}

object VinAutocodeWorkerTest {

  val readyModel: String =
    """
      |{
      |  "state": "ok",
      |  "size": 1,
      |  "stamp": "2019-04-26T12:11:06.144Z",
      |  "data": [
      |    {
      |      "domain_uid": "autoru",
      |      "report_type_uid": "autoru_7_sources@autoru",
      |      "vehicle_id": "X4X3D59430PS96744",
      |      "query": {
      |        "type": "VIN",
      |        "body": "X4X3D59430PS96744"
      |      },
      |      "progress_ok": 8,
      |      "progress_wait": 0,
      |      "progress_error": 0,
      |      "state": {
      |        "sources": [
      |          {
      |            "_id": "av.taxi",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          },
      |          {
      |            "_id": "base",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          },
      |          {
      |            "_id": "gibdd.dtp",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          },
      |          {
      |            "_id": "gibdd.history",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          },
      |          {
      |            "_id": "gibdd.restrict",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          },
      |          {
      |            "_id": "gibdd.wanted",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          },
      |          {
      |            "_id": "pledge",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          },
      |          {
      |            "_id": "sub.base",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          }
      |        ]
      |      },
      |      "content": {
      |        "identifiers": {
      |          "_comment": "Идентификаторы",
      |          "vehicle": {
      |            "vin": "X4X3D59430PS96744",
      |            "reg_num": "А329ЕВ799",
      |            "sts": "7761034183",
      |            "pts": "39НУ031727"
      |          }
      |        },
      |        "tech_data": {
      |          "_comment": "Характеристики ТС",
      |          "brand": {
      |            "_comment": "Марка",
      |            "name": {
      |              "original": "Бмв 320D Хdrivе"
      |            }
      |          },
      |          "model": {
      |            "_comment": "Модель",
      |            "name": {
      |              "original": "Бмв"
      |            }
      |          },
      |          "type": {
      |            "_comment": "Тип (Вид) ТС",
      |            "name": "Легковые Автомобили Седан"
      |          },
      |          "body": {
      |            "_comment": "Кузов",
      |            "color": {
      |              "name": "Синий",
      |              "type": "Синий"
      |            },
      |            "number": "Х4Х3D59430РS96744"
      |          },
      |          "chassis": {
      |            "_comment": "Шасси"
      |          },
      |          "engine": {
      |            "_comment": "Двигатель",
      |            "fuel": {
      |              "type": "Дизельный"
      |            },
      |            "volume": 1995,
      |            "power": {
      |              "hp": 184,
      |              "kw": 135
      |            }
      |          },
      |          "weight": {
      |            "_comment": "Масса",
      |            "netto": 1595,
      |            "max": 2135
      |          },
      |          "transmission": {
      |            "_comment": "Трансмиссия"
      |          },
      |          "drive": {
      |            "_comment": "Привод",
      |            "type": "Заднеприводной"
      |          },
      |          "wheel": {
      |            "_comment": "Рулевое колесо",
      |            "position": "LEFT"
      |          },
      |          "year": 2014
      |        },
      |        "taxi": {
      |            "history": {
      |              "items": [
      |                {
      |                  "_id": 4282534717,
      |                  "date": {
      |                    "start": "2018-06-29 00:00:00",
      |                    "end": "2023-06-28 00:00:00"
      |                  },
      |                  "license": {
      |                    "number": "0226178",
      |                    "status": "ACTIVE"
      |                  },
      |                  "company": {
      |                    "name": "Ракурс"
      |                  },
      |                  "ogrn": "1187746362726",
      |                  "tin": "7725487881",
      |                  "number_plate": {
      |                    "is_yellow": false
      |                  },
      |                  "vehicle": {
      |                    "brand": {
      |                      "name": "HYUNDAI"
      |                    },
      |                    "model": {
      |                      "name": "SOLARIS"
      |                    },
      |                    "color": "Белый (бело-желто-серый)",
      |                    "reg_num": "О992ЕР799",
      |                    "year": 2018
      |                  },
      |                  "region": {
      |                    "code": "50"
      |                  },
      |                  "permit": {
      |                    "number": "МО 0217326"
      |                  },
      |                  "city": {
      |                    "name": "Московская область"
      |                  }
      |                }
      |              ],
      |              "count": 1
      |            },
      |            "used_in_taxi": true
      |          },
      |        "accidents": {
      |          "history": {
      |            "_comment": "История ДТП",
      |            "items": [
      |              {
      |                "_id": 2702271101,
      |                "number": "450214229",
      |                "accident": {
      |                  "date": "2016-10-04 21:45:00"
      |                },
      |                "type": "Столкновение",
      |                "state": "Повреждено",
      |                "vehicle": {
      |                  "brand": {
      |                    "name": "Bmw"
      |                  },
      |                  "model": {
      |                    "name": "Прочие Модели Bmw"
      |                  },
      |                  "year": 2014
      |                },
      |                "geo": {
      |                  "region": "Москва"
      |                }
      |              }
      |            ],
      |            "count": 1
      |          },
      |          "insurance": {
      |            "_comment": "История повреждений из страховых компаний",
      |            "items": [],
      |            "count": 0
      |          },
      |          "has_accidents": true
      |        },
      |        "pledges": {
      |          "_comment": "Обременения на ТС",
      |          "items": [],
      |          "count": 0
      |        },
      |        "restrictions": {
      |          "registration_actions": {
      |            "_comment": "Ограничения на рег. действия",
      |            "items": [],
      |            "count": 0,
      |            "has_restrictions": false
      |          }
      |        },
      |        "stealings": {
      |          "_comment": "Проверка на угон",
      |          "count": 0,
      |          "is_wanted": false,
      |          "items": []
      |        },
      |        "ownership": {
      |          "history": {
      |            "_comment": "История владения",
      |            "items": [
      |              {
      |                "_id": 2465663091,
      |                "date": {
      |                  "start": "2018-08-09 00:00:00"
      |                },
      |                "owner": {
      |                  "type": "PERSON"
      |                }
      |              },
      |              {
      |                "_id": 366069639,
      |                "date": {
      |                  "start": "2018-04-13 00:00:00",
      |                  "end": "2018-08-09 00:00:00"
      |                },
      |                "owner": {
      |                  "type": "PERSON"
      |                }
      |              },
      |              {
      |                "_id": 2110008216,
      |                "date": {
      |                  "start": "2014-12-08 00:00:00",
      |                  "end": "2018-03-12 00:00:00"
      |                },
      |                "owner": {
      |                  "type": "LEGAL"
      |                }
      |              }
      |            ],
      |            "count": 3
      |          }
      |        },
      |        "additional_info": {
      |          "vehicle": {
      |            "category": {
      |              "code": "B"
      |            }
      |          }
      |        }
      |      },
      |      "uid": "autoru_7_sources_X4X3D59430PS96744@autoru",
      |      "name": "NONAME",
      |      "comment": "",
      |      "tags": "",
      |      "created_at": "2017-12-30T23:52:18.363Z",
      |      "created_by": "system",
      |      "updated_at": "2019-01-16T06:01:37.513Z",
      |      "updated_by": "system",
      |      "active_from": "1900-01-01T00:00:00.000Z",
      |      "active_to": "3000-01-01T00:00:00.000Z"
      |    }
      |  ]
      |}
    """.stripMargin

  val notReadyModel: String =
    """
      |{
      |  "state": "waiting",
      |  "size": 1,
      |  "stamp": "2019-04-26T12:11:06.144Z",
      |  "data": [
      |    {
      |      "domain_uid": "autoru",
      |      "report_type_uid": "autoru_7_sources@autoru",
      |      "vehicle_id": "X4X3D59430PS96744",
      |      "query": {
      |        "type": "VIN",
      |        "body": "X4X3D59430PS96744"
      |      },
      |      "progress_ok": 5,
      |      "progress_wait": 3,
      |      "progress_error": 0,
      |      "state": {
      |        "sources": [
      |          {
      |            "_id": "base",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          },
      |          {
      |            "_id": "gibdd.dtp",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          },
      |          {
      |            "_id": "gibdd.history",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          },
      |          {
      |            "_id": "gibdd.restrict",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          },
      |          {
      |            "_id": "gibdd.wanted",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          },
      |          {
      |            "_id": "pledge",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          },
      |          {
      |            "_id": "sub.base",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          }
      |        ]
      |      },
      |      "content": {
      |        "identifiers": {
      |          "_comment": "Идентификаторы",
      |          "vehicle": {
      |            "vin": "X4X3D59430PS96744",
      |            "reg_num": "А329ЕВ799",
      |            "sts": "7761034183",
      |            "pts": "39НУ031727"
      |          }
      |        },
      |        "tech_data": {
      |          "_comment": "Характеристики ТС",
      |          "brand": {
      |            "_comment": "Марка",
      |            "name": {
      |              "original": "Бмв 320D Хdrivе"
      |            }
      |          },
      |          "model": {
      |            "_comment": "Модель",
      |            "name": {
      |              "original": "Бмв"
      |            }
      |          },
      |          "type": {
      |            "_comment": "Тип (Вид) ТС",
      |            "name": "Легковые Автомобили Седан"
      |          },
      |          "body": {
      |            "_comment": "Кузов",
      |            "color": {
      |              "name": "Синий",
      |              "type": "Синий"
      |            },
      |            "number": "Х4Х3D59430РS96744"
      |          },
      |          "chassis": {
      |            "_comment": "Шасси"
      |          },
      |          "engine": {
      |            "_comment": "Двигатель",
      |            "fuel": {
      |              "type": "Дизельный"
      |            },
      |            "volume": 1995,
      |            "power": {
      |              "hp": 184,
      |              "kw": 135
      |            }
      |          },
      |          "weight": {
      |            "_comment": "Масса",
      |            "netto": 1595,
      |            "max": 2135
      |          },
      |          "transmission": {
      |            "_comment": "Трансмиссия"
      |          },
      |          "drive": {
      |            "_comment": "Привод",
      |            "type": "Заднеприводной"
      |          },
      |          "wheel": {
      |            "_comment": "Рулевое колесо",
      |            "position": "LEFT"
      |          },
      |          "year": 2014
      |        },
      |        "taxi": {
      |          "history": {
      |            "_comment": "Такси",
      |            "items": [],
      |            "count": 0
      |          },
      |          "used_in_taxi": false
      |        },
      |        "accidents": {
      |          "history": {
      |            "_comment": "История ДТП",
      |            "items": [
      |              {
      |                "_id": 2702271101,
      |                "number": "450214229",
      |                "accident": {
      |                  "date": "2016-10-04 21:45:00"
      |                },
      |                "type": "Столкновение",
      |                "state": "Повреждено",
      |                "vehicle": {
      |                  "brand": {
      |                    "name": "Bmw"
      |                  },
      |                  "model": {
      |                    "name": "Прочие Модели Bmw"
      |                  },
      |                  "year": 2014
      |                },
      |                "geo": {
      |                  "region": "Москва"
      |                }
      |              }
      |            ],
      |            "count": 1
      |          },
      |          "insurance": {
      |            "_comment": "История повреждений из страховых компаний",
      |            "items": [],
      |            "count": 0
      |          },
      |          "has_accidents": true
      |        },
      |        "pledges": {
      |          "_comment": "Обременения на ТС",
      |          "items": [],
      |          "count": 0
      |        },
      |        "restrictions": {
      |          "registration_actions": {
      |            "_comment": "Ограничения на рег. действия",
      |            "items": [],
      |            "count": 0,
      |            "has_restrictions": false
      |          }
      |        },
      |        "stealings": {
      |          "_comment": "Проверка на угон",
      |          "count": 0,
      |          "is_wanted": false,
      |          "items": []
      |        },
      |        "ownership": {
      |          "history": {
      |            "_comment": "История владения",
      |            "items": [
      |              {
      |                "_id": 2465663091,
      |                "date": {
      |                  "start": "2018-08-09 00:00:00"
      |                },
      |                "owner": {
      |                  "type": "PERSON"
      |                }
      |              },
      |              {
      |                "_id": 366069639,
      |                "date": {
      |                  "start": "2018-04-13 00:00:00",
      |                  "end": "2018-08-09 00:00:00"
      |                },
      |                "owner": {
      |                  "type": "PERSON"
      |                }
      |              },
      |              {
      |                "_id": 2110008216,
      |                "date": {
      |                  "start": "2014-12-08 00:00:00",
      |                  "end": "2018-03-12 00:00:00"
      |                },
      |                "owner": {
      |                  "type": "LEGAL"
      |                }
      |              }
      |            ],
      |            "count": 3
      |          }
      |        },
      |        "additional_info": {
      |          "vehicle": {
      |            "category": {
      |              "code": "B"
      |            }
      |          }
      |        }
      |      },
      |      "uid": "autoru_7_sources_X4X3D59430PS96744@autoru",
      |      "name": "NONAME",
      |      "comment": "",
      |      "tags": "",
      |      "created_at": "2017-12-30T23:52:18.363Z",
      |      "created_by": "system",
      |      "updated_at": "2019-01-16T06:01:37.513Z",
      |      "updated_by": "system",
      |      "active_from": "1900-01-01T00:00:00.000Z",
      |      "active_to": "3000-01-01T00:00:00.000Z"
      |    }
      |  ]
      |}
    """.stripMargin

  val rejectedModel: String =
    """
      |{
      |    "state": "ok",
      |    "size": 1,
      |    "version": "2.0",
      |    "stamp": "2021-07-15T07:44:37.363Z",
      |    "data": [
      |        {
      |            "domain_uid": "autoru",
      |            "report_type_uid": "autoru_main_report@autoru",
      |            "vehicle_id": "JY-12-048516",
      |            "query": {
      |                "type": "BODY",
      |                "body": "JY-12-048516",
      |                "schema_version": "1.0",
      |                "storages": {}
      |            },
      |            "progress_ok": 0,
      |            "progress_wait": 5,
      |            "progress_error": 0,
      |            "state": {
      |                "sources": []
      |            },
      |            "requested_at": "2020-12-03T08:21:33.772Z",
      |            "content": {},
      |            "uid": "autoru_main_report_123=@autoru",
      |            "name": "NONAME",
      |            "comment": "",
      |            "tags": "REJECTED",
      |            "created_at": "2020-12-03T08:21:33.612Z",
      |            "created_by": "system",
      |            "updated_at": "2021-07-15T07:43:41.205Z",
      |            "updated_by": "system",
      |            "active_from": "1900-01-01T00:00:00.000Z",
      |            "active_to": "3000-01-01T00:00:00.000Z"
      |        }
      |    ]
      |}
      |""".stripMargin

  val gibddFailedSourcesModel: String =
    """{
      |  "state": "ok",
      |  "size": 1,
      |  "version": "2.0",
      |  "stamp": "2021-01-15T16:46: 22.103Z",
      |  "data": [
      |    {
      |      "domain_uid": "autoru",
      |      "report_type_uid": "autoru_main_update_report@autoru",
      |      "vehicle_id": "WDD2211871A365790",
      |      "query": {
      |        "type": "VIN",
      |        "body": "WDD2211871A365790",
      |        "schema_version": "1.0",
      |        "storages": {}
      |      },
      |      "progress_ok": 4,
      |      "progress_wait": 0,
      |      "progress_error": 2,
      |      "state": {
      |        "sources": [
      |          {
      |            "_id": "gibdd.history",
      |            "state": "ERROR",
      |            "extended_state": "ERROR"
      |          },
      |          {
      |            "_id": "gibdd.wanted",
      |            "state": "ERROR",
      |            "extended_state": "ERROR"
      |          },
      |          {
      |            "_id": "gibdd.restrict",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          },
      |          {
      |            "_id": "gibdd.dtp",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          },
      |          {
      |            "_id": "pledge",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          },
      |          {
      |            "_id": "base",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          }
      |        ]
      |      },
      |      "requested_at": "2021-01-15T10:25: 13.043Z",
      |      "content": {
      |        "pledges": {
      |          "items": [],
      |          "count": 0
      |        },
      |        "accidents": {
      |          "history": {
      |            "items": [],
      |            "count": 0
      |          },
      |          "has_accidents": false
      |        },
      |        "restrictions": {
      |          "registration_actions": {
      |            "items": [],
      |            "count": 0,
      |            "has_restrictions": false
      |          }
      |        },
      |        "stealings": {
      |          "count": 0,
      |          "items": []
      |        },
      |        "ownership": {
      |          "history": {
      |            "items": [
      |              {
      |                "_id": 3413556985,
      |                "date": {
      |                  "start": "2014-12-19 00: 00: 00"
      |                },
      |                "owner": {
      |                  "type": "LEGAL"
      |                },
      |                "last_operation": {
      |                  "code": "94",
      |                  "description": "По сделкам в произв.форме с сохранением ГРЗ"
      |                }
      |              },
      |              {
      |                "_id": 1887285012,
      |                "date": {
      |                  "start": "2013-04-12 00: 00:00",
      |                  "end": "2014-12-19 00: 00: 00"
      |                },
      |                "owner": {
      |                  "type": "PERSON"
      |                },
      |                "last_operation": {
      |                  "code": "41",
      |                  "description": "Замена государственного регистрационного знака"
      |                }
      |              },
      |              {
      |                "_id": 2008748019,
      |                "date": {
      |                  "start": "2011-03-24 00: 00: 00",
      |                  "end": "2013-04-12 00: 00:00"
      |                },
      |                "owner": {
      |                  "type": "PERSON"
      |                },
      |                "last_operation": {
      |                  "code": "15",
      |                  "description": "Регистрация ТС, ввезенных из-за пределов Российской Федерации"
      |                }
      |              }
      |            ],
      |            "count": 3
      |          }
      |        },
      |        "report_meta": {}
      |      },
      |      "uid": "autoru_main_update_report_WDD2211871A365790@autoru",
      |      "name": "NONAME",
      |      "comment": "",
      |      "tags": "",
      |      "created_at": "2021-01-15T10: 25: 13.008Z",
      |      "created_by": "system",
      |      "updated_at": "2021-01-15T16: 35: 17.671Z",
      |      "updated_by": "system",
      |      "active_from": "1900-01-01T00: 00: 00.000Z",
      |      "active_to": "3000-01-01T00: 00: 00.000Z"
      |    }
      |  ]
      |}""".stripMargin

  val ready: AutocodeReportResponseRaw =
    AutocodeReportResponseRaw(
      readyModel,
      "200",
      Json.parse(readyModel).as[AutocodeReportResponse]
    )

  val notReady: AutocodeReportResponseRaw =
    AutocodeReportResponseRaw(
      notReadyModel,
      "200",
      Json.parse(notReadyModel).as[AutocodeReportResponse]
    )

  val rejected: AutocodeReportResponseRaw =
    AutocodeReportResponseRaw(
      rejectedModel,
      "200",
      Json.parse(rejectedModel).as[AutocodeReportResponse]
    )

  val gibddFailedSources: AutocodeReportResponseRaw =
    AutocodeReportResponseRaw(
      gibddFailedSourcesModel,
      "200",
      Json.parse(gibddFailedSourcesModel).as[AutocodeReportResponse]
    )
}
