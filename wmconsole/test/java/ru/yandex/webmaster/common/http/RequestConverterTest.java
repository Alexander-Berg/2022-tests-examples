package ru.yandex.webmaster.common.http;

import java.util.IdentityHashMap;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import ru.yandex.misc.enums.EnumResolver;

/**
 * @author aherman
 */
public class RequestConverterTest {
    @Test
    public void testExtractQueryParams() throws Exception {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setParameter("username", "user-1");
        mockRequest.setParameter("userId", "123");
        mockRequest.setParameter("hostId", "456");
        mockRequest.setParameter("boolValue", "true");
        mockRequest.setParameter("sort", "ASCENDING");
        mockRequest.setParameter("sort2", "descending");

        TestRequest testRequest = new TestRequest();
        new RequestConverter(new IdentityHashMap<Class, ParameterConverter>()).fillRequest(testRequest, mockRequest);

        Assert.assertEquals("user-1", testRequest.username);
        Assert.assertEquals(123L, testRequest.userId);
        Assert.assertEquals(456L, testRequest.hostId);
        Assert.assertEquals(true, testRequest.boolValue);
        Assert.assertEquals(TestSort.ASCENDING, testRequest.sort);
        Assert.assertEquals(TestSort2.DESCENDING, testRequest.sort2);
    }
/*
{
  "hostId": 456,
  "boolValue": true,
  "sort": "ASCENDING",
  "sort2": "descending",
  "intArray": [1, 2, 3, 4, 5, 6],
  "objArray": [
    { "id": 1, "value": "value-1" },
    { "id": 2, "value": "value-2" },
    { "id": 3, "value": "value-3" }
  ],
  "obj": { "id": 4, "value": "value-4" }
}
 */
    @Test
    public void testPostJson() throws Exception {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setContentType("application/json");
        mockRequest.setCharacterEncoding("UTF-8");
        mockRequest.setParameter("username", "user-1");
        mockRequest.setParameter("userId", "123");

        mockRequest.setContent(
                ("{\n"
                + "  \"hostId\": 456,\n"
                + "  \"boolValue\": true,\n"
                + "  \"sort\": \"ASCENDING\",\n"
                + "  \"sort2\": \"descending\",\n"
                + "  \"intArray\": [1, 2, 3, 4, 5, 6],\n"
                + "  \"objArray\": [\n"
                + "    { \"id\": 1, \"value\": \"value-1\" },\n"
                + "    { \"id\": 2, \"value\": \"value-2\" },\n"
                + "    { \"id\": 3, \"value\": \"value-3\" }\n"
                + "  ],\n"
                + "  \"obj\": { \"id\": 4, \"value\": \"value-4\" }"
                + "}\n"
                ).getBytes()
        );

        TestRequest testRequest = new TestRequest();
        new RequestConverter(new IdentityHashMap<Class, ParameterConverter>()).fillRequest(testRequest, mockRequest);

        Assert.assertEquals("user-1", testRequest.username);
        Assert.assertEquals(123L, testRequest.userId);
        Assert.assertEquals(456L, testRequest.hostId);
        Assert.assertEquals(true, testRequest.boolValue);
        Assert.assertEquals(TestSort.ASCENDING, testRequest.sort);
        Assert.assertEquals(TestSort2.DESCENDING, testRequest.sort2);

        Assert.assertEquals(new TestIdValue(4, "value-4"), testRequest.obj);

        Assert.assertArrayEquals(new Integer[]{1, 2, 3, 4, 5, 6}, testRequest.intArray);
        TestIdValue[] expectedObjArray = new TestIdValue[]{
                new TestIdValue(1, "value-1"),
                new TestIdValue(2, "value-2"),
                new TestIdValue(3, "value-3"),
        };
        Assert.assertArrayEquals(expectedObjArray, testRequest.objArray);
    }

    static class TestRequest {
        long userId;
        String username;
        boolean boolValue;

        int hostId;
        TestSort sort;
        TestSort2 sort2;
        Integer[] intArray;
        TestIdValue[] objArray;
        TestIdValue obj;


        @RequestQueryProperty
        public void setUsername(String username) {
            this.username = username;
        }

        @RequestQueryProperty
        public void setUserId(long userId) {
            this.userId = userId;
        }

        @RequestQueryProperty
        @RequestPostProperty
        public void setHostId(int hostId) {
            this.hostId = hostId;
        }

        @RequestQueryProperty
        @RequestPostProperty
        public void setBoolValue(boolean boolValue) {
            this.boolValue = boolValue;
        }

        @RequestQueryProperty
        @RequestPostProperty
        public void setSort(TestSort sort) {
            this.sort = sort;
        }

        @RequestQueryProperty
        @RequestPostProperty
        public void setSort2(TestSort2 sort2) {
            this.sort2 = sort2;
        }

        @RequestPostProperty
        public void setIntArray(Integer[] intArray) {
            this.intArray = intArray;
        }

        @RequestPostProperty
        public void setObjArray(TestIdValue[] objArray) {
            this.objArray = objArray;
        }

        @RequestPostProperty
        public void setObj(TestIdValue obj) {
            this.obj = obj;
        }
    }

    public static class TestIdValue {
        int id;
        String value;

        public TestIdValue() {
        }

        public TestIdValue(int id, String value) {
            this.id = id;
            this.value = value;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TestIdValue that = (TestIdValue) o;

            if (id != that.id) return false;
            if (value != null ? !value.equals(that.value) : that.value != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = id;
            result = 31 * result + (value != null ? value.hashCode() : 0);
            return result;
        }
    }

    enum TestSort {
        ASCENDING,
        DESCENDING
    }

    enum TestSort2 {
        ASCENDING,
        DESCENDING;

        public static final EnumResolver<TestSort2> R = EnumResolver.er(TestSort2.class);
    }
}
