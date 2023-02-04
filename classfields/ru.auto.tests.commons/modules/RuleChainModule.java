package ru.auto.tests.commons.modules;

import com.google.inject.AbstractModule;
import org.junit.rules.RuleChain;
import ru.auto.tests.commons.provider.RulesChainProvider;

/**
 * @author Artem Eroshenko <erosenkoam@me.com>
 */
public class RuleChainModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(RuleChain.class).toProvider(RulesChainProvider.class);
    }
}
