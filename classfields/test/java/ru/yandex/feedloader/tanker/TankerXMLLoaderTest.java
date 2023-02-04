package ru.yandex.feedloader.tanker;

import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ru.yandex.feedloader.localization.tanker.TankerFileDataLoader;
import ru.yandex.feedloader.localization.tanker.model.*;
import ru.yandex.feedloader.localization.tanker.parser.TankerXMLParser;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Dmitrii Tolmachev (sunlight@yandex-team.ru)
 * Date: 28.11.13
 * Time: 19:34
 */
@RunWith(JUnit4.class)
public class TankerXMLLoaderTest extends AbstractJUnit4SpringContextTests {

    @Test
    public void testLoad() {
        final List<String> list = new ArrayList<String>();
        list.add("/tanker/feedloader-tanker.xml");
        final DataStorage storage = new TankerFileDataLoader(list, new TankerXMLParser()).load();

        final Value val = storage.get(new Key("problem.common", Language.RUSSIAN));
        Assert.assertEquals(val, new Value("Ошибка при загрузке файла", Status.APPROVED));

        final Value val2 = storage.get(new Key("problem.doctype", Language.RUSSIAN));
        Assert.assertEquals(val2, new Value("Элемент DOCTYPE запрещен", Status.APPROVED));

        final Value val3 = storage.get(new Key("problem.connection", Language.ENGLISH));
        Assert.assertEquals(val3, new Value("", Status.REQUIRES_TRANSLATION));

        final Value val4 = storage.get(new Key("problem.redirects", Language.ENGLISH));
        Assert.assertEquals(val4, new Value("", Status.REQUIRES_TRANSLATION));
    }

}
