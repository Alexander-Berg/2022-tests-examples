package ru.auto.tests.commons.provider;

import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Set;

/**
 * @author Artem Eroshenko <erosenkoam@me.com>
 */
public class RulesChainProvider implements Provider<RuleChain> {

    @Inject
    private Set<TestRule> rules;

    protected Set<TestRule> getRules() {
        return rules;
    }

    @Override
    public RuleChain get() {
        RuleChain ruleChain = RuleChain.emptyRuleChain();
        for (TestRule rule : getRules()) {
            ruleChain = ruleChain.around(rule);
        }
        return ruleChain;
    }
}
