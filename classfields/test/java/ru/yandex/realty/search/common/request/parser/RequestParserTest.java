package ru.yandex.realty.search.common.request.parser;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.common.util.collections.Cf;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.realty.model.geometry.Polygon;
import ru.yandex.realty.model.location.GeoPoint;
import ru.yandex.realty.model.offer.CommercialType;
import ru.yandex.realty.model.offer.Rooms;
import ru.yandex.realty.proto.offer.ConstructionState;
import ru.yandex.realty.search.common.DeliveryDate;
import ru.yandex.realty.search.common.DeliveryDate$;
import ru.yandex.realty.util.MockUtils;
import ru.yandex.realty.util.Range;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.Assert.fail;

/**
 * @author aherman
 */
public class RequestParserTest {
    private RequestParser<Query> parser;

    @Before
    public void setup() {
        parser = RequestParser.createParser(Query.class);
    }

    @Test
    public void testBooleanPrimitive() throws RequestParsingException {
        Query query = parser.parse(createParameterSource());
        Assert.assertEquals(false, query.isBoolPrimitive());

        query = parser.parse(createParameterSource("boolPrimitive", "YES"));
        Assert.assertEquals(true, query.isBoolPrimitive());

        query = parser.parse(createParameterSource("boolPrimitive", "NO"));
        Assert.assertEquals(false, query.isBoolPrimitive());
    }

    @Test
    public void testBooleanPrimitiveDefault() throws RequestParsingException {
        Query query = parser.parse(createParameterSource());
        Assert.assertEquals(true, query.getBoolClassDefaults());
    }

    @Test
    public void testBooleanClass() throws RequestParsingException {
        Query query = parser.parse(createParameterSource());
        Assert.assertNull(query.getBoolClass());

        query = parser.parse(createParameterSource("boolClass", ""));
        Assert.assertNull(query.getBoolClass());

        query = parser.parse(createParameterSource("boolClass", "YES"));
        Assert.assertEquals(Boolean.TRUE, query.getBoolClass());

        query = parser.parse(createParameterSource("boolClass", "NO"));
        Assert.assertEquals(Boolean.FALSE, query.getBoolClass());
    }

    @Test
    public void testBooleanUnknown() throws Exception {
        try {
            parser.parse(createParameterSource("boolPrimitive", "qwe"));
            fail("Parser must not accept unknown values");
        } catch (RequestParsingException e) {}
    }

    @Test
    public void testBooleanClassDefault() throws RequestParsingException {
        Query query = parser.parse(createParameterSource());
        Assert.assertEquals(Boolean.TRUE, query.getBoolClassDefaults());
    }

    @Test
    public void testIntegerPrimitive() throws RequestParsingException {
        Query query = parser.parse(createParameterSource());
        Assert.assertEquals(0, query.getIntPrimitive());

        query = parser.parse(createParameterSource("intPrimitive", "1"));
        Assert.assertEquals(1, query.getIntPrimitive());

        query = parser.parse(createParameterSource("intPrimitive", "10000000"));
        Assert.assertEquals(10000000, query.getIntPrimitive());

        query = parser.parse(createParameterSource("intPrimitive", "0"));
        Assert.assertEquals(0, query.getIntPrimitive());

    }

    @Test
    public void testIntegerPrimitiveDefault() throws RequestParsingException {
        Query query = parser.parse(createParameterSource());
        Assert.assertEquals(12, query.getIntPrimitiveDefaults());
    }

    @Test
    public void testIntegerClass() throws RequestParsingException {
        Query query = parser.parse(createParameterSource());
        Assert.assertNull(query.getIntClass());

        query = parser.parse(createParameterSource("intClass", ""));
        Assert.assertNull(query.getIntClass());

        query = parser.parse(createParameterSource("intClass", "1"));
        Assert.assertEquals(1, query.getIntClass().intValue());

        query = parser.parse(createParameterSource("intClass", "10000000"));
        Assert.assertEquals(10000000, query.getIntClass().intValue());

        query = parser.parse(createParameterSource("intClass", "0"));
        Assert.assertEquals(0, query.getIntClass().intValue());
    }

