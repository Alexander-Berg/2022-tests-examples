package ru.yandex.vertis.vsquality.hobo.view.json

import ru.yandex.vertis.vsquality.hobo.model.generators.CoreGenerators._
import ru.yandex.vertis.vsquality.hobo.model.generators.DaoGenerators._
import ru.yandex.vertis.vsquality.hobo.util.SpecBase
import ru.yandex.vertis.vsquality.hobo.view.{View, ViewCompanion}
import org.scalacheck.Prop.forAll
import org.scalatestplus.scalacheck.Checkers.check

import scala.annotation.nowarn

/**
  * Specs on json view classes
  *
  * @author semkagtn
  */

@nowarn("cat=deprecation")
class ViewSpec extends SpecBase {

  "JSON view" should {

    "convert TaskSource" in {
      check(forAll(TaskSourceGen)(checking(_, TaskSourceView)))
    }

    "convert Task" in {
      check(forAll(TaskGen.map(_.copy(ownerInfo = None)))(checking(_, TaskView)))
    }

    "convert UserSource" in {
      check(forAll(UserSourceGen)(checking(_, UserSourceView)))
    }

    "convert User" in {
      check(forAll(UserGen)(checking(_, UserView)))
    }

    "convert TaskUpdateRequest" in {
      check(forAll(TaskUpdateRequestGen)(checking(_, TaskUpdateRequestView)))
    }

    "convert SalaryStatisticsReport" in {
      check(forAll(SalaryStatisticsReportGen)(checking(_, SalaryStatisticsReportView)))
    }

    "convert SummarySalaryStatistics" in {
      check(forAll(SummarySalaryStatisticsGen)(checking(_, SummarySalaryStatisticsView)))
    }

    "convert Count" in {
      check(forAll(CountGen)(checking(_, CountView)))
    }
  }

  private def checking[M, V <: View[M]](modelObject: M, viewCompanion: ViewCompanion[V, M]): Boolean = {
    modelObject == viewCompanion.asView(modelObject).asModel
  }
}
