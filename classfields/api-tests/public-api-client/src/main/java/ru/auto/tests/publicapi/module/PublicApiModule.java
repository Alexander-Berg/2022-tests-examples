package ru.auto.tests.publicapi.module;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.junit.rules.TestRule;
import ru.auto.tests.commons.modules.RuleChainModule;
import ru.auto.tests.passport.module.AccountWithPhoneModule;
import ru.auto.tests.passport.rules.AddParameterRule;
import ru.auto.tests.passport.rules.DeleteAccountRule;
import ru.auto.tests.passport.rules.UnlinkUserFromDealerRule;
import ru.auto.tests.publicapi.adaptor.PublicApiAdaptor;
import ru.auto.tests.publicapi.rules.CancelSharkCreditApplicationsRule;
import ru.auto.tests.publicapi.rules.CreditCardRule;
import ru.auto.tests.publicapi.rules.DeleteCarOffersRule;
import ru.auto.tests.salesman.user.module.SalesmanUserApiModule;

public class PublicApiModule extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder<TestRule> rulesBinder = Multibinder.newSetBinder(binder(), TestRule.class);
        rulesBinder.addBinding().to(CreditCardRule.class);
        // @see VSPASSPORT-208
        rulesBinder.addBinding().to(DeleteAccountRule.class);
        // @see VERTISTEST-821
        rulesBinder.addBinding().to(AddParameterRule.class);
        rulesBinder.addBinding().to(UnlinkUserFromDealerRule.class);
        rulesBinder.addBinding().to(DeleteCarOffersRule.class);
        rulesBinder.addBinding().to(CancelSharkCreditApplicationsRule.class);

        install(new RuleChainModule());
        install(new AccountWithPhoneModule());
        install(new PublicApiAdaptor());
        install(new PublicApiConfigModule());
        install(new SalesmanUserApiModule());
        install(new PublicApiClientModule());
    }
}
