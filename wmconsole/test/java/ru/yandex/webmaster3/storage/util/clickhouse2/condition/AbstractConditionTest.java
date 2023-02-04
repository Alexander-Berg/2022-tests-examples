package ru.yandex.webmaster3.storage.util.clickhouse2.condition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Oleg Bazdyrev on 04/05/2017.
 */
public class AbstractConditionTest {

    private static ObjectMapper OM = new ObjectMapper().registerModule(new ParameterNamesModule());

    @Test
    public void testIntConditions() throws Exception {
        AbstractCondition<Integer> condition;
        condition = new IntCondition("int_field", Operator.EQUAL, 1000);
        Assert.assertEquals("int_field = 1000", condition.toQuery());
        Assert.assertFalse(condition.toPredicate().test(100));
        Assert.assertTrue(condition.toPredicate().test(1000));
        // deprecated IntEqualCondition
        condition = new IntEqualCondition("int_field", 1000);
        Assert.assertEquals("int_field = 1000", condition.toQuery());
        Assert.assertFalse(condition.toPredicate().test(100));
        Assert.assertTrue(condition.toPredicate().test(1000));
        // check de/serialization
        condition = OM.readValue(OM.writeValueAsString(condition), AbstractCondition.class);
        Assert.assertEquals("int_field = 1000", condition.toQuery());
        Assert.assertFalse(condition.toPredicate().test(100));
        Assert.assertTrue(condition.toPredicate().test(1000));

        condition = new IntCondition("int_field", Operator.GREATER_EQUAL, 666);
        Assert.assertEquals("int_field >= 666", condition.toQuery());
        Assert.assertFalse(condition.toPredicate().test(100));
        Assert.assertTrue(condition.toPredicate().test(666));
        Assert.assertTrue(condition.toPredicate().test(1000));

        condition = new IntCondition("int_field", Operator.GREATER_THAN, 666);
        Assert.assertEquals("int_field > 666", condition.toQuery());
        Assert.assertFalse(condition.toPredicate().test(100));
        Assert.assertFalse(condition.toPredicate().test(666));
        Assert.assertTrue(condition.toPredicate().test(1000));

        condition = new IntCondition("int_field", Operator.LESS_THAN, 666);
        Assert.assertEquals("int_field < 666", condition.toQuery());
        Assert.assertTrue(condition.toPredicate().test(100));
        Assert.assertFalse(condition.toPredicate().test(666));
        Assert.assertFalse(condition.toPredicate().test(1000));

        condition = new IntCondition("int_field", Operator.LESS_EQUAL, 666);
        Assert.assertEquals("int_field <= 666", condition.toQuery());
        Assert.assertTrue(condition.toPredicate().test(100));
        Assert.assertTrue(condition.toPredicate().test(666));
        Assert.assertFalse(condition.toPredicate().test(1000));

        condition = OM.readValue(OM.writeValueAsString(condition), AbstractCondition.class);
        Assert.assertEquals("int_field <= 666", condition.toQuery());
        Assert.assertTrue(condition.toPredicate().test(100));
        Assert.assertTrue(condition.toPredicate().test(666));
        Assert.assertFalse(condition.toPredicate().test(1000));
    }

    @Test
    public void testLongConditions() throws Exception {
        AbstractCondition<Long> condition;
        condition = new LongCondition("long_field", Operator.EQUAL, 1000L);
        Assert.assertEquals("long_field = 1000", condition.toQuery());
        Assert.assertFalse(condition.toPredicate().test(100L));
        Assert.assertTrue(condition.toPredicate().test(1000L));
        condition = OM.readValue(OM.writeValueAsString(condition), AbstractCondition.class);
        Assert.assertEquals("long_field = 1000", condition.toQuery());
        Assert.assertFalse(condition.toPredicate().test(100L));
        Assert.assertTrue(condition.toPredicate().test(1000L));
    }

