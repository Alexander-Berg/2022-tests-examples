package ru.yandex.partner.core.transaction;

import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.libs.annotation.PartnerTransactional;


@CoreTest
public class TransactionTest {

    @Configuration
    public static class TransactionalServiceConfiguration {

        @Bean
        public SomeTransactionalService someTransactionalService(DSLContext dslContext) {
            return new SomeTransactionalService(dslContext);
        }

        @Bean
        public SomeOtherTransactionalService someOtherTransactionalService(
                SomeTransactionalService someTransactionalService) {
            return new SomeOtherTransactionalService(someTransactionalService);
        }

    }

    @Autowired
    private DSLContext dslContext;

    @Autowired
    private SomeTransactionalService service;

    @Autowired
    private SomeOtherTransactionalService otherService;

    @BeforeEach
    public void init() {
        dslContext.createTable("test_table")
                .column("test_field", SQLDataType.VARCHAR(50))
                .execute();
    }

    @AfterEach
    public void cleanup() {
        dslContext.dropTable("test_table")
                .execute();
    }

    @Test
    public void testInsertAndGet() {
        Assertions.assertEquals(0, service.get("hello"));
        service.insert("hello");
        Assertions.assertEquals(1, service.get("hello"));
    }

    @Test
    public void testInsertAndFail() {
        Assertions.assertEquals(0, service.get("hello"));
        try {
            service.insertAndFail("hello");
        } catch (Exception e) {
            // that's expected
        }
        Assertions.assertEquals(0, service.get("hello"));
    }

    @Test
    public void testNested() {
        Assertions.assertEquals(0, service.get("foo"));
        Assertions.assertEquals(0, service.get("bar"));
        otherService.insertNested("foo", "bar");
        Assertions.assertEquals(1, service.get("foo"));
        Assertions.assertEquals(0, service.get("bar"));
    }


    public static class SomeTransactionalService {

        private final DSLContext dslContext;

        public SomeTransactionalService(DSLContext dslContext) {
            this.dslContext = dslContext;
        }

        @PartnerTransactional
        public int insert(String value) {
            return dslContext.insertInto(TestTable.TEST_TABLE).set(TestTable.TEST_TABLE.testField, value).execute();
        }

        public int get(String value) {
            return dslContext.select(TestTable.TEST_TABLE.testField)
                    .from(TestTable.TEST_TABLE)
                    .where(TestTable.TEST_TABLE.testField.eq(value))
                    .execute();
        }

        @PartnerTransactional
        public int insertAndFail(String value) throws Exception {
            insert(value);
            throw new Exception("Something happened");
        }
    }

    public static class SomeOtherTransactionalService {

        private final SomeTransactionalService someTransactionalService;

        public SomeOtherTransactionalService(SomeTransactionalService someTransactionalService) {
            this.someTransactionalService = someTransactionalService;
        }

        @PartnerTransactional
        public int insertNested(String value1, String value2) {
            int r = someTransactionalService.insert(value1);
            try {
                someTransactionalService.insertAndFail(value2);
            } catch (Exception exception) {
                //that's ok
            }
            return r;
        }
    }

    public static class TestTable extends TableImpl<Record1<String>> {
        public static final TestTable TEST_TABLE = new TestTable();

        public final TableField<Record1<String>, String> testField =
                createField(DSL.name("test_field"), SQLDataType.VARCHAR(50), this);

        public TestTable() {
            super(DSL.name("test_table"));
        }

    }
}