    @Test
    public void testIntegerClassDefaults() throws RequestParsingException {
        Query query = parser.parse(createParameterSource());
        Assert.assertEquals(Integer.valueOf(34), query.getIntClassDefaults());
    }

    @Test
    public void testIntegerUnknown() throws Exception {
        try {
            parser.parse(createParameterSource("intClass", "qwe"));
            fail("Parser must not accept unknown values");
        } catch (RequestParsingException e) {}
    }

    @Test
    public void testFloatPrimitive() throws RequestParsingException {
        Query query = parser.parse(createParameterSource());
        Assert.assertEquals(0, query.getFloatPrimitive(), 0.001f);

        query = parser.parse(createParameterSource("floatPrimitive", "1"));
        Assert.assertEquals(1, query.getFloatPrimitive(), 0.001f);

        query = parser.parse(createParameterSource("floatPrimitive", "10000000"));
        Assert.assertEquals(10000000, query.getFloatPrimitive(), 0.001f);

        query = parser.parse(createParameterSource("floatPrimitive", "10.0034"));
        Assert.assertEquals(10.0034f, query.getFloatPrimitive(), 0.000001f);

        query = parser.parse(createParameterSource("floatPrimitive", "0"));
        Assert.assertEquals(0, query.getFloatPrimitive(), 0.001f);
    }

    @Test
    public void testFloatPrimitiveDefault() throws RequestParsingException {
        Query query = parser.parse(createParameterSource());
        Assert.assertEquals(0.123f, query.getFloatPrimitiveDefaults(), 0.0001f);
    }

    @Test
    public void testFloatClass() throws RequestParsingException {
        Query query = parser.parse(createParameterSource());
        Assert.assertNull(query.getFloatClass());

        query = parser.parse(createParameterSource("floatClass", ""));
        Assert.assertNull(query.getFloatClass());

        query = parser.parse(createParameterSource("floatClass", "1"));
        Assert.assertEquals(1.0f, query.getFloatClass(), 0.001f);

        query = parser.parse(createParameterSource("floatClass", "10000000"));
        Assert.assertEquals(10000000.0f, query.getFloatClass(), 0.001f);

        query = parser.parse(createParameterSource("floatClass", "10.0034"));
        Assert.assertEquals(10.0034f, query.getFloatClass(), 0.000001f);

        query = parser.parse(createParameterSource("floatClass", "0"));
        Assert.assertEquals(0.0f, query.getFloatClass(), 0.001f);
    }

    @Test
    public void testFloatClassDefault() throws RequestParsingException {
        Query query = parser.parse(createParameterSource());
        Assert.assertEquals(0.345f, query.getFloatClassDefaults(), 0.00001f);
    }

    @Test
    public void testFloatUnknown() throws Exception {
        try {
            parser.parse(createParameterSource("floatClass", "qwe"));
            fail("Parser must not accept unknown values");
        } catch (RequestParsingException e) {}
    }

    @Test
    public void testString() throws RequestParsingException {
        Query query = parser.parse(createParameterSource());
        Assert.assertNull(query.getString());

        query = parser.parse(createParameterSource("string", ""));
        Assert.assertNull(query.getString());

        query = parser.parse(createParameterSource("string", "eqqwe"));
        Assert.assertEquals("eqqwe", query.getString());
    }

    @Test
    public void testStringDefault() throws RequestParsingException {
        Query query = parser.parse(createParameterSource());
        Assert.assertEquals("qwe", query.getStringDefaults());
    }

