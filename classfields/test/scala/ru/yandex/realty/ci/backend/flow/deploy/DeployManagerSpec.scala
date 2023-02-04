package ru.yandex.realty.ci.backend.flow.deploy

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.ci.api.RealtyCiApi.{DeployEnvironment, User}

@RunWith(classOf[JUnitRunner])
class DeployManagerSpec extends SpecBase {

  "DeployManager" when {
    "mkShortAggregatedTitle" should {
      "make message with one issue" in {
        val res = DeployManager.mkShortAggregatedTitle(
          user = User.newBuilder().setLogin("userName").setFirstName("Юзер").setLastName("Катящий").build(),
          environment = DeployEnvironment.PRODUCTION,
          message = Some("качу всякое"),
          issues = Seq(Issue("TASK-1", "https://st.yandex-team.ru/TASK-1", Some("Первая задача")))
        )
        res shouldBe
          "Юзер Катящий(<a href='https://staff.yandex-team.ru/userName'>userName</a>) катит в прод задачу" +
            " <a href='https://st.yandex-team.ru/TASK-1'>TASK-1</a> - <i>Первая задача</i>\n" +
            "с сообщением: <i>качу всякое</i>"
      }
      "make message with several issue" in {
        val res = DeployManager.mkShortAggregatedTitle(
          user = User.newBuilder().setLogin("userName").setFirstName("Юзер").setLastName("Катящий").build(),
          environment = DeployEnvironment.PRODUCTION,
          message = None,
          issues = Seq(
            Issue("TASK-1", "https://st.yandex-team.ru/TASK-1", Some("Первая задача")),
            Issue("TASK-2", "https://st.yandex-team.ru/TASK-2", Some("Вторая задача")),
            Issue("TASK-3", "https://st.yandex-team.ru/TASK-3", None)
          )
        )
        res shouldBe
          "Юзер Катящий(<a href='https://staff.yandex-team.ru/userName'>userName</a>) катит в прод задачи:\n" +
            "<a href='https://st.yandex-team.ru/TASK-1'>TASK-1</a> - <i>Первая задача</i>\n" +
            "<a href='https://st.yandex-team.ru/TASK-2'>TASK-2</a> - <i>Вторая задача</i>\n" +
            "<a href='https://st.yandex-team.ru/TASK-3'>TASK-3</a> - <i>&lt;название задачи недоступно&gt;</i>"
      }
    }
  }
}
