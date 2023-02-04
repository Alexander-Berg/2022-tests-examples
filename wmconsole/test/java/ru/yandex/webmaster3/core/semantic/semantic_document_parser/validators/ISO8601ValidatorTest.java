package ru.yandex.webmaster3.core.semantic.semantic_document_parser.validators;

import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: rasifiel
 * Date: 15.10.12
 * Time: 16:15
 */
public class ISO8601ValidatorTest extends TestCase {
    public void testValidate() throws Exception {
        assertEquals(ISO8601Validator.validate("454145848"), true);
    }
}
