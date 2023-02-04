package ru.yandex.wmconsole.data.info;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.wmconsole.data.HostInfoStatusEnum;

/**
 * User: azakharov
 * Date: 25.04.13
 * Time: 13:06
 */
public class HostStatusInfoTest {

    private static final HostInfoStatusEnum NONE = null;

    private void checkStatus(
            final HostInfoStatusEnum statusHtarc,
            final HostInfoStatusEnum statusPrev,
            final HostInfoStatusEnum none,
            final Integer penaltyHtarc,
            final Boolean htarcInIndex,
            final Integer penaltyYa,
            final Boolean prevInIndex,
            final HostInfoStatusEnum expected) {
        Object o;
        HostStatusInfo hostStatusInfo = new HostStatusInfo(
                statusHtarc, statusPrev, none,
                penaltyHtarc, penaltyYa, htarcInIndex, prevInIndex);
        Assert.assertEquals(expected, hostStatusInfo.getCalculatedHostInfoStatus());
    }

    private void checkCalculatedStatus(
            HostInfoStatusEnum status,
            HostInfoStatusEnum none,
            Integer penaltyHtarcTrue,
            Integer penaltyHtarcFalse,
            Integer penaltyPrevTrue,
            Integer penaltyPrevFalse) {

        HostInfoStatusEnum outStatus = HostInfoStatusEnum.DISALLOW_YANDEX.equals(status) ? HostInfoStatusEnum.DISALLOW : status;

        checkStatus(status, status, none, penaltyHtarcFalse, false, penaltyPrevFalse, false, none);
        checkStatus(status, status, none, penaltyHtarcFalse, false, penaltyPrevFalse, true, none);
        checkStatus(status, status, none, penaltyHtarcFalse, false, penaltyPrevTrue, false, none);
        checkStatus(status, status, none, penaltyHtarcFalse, false, penaltyPrevTrue, true, warn(outStatus));

        checkStatus(status, status, none, penaltyHtarcFalse, true, penaltyPrevFalse, false, none);
        checkStatus(status, status, none, penaltyHtarcFalse, true, penaltyPrevFalse, true, none);
        checkStatus(status, status, none, penaltyHtarcFalse, true, penaltyPrevTrue, false, none);
        checkStatus(status, status, none, penaltyHtarcFalse, true, penaltyPrevTrue, true, warn(outStatus));

        checkStatus(status, status, none, penaltyHtarcTrue, false, penaltyPrevFalse, false, none);
        checkStatus(status, status, none, penaltyHtarcTrue, false, penaltyPrevFalse, true, none);
        checkStatus(status, status, none, penaltyHtarcTrue, false, penaltyPrevTrue, false, none);
        checkStatus(status, status, none, penaltyHtarcTrue, false, penaltyPrevTrue, true, outStatus);

        checkStatus(status, status, none, penaltyHtarcTrue, true, penaltyPrevFalse, false, outStatus);
        checkStatus(status, status, none, penaltyHtarcTrue, true, penaltyPrevFalse, true, will(outStatus));
        checkStatus(status, status, none, penaltyHtarcTrue, true, penaltyPrevTrue, false, outStatus);
        checkStatus(status, status, none, penaltyHtarcTrue, true, penaltyPrevTrue, true, outStatus);
    }

    @Test
    public void testGetCalculatedHostInfoStatusCF1() {
        final int PH_TRUE = 10;
        final int PH_FALSE = 9;
        final int PP_TRUE = 6;
        final int PP_FALSE = 5;
        checkCalculatedStatus(HostInfoStatusEnum.CONNECTION_FAILED, NONE, PH_TRUE, PH_FALSE, PP_TRUE, PP_FALSE);
    }

    @Test
    public void testGetCalculatedHostInfoStatusCF2() {
        final int PH_TRUE = 70;
        final int PH_FALSE = 5;
        final int PP_TRUE = 10;
        final int PP_FALSE = 1;
        checkCalculatedStatus(HostInfoStatusEnum.CONNECTION_FAILED, HostInfoStatusEnum.WAITING, PH_TRUE, PH_FALSE, PP_TRUE, PP_FALSE);
    }

    @Test
    public void testGetCalculatedHostInfoStatusCF3() {
        final int PH_TRUE = 70;
        final Integer PH_FALSE = null;
        final int PP_TRUE = 8;
        final Integer PP_FALSE = null;
        checkCalculatedStatus(HostInfoStatusEnum.CONNECTION_FAILED, HostInfoStatusEnum.FINISHED, PH_TRUE, PH_FALSE, PP_TRUE, PP_FALSE);
    }

