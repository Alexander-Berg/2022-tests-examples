package ru.yandex.realty.agency.name;


import org.junit.Assert;
import org.junit.Test;
import ru.yandex.common.util.collections.Cu;

import java.util.Map;

import static ru.yandex.common.util.collections.CollectionFactory.newHashMap;
import static ru.yandex.realty.agency.name.DefaultRegexpNameNormalizerConfig.*;

/**
 * author: rmuzhikov
 */
public class DefaultRegexpNameNormalizerConfigTest {
    private final static Map<String, String> TEST_DATA = newHashMap();
    static {
        TEST_DATA.put("Атташе Агентство недвижимости", "Атташе");
        TEST_DATA.put("Вертикаль-55 Агентство недвижимости и юридических услуг", "Вертикаль-55 Агентство недвижимости и юридических услуг");
        TEST_DATA.put("Аренда Уфа, агентство недвижимости", "Аренда Уфа");
        TEST_DATA.put("ООО  \"КрасБизнесКонсалтинг\"", "КрасБизнесКонсалтинг");
        TEST_DATA.put("Вся недвижимость для Вас", "Вся недвижимость для Вас");
        TEST_DATA.put("Центр недвижимости Мир бизнеса", "Центр недвижимости Мир бизнеса");
        TEST_DATA.put("ДСК-Недвижимость, ИП", "ДСК-Недвижимость");
        TEST_DATA.put("Мегаполис, ул. Краснодонцев, 7, 293-47-00 293-29-60", "Мегаполис");
        TEST_DATA.put("АН «Pskov-terra»", "«Pskov-terra»");
        TEST_DATA.put("Юг-недвижимость", "Юг-недвижимость");
        TEST_DATA.put("Агентство недвижимости Русский Дом г. Клин", "Русский Дом");
        TEST_DATA.put("Дворец, ул. Смирнова, 17, 259-38-00 259-38-10", "Дворец");
        TEST_DATA.put("АН ООО Гостиный Двор, г.Заволжье, пр. Дзержинского 2а, 8(83161) 7-50-50", "Гостиный Двор");
        TEST_DATA.put("Успех, пр-т Молодежный, 12б, 257-61-67 293-45-00", "Успех");
        TEST_DATA.put("Нижегородское АН, Пр-кт Гагарина 29, 4341270", "Нижегородское");
        TEST_DATA.put("Наш Нижний, пл. Театральная, 3, офис 2, 419-91-82 419-95-29", "Наш Нижний");
        TEST_DATA.put("&quot;Сити Менеджер&quot;", "Сити Менеджер");
        TEST_DATA.put("quot;Сити Менеджер&quot;", "Сити Менеджер");
        TEST_DATA.put("&amp;Сити Менеджер&quot;", "Сити Менеджер");
        TEST_DATA.put("amp;Сити Менеджер&quot;", "Сити Менеджер");
        TEST_DATA.put("&amp;amp;amp;amp;Сити Менеджер&amp;", "Сити Менеджер");
        TEST_DATA.put("amp;amp;amp;Сити Менеджер&quot;", "Сити Менеджер");
        TEST_DATA.put("Элита www.elita.26.ru", "Элита");
        TEST_DATA.put("kuku@74.ru", "");
        TEST_DATA.put("МОСКВА moskow.never@sleeps.ru", "МОСКВА");
        TEST_DATA.put("Novostroy-M.ru", "Novostroy-M.ru");
        TEST_DATA.put("M.ru", "");
        TEST_DATA.put("M.ru Супер агенство", "Супер агенство");
        TEST_DATA.put("Агентство &amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;Гамма-Риелтamp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;", "Гамма-Риелт");
    }

    @Test
    public void testDefaultConfig() throws Exception {
        RegexpNameNormalizer regexpNameNormalizer = new RegexpNameNormalizer(
                Cu.union(DEFAULT_CLEAN_REGEXPS, buildTokenRegexps(DEFAILT_AGENCY_NAME_SIGNS)),
                DEFAULT_BLOCK_REGEXPS);

        for (Map.Entry<String, String> entry : TEST_DATA.entrySet()) {
            Assert.assertEquals(entry.getValue(), regexpNameNormalizer.apply(entry.getKey()).trim());
        }
    }
}
