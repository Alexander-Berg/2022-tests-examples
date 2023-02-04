package ru.auto.tests.publicapi.module;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.junit.rules.TestRule;
import ru.auto.tests.commons.modules.RuleChainModule;
import ru.auto.tests.passport.module.AccountWithPhoneModule;
import ru.auto.tests.passport.rules.AddParameterRule;
import ru.auto.tests.passport.rules.DeleteAccountRule;
import ru.auto.tests.publicapi.ApiClient;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.anno.Prod;
import ru.auto.tests.publicapi.provider.PublicApiProdProvider;
import ru.auto.tests.publicapi.provider.PublicApiProvider;
import ru.auto.tests.publicapi.rules.DeleteSearchesRule;
import ru.auto.tests.publicapi.utils.DeviceUidKeeper;

import static com.google.inject.Scopes.SINGLETON;

public class PublicApiSearchesModule extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder<TestRule> rulesBinder = Multibinder.newSetBinder(binder(), TestRule.class);
        //VSPASSPORT-208
        rulesBinder.addBinding().to(DeleteAccountRule.class);
        rulesBinder.addBinding().to(DeleteSearchesRule.class);
        //VERTISTEST-821
        rulesBinder.addBinding().to(AddParameterRule.class);

        install(new RuleChainModule());
        install(new AccountWithPhoneModule());
        install(new PublicApiAdaptor());
        install(new PublicApiConfigModule());
        install(new PublicApiClientModule());

        bind(DeviceUidKeeper.class).toProvider(DeviceUidKeeper::new).in(SINGLETON);
    }
}
