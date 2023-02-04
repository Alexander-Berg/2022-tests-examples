package ru.auto.tests.cabinet.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import org.aeonbits.owner.ConfigFactory;
import org.junit.rules.TestRule;
import ru.auto.tests.cabinet.adaptor.CabinetApiAdaptor;
import ru.auto.tests.cabinet.SqlConfig;
import ru.auto.tests.cabinet.anno.Prod;
import ru.auto.tests.commons.modules.RuleChainModule;
import ru.auto.tests.cabinet.ApiClient;
import ru.auto.tests.cabinet.provider.CabinetApiProdProvider;
import ru.auto.tests.cabinet.provider.CabinetApiProvider;
import ru.auto.tests.passport.module.AccountWithEmailModule;
import ru.auto.tests.passport.rules.AddParameterRule;
import ru.auto.tests.passport.rules.DeleteAccountRule;

import static com.google.inject.Scopes.SINGLETON;

public class CabinetApiModule extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder<TestRule> rulesBinder = Multibinder.newSetBinder(binder(), TestRule.class);
        rulesBinder.addBinding().to(AddParameterRule.class);
        rulesBinder.addBinding().to(DeleteAccountRule.class);

        install(new RuleChainModule());
        install(new CabinetApiAdaptor());
        install(new AccountWithEmailModule());
        install(new CabinetApiConfigModule());

        bind(ApiClient.class).toProvider(CabinetApiProvider.class).in(SINGLETON);
        bind(ApiClient.class).annotatedWith(Prod.class).toProvider(CabinetApiProdProvider.class).in(SINGLETON);
    }

    @Provides
    private SqlConfig provideSqlConfig() {
        return ConfigFactory.create(SqlConfig.class, System.getProperties(), System.getenv());
    }
}
