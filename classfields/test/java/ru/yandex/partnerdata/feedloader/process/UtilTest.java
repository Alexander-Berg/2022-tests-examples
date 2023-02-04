package ru.yandex.partnerdata.feedloader.process;

import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.common.services.fs.EllipticFileStorageService;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.feedloader.data.TaskConfig;
import ru.yandex.partnerdata.feedloader.XSDValidationHelper;
import ru.yandex.feedloader.depot.XSDDepot;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * User: Dmitrii Tolmachev (sunlight@yandex-team.ru)
 * Date: 19.12.13
 * Time: 16:51
 */
public class UtilTest {

    @Test
    @Ignore
    public void testXSD() throws Exception {
        File file = new File(this.getClass().getResource("/feeds/life-realty.ru_cut.xml").getFile());
        InputStream is = new BufferedInputStream(new FileInputStream(file));

        final Map<String, String> map = new HashMap<String, String>();
        map.put("21_1", "/xsd/realty.xsd");
        map.put("21_2", "/xsd/realty.xsd");
        XSDDepot xsdDepot = new XSDDepot();
        xsdDepot.setServiceName2filePath(map);

        XSDValidationHelper xsdValidationHelper = new XSDValidationHelper(xsdDepot);
        final Pair<Boolean, String> pair =
                xsdValidationHelper.processXSDValidation(21, 1, is,
                        new TaskConfig(1, false, 10,
                                null, false, false));
        assertTrue(pair.first);
//        System.out.println(pair.second);
    }

    @Test
    @Ignore
    public void downloadToElliptics() throws Exception {
        EllipticFileStorageService ellipticFileStorageService = new EllipticFileStorageService();
        ellipticFileStorageService.setServiceName("auto-tr");
        ellipticFileStorageService.setPath("http://vs-elliptics01ht.yandex.net:80");
        ellipticFileStorageService.saveFile("/Users/sunlight/yandex_uvr.xml.gz", "yandex_uvr.xml.gz");
    }
}
