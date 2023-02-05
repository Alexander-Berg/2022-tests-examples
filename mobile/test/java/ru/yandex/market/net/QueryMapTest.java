package ru.yandex.market.net;

import androidx.annotation.NonNull;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;


import ru.yandex.market.base.network.common.address.QueryMap;
import ru.yandex.market.testcase.DefaultBuilderTestCase;
import ru.yandex.market.testcase.EqualsTestCase;
import ru.yandex.market.testcase.JavaSerializationTestCase;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(Enclosed.class)
public class QueryMapTest {

    public static class EqualsTest extends EqualsTestCase {

        @NonNull
        @Override
        protected Collection<?> getEqualInstances() {
            return Arrays.asList(

                    QueryMap.builder()
                            .add("param1", "value1")
                            .add("param2", "value2")
                            .build(),

                    QueryMap.builder()
                            .add("param2", "value2")
                            .add("param1", "value1")
                            .build(),

                    QueryMap.builder()
                            .add("param1", "value1")
                            .add("param2", "value2")
                            .build()
            );
        }

        @NonNull
        @Override
        protected Collection<?> getUnequalInstances() {
            return Arrays.asList(

                    QueryMap.builder()
                            .add("param1", "value1")
                            .add("param2", "value2")
                            .build(),

                    QueryMap.builder()
                            .add("param2", "value2")
                            .add("param1", "VALUE")
                            .build(),

                    QueryMap.builder()
                            .add("param1", "value1")
                            .build()
            );
        }
    }

    public static class DefaultBuilderTest extends DefaultBuilderTestCase {

        @Override
        protected void buildDefaultInstance() {
            QueryMap.builder().build();
        }

        @Override
        protected Class<?> getObjectClass() {
            return QueryMap.class;
        }
    }

    public static class SerializationTest extends JavaSerializationTestCase {

        @NonNull
        @Override
        protected Object getInstance() {
            return QueryMap.builder()
                    .add("name", "value")
                    .build();
        }
    }

    public static class BuilderTest {

        @Test
        public void testSetMethod() {
            final List<QueryMap.Entry> entries = Arrays.asList(
                    QueryMap.Entry.create("1", "1"),
                    QueryMap.Entry.create("2", "2"),
                    QueryMap.Entry.create("3", "3"),
                    QueryMap.Entry.create("4", "4"),
                    QueryMap.Entry.create("1", "1")
            );
            final QueryMap queryMap = QueryMap.builder()
                    .entries(entries)
                    .build();

            final QueryMap newMap = queryMap.toBuilder()
                    .setValues("1", Arrays.asList("!", "!", "!"))
                    .build();

            final List<QueryMap.Entry> expectedEntries = Arrays.asList(
                    QueryMap.Entry.create("1", "!"),
                    QueryMap.Entry.create("2", "2"),
                    QueryMap.Entry.create("3", "3"),
                    QueryMap.Entry.create("4", "4"),
                    QueryMap.Entry.create("1", "!"),
                    QueryMap.Entry.create("1", "!")
            );

            assertThat(newMap.getEntries(), equalTo(expectedEntries));
        }
    }
}