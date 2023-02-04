import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.archive.scheduler.updater.diff.{BasicOfferDiff, EnrichedOfferDiff, EnrichedOfferDiffDoNothing}
import ru.yandex.realty.archive.scheduler.updater.enrich.Enricher
import ru.yandex.realty.logging.Logging

/**
  * Created by Viacheslav Kukushkin <vykukushkin@yandex-team.ru> on 2019-01-23
  */
@RunWith(classOf[JUnitRunner])
class EnricherCloseAreasSpec extends SpecBase with Logging {

  val enricher: Enricher = new Enricher() {
    override def enrich(diff: BasicOfferDiff): EnrichedOfferDiff = {
      EnrichedOfferDiffDoNothing(diff.offerId, "mockup test")
    }
  }

  //    45.4 , 45.2 - близкие
  //    45.4, 45 - близкие
  //    45.4, 46 - близкие
  //    45.4, 47 - нет
  //    45.4, 44.9 - нет
  //    45.4, 44 - нет
  //    573, 580 - близкие
  //    573, 581 - нет
  //    19.0, 18.0 - близкие
  //    123, 126 - близкие
  //    101, 99 - нет (2й значащий знак берем по меньшему значению)
  //    100, 99 - близкие
  //    101, 100 - близкие (да, не транзитивно, но что поделать)
  //    43.8, 45.0 - нет

  def closeAreas(v1: Double, v2: Double): Boolean = {
    val ans1 = enricher.closeAreas(Some(v1), Some(v2))
    val ans2 = enricher.closeAreas(Some(v2), Some(v1))
    ans1 shouldEqual ans2
    ans1
  }

  "enricher" should {
    "recognize close areas" in {
      closeAreas(45.4, 44.0) shouldEqual false
      closeAreas(45.4, 44.2) shouldEqual false
      closeAreas(45.4, 45.0) shouldEqual true
      closeAreas(45.4, 45.2) shouldEqual true
      closeAreas(45.4, 46.0) shouldEqual true
      closeAreas(45.4, 46.1) shouldEqual false
      closeAreas(45.4, 47.0) shouldEqual false

      closeAreas(44.0, 45.0) shouldEqual true
      closeAreas(44.0, 46.0) shouldEqual false

      closeAreas(68.9, 70.0) shouldEqual false
      closeAreas(69.0, 70.0) shouldEqual true
      closeAreas(69.0, 70.3) shouldEqual false
      closeAreas(69.7, 70.0) shouldEqual true
      closeAreas(69.7, 70.3) shouldEqual false
      closeAreas(69.7, 71.0) shouldEqual false

      closeAreas(575.0, 580.0) shouldEqual true
      closeAreas(575.0, 581.0) shouldEqual false

      closeAreas(19.0, 18.0) shouldEqual true

      closeAreas(123.0, 126.0) shouldEqual true

      closeAreas(101.0, 99.0) shouldEqual false
      closeAreas(100.0, 99.0) shouldEqual true
      closeAreas(101.0, 100.0) shouldEqual true

      closeAreas(9.8, 10.0) shouldEqual true
      closeAreas(98.0, 100.0) shouldEqual false
      closeAreas(92.0, 100.0) shouldEqual false
      closeAreas(9.2, 10.0) shouldEqual true
      closeAreas(9.2, 10.2) shouldEqual false
      closeAreas(9.0, 10.0) shouldEqual true
      closeAreas(17.2, 18.5) shouldEqual false
    }
  }

}
