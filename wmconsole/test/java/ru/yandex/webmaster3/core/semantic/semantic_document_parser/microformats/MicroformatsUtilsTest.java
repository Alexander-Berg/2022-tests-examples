package ru.yandex.webmaster3.core.semantic.semantic_document_parser.microformats;

import junit.framework.TestCase;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.common.util.functional.Function;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microformats.data.MicroformatData;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microformats.exceptions.MFException;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microformats.spec.instances.HCard;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microformats.spec.instances.MicroformatsManager;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.serialize.util.APIVersion;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: rasifiel
 * Date: 01.11.12
 * Time: 18:16
 */
public class MicroformatsUtilsTest extends TestCase {
    public void testToJson() throws Exception {
        Pair<List<MicroformatData>, List<MFException>> x = MicroformatsUtils.extractMF("<div class=\"vcard\">\n" +
                " <div>\n" +
                "   <span class=\"category\">Кафе</span>\n" +
                "   <span class=\"fn org\">Ромашка</span>\n" +
                " </div>\n" +
                " <div class=\"adr\">\n" +
                "   <span class=\"locality\">г. Солнечный</span>,\n" +
                "   <span class=\"street-address\">просп. Романтиков, д. 21</span>\n" +
                " </div>\n" +
                " <div>Телефон: <span class=\"tel\">+7 (890) 123-45-67</span></div>\n" +
                " <div>Мы работаем <span class=\"workhours\">ежедневно с 11:00 до 24:00</span>\n" +
                "   <span class=\"url\">\n" +
                "     <span class=\"value-title\" title=\"http://www.romashka-cafe.ru\"> </span>\n" +
                "   </span>\n" +
                " </div>\n" +
                "</div>", "http://localhost", MicroformatsManager.managerForMFsAndIncluded(HCard.getInstance()), false);
        JSONArray result = new JSONArray(new Function<MicroformatData, JSONObject>() {
            @Override
            public JSONObject apply(final MicroformatData arg) {
                return MicroformatsUtils.toJson(arg, null, false, APIVersion.VERSION_1_1);
            }
        }.map(x.first));
        System.out.println(result);
    }
}