    @Test
    public void testFloatConditions() throws Exception {
        AbstractCondition<Double> condition;
        condition = new FloatCondition("float_field", Operator.GREATER_EQUAL, 1000.0);
        Assert.assertEquals("float_field >= 1000.0", condition.toQuery());
        Assert.assertFalse(condition.toPredicate().test(100.0));
        Assert.assertFalse(condition.toPredicate().test(999.99));
        Assert.assertTrue(condition.toPredicate().test(1000.0));
        condition = OM.readValue(OM.writeValueAsString(condition), AbstractCondition.class);
        Assert.assertEquals("float_field >= 1000.0", condition.toQuery());
        Assert.assertFalse(condition.toPredicate().test(100.0));
        Assert.assertFalse(condition.toPredicate().test(999.99));
        Assert.assertTrue(condition.toPredicate().test(1000.0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongOperator_1() {
        new IntCondition("int_field", Operator.TEXT_MATCH, 11);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongOperator_2() {
        new TextCondition("text_field", Operator.GREATER_EQUAL, "11");
    }

    @Test
    public void testTextEqualsConditions() throws Exception {
        AbstractCondition<String> condition;
        condition = new TextCondition("text_field", Operator.EQUAL, "some_text");
        Assert.assertEquals("text_field = 'some_text'", condition.toQuery());
        Assert.assertFalse(condition.toPredicate().test("other_text"));
        Assert.assertTrue(condition.toPredicate().test("some_text"));
        // check for serialization
        condition = OM.readValue(OM.writeValueAsString(condition), AbstractCondition.class);
        Assert.assertEquals("text_field = 'some_text'", condition.toQuery());
        Assert.assertFalse(condition.toPredicate().test("other_text"));
        Assert.assertTrue(condition.toPredicate().test("some_text"));

        // check for escaping
        condition = new TextCondition("text_field", Operator.EQUAL, "text with 'single quote");
        Assert.assertEquals("text_field = 'text with \\'single quote'", condition.toQuery());
        Assert.assertFalse(condition.toPredicate().test("other_text"));
        Assert.assertTrue(condition.toPredicate().test("text with 'single quote"));

        condition = new TextCondition("text_field", Operator.EQUAL, "text with % percent");
        Assert.assertEquals("text_field = 'text with % percent'", condition.toQuery());

        condition = new TextCondition("text_field", Operator.EQUAL, "text with \\ backslash");
        Assert.assertEquals("text_field = 'text with \\\\ backslash'", condition.toQuery());
    }

    @Test
    public void testTextContainsConditions() throws Exception {
        AbstractCondition<String> condition;
        condition = new TextCondition("text_field", Operator.TEXT_CONTAINS, "some_text");
        Assert.assertEquals("positionCaseInsensitive(text_field,'some_text') > 0", condition.toQuery());
        condition = new TextCondition(null, Operator.TEXT_CONTAINS, "some_text");
        Assert.assertFalse(condition.toPredicate().test("not contains"));
        Assert.assertTrue(condition.toPredicate().test("some_textcontains"));

        condition = new TextCondition("text_field", Operator.TEXT_CONTAINS, "some_'text");
        Assert.assertEquals("positionCaseInsensitive(text_field,'some_\\'text') > 0", condition.toQuery());

        condition = new TextCondition("text_field", Operator.TEXT_CONTAINS, "some_%text");
        Assert.assertEquals("positionCaseInsensitive(text_field,'some_%text') > 0", condition.toQuery());
    }

    @Test
    public void testTextLikeConditions() throws Exception {
        AbstractCondition<String> condition;
        condition = new TextCondition("text_field", Operator.TEXT_LIKE, "some_text");
        Assert.assertEquals("text_field LIKE 'some_text'", condition.toQuery());
        // predicates not supported
        condition = new TextCondition("text_field", Operator.TEXT_LIKE, "some_%text");
        Assert.assertEquals("text_field LIKE 'some_%text'", condition.toQuery());
        condition = new TextCondition("text_field", Operator.TEXT_LIKE, "some_\\%text");
        Assert.assertEquals("text_field LIKE 'some_\\\\%text'", condition.toQuery());
    }

    @Test
    public void testTextMatchConditions() throws Exception {
        AbstractCondition<String> condition;
        condition = new TextCondition("text_field", Operator.TEXT_MATCH, "\\d{4}");
        Assert.assertEquals("match(text_field,'\\\\d{4}')", condition.toQuery());
        condition = new TextCondition("", Operator.TEXT_MATCH, "\\d{4}");
        Assert.assertFalse(condition.toPredicate().test("abr123"));
        Assert.assertTrue(condition.toPredicate().test("abr1234"));
    }
}
