package ru.auto.tests.module

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import org.junit.rules.TestRule
import ru.auto.tests.recall.ApiClient
import ru.auto.tests.anno.Prod
import ru.auto.tests.commons.modules.RuleChainModule
import ru.auto.tests.provider.RecallApiProdProvider
import ru.auto.tests.provider.RecallApiProvider
import com.google.inject.Scopes.SINGLETON
import ru.auto.tests.adaptor.RecallApiAdaptor

class RecallApiModule extends AbstractModule {

  override protected def configure(): Unit = {
    val rulesBinder = Multibinder.newSetBinder(binder, classOf[TestRule])

    install(new RuleChainModule)
    install(new RecallApiAdaptor)
    install(new RecallApiConfigModule)

    bind(classOf[ApiClient]).toProvider(classOf[RecallApiProvider]).in(SINGLETON)
    bind(classOf[ApiClient]).annotatedWith(classOf[Prod]).toProvider(classOf[RecallApiProdProvider]).in(SINGLETON)
  }
}
