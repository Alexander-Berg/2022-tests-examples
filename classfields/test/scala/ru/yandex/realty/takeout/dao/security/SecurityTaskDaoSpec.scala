package ru.yandex.realty.takeout.dao.security

import org.junit.runner.RunWith
import org.scalactic.Equality
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.takeout.SecurityTaskStatus.SecurityCategory
import ru.yandex.realty.takeout.model.security.SecurityTask
import ru.yandex.realty.takeout.util.SecurityTaskModelGenerator
import ru.yandex.realty.tracing.Traced

@RunWith(classOf[JUnitRunner])
class SecurityTaskDaoSpec
  extends AsyncSpecBase
  with SecurityTaskModelGenerator
  with BeforeAndAfterAll
  with SecurityTaskDaoBase {

  "SecurityTaskDao " should {
    "handle inserts" in {
      val securityTask = securityTaskGen.next
      securityTaskDao.insert(securityTask)(Traced.empty).futureValue

      val readFromDbTasks: Seq[SecurityTask] = securityTaskDao.select(securityTask.uid)(Traced.empty).futureValue
      readFromDbTasks.head should equal(securityTask)
    }
    "handle select" in {
      val uid = 1231234L
      val chatTask = securityTaskGen(SecurityCategory.CHAT.getNumber, uid).next
      val offerTask = securityTaskGen(SecurityCategory.OFFER.getNumber, uid).next
      val eventLogTask = securityTaskGen(SecurityCategory.EVENT_LOG.getNumber, uid).next
      val securityTasks = Set(chatTask, offerTask, eventLogTask)

      securityTasks.foreach(securityTaskDao.insert(_)(Traced.empty).futureValue)

      val selectedTasks: Set[SecurityTask] = securityTaskDao.select(uid)(Traced.empty).futureValue.toSet
      selectedTasks should contain allElementsOf securityTasks
    }
    "handle select only for last task for given uid" in {
      val uid = 1231234L
      val chatTaskOld = securityTaskGen(SecurityCategory.CHAT.getNumber, uid).next
      val offerTaskOld = securityTaskGen(SecurityCategory.OFFER.getNumber, uid).next
      val eventLogTaskOld = securityTaskGen(SecurityCategory.EVENT_LOG.getNumber, uid).next
      val securityTasksOld = Set(chatTaskOld, offerTaskOld, eventLogTaskOld)

      securityTasksOld.foreach(securityTaskDao.insert(_)(Traced.empty).futureValue)

      val chatTaskNew = securityTaskGen(SecurityCategory.CHAT.getNumber, uid).next
      val offerTaskNew = securityTaskGen(SecurityCategory.OFFER.getNumber, uid).next
      val eventLogTaskNew = securityTaskGen(SecurityCategory.EVENT_LOG.getNumber, uid).next
      val securityTasksNew = Set(chatTaskNew, offerTaskNew, eventLogTaskNew)

      securityTasksNew.foreach(securityTaskDao.insert(_)(Traced.empty).futureValue)

      val selectedTasks: Set[SecurityTask] = securityTaskDao.select(uid)(Traced.empty).futureValue.toSet
      selectedTasks.size shouldBe securityTasksNew.size
      selectedTasks should contain allElementsOf securityTasksNew
    }
  }

  override def beforeAll() {
    database.run(script"sql/takeout.final.sql").futureValue
  }

  override def afterAll() {
    database.run(script"sql/deleteTables.sql").futureValue
  }

  implicit val securityTaskEqualityWithoutDbId: Equality[SecurityTask] =
    (securityTask: SecurityTask, a: Any) =>
      a match {
        case otherSecurityTask: SecurityTask =>
          securityTask.uid == otherSecurityTask.uid &&
            securityTask.categoryId == otherSecurityTask.categoryId &&
            securityTask.shardKey == otherSecurityTask.shardKey &&
            securityTask.createTime == otherSecurityTask.createTime &&
            securityTask.visitTime == otherSecurityTask.visitTime &&
            securityTask.endTime == otherSecurityTask.endTime
        case _ => false
      }
}
