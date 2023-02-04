package auto.common.manager.catalog.testkit

import auto.common.clients.catalog.Catalog
import zio.test.mock._

@mockable[Catalog.Service]
object CatalogClientMock
