package ru.yandex.webmaster3.storage.turbo.service.autoparser;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.webmaster3.core.turbo.model.autoparser.AutoparserToggleState;
import ru.yandex.webmaster3.core.turbo.model.autoparser.TurboAutoparsedHostInfo;
import ru.yandex.webmaster3.core.turbo.model.TurboSampleData;

import java.util.*;

/**
 * Created by ifilippov5 on 21.06.18.
 */
public class TurboAutoparserInfoUtilTest {

    private static final String WIKI = "wikipedia.org";
    private static final String RU_WIKI = "ru.wikipedia.org";
    private static final String EU_WIKI = "eu.wikipedia.org";
    private static final String B_RU_WIKI = "b.ru.wikipedia.org";
    private static final String A_B_RU_WIKI = "a.b.ru.wikipedia.org";
    private static final String LENTA = "lenta.ru";
    private static final String A_LENTA = "a.lenta.ru";

    private static final AutoparserToggleState ON = AutoparserToggleState.ON;
    private static final AutoparserToggleState OFF = AutoparserToggleState.OFF;
    private static final AutoparserToggleState EXTENDS = AutoparserToggleState.INHERITS;

    private static final TurboSampleData SAMPLE1 = new TurboSampleData("url1", "title1", "turbo1");
    private static final TurboSampleData SAMPLE2 = new TurboSampleData("url2", "title2", "turbo2");
    private static final TurboSampleData SAMPLE3 = new TurboSampleData("url3", "title3", "turbo3");
    private static final TurboSampleData SAMPLE4 = new TurboSampleData("url4", "title4", "turbo4");
    private static final TurboSampleData SAMPLE5 = new TurboSampleData("url5", "title5", "turbo5");
    private static final TurboSampleData SAMPLE6 = new TurboSampleData("url6", "title6", "turbo6");
    private static final TurboSampleData SAMPLE7 = new TurboSampleData("url7", "title7", "turbo7");
    private static final TurboSampleData SAMPLE8 = new TurboSampleData("url8", "title8", "turbo8");
    private static final TurboSampleData SAMPLE9 = new TurboSampleData("url9", "title9", "turbo9");
    private static final TurboSampleData SAMPLE10 = new TurboSampleData("url10", "title10", "turbo10");

    private static Map<String, String> ownersMap = new HashMap<>();
    static {
        ownersMap.put("wikipedia.org", "wikipedia.org");
        ownersMap.put("ru.wikipedia.org", "wikipedia.org");
        ownersMap.put("eu.wikipedia.org", "wikipedia.org");
        ownersMap.put("a.eu.wikipedia.org", "wikipedia.org");
        ownersMap.put("a.b.eu.wikipedia.org", "wikipedia.org");
        ownersMap.put("a.ru.wikipedia.org", "wikipedia.org");
        ownersMap.put("b.ru.wikipedia.org", "wikipedia.org");
        ownersMap.put("a.b.ru.wikipedia.org", "wikipedia.org");
        ownersMap.put("a.b.c.ru.wikipedia.org", "wikipedia.org");
        ownersMap.put(LENTA, LENTA);
        ownersMap.put(A_LENTA, LENTA);
    }

    @Test
    public void getTLDsChain() {
        List<String> tlds = TurboAutoparserInfoUtil.getParentsChain("wikipedia.org", "wikipedia.org");
        Assert.assertEquals(0, tlds.size());

        tlds = TurboAutoparserInfoUtil.getParentsChain("ru.wikipedia.org", "wikipedia.org");
        Assert.assertEquals(1, tlds.size());
        Assert.assertEquals("wikipedia.org", tlds.get(0));
    }