    @Test
    public void testCurrency() throws RequestParsingException {
        Query query = parser.parse(createParameterSource());
        Assert.assertNull(query.getCurrency());

        query = parser.parse(createParameterSource("currency", ""));
        Assert.assertNull(query.getCurrency());

        query = parser.parse(createParameterSource("currency", "RUB"));
        Assert.assertEquals(Currency.RUR, query.getCurrency());

        query = parser.parse(createParameterSource("currency", "RUR"));
        Assert.assertEquals(Currency.RUR, query.getCurrency());

        query = parser.parse(createParameterSource("currency", "USD"));
        Assert.assertEquals(Currency.USD, query.getCurrency());
    }

    @Test
    public void testPolygon() throws RequestParsingException {
        Query query = parser.parse(createParameterSource());
        Assert.assertNull(query.getPolygon());

        query = parser.parse(createParameterSource("polygon", ""));
        Assert.assertNull(query.getPolygon());

        query = parser.parse(createParameterSource("polygon",
                "55.68025309651403,37.4144239296875;" +
                        "55.757962617096716,37.36532877587887;" +
                        "55.81696437057112,37.3931379189453;" +
                        "55.86680365716931,37.39657114648438;" +
                        "55.91233747544306,37.5634260048828;" +
                        "55.89343587819687,37.70384501123042;"));
        Assert.assertNotNull(query.getPolygon());
        Assert.assertEquals(6, query.getPolygon().length());

        query = parser.parse(createParameterSource("polygon",
                "55.68025309651403,37.4144239296875;" +
                        "55.757962617096716,37.36532877587887;" +
                        "55.81696437057112,37.3931379189453;" +
                        "55.86680365716931,37.39657114648438;" +
                        "55.91233747544306,37.5634260048828;" +
                        "55.89343587819687,37.70384501123042;" +
                        "55.68025309651403,37.4144239296875"));
        Assert.assertNotNull(query.getPolygon());
        Assert.assertEquals(6, query.getPolygon().length());
    }

    @Test
    public void testCurrencyDefault() throws RequestParsingException {
        Query query = parser.parse(createParameterSource());
        Assert.assertEquals(Currency.GMD, query.getCurrencyDefaults());
    }

    @Test
    public void testCurrencyUnknown() throws Exception {
        try {
            parser.parse(createParameterSource("currency", "qweq"));
            fail("Parser must not accept unknown values");
        } catch (RequestParsingException e) {}
    }

    @Test
    public void testRange() throws RequestParsingException {
        Query query = parser.parse(createParameterSource());
        Assert.assertNull(query.getRange());

        query = parser.parse(createParameterSource("rangeMin", "10"));
        Assert.assertEquals(Range.create(10f, null), query.getRange());

        query = parser.parse(createParameterSource("rangeMin", "10", "rangeMax", ""));
        Assert.assertEquals(Range.create(10f, null), query.getRange());

        query = parser.parse(createParameterSource("rangeMax", "20"));
        Assert.assertEquals(Range.create(null, 20f), query.getRange());

        query = parser.parse(createParameterSource("rangeMin", "", "rangeMax", "20"));
        Assert.assertEquals(Range.create(null, 20f), query.getRange());

        query = parser.parse(createParameterSource("rangeMin", "10", "rangeMax", "20"));
        Assert.assertEquals(Range.create(10f, 20f), query.getRange());

        query = parser.parse(createParameterSource("rangeMin", "", "rangeMax", ""));
        Assert.assertNull(query.getCurrency());
    }

    @Test
    public void testRangeDefault() throws RequestParsingException {
        Query query = parser.parse(createParameterSource());
        Assert.assertEquals(Range.OPEN_RANGE, query.getRangeDefaults());
    }

    @Test
    public void testGeoPoint() throws RequestParsingException {
        Query query = parser.parse(createParameterSource());
        Assert.assertNull(query.getGeoPoint());

        query = parser.parse(createParameterSource("geoPointLatitude", "10"));
        Assert.assertNull(query.getGeoPoint());

        query = parser.parse(createParameterSource("geoPointLatitude", "10", "geoPointLongitude", ""));
        Assert.assertNull(query.getGeoPoint());

        query = parser.parse(createParameterSource("geoPointLongitude", "20"));
        Assert.assertNull(query.getGeoPoint());

        query = parser.parse(createParameterSource("geoPointLatitude", "", "geoPointLongitude", "20"));
        Assert.assertNull(query.getGeoPoint());

        query = parser.parse(createParameterSource("geoPointLatitude", "", "geoPointLongitude", ""));
        Assert.assertNull(query.getGeoPoint());

        query = parser.parse(createParameterSource("geoPointLatitude", "10", "geoPointLongitude", "20"));
        Assert.assertEquals(GeoPoint.getPoint(10.0f, 20.0f), query.getGeoPoint());
    }

