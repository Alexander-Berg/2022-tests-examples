package ru.yandex.wmconsole.data;

import java.util.Date;

import org.junit.Test;

import ru.yandex.wmconsole.data.info.MainPageErrorInfo;

import static org.junit.Assert.assertEquals;

/**
 * User: azakharov
 * Date: 28.11.12
 * Time: 11:39
 */
public class MainPageErrorInfoTest {
    @Test
    public void testHtarcOkPrevOk() {
        MainPageErrorInfo info = new MainPageErrorInfo(200, new Date(), 200, new Date(), new UserErrorOptions(0L, true));
        assertEquals(MainPageErrorEnum.NONE, info.getMainPageError());
    }
    @Test
    public void testHtarcOkPrevIgnore() {
        MainPageErrorInfo info = new MainPageErrorInfo(200, new Date(), 1000, new Date(), new UserErrorOptions(0L, true));
        assertEquals(MainPageErrorEnum.NONE, info.getMainPageError());
    }
    @Test
    public void testHtarcOkPrevError() {
        MainPageErrorInfo info = new MainPageErrorInfo(200, new Date(), 403, new Date(), new UserErrorOptions(0L, true));
        assertEquals(MainPageErrorEnum.WARNING_MAIN_PAGE_EXCLUDED, info.getMainPageError());
    }

    @Test
    public void testHtarcOkPrevError1() {
        MainPageErrorInfo info = new MainPageErrorInfo(302, new Date(), 403, new Date(), new UserErrorOptions(0L));
        assertEquals(MainPageErrorEnum.WARNING_MAIN_PAGE_EXCLUDED, info.getMainPageError());
    }

    @Test
    public void testHtarcIgnorePrevOk() {
        MainPageErrorInfo info = new MainPageErrorInfo(1003, new Date(), 200, new Date(), new UserErrorOptions(0L, true));
        assertEquals(MainPageErrorEnum.NONE, info.getMainPageError());
    }
    @Test
    public void testHtarcIgnorePrevIgnore() {
        MainPageErrorInfo info = new MainPageErrorInfo(1005, new Date(), 1012, new Date(), new UserErrorOptions(0L));
        assertEquals(MainPageErrorEnum.NONE, info.getMainPageError());
    }
    @Test
    public void testHtarcIgnorePrevError() {
        MainPageErrorInfo info = new MainPageErrorInfo(2016, new Date(), 403, new Date(), new UserErrorOptions(0L));
        assertEquals(MainPageErrorEnum.WARNING_MAIN_PAGE_EXCLUDED, info.getMainPageError());
    }
    @Test
    public void testHtarcErrorPrevOk() {
        MainPageErrorInfo info = new MainPageErrorInfo(1010, new Date(), 200, new Date(), new UserErrorOptions(0L));
        assertEquals(MainPageErrorEnum.WILL_ERROR_MAIN_PAGE_EXCLUDED, info.getMainPageError());
    }
    @Test
    public void testHtarcErrorPrevIgnore() {
        MainPageErrorInfo info = new MainPageErrorInfo(1014, new Date(), 1010, new Date(), new UserErrorOptions(0L));
        assertEquals(MainPageErrorEnum.ERROR_MAIN_PAGE_EXCLUDED, info.getMainPageError());
    }
    @Test
    public void testHtarcErrorPrevError() {
        MainPageErrorInfo info = new MainPageErrorInfo(403, new Date(), 1001, new Date(), new UserErrorOptions(0L));
        assertEquals(MainPageErrorEnum.ERROR_MAIN_PAGE_EXCLUDED, info.getMainPageError());
    }
}
