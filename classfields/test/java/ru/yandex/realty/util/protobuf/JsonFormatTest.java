package ru.yandex.realty.util.protobuf;

import org.json.JSONObject;
import org.junit.Test;
import ru.yandex.realty.call.CallStat;
import ru.yandex.realty.proto.AreaUnit;
import ru.yandex.realty.proto.search.inexact.AreaInexact;
import ru.yandex.realty.proto.search.inexact.DiffTrend;

import static junit.framework.Assert.assertEquals;

public class JsonFormatTest {

    @Test
    public void testPrintDefault() throws Exception {
        CallStat stat = CallStat.newBuilder()
                .setTotal(2)
                .setMissed(2)
                .build();

        JsonFormat.Printer printer = JsonFormat.printer();
        String s = printer.print(stat);

        JSONObject json = new JSONObject(s);
        assertEquals(2, json.getInt("total"));
        assertEquals(2, json.getInt("missed"));
        assertEquals(0, json.getInt("success"));
        assertEquals(0, json.getInt("target"));
        assertEquals(0, json.getInt("nonTarget"));
        assertEquals(0, json.getInt("blocked"));
    }

    @Test
    public void  testPrefixTrim() throws Exception {
        AreaInexact areaInexact = AreaInexact.newBuilder()
                .setTrend(DiffTrend.MORE)
                .setDiff(1)
                .setValue(9000)
                .setUnit(AreaUnit.AREA_UNIT_SQ_M)
                .build();

        JsonFormat.Printer printer = JsonFormat.printer();
        String s = printer.print(areaInexact);

        JSONObject json = new JSONObject(s);
        assertEquals("SQ_M", json.getString("unit"));
    }

}