    @Test
    public void testGeoPointDefault() throws RequestParsingException {
        Query query = parser.parse(createParameterSource());
        Assert.assertEquals(GeoPoint.getPoint(1.0f, 1.0f), query.getGeoPointDefault());
    }

    @Test
    public void testEnum() throws RequestParsingException {
        Query query = parser.parse(createParameterSource());
        Assert.assertNull(query.getRoom());

        query = parser.parse(createParameterSource("room", ""));
        Assert.assertNull(query.getRoom());

        query = parser.parse(createParameterSource("room", Rooms._3.name()));
        Assert.assertEquals(Rooms._3, query.getRoom());

        query = parser.parse(createParameterSource("room", Rooms.PLUS_4.name()));
        Assert.assertEquals(Rooms.PLUS_4, query.getRoom());

        query = parser.parse(createParameterSource("room", Rooms.PLUS_7.name()));
        Assert.assertEquals(Rooms.PLUS_7, query.getRoom());
    }

    @Test
    public void testEnumDefault() throws RequestParsingException {
        Query query = parser.parse(createParameterSource());
        Assert.assertEquals(Rooms._7, query.getRoomDefaults());
    }

    @Test
    public void testEnumUnknown() throws Exception {
        try {
            parser.parse(createParameterSource("room", "qweqwe"));
            fail("Parser must not accept unknown values");
        } catch (RequestParsingException e) {}
    }

    @Test
    public void testList() throws RequestParsingException {
        Query query = parser.parse(createParameterSource());
        Assert.assertNull(query.getRoom());

        query = parser.parse(createParameterSource("roomList", ""));
        Assert.assertNull(query.getRoom());

        query = parser.parse(createParameterSource("roomList", Rooms._3.name()));
        Assert.assertEquals(Cf.list(Rooms._3), query.getRoomList());

        query = parser.parse(createParameterSourceMulti(
                "roomList", Cf.list(Rooms._2.name(), Rooms.PLUS_3.name()))
        );
        Assert.assertEquals(Cf.list(Rooms._2, Rooms.PLUS_3), query.getRoomList());
    }

    @Test
    public void testEnumSet() throws RequestParsingException {
        Query query = parser.parse(createParameterSource());
        Assert.assertTrue(query.getCommercialType().isEmpty());

        query = parser.parse(createParameterSource("commercialType", ""));
        Assert.assertTrue(query.getCommercialType().isEmpty());


        query = parser.parse(createParameterSource("commercialType", "RETAIL"));
        Assert.assertEquals(query.getCommercialType().size(), 1);
        Assert.assertTrue(query.getCommercialType().contains(CommercialType.RETAIL));
    }

    @Test
    public void testPrefixedProtoEnumSet() throws RequestParsingException {
        Query query = parser.parse(createParameterSource());
        Assert.assertTrue(query.getConstructionState().isEmpty());

        query = parser.parse(createParameterSource("constructionState", ""));
        Assert.assertTrue(query.getConstructionState().isEmpty());

        query = parser.parse(createParameterSource("constructionState", "IN_PROJECT"));
        Assert.assertEquals(1, query.getConstructionState().size());
        Assert.assertTrue(query.getConstructionState().contains(ConstructionState.CONSTRUCTION_STATE_IN_PROJECT));

        query = parser.parse(createParameterSourceMulti("constructionState", asList("IN_PROJECT", "HAND_OVER")));
        Assert.assertEquals(2, query.getConstructionState().size());
        Assert.assertTrue(query.getConstructionState().contains(ConstructionState.CONSTRUCTION_STATE_IN_PROJECT));
        Assert.assertTrue(query.getConstructionState().contains(ConstructionState.CONSTRUCTION_STATE_HAND_OVER));
    }

