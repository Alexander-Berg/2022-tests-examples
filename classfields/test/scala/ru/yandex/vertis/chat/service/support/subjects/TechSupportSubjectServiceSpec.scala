package ru.yandex.vertis.chat.service.support.subjects

import ru.yandex.vertis.chat.SpecBase
import ru.yandex.vertis.chat.common.techsupport.TechSupportUtils
import ru.yandex.vertis.chat.components.dao.techsupport.subjects.TechSupportSubjectService
import ru.yandex.vertis.chat.util.test.RequestContextAware
import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.chat.model.ModelGenerators._

/**
  * TODO
  *
  * @author aborunov
  */
trait TechSupportSubjectServiceSpec extends SpecBase with RequestContextAware with ProducerProvider {

  def techSupportSubjectService: TechSupportSubjectService

  "Tech Support Subjects Service" should {
    "add subjects" in {
      val user = userId.next
      val room = TechSupportUtils.roomId(user)
      val operator = "aborunov@yandex-team.ru"
      val subject1 = "Деньги:другой_вопрос"
      val subject2 = "Деньги:Возврат"
      val subjects = Seq(subject1, subject2)

      withUserContext(user) { implicit rc =>
        techSupportSubjectService.addSubjects(room, user, operator, subjects).futureValue

        val savedSubjects = techSupportSubjectService.getForUser(user).futureValue
        savedSubjects.length shouldBe 2

        savedSubjects.head.roomId shouldBe room
        savedSubjects(1).roomId shouldBe room

        savedSubjects.head.userId shouldBe user
        savedSubjects(1).userId shouldBe user

        savedSubjects.head.operatorId shouldBe operator
        savedSubjects(1).operatorId shouldBe operator

        savedSubjects.head.subject shouldBe subject1
        savedSubjects(1).subject shouldBe subject2
      }
    }

    "return no subjects for unknown user" in {
      val user = userId.next

      withUserContext(user) { implicit rc =>
        val savedSubjects = techSupportSubjectService.getForUser(user).futureValue
        savedSubjects.isEmpty shouldBe true
      }
    }
  }
}
