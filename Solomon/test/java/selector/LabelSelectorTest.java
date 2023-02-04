package ru.yandex.solomon.labels.selector;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Stepan Koltsov
 */
public class LabelSelectorTest {

    private void testParse(String name, String[] value, LabelSelector.MatchOp matchOp, String string) {
        Assert.assertEquals(new LabelSelectorParsed(name, value, matchOp), LabelSelector.parse(string).toParsed());
    }

    @Test
    public void parse() {
        testParse("aa", new String[]{ "bb" }, LabelSelector.MatchOp.GLOB_POSITIVE, "aa=bb");
        testParse("aa", new String[]{ "bb" }, LabelSelector.MatchOp.GLOB_NEGATIVE, "aa=!bb");
        testParse("aa", new String[] {"x=b"}, LabelSelector.MatchOp.GLOB_POSITIVE, "aa=x=b");
        testParse("aa", new String[] {"x!=b"}, LabelSelector.MatchOp.GLOB_POSITIVE, "aa=x!=b");
        testParse("aa", null, LabelSelector.MatchOp.PRESENT, "aa=*");
        testParse("aa", null, LabelSelector.MatchOp.ABSENT, "aa=-");
    }

    @Test
    public void parseLegacy() {
        testParse("aa", null, LabelSelector.MatchOp.ABSENT, "!aa=*");
        testParse("aa", new String[] { "bb" }, LabelSelector.MatchOp.GLOB_NEGATIVE, "aa!=bb");
    }
}
