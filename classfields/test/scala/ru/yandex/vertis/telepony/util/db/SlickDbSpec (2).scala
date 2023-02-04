package ru.yandex.vertis.telepony.util.db

import ru.yandex.vertis.telepony.SpecBase

/**
  * @author neron
  */
class SlickDbSpec extends SpecBase {

  "Slick database" when {
    "slave" should {
      "not accept Write actions" in {
        "import slick.dbio.Effect.Write\n      " +
          "import slick.dbio.{DBIOAction, NoStream}\n      " +
          "import ru.yandex.vertis.telepony.util.db.SlickDb._\n      " +
          "val a: DBIOAction[Int, NoStream, Write] = ???\n      " +
          "val db: SlickDb[Slave] = ???\n      " +
          "db.run(\"name\", a)" shouldNot compile
      }

      "accept Read actions" in {
        "import slick.dbio.Effect._\n      " +
          "import slick.dbio.{DBIOAction, NoStream}\n      " +
          "import ru.yandex.vertis.telepony.util.db.SlickDb._\n      " +
          "val a: DBIOAction[Int, NoStream, Read] = ???\n      " +
          "val db: SlickDb[Slave] = ???\n      " +
          "db.run(\"name\", a)" should compile
      }
    }

    "master" should {
      "not accept Pure Read actions" in {

        "import slick.dbio.Effect._\n      " +
          "import slick.dbio.{DBIOAction, NoStream}\n      " +
          "import ru.yandex.vertis.telepony.util.db.SlickDb._\n      " +
          "val a: DBIOAction[Int, NoStream, Read] = ???\n      " +
          "val db: SlickDb[Master] = ???\n      " +
          "db.run(\"name\", a)" shouldNot compile
      }

      "accept Read with Write actions" in {
        "import slick.dbio.Effect._\n      " +
          "import slick.dbio.{DBIOAction, NoStream}\n      " +
          "import ru.yandex.vertis.telepony.util.db.SlickDb._\n      " +
          "val a: DBIOAction[Int, NoStream, Read with Write] = ???\n      " +
          "val db: SlickDb[Master] = ???\n      " +
          "db.run(\"name\", a)" should compile
      }

      "accept Write actions" in {
        "import slick.dbio.Effect._\n      " +
          "import slick.dbio.{DBIOAction, NoStream}\n      " +
          "import ru.yandex.vertis.telepony.util.db.SlickDb._\n      " +
          "val a: DBIOAction[Int, NoStream, Write] = ???\n      " +
          "val db: SlickDb[Master] = ???\n      " +
          "db.run(\"name\", a)" should compile
      }
    }

  }

}
