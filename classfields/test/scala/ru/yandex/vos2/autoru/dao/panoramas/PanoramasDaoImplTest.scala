package ru.yandex.vos2.autoru.dao.panoramas

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.model.UserRef

/**
  * Created by sievmi on 20.06.18
  */
@RunWith(classOf[JUnitRunner])
class PanoramasDaoImplTest extends AnyFunSuite with InitTestDbs with BeforeAndAfterAll {

  override def beforeAll(): Unit = {
    initDbs()
  }

  val panoramasDao: PanoramasDao = components.panoramasDao

  test("CRUD") {
    val userRef = UserRef(1L)
    //empty table
    assert(panoramasDao.getTiles(userRef, "123-abc", 512, 512).isEmpty)

    //insert
    panoramasDao.insertTiles(userRef, "123-abc", 512, 512, Seq("0-1" -> "A", "0-2" -> "B"))
    val tiles = panoramasDao.getTiles(userRef, "123-abc", 512, 512)
    assert(tiles.size == 2)
    assert(tiles("0-1") === "A")
    assert(tiles("0-2") === "B")

    //delete
    panoramasDao.deleteTilesWithNotEqualSizes(userRef, "123-abc", 1024, 1024)
    assert(panoramasDao.getTiles(userRef, "123-abc", 512, 512).isEmpty)
  }
}
