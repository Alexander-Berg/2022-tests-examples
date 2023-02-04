package ru.auto.tests.module

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import org.junit.rules.TestRule
import ru.auto.tests.ApiClient
import ru.auto.tests.anno.Prod
import ru.auto.tests.commons.modules.RuleChainModule
import ru.auto.tests.provider.SalesmanApiProdProvider
import ru.auto.tests.provider.SalesmanApiProvider
import com.google.inject.Scopes.SINGLETON
import ru.auto.tests.adaptor.SalesmanApiAdaptor

class SalesmanApiModule extends AbstractModule {

  override protected def configure(): Unit = {
    val rulesBinder = Multibinder.newSetBinder(binder, classOf[TestRule])

    install(new RuleChainModule)
    install(new SalesmanApiAdaptor)
    install(new SalesmanApiConfigModule)

    bind(classOf[ApiClient])
      .toProvider(classOf[SalesmanApiProvider])
      .in(SINGLETON)
    bind(classOf[ApiClient])
      .annotatedWith(classOf[Prod])
      .toProvider(classOf[SalesmanApiProdProvider])
      .in(SINGLETON)
  }
}
