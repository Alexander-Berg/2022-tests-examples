//package ru.yandex.webmaster3.storage.importanturls;
//
//import org.joda.time.DateTime;
//import org.joda.time.LocalDate;
//import org.junit.Assert;
//import org.junit.Test;
//import ru.yandex.webmaster3.core.data.WebmasterHostId;
//import ru.yandex.webmaster3.core.util.IdUtils;
//import ru.yandex.webmaster3.storage.importanturls.ImportantUrlsStatusesCHDao.Field;
//import ru.yandex.webmaster3.storage.importanturls.data.ImportantUrlStatus;
//import ru.yandex.webmaster3.storage.util.clickhouse2.CHRow;
//import ru.yandex.webmaster3.core.util.http.YandexMimeType;
//import ru.yandex.wmtools.common.util.http.YandexHttpStatus;
//
//import java.util.EnumMap;
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * @author Oleg Bazdyrev
// */
//public class ImportantUrlsStatusesCHDaoTest {
//
//
//    @Test
//    public void testStatusFromChRow_shouldNotFailOn32767() {
//        DateTime now = DateTime.now();
//        CHRowMock rowMock = new CHRowMock();
//        rowMock.put(Field.SPREAD_MIME_TYPE, 0x7fff);
//        rowMock.put(Field.SPREAD_HTTP_CODE, 0);
//        rowMock.put(Field.SPREAD_LAST_ACCESS, now);
//        rowMock.put(Field.HTTP_CODE, 0x7fff);
//        rowMock.put(Field.HOST, "https:lenta.ru:443");
//        rowMock.put(Field.TITLE_CHANGED, 0);
//        rowMock.put(Field.SPREAD_HTTP_CODE_CHANGED, 0);
//        rowMock.put(Field.URL_STATUS_CHANGED, 1);
//        rowMock.put(Field.UPDATE_TIME, now);
//
//        ImportantUrlStatus status = ImportantUrlsStatusesCHDao.statusFromChRow(rowMock, CHRowMock.FIELD_IDS);
//        Assert.assertNull(status.getSearchInfo());
//        Assert.assertNotNull(status.getIndexingInfo());
//        Assert.assertEquals(YandexMimeType.UNKNOWN, status.getIndexingInfo().getMimeType());
//        Assert.assertEquals(YandexHttpStatus.UNKNOWN, status.getIndexingInfo().getHttpCode().getStatus());
//    }
//
//    private static class CHRowMock extends CHRow {
//
//        private Map<Field, Object> values = new EnumMap<>(Field.class);
//        public static final EnumMap<Field, Integer> FIELD_IDS = new EnumMap<>(Field.class);
//        static {
//            for (Field field : Field.values()) {
//                FIELD_IDS.put(field, field.ordinal());
//            }
//        }
//
//        public CHRowMock() {
//            super(null, null, null, null);
//        }
//
//        public void put(Field field, Object value) {
//            values.put(field, value);
//        }
//
//        @Override
//        public int getInt(int id) {
//            return (Integer) values.get(Field.values()[id]);
//        }
//
//        @Override
//        public DateTime getDateTime(int id) {
//            return (DateTime) values.get(Field.values()[id]);
//        }
//
//        @Override
//        public String getString(int id) {
//            return (String) values.get(Field.values()[id]);
//        }
//    }
//}
