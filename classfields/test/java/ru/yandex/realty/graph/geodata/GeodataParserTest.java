package ru.yandex.realty.graph.geodata;

import org.apache.commons.lang.Validate;
import org.junit.Test;
import ru.yandex.common.util.functional.Callback;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * author: rmuzhikov
 */
public class GeodataParserTest {

    @Test
    public void testParse() throws Exception {
        InputStream is = new FileInputStream(new File("test-data/geosrc.houses.xml"));
        GeodataParser.parse(is, new Callback<Toponym>() {
            @Override
            public void doWith(Toponym toponym) {
                Validate.isTrue("street".equals(toponym.getKind()));
                Validate.isTrue(2988017l == toponym.getId());
                Validate.isTrue(2559823l == toponym.getParent());
                Validate.isTrue(toponym.getName().size() == 2);
                Validate.isTrue("ru_LOCAL".equals(toponym.getName().get(0).getLocale()));
                Validate.isTrue("official".equals(toponym.getName().get(0).getType()));
                Validate.isTrue("улица Черемуховая".equals(toponym.getName().get(0).getName()));
                Validate.isTrue(toponym.getGeoid() != null);
                Validate.isTrue(!toponym.getGeoid().isOwner());
                Validate.isTrue(11266 == toponym.getGeoid().getId());
                Validate.isTrue(toponym.getPoint() != null);
                Validate.isTrue(toponym.getPoint().getPos() != null);
                Validate.isTrue(toponym.getEnvelope() != null);
                Validate.isTrue(toponym.getEnvelope().getLowerCorner() != null);
                Validate.isTrue(toponym.getEnvelope().getUpperCorner() != null);
                Validate.isTrue(toponym.getGeometry().size() == 2);
                Validate.isTrue(toponym.getAddress().size() == 2);
                Validate.isTrue("ru".equals(toponym.getAddress().get(0).getLang()));
                Validate.isTrue("Россия, Иркутская область, поселок Молодежный, улица Черемуховая".equals(toponym.getAddress().get(0).getAddress()));
            }
        });
        is.close();
    }
}
