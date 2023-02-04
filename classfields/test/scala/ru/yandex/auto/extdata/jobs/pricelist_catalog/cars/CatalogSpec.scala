package ru.yandex.auto.extdata.jobs.pricelist_catalog.cars

import org.scalatest.FlatSpec

import scala.xml.Elem

class CatalogSpec extends FlatSpec {

  val requiredResult: Elem = {
    <catalog>
      <mark name="Acura" id="5">
        <code>ACURA</code>
        <folder name="NSX, II" id="21650055">
          <model id="23">NSX</model>
          <generation id="21650055">20650055</generation>
          <modification name="3.5hyb AT (573 л.с.) 4WD" id="21650130">
            <mark_id>Acura</mark_id>
            <folder_id>NSX, II</folder_id>
            <modification_id>3.5hyb AT (573 л.с.) 4WD</modification_id>
            <configuration_id>20650082</configuration_id>
            <tech_param_id>20650130</tech_param_id>
            <body_type>Купе</body_type>
            <years>2015 - по н.в.</years>
            <complectations></complectations>
          </modification>
        </folder>
        <folder name="MDX, III Рестайлинг 1" id="21703401">
          <model id="20">MDX</model>
          <generation id="21703401">20703401</generation>
          <modification name="3.5 AT (290 л.с.) 4WD" id="21703440">
            <mark_id>Acura</mark_id>
            <folder_id>MDX, III Рестайлинг 1</folder_id>
            <modification_id>3.5 AT (290 л.с.) 4WD</modification_id>
            <configuration_id>20703439</configuration_id>
            <tech_param_id>20703440</tech_param_id>
            <body_type>Внедорожник 5 дв.</body_type>
            <years>2015 - 2016</years>
            <complectations>
              <complectation id="20740208">Techno</complectation>
              <complectation id="20740221">Advance</complectation>
            </complectations>
          </modification>
        </folder>
      </mark>
    </catalog>
  }

  val catalog = Catalog(
    Seq(
      Mark(
        "Acura",
        "5",
        "ACURA",
        Seq(
          Folder(
            "NSX, II",
            "21650055",
            Model("23", "NSX"),
            Generation("21650055", 20650055),
            Seq(
              Modification(
                "3.5hyb AT (573 л.с.) 4WD",
                "21650130",
                "Acura",
                "NSX, II",
                "3.5hyb AT (573 л.с.) 4WD",
                20650082,
                20650130,
                "Купе",
                2015,
                None,
                Nil
              )
            )
          ),
          Folder(
            "MDX, III Рестайлинг 1",
            "21703401",
            Model("20", "MDX"),
            Generation("21703401", 20703401),
            Seq(
              Modification(
                "3.5 AT (290 л.с.) 4WD",
                "21703440",
                "Acura",
                "MDX, III Рестайлинг 1",
                "3.5 AT (290 л.с.) 4WD",
                20703439,
                20703440,
                "Внедорожник 5 дв.",
                2015,
                Some(2016),
                Seq(
                  Complectation("20740208", "Techno"),
                  Complectation("20740221", "Advance")
                )
              )
            )
          )
        )
      )
    )
  )

  val xmlContent: Elem = catalog.toXml

  val prettyPrinter = new scala.xml.PrettyPrinter(100, 4)
  val prettyXml: String = prettyPrinter.format(xmlContent)

  "toXml" should "build correct XML" in {
    assert(prettyPrinter.format(xmlContent) == prettyPrinter.format(requiredResult))
  }
}
