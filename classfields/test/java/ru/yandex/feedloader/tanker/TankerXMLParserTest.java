package ru.yandex.feedloader.tanker;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ru.yandex.feedloader.localization.tanker.model.*;
import ru.yandex.feedloader.localization.tanker.parser.TankerXMLParser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * User: Dmitrii Tolmachev (sunlight@yandex-team.ru)
 * Date: 28.11.13
 * Time: 20:03
 */
@RunWith(JUnit4.class)
public class TankerXMLParserTest extends AbstractJUnit4SpringContextTests {

    private String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><tanker><project id=\"capa\"><keyset id=\"Auto\"><key id=\"def_reason.1\" is_plural=\"False\"><value language=\"ru\" status=\"approved\">несоответствие требованиям к сайту Партнера</value><value language=\"en\" status=\"requires_translation\"/><value language=\"tr\" status=\"requires_translation\"/><value language=\"uk\" status=\"approved\">невідповідність вимогам до сайту Партнера</value></key></keyset></project></tanker>";

    @Test
    public void test() throws Exception {
        final InputStream is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        final DataStorage storage = new TankerXMLParser().parse(is);

        final Value value1 = storage.get(new Key("def_reason.1", Language.RUSSIAN));
        Assert.assertEquals(value1, new Value("несоответствие требованиям к сайту Партнера", Status.APPROVED));

        final Value value2 = storage.get(new Key("def_reason.1", Language.ENGLISH));
        Assert.assertEquals(value2, new Value("", Status.REQUIRES_TRANSLATION));

        final Value value3 = storage.get(new Key("def_reason.1", Language.UKRAINIAN));
        Assert.assertEquals(value3, new Value("невідповідність вимогам до сайту Партнера", Status.APPROVED));

        final Value value4 = storage.get(new Key("def_reason.1", Language.TURKISH));
        Assert.assertEquals(value4, new Value("", Status.REQUIRES_TRANSLATION));
    }
}
