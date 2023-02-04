package ru.yandex.realty.favorites

import io.grpc.ManagedChannelBuilder
import ru.yandex.realty.componenttest.env.ComponentTestEnvironmentProvider
import ru.yandex.realty.componenttest.env.initializers.ComponentTestYdbInitializer
import ru.yandex.realty.componenttest.spec.AsyncComponentTestSpec
import ru.yandex.realty.favorites.backend.link.ShortLinkGenerator

abstract class AbstractFavoritesComponentTestSpec
  extends ComponentTestEnvironmentProvider[FavoritesEnvironment.type]
  with AsyncComponentTestSpec[FavoritesEnvironment.type]
  with ComponentTestYdbInitializer {

  override val env: FavoritesEnvironment.type = FavoritesEnvironment

  protected val component: FavoritesComponent = env.component

  protected val shortLinkGenerator: ShortLinkGenerator = component.getShortLinkGenerator()

  protected lazy val favoritesService: FavoritesServiceGrpc.FavoritesServiceBlockingStub = {
    val channel: io.grpc.Channel =
      ManagedChannelBuilder
        .forAddress(
          "localhost",
          component.getApiPort.get
        )
        .usePlaintext()
        .build()
    FavoritesServiceGrpc.newBlockingStub(channel)
  }

}
