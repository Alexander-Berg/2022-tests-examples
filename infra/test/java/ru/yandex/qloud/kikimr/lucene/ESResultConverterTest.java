package ru.yandex.qloud.kikimr.lucene;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.kikimr.proto.Minikql;
import ru.yandex.qloud.kikimr.transport.YQL;

import java.nio.charset.Charset;
import java.util.Map;

/**
 * @author violin
 */
public class ESResultConverterTest {
    private ESResultConverter esResultConverter = new ESResultConverter();

    @Test
    public void testFields() {
        YQL.Result result = new YQL.Result(
                Lists.newArrayList("fields"),
                Lists.newArrayList(newRow(tByteValue("{ \"test\": 1 }")))
        );
        ESResultConverter.ESResult esResult = esResultConverter.convert(result);
        Assert.assertEquals(
                ((JsonNode) getFirstHitSource(esResult).get("@fields")).get("test").asInt(),
                1
        );
    }

    @Test
    public void testEmptyFields() {
        YQL.Result result = new YQL.Result(
                Lists.newArrayList("fields"),
                Lists.newArrayList(newRow(tByteValue("")))
        );
        ESResultConverter.ESResult esResult = esResultConverter.convert(result);
        Assert.assertEquals(
                ((JsonNode) getFirstHitSource(esResult).get("@fields")).size(),
                0
        );
    }

    @Test
    public void testTimestamp() {
        String tsValue = "2017-08-26T23:59:55.084+00:00";

        YQL.Result result = new YQL.Result(
                Lists.newArrayList("timestamp", "timestamp_raw"),
                Lists.newArrayList(newRow(tIntValue(-12345), tByteValue(tsValue)))
        );
        ESResultConverter.ESResult esResult = esResultConverter.convert(result);
        Assert.assertEquals(
                getFirstHitSource(esResult).get("@timestamp"),
                tsValue
        );
    }

    private Minikql.TValue.Builder vBuilder() {
        return Minikql.TValue.newBuilder();
    }

    private YQL.Row newRow(Minikql.TValue... supp) {
        Minikql.TValue.Builder rowValue = vBuilder();
        for (Minikql.TValue v : supp) {
            rowValue.addStruct(vBuilder().setOptional(v).build());
        }
        return new YQL.Row(rowValue.build(), supp.length);
    }

    private Minikql.TValue tByteValue(String s) {
        return vBuilder().setBytes(ByteString.copyFrom(s, Charset.defaultCharset())).build();
    }

    private Minikql.TValue tIntValue(int i) {
        return vBuilder().setInt32(i).build();
    }

    private Map<String, Object> getFirstHitSource(ESResultConverter.ESResult esResult) {
        return esResult.getHits().getHits().get(0).getSource();
    }
}
