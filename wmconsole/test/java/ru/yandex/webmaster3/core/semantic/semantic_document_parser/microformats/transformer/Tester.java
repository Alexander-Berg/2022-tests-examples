package ru.yandex.webmaster3.core.semantic.semantic_document_parser.microformats.transformer;

import ru.yandex.common.util.IOUtils;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microformats.MicroformatsUtils;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microformats.data.MicroformatData;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microformats.exceptions.InvalidActionException;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microformats.exceptions.MFException;
import ru.yandex.webmaster3.core.semantic.semantic_document_parser.microformats.spec.instances.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: rasifiel
 * Date: 7/15/11
 * Time: 11:58 AM
 * To change this template use File | Settings | File Templates.
 */
public class Tester {

    public static void main(final String[] args) throws IOException {
        final TransformStrategy transformStrategy = new TransformStrategy();
        final long startTime = System.currentTimeMillis();
        int sum = 0;
        final HashMap<String, Integer> dic = new HashMap<String, Integer>();
        for (int i = 6; i <= 10000; i++) {
            if (i % 100 == 0) {
                System.out.println(i);
            }
            final String con =
                    IOUtils.readInputStream(new FileInputStream("/home/rasifiel/hcards/chunks/data" + i + ".html"));
            //String con = IOUtils.readInputStream(new FileInputStream("/home/rasifiel/hcards/chunks/data2.html"));
            //String con = IOUtils.readInputStream(new FileInputStream("/home/rasifiel/test_tree_hcard.html"));
            final String meta =
                    IOUtils.readInputStream(new FileInputStream("/home/rasifiel/hcards/chunks/info" + i + ".txt"));
            final int start = meta.indexOf("http");
            final int end = meta.indexOf("\"", start);
            final String baseUrl = meta.substring(start, end);
            final Pair<List<MicroformatData>, List<MFException>> res =
                    MicroformatsUtils.extractMF(con, baseUrl, MicroformatsManager.managerForMFsAndIncluded(HCard.getInstance(), HReview.getInstance(), HResume.getInstance(), HProduct.getInstance()),
                            true);

            sum += res.first.size();
            for (final Object o : res.first) {
                final MicroformatData data = (MicroformatData) o;
                try {
                    Integer val = dic.get(data.getSpec().getName());
                    if (val == null) {
                        val = 0;
                    }
                    val++;
                    dic.put(data.getSpec().getName(), val);
                } catch (InvalidActionException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
                //System.out.println(o);
            }
        }
        System.out.println(sum + " mf extracted " + (System.currentTimeMillis() - startTime) + " ms");
        for (final String key : dic.keySet()) {
            System.out.println(key + " : " + dic.get(key));
        }
    }

}
