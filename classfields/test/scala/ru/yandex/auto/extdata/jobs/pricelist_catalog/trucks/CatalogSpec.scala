package ru.yandex.auto.extdata.jobs.pricelist_catalog.trucks

import org.scalatest.FlatSpec

import scala.xml.Elem

class CatalogSpec extends FlatSpec {

  val requiredResult: Elem = {
    <catalog>
      <mark name="Acerbi" id="2">
        <group name="Полуприцеп - цистерна" id="16646" category="Прицепы">
        </group>
        <group name="Полуприцеп - битумовоз" id="16650" category="Прицепы">
        </group>
      </mark>
      <mark name="Ackermann" id="3">
        <group name="Tandemanhang" id="6248" category="Прицепы">
        </group>
        <group name="Freuhauf" id="6253" category="Прицепы">
        </group>
      </mark>
    </catalog>
  }

  val catalog = Catalog(
    marks = Seq(
      Mark(
        name = "Acerbi",
        id = Some(2),
        groups = Seq(
          Group(name = "Полуприцеп - цистерна", id = Some(16646), category = "Прицепы"),
          Group(name = "Полуприцеп - битумовоз", id = Some(16650), category = "Прицепы")
        )
      ),
      Mark(
        name = "Ackermann",
        id = Some(3),
        groups = Seq(
          Group(name = "Tandemanhang", id = Some(6248), category = "Прицепы"),
          Group(name = "Freuhauf", id = Some(6253), category = "Прицепы")
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
