package ru.yandex.wmconsole.debug;

import java.util.Date;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.wmconsole.data.VerificationStateEnum;
import ru.yandex.wmconsole.data.info.UsersHostsInfo;
import ru.yandex.wmconsole.service.HostVisitorServiceForDebug;
import ru.yandex.wmconsole.verification.VerificationTypeEnum;
import ru.yandex.wmtools.common.error.InternalException;

/**
 * This class is created for debug purposes.
 */
public class VerificationDebug {
    private static final Logger log = LoggerFactory.getLogger(VerificationDebug.class);

    private final HostVisitorServiceForDebug hostVisitorService = new HostVisitorServiceForDebug();

    @Test
    public void testVerificationTxtFile() throws InternalException {

//        http://www.chess-online.ru/yandex_42250a0309dda0f4.txt
        long verificationUin = 4766226788808237300L;
        VerificationTypeEnum verificationTypeEnum = VerificationTypeEnum.TXT_FILE;
        long hostId = 288;
        String hostName = "www.chess-online.ru";

        // run
        UsersHostsInfo verInfo = new UsersHostsInfo(VerificationStateEnum.NEVER_VERIFIED,
                verificationUin, verificationTypeEnum, new Date(), "", 0, hostId, hostName, null, null);
        UsersHostsInfo res = hostVisitorService.checkVerificationForDebug(verInfo);

        log.info("RESULT: " + res.getVerificationState().toString());
    }

    @Test
    public void testVerificationMetaTag() throws InternalException {

//        http://www.chess-online.ru/yandex_42250a0309dda0f4.txt
        long verificationUin = 4700419342738259597L;
        VerificationTypeEnum verificationTypeEnum = VerificationTypeEnum.META_TAG;
        long hostId = 249;
        String hostName = "www.moda-online.ru";

        // run
        UsersHostsInfo verInfo = new UsersHostsInfo(VerificationStateEnum.NEVER_VERIFIED,
                verificationUin, verificationTypeEnum, new Date(), "", 0, hostId, hostName, null, null);
        UsersHostsInfo res = hostVisitorService.checkVerificationForDebug(verInfo);

        log.info("RESULT: " + res.getVerificationState().toString());
    }

}
