package ru.auto.salesman.api.akkahttp

import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.InnerClassNameJava8BugUtil

class InnerClassNameJava8BugUtilSpec extends BaseSpec {

  object A { object B { class C } }

  //todo Удалить после обновления версии Java (вместе с InnerClassNameJava8BugUtil)
  //https://st.yandex-team.ru/VSMONEY-2668
  ".getSimpleName" should {
    "fail in java8 for inner class" in {
      val res =
        try classOf[A.B.C].getSimpleName
        catch {
          case e: InternalError => e.getMessage()
        }
      res shouldBe "Malformed class name"
    }

    "not fail using InnerClassNameJava8BugUtil.getClassName" in {
      println(new A.B.C().getClass.getName)
      InnerClassNameJava8BugUtil.getClassName(
        new A.B.C()
      ) shouldBe "C"
    }
  }

}