    @Test
    public void testDeliveryDate() throws RequestParsingException  {
        Query query = parser.parse(createParameterSource());
        Assert.assertTrue(query.getDeliveryDate() == null);

        query = parser.parse(createParameterSource("deliveryDate", "2_2016"));
        Assert.assertEquals(query.getDeliveryDate().toString(), "2_2016");
    }


    private static StringParametersSource createParameterSource(String... values) {
        return createParameterSource(MockUtils.<String, String>map(values), Cf.<String, List<String>>newHashMap());
    }

    private static StringParametersSource createParameterSourceMulti(Object... values) {
        return createParameterSource(Cf.<String, String>newHashMap(), MockUtils.<String, List<String>>map(values));
    }

    private static StringParametersSource createParameterSource(final Map<String, String> singleValues,
            final Map<String, List<String>> multiValues) {
        return new StringParametersSource() {

            @Override
            public String getParam(String paramName) {
                return getParam(paramName, false);
            }

            @Override
            public String getParam(String paramName, boolean nullable) {
                String value = singleValues.get(paramName);
                if (value == null) {
                    return nullable ? null : "";
                }
                return value;
            }

            @NotNull
            @Override
            public List<String> getMultiParams(String paramName) {
                List<String> values = multiValues.get(paramName);
                if (values != null) {
                    return values;
                }
                String value = singleValues.get(paramName);
                if (value != null) {
                    return Cf.list(value);
                }
                return Collections.emptyList();
            }
        };
    }

    public static class Query {
        private boolean boolPrimitive;
        private boolean boolPrimitiveDefaults = true;
        private Boolean boolClass;
        private Boolean boolClassDefaults = Boolean.TRUE;

        private Polygon polygon;
        private Currency currency;
        private Currency currencyDefaults = Currency.GMD;

        private Rooms room;
        private Rooms roomDefaults = Rooms._7;
        private List<Rooms> roomList = Cf.newArrayList();

        private float floatPrimitive;
        private float floatPrimitiveDefaults = 0.123f;
        private Float floatClass;
        private Float floatClassDefaults = 0.345f;

        private int intPrimitive;
        private int intPrimitiveDefaults = 12;
        private Integer intClass;
        private Integer intClassDefaults = 34;

        private Range range;
        private Range rangeDefaults = Range.OPEN_RANGE;

        private GeoPoint geoPoint;
        private GeoPoint geoPointDefault = GeoPoint.getPoint(1.0f, 1.0f);

        private String string;
        private String stringDefaults = "qwe";

        private DeliveryDate deliveryDate;

        private Set<CommercialType> commercialType = Cf.newHashSet();

        private final Set<ConstructionState> constructionState = EnumSet.noneOf(ConstructionState.class);

        public Polygon getPolygon() {
            return polygon;
        }

        public void setPolygon(Polygon polygon) {
            this.polygon = polygon;
        }

        public boolean isBoolPrimitive() {
            return boolPrimitive;
        }

        public void setBoolPrimitive(boolean boolPrimitive) {
            this.boolPrimitive = boolPrimitive;
        }

        public boolean isBoolPrimitiveDefaults() {
            return boolPrimitiveDefaults;
        }

        public void setBoolPrimitiveDefaults(boolean boolPrimitiveDefaults) {
            this.boolPrimitiveDefaults = boolPrimitiveDefaults;
        }

        public Boolean getBoolClass() {
            return boolClass;
        }

        public void setBoolClass(Boolean boolClass) {
            this.boolClass = boolClass;
        }

        public Boolean getBoolClassDefaults() {
            return boolClassDefaults;
        }

