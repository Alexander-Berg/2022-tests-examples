package ru.yandex.webmaster3.core.semantic.semantic_document_parser.microformats.spec.instances;

import org.junit.Test;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microformats.data.MicroformatData;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microformats.transformer.DocumentContext;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microformats.transformer.DocumentProperties;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microformats.transformer.FinalContext;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microformats.transformer.TransformStrategy;

/**
 * Created by IntelliJ IDEA.
 * User: rasifiel
 * Date: 8/2/11
 * Time: 3:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class HProductTest {

    @Test
    public void testAuto() throws Exception {
        final String content = "<div class=\"hproduct\">\n" +
                "    <span class=\"category\"><span class=\"value-title\" title=\"auto\"></span></span>\n" +
                "    <h2>\n" + "        <span class=\"brand\">Ford</span> \n" +
                "        <span class=\"fn\">Focus</span> \n" + "    </h2>\n" +
                "     <a class=\"photo\" href=\"http://example.com/img/ford/focus-st.jpg\">\n" +
                "        <img class=\"\" alt=\"Ford Focus, хэтчбек 5 дв ST\"\n" +
                "        src=\"http://example.com/img/ford/focus-st-preview.jpg\" align=\"left\">\n" + "    </a>\n" +
                "    <span class=\"identifier\">\n" + "        <span class=\"type\">\n" +
                "            <span class=\"value-title\" title=\"body-type\"></span>\n" + "        </span>\n" +
                "        <span class=\"value\">хэтчбек 5 дв</span>\n" + "    </span>\n" +
                "    <span class=\"identifier\">\n" + "        <span class=\"type\">\n" +
                "            <span class=\"value-title\" title=\"configuration-name\"></span>\n" + "        </span>\n" +
                "        <span class=\"value\">ST</span>\n" + "    </span>\n" + "    <span class=\"identifier\">\n" +
                "        <span class=\"type\">\n" +
                "            <span class=\"value-title\" title=\"displacement\"></span>\n" + "        </span>\n" +
                "        2.5\n" + "    </span>\n" + "    <span class=\"identifier\">\n" +
                "        <span class=\"type\">\n" +
                "            <span class=\"value-title\" title=\"transmission\"></span>\n" + "        </span>\n" +
                "        MT\n" + "    </span>\n" + "    <span class=\"identifier\">\n" +
                "        (<span class=\"value\">225</span> \n" + "        <span class=\"type\">\n" +
                "            <span class=\"value-title\" title=\"horse-power\"></span>\n" + "            л. с.\n" +
                "        </span>)\n" + "    </span>\n" + "    <span class=\"identifier\">\n" +
                "        <span class=\"value\">2009</span> \n" + "        <span class=\"type\">\n" +
                "            <span class=\"value-title\" title=\"prodyear\"></span>\n" + "            года выпуска\n" +
                "        </span>\n" + "    </span>\n" + "    <span class=\"identifier\">\n" +
                "        (<span class=\"value\">передний</span> \n" + "        <span class=\"type\">\n" +
                "            <span class=\"value-title\" title=\"gear-type\"></span>\n" + "            привод\n" +
                "        </span>,\n" + "    </span>\n" + "    <span class=\"identifier\">\n" +
                "        <span class=\"value\">левый</span>\n" + "        <span class=\"type\">\n" +
                "            <span class=\"value-title\" title=\"steering-wheel\"></span>\n" + "            руль\n" +
                "        </span>).\n" + "    </span>\n" +
                "    <a class=\"url\" href=\"http://example.com/ford/focus-st-2.5-mt.html\">\n" +
                "    Подробнее про эту конфигурацию</a>\n" + "    <span class=\"identifier\">\n" +
                "        <span class=\"type\">\n" +
                "            <span class=\"value-title\" title=\"dtpurchased\"></span>\n" +
                "            Приобретен \n" + "        </span>\n" +
                "        <abbr class=\"value\" title=\"2010-09-15\">15 сентября 2010</abbr>\n" + "    </span>\n" +
                "    <span class=\"identifier\">\n" + "        <span class=\"type\">\n" +
                "            <span class=\"value-title\" title=\"run\"></span>\n" + "            с пробегом \n" +
                "        </span>\n" + "        15\n" + "    </span>\n" + "    <span class=\"identifier\">\n" +
                "        <span class=\"type\">\n" +
                "            <span class=\"value-title\" title=\"run-metric\"></span>\n" + "        </span>\n" +
                "        тыс. км\n" + "    </span>.\n" + "</div>";
        final TransformStrategy strategy = new TransformStrategy();
        final DocumentContext context =
                new DocumentContext(content, new DocumentProperties("http://localhost/", MicroformatsManager.managerForMFsAndIncluded(HProduct.getInstance()), "UTF-8",
                        true));
        final FinalContext result = (FinalContext) strategy.processTillDone(context);
        for (final MicroformatData data : result.getInfo()) {
            System.out.println(data.getName() + ":" + data);
            //System.out.println(new NewHCardWebEntity(data,URL).getContent());
        }
    }

    @Test
    public void testAutoInReview() throws Exception {
        final String content = "<div class=\"hreview\">\n" + "    <span class=\"type\">\n" +
                "        <span class=\"value-title\" title=\"product\">\n" + "        </span>\n" + "    </span>\n" +
                "    <a class=\"permalink\" href=\"http://example.com/reviews/11111.html\"></a>\n" +
                "    <div class=\"item hproduct\">\n" + "<div class=\"hproduct\">\n" +
                "    <span class=\"category\"><span class=\"value-title\" title=\"auto\"></span></span>\n" +
                "    <h2>\n" + "        <span class=\"brand\">Ford</span> \n" +
                "        <span class=\"fn\">Focus</span> \n" + "    </h2>\n" +
                "     <a class=\"photo\" href=\"http://example.com/img/ford/focus-st.jpg\">\n" +
                "        <img class=\"\" alt=\"Ford Focus, хэтчбек 5 дв ST\"\n" +
                "        src=\"http://example.com/img/ford/focus-st-preview.jpg\" align=\"left\">\n" + "    </a>\n" +
                "    <span class=\"identifier\">\n" + "        <span class=\"type\">\n" +
                "            <span class=\"value-title\" title=\"body-type\"></span>\n" + "        </span>\n" +
                "        <span class=\"value\">хэтчбек 5 дв</span>\n" + "    </span>\n" +
                "    <span class=\"identifier\">\n" + "        <span class=\"type\">\n" +
                "            <span class=\"value-title\" title=\"configuration-name\"></span>\n" + "        </span>\n" +
                "        <span class=\"value\">ST</span>\n" + "    </span>\n" + "    <span class=\"identifier\">\n" +
                "        <span class=\"type\">\n" +
                "            <span class=\"value-title\" title=\"displacement\"></span>\n" + "        </span>\n" +
                "        2.5\n" + "    </span>\n" + "    <span class=\"identifier\">\n" +
                "        <span class=\"type\">\n" +
                "            <span class=\"value-title\" title=\"transmission\"></span>\n" + "        </span>\n" +
                "        MT\n" + "    </span>\n" + "    <span class=\"identifier\">\n" +
                "        (<span class=\"value\">225</span> \n" + "        <span class=\"type\">\n" +
                "            <span class=\"value-title\" title=\"horse-power\"></span>\n" + "            л. с.\n" +
                "        </span>)\n" + "    </span>\n" + "    <span class=\"identifier\">\n" +
                "        <span class=\"value\">2009</span> \n" + "        <span class=\"type\">\n" +
                "            <span class=\"value-title\" title=\"prodyear\"></span>\n" + "            года выпуска\n" +
                "        </span>\n" + "    </span>\n" + "    <span class=\"identifier\">\n" +
                "        (<span class=\"value\">передний</span> \n" + "        <span class=\"type\">\n" +
                "            <span class=\"value-title\" title=\"gear-type\"></span>\n" + "            привод\n" +
                "        </span>,\n" + "    </span>\n" + "    <span class=\"identifier\">\n" +
                "        <span class=\"value\">левый</span>\n" + "        <span class=\"type\">\n" +
                "            <span class=\"value-title\" title=\"steering-wheel\"></span>\n" + "            руль\n" +
                "        </span>).\n" + "    </span>\n" +
                "    <a class=\"url\" href=\"http://example.com/ford/focus-st-2.5-mt.html\">\n" +
                "    Подробнее про эту конфигурацию</a>\n" + "    <span class=\"identifier\">\n" +
                "        <span class=\"type\">\n" +
                "            <span class=\"value-title\" title=\"dtpurchased\"></span>\n" +
                "            Приобретен \n" + "        </span>\n" +
                "        <abbr class=\"value\" title=\"2010-09-15\">15 сентября 2010</abbr>\n" + "    </span>\n" +
                "    <span class=\"identifier\">\n" + "        <span class=\"type\">\n" +
                "            <span class=\"value-title\" title=\"run\"></span>\n" + "            с пробегом \n" +
                "        </span>\n" + "        15\n" + "    </span>\n" + "    <span class=\"identifier\">\n" +
                "        <span class=\"type\">\n" +
                "            <span class=\"value-title\" title=\"run-metric\"></span>\n" + "        </span>\n" +
                "        тыс. км\n" + "    </span>.\n" + "</div>\n" + "    </div>\n" + "    <div class=\"rating\">\n" +
                "        Оценка:\n" +
                "        <img class=\"value\" src=\"http://example.com/img/stars5.png\" alt=\"5\" />\n" +
                "        <abbr class=\"worst\" title=\"1\"></abbr>\n" +
                "        (5 из <span class=\"best\">5</span>)\n" + "    </div>\n" + "    <div>\n" +
                "        Время владения\n" + "        <span class=\"owning-time\">От 6 мес до одного года</span>\n" +
                "    </div>\n" + "    <blockquote class=\"description\">\n" +
                "        Проехал на машине уже 20 тыс. км — пока все нравится. Делал только ТО, \n" +
                "        плюс пришлось заменить прогоревший глушитель...  \n" + "    </blockquote>\n" +
                "    Достоинства:\n" + "    <ul>\n" + "        <li class=\"pro\">Комфорт</li>\n" +
                "        <li class=\"pro\">Надежность</li>\n" + "        <li class=\"pro\">Экономичность</li>\n" +
                "        <li class=\"pro\">Отлично слушается руля</li>\n" +
                "        <li class=\"pro\">Мощность и маневренность</li>\n" +
                "        <li class=\"pro\">Выглядит очень свежо и современно</li>\n" + "    </ul>        \n" +
                "    Недостатки:\n" + "    <ul>\n" + "        <li class=\"contra\">Ненадежный глушитель</li>\n" +
                "    </ul>        \n" + "    Общее впечатление:\n" + "    <blockquote class=\"summary\">\n" +
                "        Отличная машина! Рекомендую всем! \n" + "    </blockquote>\n" +
                "    <p class=\"dtreviewed\">\n" + "        Отзыв написан\n" +
                "        <abbr class=\"value\" title=\"2009-10-27 09:34\">27 октября 2009</abbr>\n" + "    </p>\n" +
                "    См. также отзывы про \n" +
                "    <a class=\"reviewsurl\" href=\"http://example.com/reviews/ford/focus/\">Ford Focus</a>,\n" +
                "    про \n" +
                "    <a class=\"reviewsurl\" href=\"http://example.com/reviews/ford/focus/st/\">Ford Focus ST</a>\n" +
                "    <div class=\"reviewer vcard\">\n" +
                "        <img src=\"http://example.com/~yuri-ivanov/photo.jpg\" title=\"Юрий Иванов\" class=\"photo\" />\n" +
                "        Автор:\n" +
                "        <a class=\"url fn\" href=\"http://example.com/~yuri-ivanov\">Юрий Иванов</a>\n" +
                "    </div>\n" + "</div>";
        final TransformStrategy strategy = new TransformStrategy();
        final DocumentContext context =
                new DocumentContext(content, new DocumentProperties("http://localhost/", MicroformatsManager.managerForMFsAndIncluded(HReview.getInstance()), "UTF-8",
                        true));
        final FinalContext result = (FinalContext) strategy.processTillDone(context);
        for (final MicroformatData data : result.getInfo()) {
            System.out.println(data.getName() + ":" + data);
            //System.out.println(new NewHCardWebEntity(data,URL).getContent());
        }
    }
}
