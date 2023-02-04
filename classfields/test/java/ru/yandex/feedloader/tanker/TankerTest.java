package ru.yandex.feedloader.tanker;

import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ru.yandex.feedloader.localization.tanker.model.*;

import java.util.NoSuchElementException;

/**
 * User: Dmitrii Tolmachev (sunlight@yandex-team.ru)
 * Date: 28.11.13
 * Time: 18:00
 */
@RunWith(JUnit4.class)
public class TankerTest extends AbstractJUnit4SpringContextTests {

    private DataStorage initAll() {
        final DataStorage dataStorage = new DataStorage();

        final Key rusKey = new Key("problem.common", Language.RUSSIAN);
        final Key engKey = new Key("problem.common", Language.ENGLISH);
        final Key ukKey = new Key("problem.common", Language.UKRAINIAN);
        final Key trKey = new Key("problem.common", Language.TURKISH);

        final Value rusVal = new Value("Ошибка при загрузке файла", Status.APPROVED);
        final Value engVal = new Value("Error!", Status.APPROVED);
        final Value ukVal = new Value("Помилка під час завантаження файлу", Status.APPROVED);
        final Value trVal = new Value("Dosyası yüklenirken hata", Status.APPROVED);

        dataStorage.put(rusKey, rusVal);
        dataStorage.put(engKey, engVal);
        dataStorage.put(ukKey, ukVal);
        dataStorage.put(trKey, trVal);

        return dataStorage;
    }

    @Test
    public void testGet() {
        final DataStorage dataStorage = initAll();

        final Value rusVal = new Value("Ошибка при загрузке файла", Status.APPROVED);
        final Value engVal = new Value("Error!", Status.APPROVED);
        final Value ukVal = new Value("Помилка під час завантаження файлу", Status.APPROVED);
        final Value trVal = new Value("Dosyası yüklenirken hata", Status.APPROVED);

        final Value val1 = dataStorage.get(new Key("problem.common", Language.RUSSIAN));
        Assert.assertNotNull(val1);
        Assert.assertEquals(val1, rusVal);

        final Value val2 = dataStorage.get(new Key("problem.common", Language.ENGLISH));
        Assert.assertNotNull(val2);
        Assert.assertEquals(val2, engVal);

        final Value val3 = dataStorage.get(new Key("problem.common", Language.UKRAINIAN));
        Assert.assertNotNull(val3);
        Assert.assertEquals(val3, ukVal);

        final Value val4 = dataStorage.get(new Key("problem.common", Language.TURKISH));
        Assert.assertNotNull(val4);
        Assert.assertEquals(val4, trVal);
    }

    @Test
    public void testRusLocalization() {
        final DataStorage dataStorage = new DataStorage();

        final Value rusVal = new Value("ошибка XSD-формата", Status.APPROVED);
        final Key key = new Key("problem.xsd", Language.RUSSIAN);
        dataStorage.put(key, rusVal);

        final boolean res = dataStorage.exists(key);
        Assert.assertEquals(res, true);

        final Value v1 = dataStorage.get(key);
        Assert.assertEquals(v1, rusVal);
    }

    @Test
    public void testEngLocalization() {
        final DataStorage dataStorage = new DataStorage();

        final Value engVal = new Value("XSD-format error", Status.APPROVED);
        final Key key = new Key("problem.xsd", Language.ENGLISH);
        dataStorage.put(key, engVal);

        final boolean res = dataStorage.exists(key);
        Assert.assertEquals(res, true);

        final Value v2 = dataStorage.get(key);
        Assert.assertEquals(v2, engVal);
    }

    @Test(expected = NoSuchElementException.class)
    public void testException() {
        final DataStorage dataStorage = new DataStorage();

        final Value rusVal = new Value("ошибка XSD-формата", Status.APPROVED);
        final Key key = new Key("problem.xsd", Language.RUSSIAN);
        dataStorage.put(key, rusVal);

        final Value v2 = dataStorage.get(new Key("problem.common", Language.RUSSIAN));
        Assert.assertEquals(v2, rusVal);
    }

    @Test
    public void remove() {
        final DataStorage dataStorage = initAll();
        boolean res = dataStorage.delete(new Key("problem.common", Language.RUSSIAN));
        Assert.assertEquals(res, true);

        boolean res2 = dataStorage.delete(new Key("problem.common1", Language.RUSSIAN));
        Assert.assertEquals(res2, false);
    }

    @Test
    public void appendAll() {
        final DataStorage dataStorage = initAll();

        final Key rusKey = new Key("problem.common", Language.RUSSIAN);
        final Key engKey = new Key("problem.common", Language.ENGLISH);
        final Key ukKey = new Key("problem.common", Language.UKRAINIAN);
        final Key trKey = new Key("problem.common", Language.TURKISH);

        boolean res1 = dataStorage.exists(rusKey);
        Assert.assertEquals(res1, true);

        boolean res2 = dataStorage.exists(engKey);
        Assert.assertEquals(res2, true);

        boolean res3 = dataStorage.exists(ukKey);
        Assert.assertEquals(res3, true);

        boolean res4 = dataStorage.exists(trKey);
        Assert.assertEquals(res4, true);

        boolean res5 = dataStorage.exists(new Key("problem", Language.ENGLISH));
        Assert.assertEquals(res5, false);
    }
}

