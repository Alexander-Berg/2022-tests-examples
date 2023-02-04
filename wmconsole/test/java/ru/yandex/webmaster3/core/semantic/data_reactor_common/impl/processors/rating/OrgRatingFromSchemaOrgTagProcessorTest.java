package ru.yandex.webmaster3.core.semantic.data_reactor_common.impl.processors.rating;

import junit.framework.TestCase;
import ru.yandex.webmaster3.core.semantic.data_reactor_common.Entity;
import ru.yandex.webmaster3.core.semantic.data_reactor_common.impl.processors.markup.GetMicrodataProcessor;
import ru.yandex.webmaster3.core.semantic.data_reactor_common.impl.processors.schemaorg.rating.OrgRatingFromSchemaOrgTagProcessor;
import ru.yandex.webmaster3.core.semantic.data_reactor_common.impl.utils.Html2EntityConverter;
import ru.yandex.webmaster3.core.semantic.data_reactor_common.impl.utils.testserializers.EntityToClearJsonSerializer;

import java.util.Collection;

/**
 * Created by aleksart on 19.02.14.
 */
public class OrgRatingFromSchemaOrgTagProcessorTest extends TestCase {
    public void testProcess() throws Exception {
        final String doc = "<div itemscope itemtype=\"http://schema.org/Review\">\n" +
                "  <h2 itemprop=\"name\"><a href=\"http://example.com/review?10231\" itemprop=\"url\">Русская кухня в изгнании</a></h2>\n" +
                "  <div>Отзыв написал <span itemprop=\"author\" itemscope itemtype=\"http://schema.org/Person\">\n" +
                "    <span itemprop=\"name\">\n" +
                "      <a itemprop=\"url\" href=\"http://example.com/users/vasya\">Вася Пупкин</a>\n" +
                "    </span>\n" +
                "  </span>\n" +
                "    <meta itemprop=\"datePublished\" content=\"2012-07-15\" />\n" +
                "    15 июля 2012.\n" +
                "  </div>\n" +
                "  <div itemprop=\"reviewRating\" itemscope itemtype=\"http://schema.org/Rating\">\n" +
                "    <meta itemprop=\"worstRating\" content=\"0\"/>\n" +
                "    <p>Оценка: <span itemprop=\"ratingValue\">9</span> из <span itemprop=\"bestRating\">10</span>.</p>\n" +
                "  </div>\n" +
                "  <div itemprop=\"pro\">Бесплатная стоянка, прекрасная детская комната и предупредительные официанты.</div>\n" +
                "  <div itemprop=\"contra\">Большой и шумный некурящий зал.</div>\n" +
                //    "  <div itemprop=\"contra\">Vfktymrbq и шумный dwfv зал.</div>\n" +
                "  <div itemprop=\"reviewBody\">\n" +
                "    <p>Заказ был готов сравнительно быстро, а напитки приготовили практически сразу.\n" +
                "    Обслуживание на уровне, хотя грязная посуда иногда застаивалась.</p>\n" +
                "    <p>Рекомендую русскую кухню, особенно супы.</p>\n" +
                "  </div>\n" +
                "  <div>Автор посетил заведение <meta itemprop=\"dateVisited\" content=\"2012-07-10\">10 июля 2012.</div>\n" +
                "  <div>Оценки характеристик ресторана:\n" +
                "    <ul>\n" +
                "      <li itemprop=\"tag\" itemscope itemtype=\"http://schema.org/Rating\">\n" +
                "        <link itemprop=\"ratingTarget\" href=\"http://webmaster.yandex.ru/vocabularies/ReviewBusiness/Cuisine.xml\">\n" +
                "        Кухня: <span itemprop=\"ratingValue\">5</span> из <span itemprop=\"bestRating\">5</span>;\n" +
                "      </li>\n" +
                "      <li itemprop=\"tag\" itemscope itemtype=\"http://schema.org/Rating\">\n" +
                "        <link itemprop=\"ratingTarget\" href=\"http://webmaster.yandex.ru/vocabularies/ReviewBusiness/Hall.xml\">\n" +
                "        Зал: <span itemprop=\"ratingValue\">3</span> из <span itemprop=\"bestRating\">5</span>;\n" +
                "      </li>\n" +
                "    </ul>\n" +
                "  </div>\n" +
                "  <div itemprop=\"itemReviewed\" itemscope itemtype=\"http://schema.org/Hotel\">\n" +
                "    <h3>Информация о ресторане</h3>\n" +
                "    <p>Название: <span itemprop=\"name\">Заграница</span></p>\n" +
                "    <p itemprop=\"address\" itemscope itemtype=\"http://schema.org/PostalAddress\">Адрес:\n" +
                "      <span itemprop=\"addressLocality\">Москва</span>, <span itemprop=\"streetAddress\">Тверская, 7</span>.\n" +
                "    </p>\n" +
                "    <p>Телефон: <span itemprop=\"telephone\">123-45-12</span>.</p>\n" +
                "    <p>Сайт ресторана: <a itemprop=\"url\" href=\"http://zagranica.ru\">http://zagranica.ru</a></p>\n" +
                "    <p>Электронный адрес администрации:\n" +
                "      <a itemprop=\"email\" href=\"mailto:hostess@zagranica.ru\">hostess@zagranica.ru</a>\n" +
                "    </p>\n" +
                "  </div>\n" +
                "</div>";
        Entity e = Html2EntityConverter.fromHtml(doc);
        GetMicrodataProcessor processor = new GetMicrodataProcessor();
        Collection<Entity> entities = processor.process(e);
        EntityToClearJsonSerializer serializer = new EntityToClearJsonSerializer();
        serializer.setGap(4);

        System.out.println(entities.size());

        for(Entity ee : entities){

            System.out.println(serializer.serialize(ee));
        }
        System.out.println("--------------------------------------------------------------------------------");
        OrgRatingFromSchemaOrgTagProcessor processor2 = new OrgRatingFromSchemaOrgTagProcessor();
        for(Entity ee: entities){
            Collection<Entity> ratings = processor2.process(ee);
            for(Entity rating: ratings){
                System.out.println(serializer.serialize(rating));
            }
        }
    }
}
