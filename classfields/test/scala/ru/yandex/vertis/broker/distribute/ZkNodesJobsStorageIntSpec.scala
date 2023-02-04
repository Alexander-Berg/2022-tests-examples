package ru.yandex.vertis.broker.distribute

import io.circe.Codec
import io.circe.generic.semiauto._
import ru.yandex.vertis.broker.conf.ZookeeperPaths
import ru.yandex.vertis.broker.distribute.storage.{NodesJobsStorage, ZkNodesJobsStorage}
import vertis.core.model.DataCenters
import vertis.zio.test.ZioEventually
import vertis.zio.test.ZioSpecBase.TestBody
import vertis.zio.zk.CirceZkSerializer._
import vertis.zio.zk.jobs.distribution.ZkJobsSubscriber
import vertis.zio.zk.jobs.distribution.model.InstanceNode
import vertis.zio.zk.test.ZkTest

class ZkNodesJobsStorageIntSpec extends ZkTest with ZioEventually {

  private case class SomeJob(id: String)
  private case class OtherJob(id: String, partitions: Seq[Int])
  implicit private val someJobCodec: Codec[SomeJob] = deriveCodec
  implicit private val otherJobCodec: Codec[OtherJob] = deriveCodec

  private val someNode = InstanceNode("123", DataCenters.Iva)
  private val someWork = Set(SomeJob("test"))

  private val basePath = "/test"

  "ZkNodesStorage" should {
    "return empty everything" in storageTest { storage =>
      for {
        nodes <- storage.aliveNodes
        state <- storage.assignedWork
        _ <- check {
          nodes shouldBe empty
          state shouldBe empty
        }
      } yield ()
    }
    "assign work, get it back and free it" in storageTest { storage =>
      for {
        _ <- storage.assignWork(someNode, someWork)
        _ <- checkEventually {
          storage.assignedWork.flatMap { state =>
            check {
              state.size shouldBe 1
              state(someNode.id) shouldBe someWork
            }
          }
        }
        _ <- storage.freeNode(someNode.id)
        _ <- checkEventually {
          storage.assignedWork.flatMap { state =>
            check {
              state shouldBe empty
            }
          }
        }
      } yield ()
    }

    "be compatible with ZkJobsSubscriber" in zkTest { curator =>
      ZkNodesJobsStorage.make[InstanceNode, SomeJob](curator, ZookeeperPaths.distributionPath).use { storage =>
        val me = InstanceNode("321", DataCenters.Vla)
        ZkJobsSubscriber
          .make[SomeJob](
            curator,
            ZookeeperPaths.distributionPath,
            me,
            makeSeqSerializer[SomeJob]
          )
          .use { subscriber =>
            for {
              _ <- checkEventually {
                storage.aliveNodes.flatMap { nodes =>
                  check {
                    nodes.size shouldBe 1
                    nodes.head shouldBe me
                  }
                }
              }
              _ <- storage.assignWork(me, someWork)
              _ <- checkEventually {
                subscriber.getCurrent.flatMap { myWork =>
                  check {
                    myWork should contain theSameElementsAs someWork
                  }
                }
              }
            } yield ()
          }
      }
    }

    "ignore incompatible jobs" in zkTest { curator =>
      val oldStorage = new ZkNodesJobsStorage[InstanceNode, SomeJob](curator, basePath)
      val newStorage = new ZkNodesJobsStorage[InstanceNode, OtherJob](curator, basePath)
      for {
        _ <- oldStorage.assignWork(someNode, someWork)
        _ <- checkEventually {
          oldStorage.assignedWork.flatMap { viewedByOld =>
            check {
              viewedByOld should contain theSameElementsAs Map(someNode.id -> someWork)
            }
          }
        }
        _ <- checkEventually {
          newStorage.assignedWork.flatMap { viewedByNew =>
            check {
              viewedByNew should contain theSameElementsAs Map(someNode.id -> Set.empty)
            }
          }
        }
      } yield ()
    }
  }

  private def storageTest(f: NodesJobsStorage[InstanceNode, SomeJob] => TestBody): Unit = {
    zkTest { curator =>
      f(new ZkNodesJobsStorage[InstanceNode, SomeJob](curator, basePath))
    }
  }
}
