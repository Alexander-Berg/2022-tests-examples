package billing.finstat.storage.test

import billing.finstat.model.domain.Workspace
import billing.finstat.storage.schema_service.{SchemaLoader, SchemaWorkspaceMap}
import billing.finstat.storage.test.FinstatClickhouseJdbcSpec.testM
import zio.ZIO
import zio.test.Assertion.isNonEmpty
import zio.test._

object SchemaLoaderTests {

  val loadSchemaForAutoDealersWorkspace = testM("load schema for AutoDealersWorkspace") {
    for {
      _ <- SchemaWorkspaceMap.getSchema(Workspace.AutoDealers)
      schemaLoader <- ZIO.service[SchemaLoader.Service]
      _ <- schemaLoader.runSync
      schemaAfterUpdate <- SchemaWorkspaceMap.getSchema(Workspace.AutoDealers)
    } yield assert(schemaAfterUpdate.customFields)(isNonEmpty)
  }
}
