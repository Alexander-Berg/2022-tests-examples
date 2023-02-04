package ru.yandex.auto.garage.managers.models

import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.garage.models.TaskView
import ru.yandex.auto.garage.models.TaskView.Photo

class TaskViewTest extends AnyFunSuite {

  test("json format") {
    val taskView = TaskView(Seq(Photo("url", 1000)), "user:123", 9876)

    assert(
      taskView.json === "{\"photos\":[{\"url\":\"url\",\"timestamp\":1000}],\"userId\":\"user:123\",\"cardId\":9876}"
    )

  }
}
