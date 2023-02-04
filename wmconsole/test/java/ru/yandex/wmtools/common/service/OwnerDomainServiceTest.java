package ru.yandex.wmtools.common.service;

import org.junit.Test;
import ru.yandex.wmtools.common.error.InternalException;

import static org.junit.Assert.assertEquals;

/**
 * User: azakharov
 * Date: 07.02.14
 * Time: 18:14
 */
public class OwnerDomainServiceTest {

    @Test
    public void testWwwOwnerDomain() throws InternalException {
        assertEquals("domain.ru", OwnerDomainService.getOwnerDomain("www.domain.ru"));
    }

    @Test
    public void testWwwCaseOwnerDomain() throws InternalException {
        assertEquals("DoMain.ru", OwnerDomainService.getOwnerDomain("www.DoMain.ru"));
    }

    @Test
    public void testWwwOwnerHost() throws InternalException {
        assertEquals("domain.ru", OwnerDomainService.getOwnerHost("www.domain.ru"));
    }

    @Test
    public void testWwwCaseOwnerHost() throws InternalException {
        assertEquals("DoMain.ru", OwnerDomainService.getOwnerDomain("www.DoMain.ru"));
    }

    @Test
    public void testWwwPortOwnerHost() throws InternalException {
        assertEquals("domain.ru:81", OwnerDomainService.getOwnerHost("www.domain.ru:81"));
    }

    @Test
    public void testWwwProtocolOwnerHost() throws InternalException {
        assertEquals("https://domain.ru", OwnerDomainService.getOwnerHost("https://www.domain.ru"));
    }

    @Test
    public void testWwwCaseProtocolOwnerHost() throws InternalException {
        assertEquals("https://Domain.ru", OwnerDomainService.getOwnerHost("https://www.Domain.ru"));
    }

    @Test
    public void testWwwCasePortOwnerHost() throws InternalException {
        assertEquals("Domain.ru:81", OwnerDomainService.getOwnerHost("www.Domain.ru:81"));
    }

    @Test
    public void testWwwPunycodeOwnerHost() throws InternalException {
        assertEquals("xn--d1abbgf6aiiy.xn--p1ai", OwnerDomainService.getOwnerHost("www.xn--d1abbgf6aiiy.xn--p1ai"));
    }

    @Test
    public void testWwwCyrOwnerHost() throws InternalException {
        assertEquals("президент.рф", OwnerDomainService.getOwnerHost("www.президент.рф"));
    }

    @Test
    public void testWwwCaseProtocolPortOwnerHost() throws InternalException {
        assertEquals("https://Domain.ru:81", OwnerDomainService.getOwnerHost("https://www.Domain.ru:81"));
    }
}
