package ru.yandex.realty.componenttest.extdata.core

import ru.yandex.extdata.core.replicate.ReplicateService
import ru.yandex.realty.clients.resource.{ResourceServiceClient, ResourceServiceClientImpl}

trait TestingReplicateServiceProvider {

  def testingReplicateService: ReplicateService

}

trait ComponentTestReplicateServiceProvider
  extends TestingReplicateServiceProvider
  with ComponentTestExtdataServiceProvider {

  private lazy val testingResourceServiceClient: ResourceServiceClient =
    new ResourceServiceClientImpl(
      url = typesafeConfig.getString("realty.extdata.remote.testing-url"),
      maxConcurrent = 10
    )

  override lazy val testingReplicateService: ReplicateService =
    new ReplicateService(
      testingResourceServiceClient,
      extDataService,
      numRetires = None
    )

}