        public void setBoolClassDefaults(Boolean boolClassDefaults) {
            this.boolClassDefaults = boolClassDefaults;
        }

        public Currency getCurrency() {
            return currency;
        }

        public void setCurrency(Currency currency) {
            this.currency = currency;
        }

        public Currency getCurrencyDefaults() {
            return currencyDefaults;
        }

        public void setCurrencyDefaults(Currency currencyDefaults) {
            this.currencyDefaults = currencyDefaults;
        }

        public Rooms getRoom() {
            return room;
        }

        public void setRoom(Rooms room) {
            this.room = room;
        }

        public Rooms getRoomDefaults() {
            return roomDefaults;
        }

        public void setRoomDefaults(Rooms roomDefaults) {
            this.roomDefaults = roomDefaults;
        }

        public List<Rooms> getRoomList() {
            return roomList;
        }

        public void setRoomList(Rooms room) {
            this.roomList.add(room);
        }

        public float getFloatPrimitive() {
            return floatPrimitive;
        }

        public void setFloatPrimitive(float floatPrimitive) {
            this.floatPrimitive = floatPrimitive;
        }

        public float getFloatPrimitiveDefaults() {
            return floatPrimitiveDefaults;
        }

        public void setFloatPrimitiveDefaults(float floatPrimitiveDefaults) {
            this.floatPrimitiveDefaults = floatPrimitiveDefaults;
        }

        public Float getFloatClass() {
            return floatClass;
        }

        public void setFloatClass(Float floatClass) {
            this.floatClass = floatClass;
        }

        public Float getFloatClassDefaults() {
            return floatClassDefaults;
        }

        public void setFloatClassDefaults(Float floatClassDefaults) {
            this.floatClassDefaults = floatClassDefaults;
        }

        public int getIntPrimitive() {
            return intPrimitive;
        }

        public void setIntPrimitive(int intPrimitive) {
            this.intPrimitive = intPrimitive;
        }

        public int getIntPrimitiveDefaults() {
            return intPrimitiveDefaults;
        }

        public void setIntPrimitiveDefaults(int intPrimitiveDefaults) {
            this.intPrimitiveDefaults = intPrimitiveDefaults;
        }

        public Integer getIntClass() {
            return intClass;
        }

        public void setIntClass(Integer intClass) {
            this.intClass = intClass;
        }

        public Integer getIntClassDefaults() {
            return intClassDefaults;
        }

        public void setIntClassDefaults(Integer intClassDefaults) {
            this.intClassDefaults = intClassDefaults;
        }

        public Range getRange() {
            return range;
        }

        public void setRange(Range range) {
            this.range = range;
        }

        public Range getRangeDefaults() {
            return rangeDefaults;
        }

        public void setRangeDefaults(Range rangeDefaults) {
            this.rangeDefaults = rangeDefaults;
        }

        public GeoPoint getGeoPoint() {
            return geoPoint;
        }

        public void setGeoPoint(GeoPoint geoPoint) {
            this.geoPoint = geoPoint;
        }

        public GeoPoint getGeoPointDefault() {
            return geoPointDefault;
        }

        public void setGeoPointDefault(GeoPoint geoPointDefault) {
            this.geoPointDefault = geoPointDefault;
        }

        public String getString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }

        public String getStringDefaults() {
            return stringDefaults;
        }

        public void setStringDefaults(String stringDefaults) {
            this.stringDefaults = stringDefaults;
        }

        public Set<CommercialType> getCommercialType() {
            return commercialType;
        }

        public void setCommercialType(CommercialType commercialType) {
            this.commercialType.add(commercialType);
        }

        public DeliveryDate getDeliveryDate() {
            return deliveryDate;
        }

        public void setDeliveryDate(String deliveryDate) {
            this.deliveryDate = DeliveryDate$.MODULE$.apply(deliveryDate);
        }

        public Set<ConstructionState> getConstructionState() {
            return constructionState;
        }

        public void setConstructionState(ConstructionState state) {
            constructionState.add(state);
        }
    }
}
