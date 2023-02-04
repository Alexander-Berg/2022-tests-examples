package ru.yandex.webmaster3.storage;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.webmaster3.storage.searchurl.ExcludedUrlFilter;
import ru.yandex.webmaster3.storage.util.clickhouse2.condition.BoolOpCondition;
import ru.yandex.webmaster3.storage.util.clickhouse2.condition.Condition;
import ru.yandex.webmaster3.storage.util.clickhouse2.condition.ConditionFieldExtractor;

import java.util.function.Predicate;

/**
 * Created by ifilippov5 on 31.03.17.
 */
public class TextFilterUtilTest {

    @Test
    public void testGetTextCondition() {
        AbstractFilter filter = new ExcludedUrlFilter(null, AbstractFilter.Operation.TEXT_MATCH, "", BoolOpCondition.Operator.AND);
        Assert.assertEquals("(" + Condition.trueCondition().toQuery() + ")", TextFilterUtil.getTextCondition(filter, "host_id").toQuery());

        filter = new ExcludedUrlFilter(null, AbstractFilter.Operation.TEXT_MATCH, "  @webmaster \n *\n!~\\d", BoolOpCondition.Operator.AND);
        Assert.assertEquals("(positionCaseInsensitive(host_id,'webmaster') > 0 AND host_id LIKE '%' AND NOT (match(host_id,'\\\\d')))",
                TextFilterUtil.getTextCondition(filter, "host_id").toQuery());

        filter = new ExcludedUrlFilter(null, AbstractFilter.Operation.TEXT_MATCH, "  \n  \n         ", BoolOpCondition.Operator.AND);
        Assert.assertEquals("(1=1 AND 1=1 AND 1=1)",
                TextFilterUtil.getTextCondition(filter, "host_id").toQuery());

        filter = new ExcludedUrlFilter(null, AbstractFilter.Operation.TEXT_MATCH, "     aa", BoolOpCondition.Operator.AND);
        Assert.assertEquals("(host_id LIKE 'aa')",
                TextFilterUtil.getTextCondition(filter, "host_id").toQuery());

        filter = new ExcludedUrlFilter(null, AbstractFilter.Operation.TEXT_MATCH, " !     aa", BoolOpCondition.Operator.AND);
        Assert.assertEquals("(NOT (host_id LIKE '     aa'))",
                TextFilterUtil.getTextCondition(filter, "host_id").toQuery());

        filter = new ExcludedUrlFilter(null, AbstractFilter.Operation.TEXT_MATCH, "!!\n!\n!\n!\n!\n!", BoolOpCondition.Operator.OR);
        Assert.assertEquals("(NOT (host_id LIKE '!') OR 1=1 OR 1=1 OR 1=1 OR 1=1 OR 1=1)",
                TextFilterUtil.getTextCondition(filter, "host_id").toQuery());

        filter = new ExcludedUrlFilter(null, AbstractFilter.Operation.TEXT_MATCH, "\n\n\n\n\n\n\n\n\n\n\n\n", BoolOpCondition.Operator.AND);
        Assert.assertEquals(Condition.trueCondition().toQuery(),
                TextFilterUtil.getTextCondition(filter, "host_id").toQuery());
    }

    @Test
    public void testParseCondition() {
        Condition condition;

        condition = TextFilterUtil.parseCondition("host_id","~a");
        Assert.assertEquals("match(host_id,'a')", condition.toQuery());

        condition = TextFilterUtil.parseCondition("host_id","~d");
        Assert.assertEquals("match(host_id,'d')", condition.toQuery());

        condition = TextFilterUtil.parseCondition("host_id","~\\d");
        Assert.assertEquals("match(host_id,'\\\\d')", condition.toQuery());

        condition = TextFilterUtil.parseCondition("host_id","\\d");
        Assert.assertEquals("host_id LIKE '\\\\d'", condition.toQuery());

        condition = TextFilterUtil.parseCondition("host_id","webmaster");
        Assert.assertEquals("host_id LIKE 'webmaster'", condition.toQuery());

        condition = TextFilterUtil.parseCondition("host_id","\b");
        Assert.assertEquals(Condition.trueCondition().toQuery(), condition.toQuery());

        condition = TextFilterUtil.parseCondition("host_id","@**P");
        Assert.assertEquals("positionCaseInsensitive(host_id,'**P') > 0", condition.toQuery());

        condition = TextFilterUtil.parseCondition("host_id","*");
        Assert.assertEquals("host_id LIKE '%'", condition.toQuery());

        condition = TextFilterUtil.parseCondition("host_id","@");
        Assert.assertEquals("positionCaseInsensitive(host_id,'') > 0", condition.toQuery());

        condition = TextFilterUtil.parseCondition("host_id","");
        Assert.assertEquals(Condition.trueCondition().toQuery(), condition.toQuery());

        condition = TextFilterUtil.parseCondition("host_id","ab' and region_id='*"); // check inability to inject
        Assert.assertEquals("host_id LIKE 'ab\\' and region\\\\_id=\\'%'", condition.toQuery());

        condition = TextFilterUtil.parseCondition("host_id","%");
        Assert.assertEquals("host_id LIKE '\\\\%'", condition.toQuery());

        condition = TextFilterUtil.parseCondition("host_id","%*");
        Assert.assertEquals("host_id LIKE '\\\\%%'", condition.toQuery());

        condition = TextFilterUtil.parseCondition("host_id","%\\*");
        Assert.assertEquals("host_id LIKE '\\\\%*'", condition.toQuery());

        condition = TextFilterUtil.parseCondition("host_id","%\\\\*");
        Assert.assertEquals("host_id LIKE '\\\\%\\\\%'", condition.toQuery());

        condition = TextFilterUtil.parseCondition("host_id","~\\d\\.\\d+");
        Assert.assertEquals("match(host_id,'\\\\d\\\\.\\\\d+')", condition.toQuery());
    }

    @Test
    public void testUnescapeSpecialSymbols() throws Exception {
        Assert.assertEquals("", TextFilterUtil.unescapeSpecialSymbols(""));
        Assert.assertEquals("@", TextFilterUtil.unescapeSpecialSymbols("@"));
        Assert.assertEquals("@", TextFilterUtil.unescapeSpecialSymbols("\\@"));
        Assert.assertEquals("\\@", TextFilterUtil.unescapeSpecialSymbols("\\\\@"));
        Assert.assertEquals("~\\", TextFilterUtil.unescapeSpecialSymbols("~\\"));
        Assert.assertEquals("~*", TextFilterUtil.unescapeSpecialSymbols("~\\*"));
    }

    @Test
    public void testOnlyStarExpression() throws Exception {
        Predicate<String> predicate = TextFilterUtil.parseCondition("test", "\\*")
                .toPredicate(ConditionFieldExtractor.identity(String.class));
        Assert.assertTrue(predicate.test("*"));
        Assert.assertFalse(predicate.test("wrong string"));
    }
}
