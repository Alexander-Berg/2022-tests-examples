package ru.auto.tests.module

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import org.junit.rules.TestRule
import ru.auto.tests.garage.ApiClient
import ru.auto.tests.anno.Prod
import ru.auto.tests.commons.modules.RuleChainModule
import ru.auto.tests.provider.GarageApiProdProvider
import ru.auto.tests.provider.GarageApiProvider
import com.google.inject.Scopes.SINGLETON
import ru.auto.tests.adaptor.GarageApiAdaptor

class GarageApiModule extends AbstractModule {

  override protected def configure(): Unit = {
    val rulesBinder = Multibinder.newSetBinder(binder, classOf[TestRule])

    install(new RuleChainModule)
    install(new GarageApiAdaptor)
    install(new GarageApiConfigModule)

    bind(classOf[ApiClient]).toProvider(classOf[GarageApiProvider]).in(SINGLETON)
    bind(classOf[ApiClient]).annotatedWith(classOf[Prod]).toProvider(classOf[GarageApiProdProvider]).in(SINGLETON)
  }
}