    @Test
    public void testGetCalculatedHostInfoStatusCF4() {
        final int PH_TRUE = 10;
        final Integer PH_FALSE = null;
        final int PP_TRUE = 6;
        final Integer PP_FALSE = null;
        checkCalculatedStatus(HostInfoStatusEnum.CONNECTION_FAILED, HostInfoStatusEnum.ROBOTS_TXT, PH_TRUE, PH_FALSE, PP_TRUE, PP_FALSE);
    }

    @Test
    public void testGetCalculatedHostInfoStatusDNS1() {
        final int PH_TRUE = 10;
        final int PH_FALSE = 9;
        final int PP_TRUE = 6;
        final int PP_FALSE = 5;
        checkCalculatedStatus(HostInfoStatusEnum.DNS_ERROR, NONE, PH_TRUE, PH_FALSE, PP_TRUE, PP_FALSE);
    }

    @Test
    public void testGetCalculatedHostInfoStatusDNS2() {
        final int PH_TRUE = 10;
        final int PH_FALSE = 9;
        final int PP_TRUE = 6;
        final Integer PP_FALSE = null;
        checkCalculatedStatus(HostInfoStatusEnum.DNS_ERROR, NONE, PH_TRUE, PH_FALSE, PP_TRUE, PP_FALSE);
    }

    @Test
    public void testGetCalculatedHostInfoStatusDISALLOW1() {
        final int PH_TRUE = 6;
        final int PH_FALSE = 5;
        final int PP_TRUE = 2;
        final int PP_FALSE = 1;
        checkCalculatedStatus(HostInfoStatusEnum.DISALLOW, HostInfoStatusEnum.FINISHED, PH_TRUE, PH_FALSE, PP_TRUE, PP_FALSE);
    }

    @Test
    public void testGetCalculatedHostInfoStatusDISALLOW2() {
        final int PH_TRUE = 6;
        final int PH_FALSE = 5;
        final int PP_TRUE = 10;
        final int PP_FALSE = 1;
        checkCalculatedStatus(HostInfoStatusEnum.DISALLOW, HostInfoStatusEnum.FINISHED, PH_TRUE, PH_FALSE, PP_TRUE, PP_FALSE);
    }

    @Test
    public void testGetCalcualtedStatusDISALLOW_YANDEX1() {
        final int PH_TRUE = 6;
        final int PH_FALSE = 5;
        final int PP_TRUE = 10;
        final int PP_FALSE = 1;
        checkCalculatedStatus(HostInfoStatusEnum.DISALLOW_YANDEX, HostInfoStatusEnum.FINISHED, PH_TRUE, PH_FALSE, PP_TRUE, PP_FALSE);
    }

    @Test
    public void testGetCalcualtedStatusDISALLOW_YANDEX2() {
        final int PH_TRUE = 6;
        final int PH_FALSE = 5;
        final int PP_TRUE = 10;
        final int PP_FALSE = 1;
        checkCalculatedStatus(HostInfoStatusEnum.DISALLOW_YANDEX, HostInfoStatusEnum.INTERNAL_ERROR, PH_TRUE, PH_FALSE, PP_TRUE, PP_FALSE);
    }

    @Test
    public void testGetCalcualtedStatusDISALLOW_YANDEX3() {
        final int PH_TRUE = 6;
        final int PH_FALSE = 5;
        final int PP_TRUE = 10;
        final int PP_FALSE = 1;
        checkCalculatedStatus(HostInfoStatusEnum.DISALLOW_YANDEX, NONE, PH_TRUE, PH_FALSE, PP_TRUE, PP_FALSE);
    }

    private static HostInfoStatusEnum will(HostInfoStatusEnum s) {
        switch (s) {
            case CONNECTION_FAILED: return HostInfoStatusEnum.WILL_CONNECTION_FAILED;
            case DNS_ERROR: return HostInfoStatusEnum.WILL_DNS_ERROR;
            case DISALLOW: return HostInfoStatusEnum.WILL_DISALLOW;
            case DISALLOW_YANDEX: return HostInfoStatusEnum.WILL_DISALLOW;
            default: return s;
        }
    }

    private static HostInfoStatusEnum warn(HostInfoStatusEnum s) {
        switch (s) {
            case CONNECTION_FAILED: return HostInfoStatusEnum.WARN_CONNECTION_FAILED;
            case DNS_ERROR: return HostInfoStatusEnum.WARN_DNS_ERROR;
            case DISALLOW: return HostInfoStatusEnum.WARN_DISALLOW;
            case DISALLOW_YANDEX: return HostInfoStatusEnum.WARN_DISALLOW;
            default: return s;
        }
    }
}