    @Test
    public void pushUpAutoparsedSamples1() {
        DateTime date = new DateTime("2018-01-01");
        Map<String, TurboAutoparsedHostInfo> data = new HashMap<>();
        data.put(WIKI, new TurboAutoparsedHostInfo(WIKI, OFF, new ArrayList<>(), date, true));
        data.put(RU_WIKI, new TurboAutoparsedHostInfo(RU_WIKI, ON, new ArrayList<>(), date, true));
        data.put(EU_WIKI, new TurboAutoparsedHostInfo(EU_WIKI, ON, Arrays.asList(SAMPLE1, SAMPLE2, SAMPLE3), date, true));
        TurboAutoparserInfoUtil.pushUpAutoparsedSamples(data, ownersMap);
        Assert.assertEquals( 3, data.keySet().size());
        Assert.assertEquals( 3, data.get(WIKI).getSamples().size());
        Assert.assertEquals( AutoparserToggleState.OFF, data.get(WIKI).getCheckboxState());
        Assert.assertEquals( 3, data.get(EU_WIKI).getSamples().size());
        Assert.assertEquals( 0, data.get(RU_WIKI).getSamples().size());

        data.clear();
        data.put(WIKI, new TurboAutoparsedHostInfo(WIKI, OFF, new ArrayList<>(), date, true));
        data.put(RU_WIKI, new TurboAutoparsedHostInfo(RU_WIKI, ON, new ArrayList<>(), date, true));
        data.put(EU_WIKI, new TurboAutoparsedHostInfo(EU_WIKI, ON, Arrays.asList(SAMPLE1, SAMPLE2, SAMPLE3), date, true));
        TurboAutoparserInfoUtil.pushUpAutoparsedSamples(data, ownersMap);
        Assert.assertEquals( 3, data.get(WIKI).getSamples().size());
        Assert.assertEquals( "title1", data.get(WIKI).getSamples().get(0).getTitle());
        Assert.assertEquals( 3, data.get(EU_WIKI).getSamples().size());
        Assert.assertEquals( 0, data.get(RU_WIKI).getSamples().size());

        data.clear();
        data.put(WIKI, new TurboAutoparsedHostInfo(WIKI, OFF, new ArrayList<>(), date, true));
        data.put(RU_WIKI, new TurboAutoparsedHostInfo(RU_WIKI, OFF, Arrays.asList(SAMPLE1, SAMPLE2, SAMPLE3, SAMPLE4, SAMPLE5, SAMPLE1, SAMPLE10, SAMPLE3), date, true));
        data.put(EU_WIKI, new TurboAutoparsedHostInfo(EU_WIKI, ON, Arrays.asList(SAMPLE6, SAMPLE7, SAMPLE8, SAMPLE9), date, true));
        TurboAutoparserInfoUtil.pushUpAutoparsedSamples(data, ownersMap);
        Assert.assertEquals( 10, data.get(WIKI).getSamples().size());
        Assert.assertEquals( "title6", data.get(WIKI).getSamples().get(0).getTitle());
        Assert.assertEquals( "title1", data.get(WIKI).getSamples().get(1).getTitle());
        Assert.assertEquals( "title7", data.get(WIKI).getSamples().get(2).getTitle());
        Assert.assertEquals( 4, data.get(EU_WIKI).getSamples().size());
        Assert.assertEquals( 8, data.get(RU_WIKI).getSamples().size());
        Assert.assertEquals(AutoparserToggleState.OFF, data.get(RU_WIKI).getCheckboxState());
        Assert.assertEquals(AutoparserToggleState.OFF, data.get(WIKI).getCheckboxState());

        data.clear();
        data.put(WIKI, new TurboAutoparsedHostInfo(WIKI, ON, new ArrayList<>(Collections.singletonList(SAMPLE10)), date, true));
        data.put(RU_WIKI, new TurboAutoparsedHostInfo(RU_WIKI, OFF, Arrays.asList(SAMPLE1, SAMPLE2, SAMPLE3, SAMPLE4, SAMPLE5, SAMPLE1, SAMPLE10, SAMPLE3), date, true));
        data.put(EU_WIKI, new TurboAutoparsedHostInfo(EU_WIKI, ON, Arrays.asList(SAMPLE6, SAMPLE7, SAMPLE8, SAMPLE9), date, true));
        TurboAutoparserInfoUtil.pushUpAutoparsedSamples(data, ownersMap);
        Assert.assertEquals( 10, data.get(WIKI).getSamples().size());
        Assert.assertEquals( "title10", data.get(WIKI).getSamples().get(0).getTitle());
        Assert.assertEquals( "title6", data.get(WIKI).getSamples().get(1).getTitle());
        Assert.assertEquals( "title1", data.get(WIKI).getSamples().get(2).getTitle());
        Assert.assertEquals(AutoparserToggleState.ON, data.get(WIKI).getCheckboxState());
    }

    @Test
    public void pushUpAutoparsedSamples2() {
        DateTime date = DateTime.parse("2017-01-01");
        Map<String, TurboAutoparsedHostInfo> data = new HashMap<>();

        data.put(RU_WIKI, new TurboAutoparsedHostInfo(RU_WIKI, ON, new ArrayList<>(Collections.singletonList(SAMPLE1)), date, true));
        data.put(A_B_RU_WIKI, new TurboAutoparsedHostInfo(A_B_RU_WIKI, ON, Arrays.asList(SAMPLE2, SAMPLE3, SAMPLE4), date, true));
        data.put(A_LENTA, new TurboAutoparsedHostInfo(A_LENTA, ON, new ArrayList<>(Collections.singletonList(SAMPLE5)), date, true));
        data.put(LENTA, new TurboAutoparsedHostInfo(LENTA, ON, new ArrayList<>(), date, true));
        data.put(WIKI, new TurboAutoparsedHostInfo(WIKI, ON, new ArrayList<>(), date, true));
        data.put(B_RU_WIKI, new TurboAutoparsedHostInfo(B_RU_WIKI, ON, new ArrayList<>(), date, true));
        TurboAutoparserInfoUtil.pushUpAutoparsedSamples(data, ownersMap);
        Assert.assertEquals(6, data.keySet().size());
        Assert.assertEquals(4, data.get(WIKI).getSamples().size());
        Assert.assertEquals(4, data.get(RU_WIKI).getSamples().size());
        Assert.assertEquals(3, data.get(B_RU_WIKI).getSamples().size());
        Assert.assertEquals(3, data.get(A_B_RU_WIKI).getSamples().size());
        Assert.assertEquals(1, data.get(LENTA).getSamples().size());
        Assert.assertEquals("title5", data.get(LENTA).getSamples().get(0).getTitle());
        Assert.assertEquals(1, data.get(A_LENTA).getSamples().size());
        Assert.assertEquals("title5", data.get(A_LENTA).getSamples().get(0).getTitle());
    }
}
