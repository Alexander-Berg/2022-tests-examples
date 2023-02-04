package ru.yandex.webmaster3.storage.nca;

import org.junit.Assert;
import org.junit.Test;


/**
 * @author kravchenko99
 * @date 3/23/22
 */

public class NcaRulesServiceTest {


    @Test
    public void test1() {
        Assert.assertTrue(NcaRulesService.checkRuleForDomain("abc.com", "abc.com"));
    }

    @Test
    public void test2() {
        Assert.assertTrue(NcaRulesService.checkRuleForDomain("abc.com", "*.abc.com"));
    }

    @Test
    public void test3() {
        Assert.assertTrue(NcaRulesService.checkRuleForDomain("true.abc.com", "abc.com"));
    }

    @Test
    public void test4() {
        Assert.assertFalse(NcaRulesService.checkRuleForDomain("abc.com", "true.abc.com"));
    }

    @Test
    public void test5() {
        Assert.assertFalse(NcaRulesService.checkRuleForDomain("abc.com", "qwe.true.abc.com"));
    }

    @Test
    public void test6() {
        Assert.assertFalse(NcaRulesService.checkRuleForDomain("abc.com", "qabc.com"));
    }

    @Test
    public void test7() {
        Assert.assertFalse(NcaRulesService.checkRuleForDomain("abc.com", "*.qabc.com"));
    }
    @Test
    public void test8() {
        Assert.assertFalse(NcaRulesService.checkRuleForDomain("abc.com", ".abc.com"));
    }

    @Test
    public void test9() {
        Assert.assertFalse(NcaRulesService.checkRuleForDomain("abc.com", "abc.coma"));
    }

    @Test
    public void test10() {
        Assert.assertFalse(NcaRulesService.checkRuleForDomain("abc.com", "abc.com.ru"));
    }
}
