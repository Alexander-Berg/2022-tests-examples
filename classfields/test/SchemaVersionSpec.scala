package common.zio.events_broker.test

import common.zio.events_broker.schema.SchemaVersion
import zio.test._

object SchemaVersionSpec extends DefaultRunnableSpec {

  def spec = suite("SchemaVersion")(
    test("should return version") {
      assertTrue(SchemaVersion.version != "" && SchemaVersion.version.startsWith("v"))
    }
  )
}
