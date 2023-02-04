package vertis.broker.yt

import common.yt.schema.YtTypes
import org.scalatest.AppendedClues.convertToClueful
import vertis.broker.yt.YtTableSchemaMerge.mergeState
import vertis.yt.model.{YtColumn, YtSchema}
import vertis.zio.test.ZioSpecBase

/** @author kusaeva
  */
class YtTableSchemaMergeSpec extends ZioSpecBase {
  private val oldVersions = Set("v0.0.1")
  private val newVersions = Set("v0.1.0")

  "YtTableSchemaMerge.mergeState" should {
    val column1 = "foo"
    val column2 = "bar"

    "prefer definition of the latest version" in {
      val current = DayTableCachedState(
        newVersions,
        YtSchema(
          Seq(
            YtColumn(column1, YtTypes.string)
          )
        )
      )
      val recent = DayTableCachedState(
        oldVersions,
        YtSchema(
          Seq(
            YtColumn(column1, YtTypes.boolean),
            YtColumn(column2, YtTypes.string)
          )
        )
      )
      val result = mergeState(current, recent)
      result.versions should contain theSameElementsAs oldVersions ++ newVersions
      result.schema.columnNames should contain theSameElementsAs Seq(column1, column2)
      result.schema.columns.find(_.name == column1).get.`type` shouldBe YtTypes.string
    }

    "keep order of columns from current version" in {
      val current = DayTableCachedState(
        oldVersions,
        YtSchema(
          Seq(
            YtColumn("a", YtTypes.string),
            YtColumn("b", YtTypes.string),
            YtColumn("c", YtTypes.string)
          )
        )
      )
      val recent = DayTableCachedState(
        newVersions,
        YtSchema(
          Seq(
            YtColumn("b", YtTypes.string),
            YtColumn("a", YtTypes.string),
            YtColumn("e", YtTypes.string),
            YtColumn("d", YtTypes.string)
          )
        )
      )
      val result = mergeState(current, recent)
      result.versions should contain theSameElementsAs oldVersions ++ newVersions
      (result.schema.columnNames should contain).theSameElementsInOrderAs(Seq("a", "b", "c", "e", "d"))
    }

    "keep deleted columns in schema" in {
      val current = DayTableCachedState(
        oldVersions,
        YtSchema(
          Seq(
            YtColumn("a", YtTypes.string),
            YtColumn("c", YtTypes.string)
          )
        )
      )
      val recent = DayTableCachedState(
        newVersions,
        YtSchema(
          Seq(
            YtColumn("a", YtTypes.string),
            YtColumn("b", YtTypes.string)
          )
        )
      )
      val result = mergeState(current, recent)
      result.versions should contain theSameElementsAs oldVersions ++ newVersions
      (result.schema.columnNames should contain).theSameElementsInOrderAs(Seq("a", "c", "b"))
    }
  }

  "make partially missing fields optional" in {
    val current = DayTableCachedState(
      oldVersions,
      YtSchema(
        Seq(
          YtColumn("a", YtTypes.string, required = true),
          YtColumn("c", YtTypes.uint64, required = true),
          YtColumn("d", YtTypes.int32)
        )
      )
    )
    val recent = DayTableCachedState(
      newVersions,
      YtSchema(
        Seq(
          YtColumn("b", YtTypes.boolean, required = true),
          YtColumn("c", YtTypes.uint64, required = true),
          YtColumn("d", YtTypes.int32, required = true)
        )
      )
    )
    val result = mergeState(current, recent)
    result.versions should contain theSameElementsAs oldVersions ++ newVersions
    (result.schema.columnNames should contain).theSameElementsInOrderAs(Seq("a", "c", "d", "b"))
    (result.schema.getColumn("a").required shouldBe false).withClue("no matching column in an old schema")
    (result.schema.getColumn("b").required shouldBe false).withClue("no matching column in a new schema")
    (result.schema.getColumn("c").required shouldBe true).withClue("merging two required columns")
    (result.schema.getColumn("d").required shouldBe false).withClue("the column is optional in the new schema")
  }
}
