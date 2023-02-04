package ru.yandex.vertis.chat.service.support.subjects

import ru.yandex.vertis.chat.components.dao.techsupport.subjects.JdbcTechSupportSubjectService
import ru.yandex.vertis.chat.components.dao.techsupport.subjects.TechSupportSubjectService
import ru.yandex.vertis.chat.service.impl.jdbc.JdbcSpec

/**
  * TODO
  *
  * @author aborunov
  */
class JdbcTechSupportSubjectServiceSpec extends TechSupportSubjectServiceSpec with JdbcSpec {

  override val techSupportSubjectService: TechSupportSubjectService = {
    new JdbcTechSupportSubjectService(database)
  }
}
